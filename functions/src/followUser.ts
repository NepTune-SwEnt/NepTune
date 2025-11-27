import {onCall, HttpsError} from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();

export const followUser = onCall({region: "us-east1"},
  async (request): Promise<{success: boolean}> => {
    const currentUid = request.auth?.uid;
    if (!currentUid) {
      throw new HttpsError("unauthenticated", "Must be signed in");
    }

    const targetUid = request.data.targetUid as string | undefined;
    const follow = request.data.follow as boolean | undefined;

    if (!targetUid || typeof follow !== "boolean") {
      throw new HttpsError(
        "invalid-argument",
        "Expected { targetUid: string, follow: boolean }",
      );
    }

    if (currentUid === targetUid) {
      throw new HttpsError(
        "failed-precondition",
        "Cannot follow/unfollow oneself",
      );
    }

    const profiles = db.collection("profiles");
    const currentRef = profiles.doc(currentUid);
    const otherRef = profiles.doc(targetUid);

    await db.runTransaction(async (tx) => {
      const currentSnap = await tx.get(currentRef);
      const otherSnap = await tx.get(otherRef);

      if (!currentSnap.exists || !otherSnap.exists) {
        throw new HttpsError("not-found", "Profile document not found");
      }

      const dataCurrent = currentSnap.data() || {};
      const dataOther = otherSnap.data() || {};

      const currentFollowing =
        (dataCurrent.following as string[] | undefined) ?? [];
      const otherSubscribers =
        (dataOther.subscribers as number | undefined) ?? 0;
      const currentSubscriptions =
        (dataCurrent.subscriptions as number | undefined) ?? 0;

      if (follow) {
        // Follow user
        if (!currentFollowing.includes(targetUid)) {
          tx.update(currentRef, {
            following: admin.firestore.FieldValue.arrayUnion(targetUid),
            subscriptions: currentSubscriptions + 1,
          });
          tx.update(otherRef, {
            subscribers: otherSubscribers + 1,
          });
        }
      } else {
        // Unfollow user
        if (currentFollowing.includes(targetUid)) {
          tx.update(currentRef, {
            following: admin.firestore.FieldValue.arrayRemove(targetUid),
            subscriptions: Math.max(0, currentSubscriptions - 1),
          });
          tx.update(otherRef, {
            subscribers: Math.max(0, otherSubscribers - 1),
          });
        }
      }
    });

    return {success: true};
  },
);
