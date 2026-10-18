# Connect this project to VS Code + Codex (10 minutes)

You need: a computer, [VS Code](https://code.visualstudio.com) (free), Node.js LTS (free),
and a ChatGPT account with Codex access (Plus/Pro/Business) **or** an OpenAI API key.

## 1. Get the project on your computer
1. In the Arena chat, download **`jobtracker-project.zip`** (attached with this guide).
2. Unzip it anywhere, e.g. `C:\projects\jobtracker` or `~/projects/jobtracker`.

## 2. Open it in VS Code
VS Code → **File → Open Folder** → select the unzipped `jobtracker-project` folder.
Open a terminal inside VS Code: **Ctrl+`** (backtick).

## 3. Install Codex (pick ONE route)

### Route A — Codex extension inside VS Code (easiest)
1. Left sidebar → Extensions (Ctrl+Shift+X) → search **"Codex"** → publisher **OpenAI** → Install.
2. Click the Codex icon in the sidebar → **Sign in** with your ChatGPT account (or paste API key).
3. It auto-reads `AGENTS.md` in the folder — that file explains the whole project to Codex
   (data pipeline, CSV quirks, APK build, house rules). You don't need to explain anything.

### Route B — Codex CLI in the VS Code terminal
```bash
npm install -g @openai/codex     # once
cd <project folder>
codex                            # signs in via browser, or set OPENAI_API_KEY first
```

## 4. Give Codex a first task (copy-paste one of these)
- `Read AGENTS.md, then run seed_regen.py and summarise the seed counts.`
- `Read AGENTS.md and rebuild the APK (BUILD_APK.sh). If the Android SDK is missing, install it first following AGENTS.md.`
- `Add a dark-mode toggle to the app keeping the house style and offline rule, then rebuild the APK as v2.`
- `My new job_tracker.csv is in jobs/. Refresh the seed, keep the merge-protection rules, rebuild APK v2.`

## 5. Your weekly routine with me + Codex
1. I deliver the daily job pack + updated `job_tracker.csv` here in Arena chat.
2. Copy that CSV into the project's `jobs/` folder.
3. Tell Codex: *"refresh seed and rebuild APK vN"* → install the new APK on your phone
   (same signing key ⇒ clean upgrade, your in-app data survives).
   Or skip the APK: import the CSV inside the app (Data tab) — your choice.

## 6. Optional but recommended: git
```bash
git init && git add -A && git commit -m "job tracker handoff"
```
Codex works better with git (diffs, safe reverts). Keep the repo **private** if you push it
(it contains your personal job-search data + the APK signing key).

## Folder map
```
AGENTS.md            ← Codex briefing (project rules, CSV quirks, build internals)
seed_regen.py        ← CSV → embedded SEED in app/index.html
BUILD_APK.sh         ← one-command signed APK build
app/                 ← the PWA (index.html = the whole app; manifest; sw; icons; csv copy)
apkbuild/            ← MainActivity.java, AndroidManifest.xml, key.pk8 + cert.pem (signing!)
jobs/job_tracker.csv ← source-of-truth tracker copy
```
**Never delete or regenerate `apkbuild/key.pk8` / `cert.pem`** — a new key would force
uninstall+reinstall and could orphan in-app data.
