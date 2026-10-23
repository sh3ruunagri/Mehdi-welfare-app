# Mehdi Welfare Organization Android App — Starter v1.1 (Login + Online Database scaffold)

## What's added
- Email/password login screen using Firebase Authentication SDK.
- Account creation flow; newly created profiles are marked `pending`.
- Firestore integration scaffold for saving and loading records online.
- Local records remain as a convenience fallback when Firebase is not configured.
- Branch selector for Nagar Khas and Karachi.
- Basic donor/member/spending forms and voucher preview/share.

## IMPORTANT: Firebase must be configured before online login/database works
This project does **not** include your Firebase project's private setup values. You must create/configure your own Firebase project.

1. Go to https://console.firebase.google.com/ and create a project.
2. In Project settings, add an Android app with package name `org.mehdiwelfare.app`.
3. Enable Authentication > Sign-in method > Email/Password.
4. Create a Cloud Firestore database.
5. In Project settings, copy these values into `MainActivity.kt` inside `initFirebaseIfConfigured()`:
   - `apiKey` = Web API Key
   - `appId` = App ID
   - `projectId` = Project ID
   - `senderId` = Messaging Sender ID
6. Set Firestore Security Rules before using real data. Do not leave unrestricted test rules in production.
7. Open the folder in Android Studio and allow Gradle sync.

## Security limitations / next work
- This is a scaffold, **not production-ready secure access control**.
- The branch selector is not authorization. Central-admin and branch roles need trusted server-side role claims/rules before real use.
- Newly created accounts are marked `pending`, but account creation itself is not admin approval.
- Firestore rules must restrict users to their own approved branch; never rely on a hidden UI button for security.
- One-device-at-a-time enforcement needs a trusted backend/Cloud Function or equivalent server logic.
- OTP/new-device verification is not implemented in this version.
- Automated WhatsApp reminders are not implemented; they need WhatsApp Business Platform setup and a trusted scheduled backend.
- Voucher image/gallery export is not implemented.
- Do not store real sensitive beneficiary details until role-based rules, access review, backups and privacy controls have been implemented.

## Build note
This ZIP contains source files. It is not an APK. Build it with Android Studio after configuring Firebase and Android SDK.
