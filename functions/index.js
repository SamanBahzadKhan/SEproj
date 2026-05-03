const admin = require('firebase-admin');

admin.initializeApp();

const meetLink = require('./src/meetLink');
const createCounsellorAccount = require('./src/createCounsellorAccount');

exports.generateMeetLink = meetLink.generateMeetLink;
exports.createCounsellorAccount = createCounsellorAccount.createCounsellorAccount;
