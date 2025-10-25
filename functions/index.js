const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.sendNotificationOnRequestCreated = functions.firestore
    .document('locationRequests/{requestId}')
    .onCreate(async (snap, context) => {
        const requestData = snap.data();
        const receiverId = requestData.receiverId;
        const senderId = requestData.senderId;

        // Get receiver's FCM token
        const receiverDoc = await admin.firestore().collection('users').doc(receiverId).get();
        const receiverFCMToken = receiverDoc.data().fcmToken;

        if (receiverFCMToken) {
            const senderDoc = await admin.firestore().collection('users').doc(senderId).get();
            const senderName = senderDoc.data().name || 'Unknown User';

            const payload = {
                notification: {
                    title: 'New Location Request',
                    body: `${senderName} has requested your location`,
                },
                data: {
                    requestId: context.params.requestId,
                    type: 'location_request',
                    openTab: 'requests'
                },
            };

            try {
                await admin.messaging().sendToDevice(receiverFCMToken, payload);
                console.log('Notification sent to receiver:', receiverId);
            } catch (error) {
                console.error('Error sending notification:', error);
            }
        }
        return null;
    });

exports.sendNotificationOnRequestApproval = functions.firestore
    .document('locationRequests/{requestId}')
    .onUpdate(async (change, context) => {
        const newValue = change.after.data();
        const previousValue = change.before.data();

        // Check if the request status changed to 'approved'
        if (newValue.status === 'approved' && previousValue.status !== 'approved') {
            const senderId = newValue.senderId;
            const receiverId = newValue.receiverId;

            // Get the sender's FCM token
            const senderDoc = await admin.firestore().collection('users').doc(senderId).get();
            const senderFCMToken = senderDoc.data().fcmToken;

            if (senderFCMToken) {
                const payload = {
                    notification: {
                        title: 'Location Request Approved!',
                        body: `Your location request to ${newValue.receiverName} has been approved.`,
                    },
                    data: {
                        // You can add custom data here if needed
                        requestId: context.params.requestId,
                        type: 'location_request_approved',
                    },
                };

                try {
                    await admin.messaging().sendToDevice(senderFCMToken, payload);
                    console.log('Notification successfully sent to sender:', senderId);
                } catch (error) {
                    console.error('Error sending notification to sender:', senderId, error);
                }
            }
        }
        return null;
    });