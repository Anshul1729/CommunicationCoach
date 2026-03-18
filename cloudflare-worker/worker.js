/**
 * Communication Coach — Cloudflare Worker proxy
 *
 * Holds the Groq API key as a Worker Secret (never in the APK).
 * The Android app authenticates with a simple APP_TOKEN bearer token.
 *
 * Secrets to set via `wrangler secret put`:
 *   GROQ_API_KEY  — your Groq API key (from console.groq.com)
 *   APP_TOKEN     — any random string (e.g. openssl rand -hex 32)
 *
 * Endpoints:
 *   POST /transcribe  { audio: "<base64 WAV bytes>" }
 *   POST /gemini      { prompt: "...", maxTokens: 4096 }
 *
 * Responses are shaped to match the existing Android model classes
 * (GeminiResponse / SpeechResponse) so the app code is unchanged.
 */

export default {
  async fetch(request, env) {
    // ── Auth ──────────────────────────────────────────────────────────────────
    if (request.headers.get('Authorization') !== `Bearer ${env.APP_TOKEN}`) {
      return new Response('Unauthorized', { status: 401 });
    }

    const path = new URL(request.url).pathname;
    const body = await request.json().catch(() => ({}));

    try {
      if (path === '/transcribe') return await transcribe(body, env.GROQ_API_KEY);
      if (path === '/gemini')     return await chat(body, env.GROQ_API_KEY);
      return new Response('Not found', { status: 404 });
    } catch (err) {
      return new Response(JSON.stringify({ error: err.message }), {
        status: 500,
        headers: { 'Content-Type': 'application/json' },
      });
    }
  },
};

// ── Transcription (Groq Whisper) ──────────────────────────────────────────────

async function transcribe(body, groqApiKey) {
  // body.audio is base64-encoded full WAV (including header)
  const wavBytes = base64ToBytes(body.audio);

  const formData = new FormData();
  formData.append('file', new Blob([wavBytes], { type: 'audio/wav' }), 'audio.wav');
  formData.append('model', 'whisper-large-v3');
  // Omit language for auto-detection (handles English / Hindi / Hinglish)
  formData.append('prompt', 'Conversation in India. Mix of English, Hindi, and Hinglish. Hindi words in Roman script.');
  formData.append('response_format', 'json');

  const resp = await fetch('https://api.groq.com/openai/v1/audio/transcriptions', {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${groqApiKey}` },
    body: formData,
  });

  const data = await resp.json();

  // Transform to SpeechResponse format (matches the Android model class)
  return Response.json({
    results: [{
      alternatives: [{
        transcript: data.text ?? '',
        confidence: 1.0,
      }],
    }],
  }, { status: resp.ok ? 200 : resp.status });
}

// ── LLM analysis (Groq — llama-3.3-70b-versatile) ────────────────────────────

async function chat(body, groqApiKey) {
  const resp = await fetch('https://api.groq.com/openai/v1/chat/completions', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${groqApiKey}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      model: 'llama-3.3-70b-versatile',
      messages: [{ role: 'user', content: body.prompt }],
      max_tokens: body.maxTokens ?? 4096,
    }),
  });

  const data = await resp.json();
  const text  = data.choices?.[0]?.message?.content ?? '';
  const usage = data.usage ?? {};

  // Transform to Gemini format (matches GeminiResponse in Android)
  return Response.json({
    candidates: [{
      content: {
        role: 'model',
        parts: [{ text }],
      },
    }],
    usageMetadata: {
      promptTokenCount:     usage.prompt_tokens     ?? 0,
      candidatesTokenCount: usage.completion_tokens ?? 0,
    },
  }, { status: resp.ok ? 200 : resp.status });
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function base64ToBytes(b64) {
  const binary = atob(b64);
  const bytes  = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes.buffer;
}
