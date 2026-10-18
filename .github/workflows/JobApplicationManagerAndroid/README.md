# Job Action Manager — Android source project

This is a native Android application project, not an HTML wrapper. It is seeded with the 56 records from the supplied `job_tracker.csv`.

## Included in this first native version
- Local SQLite job database.
- Starter jobs imported from the provided CSV, with a CSV import button for future Arena AI exports.
- Job application links and a way to attach a resume PDF from the phone.
- Status actions: mark applied, record a confirmation detail, or dismiss a job.
- Daily repeating reminder notifications at 9:00 AM for jobs that still need action. Notification actions can mark a job applied or dismiss it.
- Reminder date per job.
- Shareable progress report.
- Duplicate check on CSV import by job title + company.

## Important limitations
- Arena AI direct API/account integration is **not connected** in this version. This version imports the CSV export; a direct connection requires a documented Arena AI API, webhook, or supported export integration.
- Application status is **not automatically verified from job websites or email**. The app asks you to record confirmation evidence, because opening a link does not prove an application was submitted.
- The app uses Android's inexact daily alarm. Android battery management can delay notifications; allow notifications and exempt the app from battery optimization if reminders are delayed.
- Resume names in the source CSV are labels only. Attach the actual PDF resume from your phone with the “Attach resume” button if you want it accessible from the app.
- This starter build uses local storage only; there is no cloud sync between devices.

## Build the APK with Android Studio
1. Install Android Studio and its Android SDK (API 35 or compatible).
2. Open this project folder (`JobApplicationManagerAndroid`) in Android Studio.
3. Let Gradle sync finish.
4. Choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. Android Studio will create `app/build/outputs/apk/debug/app-debug.apk`.
6. Transfer the APK to your Android phone and install it. You may need to allow installation from that source.

## Build with GitHub Actions
The included workflow `.github/workflows/build-apk.yml` builds a debug APK when pushed to a GitHub repository or manually triggered. After it finishes, download the `job-action-manager-debug-apk` artifact from the workflow run.

## CSV format
Expected header names are the same as the uploaded file:
`date_shown,job_title,company,entity_or_division,location,posted,match_pct,resume_file,application_link,status,notes`
