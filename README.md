# Counseling Appointment System

**Group:** Fridge | **Group Number:** 30 | **Course:** CS360

## Quick Links

- [Project Backlog](https://github.com/orgs/CS360S26fridge/projects/1/views/1?verticalGroupedBy%5BcolumnId%5D=Status)
- [Figma Wireframes](https://www.figma.com/design/4fzggh6GxJXPUFlywXCYmJ/FINAL-UI?node-id=0-1&p=f&t=LWpNH0ZXlM9t9m6A-0)
- [Figma Prototype](https://www.figma.com/proto/4fzggh6GxJXPUFlywXCYmJ/FINAL-UI?node-id=2-2&p=f&t=LWpNH0ZXlM9t9m6A-0&scaling=min-zoom&content-scaling=fixed&page-id=0%3A1&starting-point-node-id=2%3A2)
- [Storyboard](https://github.com/CS360S26fridge/fridge-project/blob/main/doc/storyboard.md)
- [Meeting Minutes & Sprint Documentation](https://github.com/CS360S26fridge/fridge-project/blob/main/PROJECT_DOCUMENTATION.md)

---

## Team Information

| Name | Roll Number | GitHub Username |
|------|-------------|-----------------|
| Saman Bahzad Khan | 27100111 | [@SamanBahzadKhan](https://github.com/SamanBahzadKhan) |
| Ishal Rahat | 27100335 | [@ishal27100335](https://github.com/ishal27100335) |
| Husnain Khattak | 27100351 | [@panda-wav](https://github.com/panda-wav) |
| Owais Amir | 27100429 | [@owais-amir-27](https://github.com/owais-amir-27) |
| Aurangzaib | 26100345 | [@Aurangzaib-gill](https://github.com/Aurangzaib-gill) |

**TA:** Muhammad Mustafa — Roll No. 26100038

---

## Project Overview

**Project Name:** Counseling Appointment System
**Course:** CS360
**Group Name:** Fridge
**Group Number:** 30

**Concept:** An appointment management platform that connects students with counseling services, enabling easy booking, scheduling, and session management.

**Primary Users:** Students, Counselors, Counseling Office Administrators

**Key Challenges:** Privacy and confidentiality, scheduling conflict management, reliable notification system, no-show prevention

---

## Repository Structure

```
project-repository/

README.md
PROJECT_DOCUMENTATION.md

docs/
   UMLFINAL.png
   storyboard.md
   team.txt

app/

crc-cards/
   c1.png
   c2.png

UI_Mockups/
   ptype.png
   mockups.png
```

---

## Repository Setup

**Visibility:** Private
**Collaborators with Read Access:**

| GitHub Account | Role |
|----------------|------|
| [@Negatrin](https://github.com/Negatrin) | Assigned TA |
| [@abdulali](https://github.com/abdulali) | Course Instructor |
| [@sulemanshahid](https://github.com/sulemanshahid) | Course Instructor |
| [@SafaSalam](https://github.com/SafaSalam) | Safa |

---

## Phase Deliverables

### Phase 1 — Core Features

Initial scope definition and project setup.

**User Stories:**
- Counselor discovery and availability viewing
- Time slot booking system
- Appointment reminders and notifications
- Rescheduling and cancellation functionality
- Counselor availability management dashboard

---

### Phase 2 — Design & Architecture

#### 1. Product Backlog

The product backlog contains a list of user stories describing the functional requirements of the system. Each user story includes a User Story ID, story description, story points (effort estimation), risk level (Low / Medium / High), and halfway release indicator.

The backlog is maintained using **GitHub Issues**: https://github.com/orgs/CS360S26fridge/projects/1

#### 2. CRC Cards

CRC (Class–Responsibility–Collaborator) cards identify the key classes in the system, their responsibilities, and the classes they interact with.

Location: `crc-cards/`

![CRC Cards Part 1](crc-cards/c1.png)
![CRC Cards Part 2](crc-cards/c2.png)

Classes identified: Student, Counselor, Appointment, Schedule, TimeSlot, Notification, Admin, Feedback, Authentication Service

#### 3. UI Mockups

Location: `UI_Mockups/`

![UI Mockups](UI_Mockups/mockups.png)

Screens included:
- Splash Screen
- Login Screen
- Student Sign Up Screen
- Admin Sign In Screen
- AI Assistant / Chatbot Screen
- Counselor Dashboard
- Counselor Profile Screen
- Student Dashboard
- Edit Profile Screen
- Appointment Booking Screen (Date & Time Slot Selection)
- Appointment Booking Screen (Appointment Type & Notes)
- Notifications Screen
- Student Profile Screen
- Feedback Screen (Appointment History)
- Leave Feedback Screen
- Counselor Appointments Screen
- Admin Panel (Overview, Reported Users, Manage Counselors)
- Student Journal Screen
- Session Notes Screen (Counselor view — Diagnosis, Prescription, Recommendations)
- Session Notes Screen (Student view — Uploaded Documents)

#### 4. Storyboards

Location: `docs/storyboard.md`

Scenarios covered:
1. Authentication / Login
2. Student Books Appointment
3. Student Cancels or Reschedules Appointment
4. Counselor Manages Availability

#### 5. UML Class Diagram

Location: `doc/UMLFINAL.png`

![Class Diagram](doc/UMLFINAL.png)

The system follows an MVC-inspired structure:

- **Views (Activities & Fragments)** — Handle user interaction and UI rendering (e.g., BookAppointmentActivity, StudentDashboardActivity)
- **Controllers** — Contain business logic and coordinate between UI and data layers (e.g., AppointmentController, NotificationController, AuthController)
- **Models** — Represent core data entities (e.g., Appointment, TimeSlot, Student, Counselor, Notification)
- **Workers** — Handle background tasks such as reminders (e.g., ReminderWorker)
- **Utilities** — Provide helper functions for common operations (e.g., DateUtils, NotificationUtils)

Firebase is used as the backend for storing application data.

---

### Phase 3 — Halfway Release

Following TA feedback, the UI was fully redesigned for improved usability and visual consistency. The halfway release prototype covered the following screens and functionality:

- **Splash Screen** — Entry point of the app displaying the CAPs branding before routing the user to login.
- **Login Screen** — Allows students and counselors to sign in using their credentials, with an option to navigate to student registration or admin login.
- **Student Sign Up Screen** — New students can register by providing their full name, email, password, phone number, department, and year of study.
- **Student Dashboard** — The central hub for students. Displays upcoming appointments with options to reschedule or cancel, past appointment history, and quick action buttons for booking a new appointment, accessing the journal, viewing notifications, and opening the chatbot.
- **Counselor Dashboard** — Shows the counselor their full list of upcoming appointments and provides an availability management interface where they can set which days and time slots they are open for bookings.
- **Appointment Booking Screen** — A multi-step booking flow where students select a counselor, pick a date, choose an available time slot, select the appointment type (in-person or online), and optionally add notes for the counselor before confirming.
- **Appointment Confirmation Screen** — Displays a summary of the booked appointment and confirms the booking was successful.
- **Appointment History Screen** — Lists all past appointments for the student, showing counselor name, date, appointment type, and status.
- **Notifications Screen** — Displays real-time and scheduled notifications for the student, including appointment confirmations, reminders, and cancellation alerts.
- **Student Profile Screen** — Shows the student's personal information, department, year of study, and account settings including notification preferences and privacy settings.
- **Counselor Profile Screen** — Displays the counselor's information, star rating based on student feedback, about section, and student reviews.
- **Edit Profile Screen** — Allows users to update their personal details including name, phone number, department, and year of study.
- **Admin Sign In Screen** — A separate login portal for administrators to access the admin dashboard.
- **Admin Dashboard** — At this stage, administrators could view platform-wide statistics including total appointments and active users.
- **Feedback Screen** — Displays the student's past appointments with an option to leave feedback on completed sessions.
- **Leave Feedback Screen** — Students can rate their session and write a comment about their experience with a counselor.
- **Counselor Appointments Screen** — Provides counselors with a dedicated view of all their appointments, filtered by upcoming and past, with appointment type and status indicators.

Sprint planning and review records are documented in `PROJECT_DOCUMENTATION.md`.

---

### Phase 4 — Final Prototype

The complete, release-ready application covering all planned requirements.

#### What Was Built

- **Full appointment booking flow** — Students can discover counselors, view availability, book, reschedule, and cancel appointments.
- **Counselor availability management** — Counselors can manage their schedules through a dedicated dashboard.
- **Notifications and reminders** — Appointment reminders are sent via a background worker.
- **No-show tracking and analytics** — Tracked in the admin dashboard.
- **Post-session feedback** — Students can leave feedback after a completed session.
- **Appointment history** — Available to both students and counselors.

#### Design Decisions

**Counselor onboarding via Admin:** Counselor accounts are created exclusively by administrators. Students register independently through the app, while counselors are added to the platform by the admin and assigned credentials. This ensures all counselors on the platform are verified and authorized before becoming visible to students.

**Scoped AI chatbot for counselor recommendation:** Rather than implementing a general-purpose open-ended AI assistant, the chatbot was deliberately scoped to a single function: recommending which counselor to book based on the student's described needs. This was an intentional design choice given the mental health context of the app — open-ended AI conversations on sensitive topics carry a risk of causing harm or providing inappropriate guidance. By limiting the chatbot to counselor recommendations only, the feature remains useful without overstepping into territory that should be handled by qualified professionals.

**Student daily journal:** Students have access to a private journaling feature within the app to log their thoughts and track their mental well-being between sessions. Journal entries are entirely private and not accessible to counselors or administrators.

**Structured session notes:** At the end of a session, counselors can create a structured session note that functions as a formal session record. This includes fields for diagnosis, prescribed medication (if applicable), recommendations and follow-up suggestions, and a counselor signature. The completed note is then made visible to the student on their dashboard, giving them an accessible record of their session.

**Report to Admin moderation system:** Both students and counselors can submit reports against other users. Administrators review all incoming reports through the Admin Panel and can either remove the reported user from the platform or dismiss the report if unwarranted.

---

## Project Backlog

The backlog is maintained using the **GitHub Projects** tab. See the board linked at the top for the full, up-to-date list of features, task assignments, and completion status across all phases.

### Phase 1 — Core (Completed)
- [x] Counselor discovery and availability viewing
- [x] Time slot booking system
- [x] Appointment reminders and notifications
- [x] Rescheduling and cancellation functionality
- [x] Counselor availability management dashboard

### Phase 2 — Progressive (Completed)
- [x] No-show tracking and analytics
- [x] Pre-session intake forms
- [x] Post-session feedback mechanism
- [x] Appointment history for students and counselors

### Phase 4 Additions (Completed)
- [x] AI counselor recommendation chatbot
- [x] Student daily journal
- [x] Counselor session notes (visible to student)
- [x] Admin-only counselor approval flow
- [x] Report to Admin with admin moderation panel
