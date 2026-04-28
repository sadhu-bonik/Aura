const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

/**
 * Triggered when a new notification document is written to `notifications/{notifId}`.
 * Reads the recipient's FCM token from `users/{recipientId}` and sends a push notification.
 *
 * Deploy: firebase deploy --only functions
 * Requires: firebase-functions v2, firebase-admin
 */
exports.sendPushOnNotification = onDocumentCreated(
  "notifications/{notifId}",
  async (event) => {
    const notif = event.data?.data();
    if (!notif) return;

    const { recipientId, actorName, message, type, dealId, notifId } = notif;
    if (!recipientId || !message) return;

    // Fetch the recipient's FCM token
    const userDoc = await getFirestore().collection("users").doc(recipientId).get();
    const fcmToken = userDoc.data()?.fcmToken;
    if (!fcmToken) return;

    const title = actorName?.trim() || "Aura";

    try {
      await getMessaging().send({
        token: fcmToken,
        notification: { title, body: message },
        data: {
          notifId: event.params.notifId ?? "",
          type: type ?? "",
          dealId: dealId ?? "",
          actorName: actorName ?? "",
          message: message ?? "",
        },
        android: {
          priority: "high",
          notification: { channelId: "aura_notifications" },
        },
        apns: {
          payload: { aps: { sound: "default" } },
        },
      });
    } catch (err) {
      console.error("FCM send failed for", recipientId, err?.message);
    }
  }
);
