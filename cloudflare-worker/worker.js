/**
 * Communication Coach — Cloudflare Worker proxy
 *
 * Holds GCP credentials as Worker Secrets (never in the APK).
 * The Android app authenticates with a simple APP_TOKEN bearer token.
 *
 * Secrets to set via `wrangler secret put`:
 *   SERVICE_ACCOUNT_JSON  — full content of your GCP service account JSON
 *   APP_TOKEN             — any random string (e.g. openssl rand -hex 32)
 *
 * Endpoints:
 *   POST /transcribe  { audio: "<base64 raw PCM>" }
 *   POST /gemini      { prompt: "...", maxTokens: 4096 }
 */

let _gcpToken = null;
let _gcpTokenExpiry = 0;

export default {
  async fetch(request, env) {
    // ── Auth ──────────────────────────────────────────────────────────────────
    if (request.headers.get('Authorization') !== `Bearer ${env.APP_TOKEN}`) {
      return new Response('Unauthorized', { status: 401 });
    }

    const path = new URL(request.url).pathname;
    const body = await request.json().catch(() => ({}));

    try {
      const gcpToken = await getGcpToken(env);
      if (path === '/transcribe') return await transcribe(body, gcpToken);
      if (path === '/gemini')     return await gemini(body, gcpToken, env);
      return new Response('Not found', { status: 404 });
    } catch (err) {
      return new Response(JSON.stringify({ error: err.message }), {
        status: 500,
        headers: { 'Content-Type': 'application/json' },
      });
    }
  },
};

// ── GCP OAuth2 token (module-level cache, ~1 hour) ────────────────────────────

async function getGcpToken(env) {
  const now = Math.floor(Date.now() / 1000);
  if (_gcpToken && now < _gcpTokenExpiry - 60) return _gcpToken;

  const sa  = JSON.parse(env.SERVICE_ACCOUNT_JSON);
  const jwt = await buildJwt(sa.client_email, sa.private_key, now);

  const resp = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`,
  });

  const data = await resp.json();
  if (!data.access_token) throw new Error(`GCP token error: ${JSON.stringify(data)}`);

  _gcpToken       = data.access_token;
  _gcpTokenExpiry = now + 3600;
  return _gcpToken;
}

async function buildJwt(clientEmail, privateKeyPem, now) {
  const header  = b64url(JSON.stringify({ alg: 'RS256', typ: 'JWT' }));
  const payload = b64url(JSON.stringify({
    iss: clientEmail,
    scope: 'https://www.googleapis.com/auth/cloud-platform',
    aud: 'https://oauth2.googleapis.com/token',
    iat: now,
    exp: now + 3600,
  }));
  const signingInput = `${header}.${payload}`;
  const key = await importPrivateKey(privateKeyPem);
  const sig = await crypto.subtle.sign(
    'RSASSA-PKCS1-v1_5', key, new TextEncoder().encode(signingInput)
  );
  return `${signingInput}.${b64url(sig)}`;
}

async function importPrivateKey(pem) {
  const der = pemToDer(pem);
  return crypto.subtle.importKey(
    'pkcs8', der,
    { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
    false, ['sign']
  );
}

function pemToDer(pem) {
  const b64 = pem.replace(/-----[^-]+-----/g, '').replace(/\s/g, '');
  const bin = atob(b64);
  const buf = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) buf[i] = bin.charCodeAt(i);
  return buf.buffer;
}

function b64url(data) {
  const str = typeof data === 'string'
    ? btoa(data)
    : btoa(String.fromCharCode(...new Uint8Array(data)));
  return str.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

// ── Speech-to-Text ────────────────────────────────────────────────────────────

async function transcribe(body, gcpToken) {
  const resp = await fetch('https://speech.googleapis.com/v1p1beta1/speech:recognize', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${gcpToken}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      config: {
        encoding: 'LINEAR16',
        sampleRateHertz: 16000,
        languageCode: 'en-IN',
        alternativeLanguageCodes: ['hi-IN'],
        model: 'latest_long',
        enableAutomaticPunctuation: true,
      },
      audio: { content: body.audio },
    }),
  });
  return new Response(resp.body, {
    status: resp.status,
    headers: { 'Content-Type': 'application/json' },
  });
}

// ── Vertex AI Gemini ──────────────────────────────────────────────────────────

async function gemini(body, gcpToken, env) {
  const sa  = JSON.parse(env.SERVICE_ACCOUNT_JSON);
  const url = `https://us-central1-aiplatform.googleapis.com/v1/projects/${sa.project_id}`
    + `/locations/us-central1/publishers/google/models/gemini-2.5-flash:generateContent`;

  const resp = await fetch(url, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${gcpToken}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      contents: [{ role: 'user', parts: [{ text: body.prompt }] }],
      generationConfig: {
        maxOutputTokens: body.maxTokens ?? 4096,
        thinkingConfig: { thinkingBudget: 0 },
      },
    }),
  });
  return new Response(resp.body, {
    status: resp.status,
    headers: { 'Content-Type': 'application/json' },
  });
}
