/* This file was implemented with ChatGPT's help */
import {onDocumentCreated} from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";

if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();

export const onMessageCreated = onDocumentCreated(
  {
    document: "messages/{conversationId}/messages/{messageId}",
    region: "us-east1",
  },
  async (event) => {
    const message = event.data?.data();
    if (!message) return;

    const conversationId = event.params.conversationId;
    const convRef = db.collection("messages").doc(conversationId);

    const participants = conversationId.split("_");

    await convRef.set(
      {
        participants,
        lastMessage: message.text ?? "",
        lastTimestamp: admin.firestore.Timestamp.now(),
      },
      {merge: true}
    );
  }
);
