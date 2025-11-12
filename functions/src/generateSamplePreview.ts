/* This file was implemented with ChatGPT's help */
import {tmpdir} from "os";
import {join} from "path";
import {mkdir, writeFile, rm, copyFile} from "fs/promises";
import * as logger from "firebase-functions/logger";
import {onObjectFinalized} from "firebase-functions/v2/storage";
import {getFirestore} from "firebase-admin/firestore";
import {getStorage} from "firebase-admin/storage";
import {initializeApp} from "firebase-admin/app";
import AdmZip from "adm-zip";
import ffmpeg from "fluent-ffmpeg";
import ffmpegStatic from "ffmpeg-static";

initializeApp();

if (typeof ffmpegStatic === "string") {
  ffmpeg.setFfmpegPath(ffmpegStatic);
} else {
  logger.warn("ffmpeg-static path not found; using system ffmpeg");
}


export const generateSamplePreview = onObjectFinalized(
  {timeoutSeconds: 540, memory: "1GiB"},
  async (event) => {
    const objectPath = event.data?.name ?? "";
    if (!objectPath.endsWith(".zip") ||
      objectPath.startsWith("sample_previews/")) return;

    const tmpRoot = join(tmpdir(), `preview-${Date.now()}`);
    await mkdir(tmpRoot, {recursive: true});
    const sourceZipPath = join(tmpRoot, "source.zip");
    const bucket = getStorage().bucket(event.data.bucket);
    await bucket.file(objectPath).download({destination: sourceZipPath});

    const zip = new AdmZip(sourceZipPath);
    const entries = zip.getEntries();
    const audioEntry = entries.find((entry: AdmZip.IZipEntry) =>
      /\.(wav|mp3)$/i.test(entry.entryName)
    );

    const metadataEntry = entries.find((entry: AdmZip.IZipEntry) =>
      entry.entryName.endsWith(".json")
    );

    if (!audioEntry || !metadataEntry) {
      logger.warn("Zip missing audio or metadata", {objectPath});
      await rm(tmpRoot, {recursive: true, force: true});
      return;
    }

    const audioSrc = join(tmpRoot, audioEntry.entryName);
    await writeFile(audioSrc, audioEntry.getData());
    const previewAudio = join(tmpRoot, "preview.mp3");

    if (audioEntry.entryName.toLowerCase().endsWith(".wav")) {
      await new Promise<void>((resolve, reject) => {
        ffmpeg(audioSrc)
          .audioBitrate("128k")
          .format("mp3")
          .save(previewAudio)
          .on("end", () => resolve())
          .on("error", reject);
      });
    } else {
      await copyFile(audioSrc, previewAudio);
    }

    const previewPath = objectPath
      .replace(/^samples\//, "sample_previews/")
      .replace(/\.zip$/, ".mp3");
    await bucket.upload(previewAudio, {
      destination: previewPath,
      contentType: "audio/mpeg",
    });

    const sampleId = metadataEntry.entryName.replace(".json", "");
    await getFirestore().collection("samples").doc(sampleId).update({
      storagePreviewSamplePath: previewPath,
    });

    await rm(tmpRoot, {recursive: true, force: true});
    logger.info("Preview generated", {sampleId, previewPath});
  }

);
