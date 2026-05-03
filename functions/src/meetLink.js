/*
MANUAL SETUP STEPS (one time only):
1. Cloud project: Enable "Google Calendar API" (APIs & Services → Library).
2. Service account: IAM → Service Accounts → create → download JSON key as functions/service-account-key.json
3. MEET LINKS OFTEN FAIL ON THE SERVICE ACCOUNT'S OWN "primary" CALENDAR — Google may not allow
   hangoutsMeet there. Fix ONE of:
   A) WORKSPACE (recommended): Enable Domain-wide delegation on the service account; in Admin Console
      authorize the Calendar scope; then set env GOOGLE_CALENDAR_IMPERSONATE (or firebase config
      meet.impersonate) to a Workspace user email. Events are created as that user — Meet works.
   B) SHARED CALENDAR: In Google Calendar (browser), open Settings → your calendar → Share with
      specific people → add the service account email (...@....iam.gserviceaccount.com) with
      "Make changes to events". Set MEET_CALENDAR_ID (or meet.calendar_id) to that calendar's ID
      (often your Gmail address for the primary calendar).
4. Deploy: firebase deploy --only functions
5. Optional env (same GCP project / Functions runtime):
   MEET_CALENDAR_ID=user@gmail.com
   GOOGLE_CALENDAR_IMPERSONATE=admin@yourworkspace.com
*/

const path = require('path');
const { google } = require('googleapis');
const { JWT } = require('google-auth-library');
const functions = require('firebase-functions');
const admin = require('firebase-admin');

let serviceAccount;
try {
  serviceAccount = require(path.join(__dirname, '..', 'service-account-key.json'));
} catch (e) {
  serviceAccount = null;
}

/** Workspace delegation OR plain SA auth for Calendar. */
async function getCalendarAuth() {
  const key = serviceAccount;
  if (!key) {
    return null;
  }
  const impersonate =
    process.env.GOOGLE_CALENDAR_IMPERSONATE ||
    (functions.config().meet && functions.config().meet.impersonate);
  if (impersonate) {
    return new JWT({
      email: key.client_email,
      key: key.private_key,
      scopes: ['https://www.googleapis.com/auth/calendar'],
      subject: impersonate,
    });
  }
  const ga = new google.auth.GoogleAuth({
    credentials: key,
    scopes: ['https://www.googleapis.com/auth/calendar'],
  });
  return ga.getClient();
}

function resolveCalendarId() {
  return (
    process.env.MEET_CALENDAR_ID ||
    (functions.config().meet && functions.config().meet.calendar_id) ||
    'primary'
  );
}

function extractMeetLink(eventData) {
  if (!eventData) {
    return null;
  }
  if (eventData.hangoutLink) {
    return eventData.hangoutLink;
  }
  const eps =
    eventData.conferenceData && eventData.conferenceData.entryPoints;
  if (eps && eps.length > 0) {
    const video = eps.find((p) => p.entryPointType === 'video') || eps[0];
    return video.uri || null;
  }
  return null;
}

function formatCalendarError(err) {
  if (!err) {
    return 'Unknown error';
  }
  const base = err.message || String(err);
  const apiErr = err.response && err.response.data && err.response.data.error;
  if (!apiErr) {
    return base;
  }
  let s = apiErr.message || base;
  if (apiErr.errors && apiErr.errors.length > 0) {
    s +=
      ' [' +
      apiErr.errors.map((e) => `${e.reason}: ${e.message}`).join('; ') +
      ']';
  }
  return s;
}

function buildEventPayload(data, withAttendees) {
  const {
    studentEmail,
    studentName,
    counsellorEmail,
    counsellorName,
    startTime,
    endTime,
  } = data || {};
  const requestId = `${(data && data.appointmentId) || 'session'}-${Date.now()}`;
  const descParts = [
    `CAPs online counselling session.`,
    studentName ? `Student: ${studentName}` : '',
    studentEmail ? `<${studentEmail}>` : '',
    counsellorName ? `Counsellor: ${counsellorName}` : '',
    counsellorEmail ? `<${counsellorEmail}>` : '',
  ].filter(Boolean);
  const event = {
    summary: 'CAPs Counselling Session',
    description: descParts.join(' — '),
    start: {
      dateTime: startTime,
      timeZone: 'Asia/Karachi',
    },
    end: {
      dateTime: endTime,
      timeZone: 'Asia/Karachi',
    },
    conferenceData: {
      createRequest: {
        requestId,
        conferenceSolutionKey: { type: 'hangoutsMeet' },
      },
    },
  };
  if (withAttendees && (studentEmail || counsellorEmail)) {
    event.attendees = [{ email: studentEmail }, { email: counsellorEmail }].filter(
      (a) => a.email
    );
  }
  return event;
}

