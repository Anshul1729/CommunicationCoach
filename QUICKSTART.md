# Quick Start Guide

## Setup in 3 Steps

### Step 1: Add Your API Keys

Create a `local.properties` file in the project root (copy from `local.properties.template`):

```properties
whisperApiKey=sk-your-openai-key-here
claudeApiKey=sk-ant-your-anthropic-key-here
```

Get your keys from:
- OpenAI (Whisper): https://platform.openai.com/api-keys
- Anthropic (Claude): https://console.anthropic.com/settings/keys

### Step 2: Open in Android Studio

1. Open Android Studio
2. Click **Open an Existing Project**
3. Select the `CommunicationCoach` folder
4. Wait for Gradle sync to complete

### Step 3: Run on Your Device

1. Connect your Android phone via USB
2. Enable **USB Debugging**: `Settings → Developer options → USB debugging`
3. Click the green **Run** button in Android Studio
4. Select your device

---

## First Run

1. Tap **Start Coaching**
2. Grant **Microphone** and **Notification** permissions
3. Minimize the app and speak naturally for a minute or two
4. After ~90 seconds of silence, analysis will trigger automatically
5. You'll get a notification: "Conversation analyzed — X insights waiting"

---

## Important: Battery Optimization (Samsung)

Samsung's One UI aggressively kills background apps. You must exempt this app or it will stop recording when the screen turns off.

`Settings → Device care → Battery → Background usage limits → Never sleeping apps → Add CommunicationCoach`

Or: `Settings → Apps → CommunicationCoach → Battery → Unrestricted`

---

## Project Files Overview

```
CommunicationCoach/
├── app/src/main/java/com/communicationcoach/
│   ├── data/              # API clients, database, models, repository
│   ├── service/           # Foreground recording service + silence detection
│   ├── ui/                # Compose screens (home, conversations, progress)
│   ├── util/              # AudioRecorder, SilenceDetector, etc.
│   └── worker/            # WorkManager tasks (transcription, analysis)
├── app/src/main/res/      # Layouts, strings, themes
├── README.md              # Full documentation
└── API_KEY_SETUP.md       # Detailed API key instructions
```

---

## Troubleshooting

**Build fails?**
```bash
./gradlew clean
./gradlew build
```

**App stops recording when screen is off?**
- Follow the battery optimization steps above — this is the most common issue on Samsung

**No insights after conversation?**
- Check internet connection (audio queues offline and syncs when connected)
- Check LogCat for `TranscriptionWorker` and `AnalysisWorker` tags

**Permission denied?**
- `Settings → Apps → CommunicationCoach → Permissions → Microphone → Allow`

---

**See `README.md` for full architecture and documentation.**
