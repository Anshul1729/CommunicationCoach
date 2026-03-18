# Communication Coach

A personal Android app that passively listens to your conversations throughout the day and gives you coaching feedback — so you can become a more calm, clear, and confident communicator over time.

---

## Why I Built This

I wanted to grow as a communicator: stay composed in difficult conversations, express myself more clearly, and be more mindful of my tone and word choice. The problem is that in the heat of the moment, you rarely notice your own patterns. And by the time a conversation ends, you've already forgotten what you said.

So I built a tool that does what a personal coach would: listen, observe, and give you specific, actionable feedback after the fact — without interrupting you in the moment.

The app runs silently in the background. After each conversation, you get 2-3 practical tips. At the end of the day, you get a digest that surfaces recurring patterns and tracks your progress over time.

---

## What It Coaches On

- **Tone and emotional regulation** — catching moments where you come across as sharper or more reactive than intended
- **Clarity and structure** — are your points landing the way you think they are?
- **Confidence signals** — hedging, filler words, upspeak, weak qualifiers
- **Language fluency** — grammar, word choice, vocabulary
- **Listening and composure** — how you show up in back-and-forth exchanges

---

## How It Works

```
┌─────────────────────────────────────────────────────┐
│  FOREGROUND SERVICE (runs in background always)     │
│  - Records 30s audio chunks continuously            │
│  - Silence for 90s → conversation marked complete   │
│  - Stores audio + transcripts locally               │
└─────────────┬───────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────┐
│  GROQ WHISPER — transcription per chunk             │
│  - Supports English, Hindi, and Hinglish            │
│  - Offline: queued via WorkManager                  │
│  - Syncs automatically when internet is available   │
└─────────────┬───────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────┐
│  GROQ LLAMA 3.3 70B — analysis on conversation end  │
│  Input: full transcript + your coaching profile     │
│  Output:                                            │
│  - 2-3 specific, actionable coaching tips           │
│  - Patterns detected (tone, clarity, fluency)       │
│  - Profile update: new observations recorded        │
└─────────────┬───────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────┐
│  DAILY DIGEST (notification at 10 PM)               │
│  - Patterns across all conversations today          │
│  - Progress vs previous days                        │
│  - Top focus areas for tomorrow                     │
└─────────────────────────────────────────────────────┘
```

**Feedback is delivered as:**
- A push notification after each conversation ends (2-3 tips)
- An end-of-day digest with broader patterns and progress
- In-app history with full transcripts and coaching insights
- An evolving profile that gets more personalized the more you use it

---

## Key Design Decisions

### Runs entirely in the background
The foreground service runs independently. Once started, minimize the app and go about your day. A persistent notification shows it's active.

> **Samsung users:** One UI aggressively kills background apps. To keep recording when the screen is off:
> `Settings → Device care → Battery → Background usage limits → Never sleeping apps → Add CommunicationCoach`
> Or: `Settings → Apps → CommunicationCoach → Battery → Unrestricted`

### Smart conversation boundaries
The app monitors audio volume (RMS). When near-silence is detected for 90 consecutive seconds, it treats the conversation as ended and triggers analysis — so you get per-conversation feedback, not arbitrary time chunks.

### Memory-based personalization
The LLM is given your full coaching profile with every analysis. The more you use the app, the more it knows your specific patterns, triggers, and areas of improvement — making feedback increasingly tailored over time.

### No real-time interruptions
Feedback is always post-conversation. The app never interrupts you while you're speaking.

### Offline support
If you lose internet mid-conversation, recordings are queued locally. WorkManager retries automatically when connectivity is restored.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Transcription | Groq — Whisper large-v3 |
| AI Analysis | Groq — Llama 3.3 70B Versatile |
| Proxy / Key vault | Cloudflare Workers |
| Background | Android Foreground Service + WorkManager |
| Database | Room (SQLite) |
| Architecture | MVVM + Repository pattern |

---

## Project Structure

```
app/src/main/java/com/communicationcoach/
├── data/
│   ├── api/
│   │   ├── ApiService.kt              # Retrofit interface (Cloudflare Worker)
│   │   └── ApiClient.kt               # API client + prompt builder
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   ├── dao/                       # Room DAOs
│   │   └── entity/                    # DB entities
│   ├── model/
│   │   └── ApiModels.kt               # Request/response data classes
│   └── repository/
│       └── CoachingRepository.kt      # Single source of truth
├── service/
│   ├── RecordingForegroundService.kt  # Recording loop + silence detection
│   └── BootReceiver.kt               # Reminder notification after reboot
├── ui/
│   ├── home/                          # Recording toggle + today's stats
│   ├── conversations/                 # Session history list
│   ├── detail/                        # Transcript + coaching insights
│   ├── progress/                      # Weekly trends + personality profile
│   └── profile/                       # Spend tracking + settings
├── util/
│   ├── AudioRecorder.kt               # 30s WAV chunks (16kHz, mono)
│   ├── SilenceDetector.kt             # RMS-based conversation boundary detection
│   └── CostTracker.kt                 # Cumulative API cost tracking
└── worker/
    ├── AnalysisWorker.kt              # LLM analysis + push notification
    └── DailyDigestWorker.kt           # 10 PM daily digest
```

---

## Database Schema

```
conversations     — id, startTime, endTime, status (RECORDING/READY/ANALYZING/ANALYZED/FAILED)
transcript_chunks — id, conversationId, chunkNum, timestamp, text
insights          — id, conversationId, createdAt, tipsJson, issuesJson, summary
daily_digests     — id, date, summary, topPatternsJson, progressNotes
user_profile      — singleton JSON (patterns, improvements, focusAreas — updated per analysis)
upload_queue      — id, conversationId, status, retryCount
```

---

## Using This Repo

This is a fully working app — you just need a free Groq API key and a Cloudflare Worker to proxy it. Everything else is ready to go.

**What you need:**
- A [Groq](https://console.groq.com/) account (free — no credit card required)
- A [Cloudflare](https://cloudflare.com/) account (free Workers tier)
- Android Studio
- An Android phone (API 26+)

**See [`API_KEY_SETUP.md`](API_KEY_SETUP.md) for step-by-step setup instructions.**

Short version:
1. Get a free Groq API key at [console.groq.com](https://console.groq.com/)
2. Deploy the Cloudflare Worker in `cloudflare-worker/` → set `GROQ_API_KEY` and `APP_TOKEN` as Worker Secrets
3. Copy `local.properties.template` → `local.properties`, fill in your Worker URL + APP_TOKEN
4. Open in Android Studio, sync Gradle, run on device
5. Tap **Start Coaching**, grant mic + notification permissions, and go about your day

---

## Cost Estimate

Both services are free for personal usage levels:

| Service | Free Tier |
|---|---|
| Groq Whisper (transcription) | 7,200 seconds/day (~2 hours of audio) |
| Groq Llama 3.3 70B (analysis) | 14,400 requests/day |
| Cloudflare Workers | 100,000 requests/day |

If you somehow exceed the free tier, Groq's paid pricing is ~$0.11/hour of audio and ~$0.60-0.80 per 1M tokens. For a few conversations a day, you'll stay free indefinitely. The app tracks cumulative spend on the Profile screen.

---

## Limitations & Notes

- The app needs to stay running as a foreground service — Samsung and other battery-aggressive OEMs may kill it unless you whitelist it (see setup above)
- Audio is processed on-device before being sent to Groq's APIs — no audio is stored in the cloud
- This is a personal project, not a polished consumer app — use it as a starting point and customize the coaching prompts to your own focus areas
