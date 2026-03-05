# Communication Coach - API Key Setup

## Required API Keys

You need two API keys to run this app:

### 1. OpenAI Whisper API Key

**Get it here:** https://platform.openai.com/api-keys

1. Sign up / Log in to OpenAI
2. Go to API Keys section
3. Create a new secret key
4. Copy the key (starts with `sk-...`)

### 2. Anthropic Claude API Key

**Get it here:** https://console.anthropic.com/settings/keys

1. Sign up / Log in to Anthropic
2. Go to Settings → API Keys
3. Create a new key
4. Copy the key (starts with `sk-ant-...`)

## Where to Add Keys

### Option 1: In local.properties (Recommended)

Create `local.properties` in project root:

```properties
whisperApiKey=sk-your-openai-key-here
claudeApiKey=sk-ant-your-anthropic-key-here
```

Then update `RecordingForegroundService.kt`:

```kotlin
// Load from BuildConfig or hardcode for MVP
const val WHISPER_API_KEY = "sk-your-openai-key-here"
const val CLAUDE_API_KEY = "sk-ant-your-anthropic-key-here"
```

### Option 2: Direct in RecordingForegroundService.kt

Open `app/src/main/java/com/communicationcoach/service/RecordingForegroundService.kt`

Find these lines (around line 27-28):

```kotlin
const val WHISPER_API_KEY = "YOUR_WHISPER_API_KEY"
const val CLAUDE_API_KEY = "YOUR_CLAUDE_API_KEY"
```

Replace with your actual keys.

## Security Note

⚠️ **Never commit API keys to version control!**

The `local.properties` file should be in your `.gitignore`.

For production, use:
- Android Keystore for key encryption
- BuildConfig with secret management
- Environment variables in CI/CD

## Testing Your Keys

After adding keys, test them:

1. Build and run the app
2. Start a recording session
3. Speak for 30 seconds
4. Check LogCat for:
   - "Transcript: ..." (Whisper working)
   - "Analysis: ..." (Claude working)
   - "Nudge triggered for: ..." (Vibration working)

## Cost Monitoring

Monitor your API usage:
- OpenAI: https://platform.openai.com/usage
- Anthropic: https://console.anthropic.com/settings/billing

Expected cost: **$1-4/day** for 4-6 hours of usage.
