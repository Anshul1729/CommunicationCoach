# API Key Setup

This app uses two services, both completely free on their free tier:

- **Groq** — transcription (Whisper large-v3) + LLM analysis (Llama 3.3 70B)
- **Cloudflare Workers** — a proxy that holds your Groq API key (keeps it out of the APK)

---

## Step 1 — Get a Groq API Key

1. Go to [console.groq.com](https://console.groq.com/) and sign up (free)
2. Go to **API Keys → Create API Key**
3. Copy the key — you'll need it in Step 3

**Free tier limits** (more than enough for personal use):
- Whisper: 7,200 seconds/day (~2 hours of audio)
- Llama 3.3 70B: 14,400 requests/day

---

## Step 2 — Deploy the Cloudflare Worker

The Cloudflare Worker is a thin proxy that holds your Groq API key as a secret and calls Groq on behalf of the Android app. It also keeps your API key out of the APK binary.

**Prerequisites:** [Node.js](https://nodejs.org/) installed

**Install Wrangler (Cloudflare's CLI):**
```bash
npm install -g wrangler
wrangler login
```

**Deploy the worker:**
```bash
cd cloudflare-worker
wrangler deploy
```

After deploying, Wrangler prints your Worker URL — something like:
```
https://communication-coach-proxy.<your-name>.workers.dev
```
Save this URL.

---

## Step 3 — Set Worker Secrets

Set two secrets on your deployed Worker:

```bash
# Your Groq API key from Step 1
wrangler secret put GROQ_API_KEY

# A random token the app uses to authenticate to your Worker
# Generate one: openssl rand -hex 32
wrangler secret put APP_TOKEN
```

Both prompts will ask you to paste the value. These are stored encrypted by Cloudflare and never appear in your code.

---

## Step 4 — Configure local.properties

Copy the template:
```bash
cp local.properties.template local.properties
```

Fill in your values:
```properties
sdk.dir=/path/to/your/Android/sdk
WORKER_URL=https://communication-coach-proxy.<your-name>.workers.dev
APP_TOKEN=<same random token you set in Step 3>
```

> `sdk.dir` is typically `~/Library/Android/sdk` on macOS or `C:\Users\<you>\AppData\Local\Android\Sdk` on Windows.

---

## Step 5 — Build and Run

1. Open the project in Android Studio
2. Wait for Gradle sync to complete
3. Connect your Android phone via USB with USB Debugging enabled
4. Click the green **Run** button

---

## Why the Cloudflare Worker?

The Groq API key can't live in the APK — APKs are easily decompiled and any hardcoded secret would be exposed. The Cloudflare Worker acts as a private backend:

- The APK authenticates to your Worker using a random `APP_TOKEN`
- The Worker calls Groq using your `GROQ_API_KEY`
- Neither secret is ever in the APK or committed to git

Both Cloudflare Workers and Groq are free for personal use levels of traffic.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| Build fails with "WORKER_URL not found" | Make sure `local.properties` exists with correct values (no spaces around `=`) |
| 401 errors from Worker | Check that `APP_TOKEN` in `local.properties` matches what you set via `wrangler secret put` |
| 500 errors from Worker | Check Worker logs via `wrangler tail` — likely a bad Groq key or quota exceeded |
| No transcription after conversation | Check internet; check LogCat for `AnalysisWorker` tag |
| App stops when screen turns off (Samsung) | `Settings → Device care → Battery → Background usage limits → Never sleeping apps → Add CommunicationCoach` |
| Build fails after clean | `./gradlew clean && ./gradlew build` |
