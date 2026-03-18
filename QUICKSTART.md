# Quick Start

> Full setup instructions: [`API_KEY_SETUP.md`](API_KEY_SETUP.md)

## Prerequisites

- [Groq](https://console.groq.com/) account (free)
- [Cloudflare](https://cloudflare.com/) account (free)
- Node.js + Wrangler CLI (`npm install -g wrangler`)
- Android Studio
- Android phone with USB Debugging enabled

## Setup (10 minutes)

**1. Get your Groq API key**

Sign up at [console.groq.com](https://console.groq.com/) → API Keys → Create API Key.

**2. Deploy the Cloudflare Worker**
```bash
cd cloudflare-worker
wrangler login
wrangler deploy
# Note the Worker URL printed after deploy
```

**3. Set Worker secrets**
```bash
wrangler secret put GROQ_API_KEY   # paste your Groq key
wrangler secret put APP_TOKEN      # paste any random string (openssl rand -hex 32)
```

**4. Configure local.properties**
```bash
cp local.properties.template local.properties
# Edit local.properties: fill in WORKER_URL and APP_TOKEN
```

**5. Open in Android Studio**
- File → Open → select this folder
- Wait for Gradle sync

**6. Run on device**
- Connect phone via USB
- Click the green Run button
- Grant Microphone and Notification permissions when prompted

## First Use

1. Tap **Start Coaching**
2. Have a conversation (or just talk for a minute)
3. Go silent for ~90 seconds — analysis triggers automatically
4. You'll receive a notification with 2-3 coaching tips

## Samsung Battery Fix

Samsung's One UI kills background services when the screen turns off. Whitelist the app:

`Settings → Device care → Battery → Background usage limits → Never sleeping apps → Add CommunicationCoach`

## Troubleshooting

| Problem | Fix |
|---|---|
| App stops when screen turns off | Follow Samsung battery fix above |
| No insights after conversation | Check internet; check LogCat for `AnalysisWorker` |
| 401 errors | Verify `APP_TOKEN` matches the Worker secret |
| Worker errors | Run `wrangler tail` to see live Worker logs |
| Build fails | `./gradlew clean && ./gradlew build` |
