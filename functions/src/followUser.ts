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
    const currentFollowingRef = currentRef.collection("following")
      .doc(targetUid);
    const targetFollowersRef = otherRef.collection("followers")
      .doc(currentUid);

    await db.runTransaction(async (tx) => {
      const [currentSnap, otherSnap, followEdgeSnap] = await Promise.all([
        tx.get(currentRef),
        tx.get(otherRef),
        tx.get(currentFollowingRef),
      ]);

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

      const alreadyFollowing = followEdgeSnap.exists ||
        currentFollowing.includes(targetUid);
      const now = admin.firestore.FieldValue.serverTimestamp();

      if (follow) {
        // Follow user
        if (alreadyFollowing) return;

        tx.set(currentFollowingRef, {
          uid: targetUid,
          createdAt: now,
        }, {merge: true});
        tx.set(targetFollowersRef, {
          uid: currentUid,
          createdAt: now,
        }, {merge: true});

        tx.update(currentRef, {
          following: admin.firestore.FieldValue.arrayUnion(targetUid),
          subscriptions: currentSubscriptions + 1,
        });
        tx.update(otherRef, {
          subscribers: otherSubscribers + 1,
        });
      } else {
        // Unfollow user
        if (!alreadyFollowing) return;

        tx.delete(currentFollowingRef);
        tx.delete(targetFollowersRef);

        tx.update(currentRef, {
          following: admin.firestore.FieldValue.arrayRemove(targetUid),
          subscriptions: Math.max(0, currentSubscriptions - 1),
        });
        tx.update(otherRef, {
          subscribers: Math.max(0, otherSubscribers - 1),
        });
      }
    });

    return {success: true};
  },
);
