const functions = require('firebase-functions');
const admin = require('firebase-admin');

/**
 * Admin-only: create Firebase Auth user + counsellor profile (email pre-verified).
 */
exports.createCounsellorAccount = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Sign in required.');
  }

  const adminDoc = await admin
    .firestore()
    .collection('admins')
    .doc(context.auth.uid)
    .get();
  if (!adminDoc.exists) {
    throw new functions.https.HttpsError('permission-denied', 'Not admin');
  }

  const name = data && data.name;
  const email = data && data.email;
  const password = data && data.password;
  const specialization = (data && data.specialization) || '';
  const department = (data && data.department) || '';
  const phone = (data && data.phone) || '';
  const acceptingClients =
    data && data.acceptingClients !== undefined ? !!data.acceptingClients : true;

  if (!name || !email || !password) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'name, email, and password are required.'
    );
  }

  let userRecord;
  try {
    userRecord = await admin.auth().createUser({
      email: String(email).trim(),
      password: String(password),
      displayName: name,
      emailVerified: true,
    });
  } catch (e) {
    console.error('createUser', e);
    throw new functions.https.HttpsError('internal', e.message || 'Could not create user');
  }

  const d = new Date();
  const pad = (n) => (n < 10 ? '0' : '') + n;
  const createdAt =
    d.getFullYear() +
    '-' +
    pad(d.getMonth() + 1) +
    '-' +
    pad(d.getDate()) +
    'T' +
    pad(d.getHours()) +
    ':' +
    pad(d.getMinutes()) +
    ':' +
    pad(d.getSeconds());

  await admin
    .firestore()
    .collection('counselors')
    .doc(userRecord.uid)
    .set({
      name,
      email: String(email).trim(),
      role: 'COUNSELOR',
      specialization,
      department,
      phone,
      bio: '',
      isAcceptingClients: acceptingClients,
      isActive: true,
      isDeleted: false,
      rating: 0,
      ratingCount: 0,
      createdAt,
      createdByAdmin: true,
    });

  return { success: true, uid: userRecord.uid };
});
