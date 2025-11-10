package com.neptune.neptune.data

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.yalantis.ucrop.UCrop
import java.io.File

/**
 * A reusable Composable that prepares and returns an [ActivityResultLauncher] configured to pick an
 * image from the gallery and then crop it using uCrop.
 *
 * This function encapsulates the entire pick-and-crop flow.
 *
 * @param onImageCropped A callback lambda that is invoked with the [Uri] of the successfully
 *   cropped image, or null if an error occurred or the user canceled.
 * @return An [ActivityResultLauncher] to launch the image selection process.
 *
 * This function was made using AI assistance
 */
@Composable
fun rememberImagePickerLauncher(
    onImageCropped: (Uri?) -> Unit,
    aspectRatioX: Float = 1f,
    aspectRatioY: Float = 1f,
    circleDimmedLayer: Boolean = true
): ActivityResultLauncher<String> {

  val context = LocalContext.current

  // A stable Uri for uCrop's output, using a constant file name to avoid cache buildup.
  val uCropDestinationUri = remember {
    Uri.fromFile(File(context.cacheDir, "ucrop_temp_output.jpg"))
  }

  // This handles the result *after* the image has been cropped.
  val uCropLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
              val resultUri = UCrop.getOutput(result.data!!)

              if (resultUri != null) {
                // Append a timestamp to the URI to act as a cache buster.
                // This forces to reload the image from disk.
                val cacheBustedUri =
                    resultUri
                        .buildUpon()
                        .appendQueryParameter("t", System.currentTimeMillis().toString())
                        .build()
                onImageCropped(cacheBustedUri)
              } else {
                onImageCropped(null)
              }
            } else {
              onImageCropped(null)
            }
          }

  // This is the main launcher that we'll return. It starts the process.
  val imagePickerLauncher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
          sourceUri: Uri? ->
        if (sourceUri != null) {

          val options =
              UCrop.Options().apply {
                setCircleDimmedLayer(circleDimmedLayer)
                setShowCropGrid(false)
                setCompressionFormat(Bitmap.CompressFormat.JPEG)
                setCompressionQuality(90)
              }

          val uCropIntent =
              UCrop.of(sourceUri, uCropDestinationUri)
                  .withAspectRatio(aspectRatioX, aspectRatioY)
                  .withOptions(options)
                  .getIntent(context)

          uCropLauncher.launch(uCropIntent)
        } else {
          // User canceled
          onImageCropped(null)
        }
      }

  return imagePickerLauncher
}
