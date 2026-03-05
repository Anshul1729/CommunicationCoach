# Communication Coach

An Android app that passively listens to your conversations throughout the day and provides personalized coaching feedback to help you improve over time.

## What It Does

The app quietly records your conversations via your OnePlus Bud earbuds (or phone mic), transcribes them, and uses Claude AI to give you coaching feedback based on your specific communication patterns.

**Coaching focus areas:**
- Defensive communication patterns
- Short temper / aggressive tone
- Grammar and incorrect English
- Clarity and structure
- Confidence signals (hedging, upspeak, weak language)
- Vocabulary and word choice

**How feedback is delivered:**
- Notification after each conversation ends with 2-3 specific tips
- End-of-day digest summarizing patterns and progress
- In-app history with full transcripts and insights
- Evolving personality profile that tracks your progress over time

## How It Works

```
┌─────────────────────────────────────────────────────┐
│  FOREGROUND SERVICE (runs in background always)     │
│  - Records 30s audio chunks continuously            │
│  - Silence Detector: near-silence for 90s           │
│    → marks current conversation as ended            │
│  - Stores encrypted chunks + transcripts locally    │
└─────────────┬───────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────┐
│  WHISPER API — transcription per chunk              │
│  - Offline: queued via WorkManager                  │
│  - Syncs automatically when internet available      │
│  - Transcripts appended to conversation buffer      │
└─────────────┬───────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────┐
│  CLAUDE ANALYSIS — triggered on conversation end    │
│  Input: full transcript + your personality profile  │
│  Output:                                            │
│  - 2-3 specific actionable coaching tips            │
│  - Issues detected (defensive, grammar, temper)     │
│  - Profile patch: new patterns observed             │
└─────────────┬───────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────┐
│  DAILY DIGEST (notification at end of day)          │
│  - Patterns across all conversations                │
│  - Progress vs previous days                        │
│  - Top 3 things to work on tomorrow                 │
└─────────────────────────────────────────────────────┘
```

## Key Design Decisions

### No need to keep the app open
The foreground service runs independently. Once started, you can minimize the app, lock the screen, or use other apps. A persistent notification shows when it's recording.

> **Samsung users**: Disable battery optimization for this app or One UI will kill the service when the screen turns off.
> `Settings → Device care → Battery → Background usage limits → Never sleeping apps → Add CommunicationCoach`
> Or: `Settings → Apps → CommunicationCoach → Battery → Unrestricted`

### Smart conversation boundaries (not fixed time chunks)
The app monitors audio volume (RMS). When near-silence is detected for 90 seconds, it treats the conversation as ended and triggers analysis. This means you get feedback per natural conversation rather than arbitrary time chunks.

### Memory-based personalization
Claude is given your evolving personality profile with every analysis request. The more you use the app, the more personalized the feedback becomes — it knows your patterns, your triggers, and what you've improved.

### Offline support
If you lose internet, recordings queue locally. WorkManager retries uploads automatically when connectivity is restored.

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Transcription**: OpenAI Whisper API
- **AI Analysis**: Anthropic Claude API (claude-sonnet-4-6)
- **Background**: Android Foreground Service + WorkManager
- **Database**: Room
- **Architecture**: MVVM + Repository pattern

## Project Structure

```
app/src/main/java/com/communicationcoach/
├── data/
│   ├── api/
│   │   ├── ApiService.kt           # Retrofit interfaces (Whisper + Claude)
│   │   └── ApiClient.kt            # API client + prompt builder
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   ├── dao/                    # Room DAOs
│   │   └── entity/                 # DB entities (see schema below)
│   ├── model/
│   │   └── ApiModels.kt            # Request/Response data classes
│   └── repository/
│       └── CoachingRepository.kt   # Single source of truth
├── service/
│   └── RecordingForegroundService.kt  # Background recording + VAD
├── ui/
│   ├── home/                       # Home screen (recording toggle + today's stats)
│   ├── conversations/              # List of all recorded sessions
│   ├── detail/                     # Conversation detail (transcript + insights)
│   ├── progress/                   # Personality profile + weekly trends
│   └── settings/                   # API keys, thresholds, digest time
├── util/
│   ├── AudioRecorder.kt            # Audio recording (30s chunks, WAV)
│   ├── SilenceDetector.kt          # RMS-based conversation boundary detection
│   ├── VibrationController.kt      # (retained for optional nudges)
│   └── BehaviorAnalyzer.kt         # Parse Claude JSON responses
└── worker/
    ├── TranscriptionWorker.kt      # Whisper upload + transcription
    └── AnalysisWorker.kt           # Claude analysis + profile update
```

## Database Schema

```
conversations     — id, startTime, endTime, status (recording/ready/analyzed)
transcript_chunks — id, conversationId, chunkNum, timestamp, text
insights          — id, conversationId, createdAt, tips (JSON), issuesFound (JSON)
daily_digests     — id, date, summary, topPatterns, progress
user_profile      — singleton JSON document (updated after each analysis)
upload_queue      — id, conversationId, status, retryCount
```

## Setup

See `API_KEY_SETUP.md` for detailed instructions.

**Short version:**
1. Create `local.properties` with your API keys (see template)
2. Open in Android Studio, wait for Gradle sync
3. Run on your device
4. Tap **Start** and grant microphone + notification permissions
5. Minimize the app and go about your day

## Cost Estimate

- **Whisper**: ~$0.006/min → ~$0.36/hr of speech
- **Claude**: ~$0.01-0.05 per conversation analysis
- **Expected**: ~$0.50-2.00/day for typical usage

## Build Phases

- **Phase 1** ✅ — Basic recording + Whisper + vibration nudges
- **Phase 2A** — Conversation boundary detection, full-conversation Claude analysis, memory/profile system, offline queue
- **Phase 2B** — Multi-screen Compose UI (home, history, detail, progress)
- **Phase 2C** — Daily digest, progress charts, personality profile screen