exports.generateMeetLink = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Sign in required.');
  }

  if (!serviceAccount) {
    throw new functions.https.HttpsError(
      'failed-precondition',
      'Add functions/service-account-key.json (Google service account JSON). See comment block in meetLink.js.'
    );
  }

  const {
    appointmentId,
    startTime,
    endTime,
    studentEmail,
    studentId,
    studentName,
    counsellorEmail,
    counsellorId,
    counsellorName,
  } = data || {};

  if (!appointmentId || !startTime || !endTime) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'appointmentId, startTime, and endTime are required.'
    );
  }
  if (!counsellorId || context.auth.uid !== counsellorId) {
    throw new functions.https.HttpsError('permission-denied', 'Invalid counsellor.');
  }

  const auth = await getCalendarAuth();
  const calendar = google.calendar({ version: 'v3', auth });
  const calendarId = resolveCalendarId();

  let response;

  // Prefer insert WITHOUT attendees first — avoids many service-account + Calendar bugs; Meet link works for everyone anyway.
  try {
    response = await calendar.events.insert({
      calendarId,
      requestBody: buildEventPayload(data, false),
      conferenceDataVersion: 1,
      sendUpdates: 'none',
    });
  } catch (e1) {
    console.warn('Calendar insert (no attendees) failed:', formatCalendarError(e1));
    try {
      response = await calendar.events.insert({
        calendarId,
        requestBody: buildEventPayload(data, true),
        conferenceDataVersion: 1,
        sendUpdates: 'none',
      });
    } catch (e2) {
      console.error('Calendar insert (with attendees) failed:', formatCalendarError(e2));
      throw new functions.https.HttpsError(
        'internal',
        formatCalendarError(e2) +
          ' Meet requires a calendar that allows Google Meet: share your calendar with the service account and set MEET_CALENDAR_ID, or use GOOGLE_CALENDAR_IMPERSONATE with Workspace delegation. See functions/src/meetLink.js header.'
      );
    }
  }

  const meetLink = extractMeetLink(response.data);
  const calendarEventId = response.data.id;

  if (!meetLink) {
    throw new functions.https.HttpsError(
      'internal',
      'Calendar accepted the event but returned no Meet link. Configure MEET_CALENDAR_ID or GOOGLE_CALENDAR_IMPERSONATE per meetLink.js.'
    );
  }

  const slotUpdate = {
    meetLink,
    meetLinkGeneratedAt: admin.firestore.FieldValue.serverTimestamp(),
  };
  if (calendarEventId) {
    slotUpdate.calendarEventId = calendarEventId;
  }

  try {
    await admin.firestore().collection('timeslots').doc(appointmentId).update(slotUpdate);
  } catch (fsErr) {
    console.error('Firestore timeslots update failed after Meet created:', fsErr);
    throw new functions.https.HttpsError(
      'internal',
      'Meet link was created but saving it failed: ' + (fsErr.message || fsErr)
    );
  }

  const ts = Date.now();
  try {
    const batch = admin.firestore().batch();
    if (studentId) {
      const n1 = admin.firestore().collection('notifications').doc();
      batch.set(n1, {
        recipientId: studentId,
        title: 'Your Meeting Link is Ready',
        message:
          'Your online session with ' +
          (counsellorName || 'your counsellor') +
          ' is confirmed. Tap to join.',
        type: 'meet_link',
        meetLink,
        appointmentId,
        timestamp: ts,
        read: false,
        isRead: false,
      });
    }
    if (counsellorId) {
      const n2 = admin.firestore().collection('notifications').doc();
      batch.set(n2, {
        recipientId: counsellorId,
        title: 'Session Confirmed',
        message:
          'Online session with ' +
          (studentName || 'your student') +
          ' confirmed. Google Meet link generated.',
        type: 'meet_link',
        meetLink,
        appointmentId,
        timestamp: ts,
        read: false,
        isRead: false,
      });
    }
    await batch.commit();
  } catch (noteErr) {
    console.error('Notification batch failed (Meet link still saved on slot):', noteErr);
  }

  return { meetLink, calendarEventId };
});
