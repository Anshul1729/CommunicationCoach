# Quick Start Guide

## Setup in 3 Steps

### Step 1: Add GCP credentials

1. Follow `API_KEY_SETUP.md` to create a GCP service account
2. Place `service_account.json` at `app/src/main/res/raw/service_account.json`
3. Copy `local.properties.template` → `local.properties` and fill in your GCP project ID

### Step 2: Open in Android Studio

1. Open Android Studio → **Open an Existing Project** → select `CommunicationCoach`
2. Wait for Gradle sync to complete

### Step 3: Run on Your Device

1. Connect your Android phone via USB
2. Enable **USB Debugging**: `Settings → Developer options → USB debugging`
3. Click the green **Run** button in Android Studio

---

## First Run

1. Tap **Start Coaching**
2. Grant **Microphone** and **Notification** permissions
3. Minimize the app and speak naturally
4. After ~90 seconds of silence, analysis triggers automatically
5. You'll get a notification with coaching tips

---

## Important: Battery Optimization (Samsung)

Samsung's One UI aggressively kills background apps. Exempt this app or it will stop recording when the screen turns off.

`Settings → Device care → Battery → Background usage limits → Never sleeping apps → Add CommunicationCoach`

---

## Project Structure

```
app/src/main/java/com/communicationcoach/
├── data/         # API clients (Speech-to-Text, Vertex AI), Room DB
├── service/      # Foreground recording service, silence detection
├── ui/           # Jetpack Compose screens
├── util/         # AudioRecorder, SilenceDetector, CostTracker
└── worker/       # WorkManager tasks (AnalysisWorker, DailyDigestWorker)
```

---

## Troubleshooting

**App stops recording when screen is off?**
→ Follow the battery optimization steps above

**No insights after conversation?**
→ Check internet connection; check LogCat for `AnalysisWorker` tag

**Build fails?**
```bash
./gradlew clean && ./gradlew build
```
