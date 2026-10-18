# AGENTS.md — Job Tracker project briefing (read this first)

Personal job-search tracker for Mohammed Mansoor PV (Dubai, aerospace mechanical engineer).
Two deliverables live in this repo:

1. **PWA web app** — `app/index.html` (single file, ALL CSS/JS inline, zero external CDNs, offline-first)
   plus `manifest.webmanifest`, `sw.js`, `icon-192.png`, `icon-512.png`, and a copy of `job_tracker.csv`.
2. **Android APK wrapper** — `apkbuild/` (WebView shell that loads the bundled assets from
   `file:///android_asset/index.html`). Sideloaded, signed with the repo's own key.

## Hard rules
- Mobile-first UI, house style: teal `#2E6E8E`, gold `#C9B489`, bg `#F3F6F7`, radius 14px cards, bottom tab nav.
- NEVER add external scripts/styles/CDNs — the app must work fully offline (file:// and APK assets).
- No frameworks, no build step for the web app: everything stays inline in `index.html`.
- Job data is REAL and sensitive: only ever seed from `job_tracker.csv` (or user edits in-app).
  Never invent companies, roles, dates or statuses.
- localStorage key: `mjt_db_v1`. Wrap all storage access in try/catch (file:// and sandboxed frames can throw).
- Auto-sync (`fetch('job_tracker.csv')`) only when `location.protocol` is http/https — see `canAutoSync()`.

## Data pipeline (the part that breaks if you're careless)
`jobs job_tracker.csv` (source of truth, maintained externally by the career assistant)
  → `seed_regen.py` normalizes it into a JSON seed
  → seed is EMBEDDED in `app/index.html` at the line `const SEED = [...];`
  → `app/index.html` + csv are copied into APK `assets/` at build time.

### The CSV is messy on purpose — three row layouts exist:
- **Header order (11+ cols):** date,title,company,division,location,posted,match,resume,link,status,notes…
  (notes may be split across extra cols when unquoted commas exist — join cols 10+ with ',')
- **Variant A (10 cols):** date,title,LOCATION,company,summary,req,match,status,link,resume
- **Variant B (10 cols):** date,title,company,division,location,req,match,status,link,resume
  Discriminator: variant A iff col[2] matches location vocabulary (abu dhabi|dubai|al ain|uae|masdar|sharjah…)
  and col[3] does not. `seed_regen.py` implements this exactly — DO NOT rewrite it with csv.DictReader.
Status mapping: contains APPLIED→applied; SHOWN→shown; WATCHLIST→watchlist; CLOSED→closed; else reference.
Resume filenames ending `.docx` are rewritten to `.pdf`.

### In-app merge protection
When importing a CSV over an existing DB, never downgrade a status the user advanced in-app
(`followed_up/interview/offer/rejected/withdrawn`, flagged `userStatus:true`) back to a CSV state.

## Commands
- Regenerate seed + patch index.html:
  `python3 seed_regen.py [path/to/job_tracker.csv] [path/to/index.html]`
  (defaults: `jobs/job_tracker.csv`, `app/index.html`)
- Build the APK (needs JDK 8+, Android SDK platform-34 + build-tools 34.0.0):
  `bash BUILD_APK.sh [versionCode]`
  Script expects `$ANDROID_HOME` or `./android-sdk` next to the repo; downloads nothing by itself.
  Signing key: `apkbuild/key.pk8` + `apkbuild/cert.pem` — ALWAYS reuse them so updates install over old versions.
- Serve the PWA locally: `python3 -m http.server 8000 --directory app`

## APK build internals (what BUILD_APK.sh does)
javac (-bootclasspath android.jar) → d8 (--min-api 24) → aapt2 compile/link
(manifest `apkbuild/AndroidManifest.xml`, assets `-A`, res zip) → zip classes.dex (stored, -0)
→ zipalign -4 → apksigner sign (pk8/cert) → verify.
WebView specifics that MUST stay in MainActivity.java:
- `setDomStorageEnabled(true)` (localStorage on file:// assets)
- `shouldOverrideUrlLoading` → external http(s)/mailto/tel open in the browser app
- `onShowFileChooser` → Android file picker (CSV/JSON import in the Data tab depends on it)
- back key → `wv.goBack()`

## Feature map (index.html)
Tabs: Today (chase-due list, daily checklist, reminders via Notification API while open),
Jobs (search + filter chips + detail sheet + add/edit), Log (streak calendar 84d + entries),
Data (CSV merge import, JSON backup/restore, chase-window days, task editor, sync-now for hosted mode).
Chase logic: `applied` + `daysSince(appliedDate) >= DB.chase (default 8)` → due;
`followed_up` + `daysSince(lastFollowUp) >= chase+4` → 2nd chase. Streak = consecutive days with
any log entry or ticked task.

## Hosting notes (if asked)
Any static HTTPS host works (PWA installable: manifest + sw + icons present).
SW strategy: cache-first shell, network-first for `job_tracker.csv`.
Anonymous free hosts are bot-blocked as of 2026; user-owned Netlify/GitHub Pages is the sanctioned route.
