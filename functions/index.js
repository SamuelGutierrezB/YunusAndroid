// Import necessary modules from Firebase
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const bank = "gIzw5hmt9hALqPy6hER9";

// Initialize the Firebase Admin SDK to access Firebase services
admin.initializeApp();

// Firestore trigger to run when a document in 'users' collection is written
exports.updateUserDisplayName = functions.firestore
  .document("users/{userId}")
  .onWrite(async (change, context) => {
    // Retrieve the new data after the update
    const newData = change.after.data();
    // Retrieve the previous data before the update
    const previousData = change.before.data();
    // Extract userId from the document path parameters
    const userId = context.params.userId;

    if (!newData) {
      // No action needed on delete
      console.log(`Document with UID: ${userId} was deleted.`);
      return;
    }

    // Check if 'firstName' or 'lastName' fields were changed
    const firstNameChanged = newData.firstName !== previousData?.firstName;
    const lastNameChanged = newData.lastName !== previousData?.lastName;

    // If either name has changed, update the user's displayName in FirebaseAuth
    if (firstNameChanged || lastNameChanged) {
      const newDisplayName = `${newData.firstName} ${newData.lastName}`.trim();
      console.log(`Display name: ${newDisplayName}`);

      try {
        // Update the user's displayName using Firebase Admin SDK
        await admin.auth().updateUser(userId, { displayName: newDisplayName });
        console.log(`Successfully updated displayName for UID: ${userId}`);
      } catch (error) {
        // Log any errors encountered during the update
        console.error(`Error updating displayName for UID: ${userId}`, error);
      }
    }
  });

// Firestore trigger to run when a document in 'users' collection is updated
// exports.calculateYunusOnUpdate = functions.firestore
//   .document("users/{userId}")
//   .onUpdate(async (change, context) => {
//     // Get old document and new document
//     const newValue = change.after.data();
//     const oldValue = change.before.data();

//     // Get both last login
//     const lastLoginDate = oldValue.lastLogin.toDate();
//     const currentTimeDate = newValue.lastLogin.toDate();

//     // Remove hours, minutes, seconds and milliseconds
//     lastLoginDate.setHours(0, 0, 0, 0);
//     currentTimeDate.setHours(0, 0, 0, 0);

//     // Get diff time and diff days
//     const diffTime = currentTimeDate - lastLoginDate;
//     const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));

//     if (diffDays > 0) {
//       // Calculate yunus to add and to remove
//       const yunusToAdd = diffDays * 200;
//       let yunus = newValue.yunus;
//       let contributionCup = 0;

//       for (let i = 0; i < diffDays; i++) {
//         contributionCup += yunus * 0.05;
//         yunus -= yunus * 0.05;
//         yunus += 200;
//       }

//       // Connect to Firebase Firestore
//       const db = admin.firestore();

//       // Update yunus field of the user document
//       await change.after.ref.update({
//         yunus: yunus,
//       });

//       // Update yunus field of the bank document
//       const bankRef = db.collection("users").doc(bank);
//       const yunusBank = contributionCup - yunusToAdd;
//       return bankRef.update({
//         yunus: admin.firestore.FieldValue.increment(yunusBank),
//       });
//     } else {
//       // There are no days diff, do nothing
//       return null;
//     }
//   });

// Firestore trigger to the delete user on FirebaseAuth too
exports.deleteUserFromAuth = functions.firestore
  .document("users/{userId}")
  .onDelete(async (snap, context) => {
    const userId = context.params.userId;
    const userData = snap.data();

    try {
      // Transfer Yunus to the bank
      const db = admin.firestore();
      const bankRef = db.collection("users").doc(bank);
      const yunusAmount = userData.yunus || 0;

      await bankRef.update({
        yunus: admin.firestore.FieldValue.increment(yunusAmount),
      });

      console.log(
        `Transferred ${yunusAmount} Yunus from UID: ${userId} to bank.`
      );

      // Delete user from Firebase Authentication
      await admin.auth().deleteUser(userId);
      console.log(
        `Usuario con UID: ${userId} eliminado exitosamente de FirebaseAuth.`
      );
    } catch (error) {
      console.error(`Error al eliminar el usuario con UID: ${userId}`, error);
    }
  });
