# Setup Guide

This app uses **Google Cloud Vertex AI** (Gemini 2.5 Flash) and **Google Cloud Speech-to-Text**.
Both are accessed via a single GCP service account — no separate API keys needed.

## Steps

### 1. Create a GCP Service Account
1. Go to [Google Cloud Console](https://console.cloud.google.com/) → IAM & Admin → Service Accounts
2. Create a service account with these roles:
   - `Cloud Speech-to-Text User`
   - `Vertex AI User`
3. Create a JSON key and download it

### 2. Add the service account file
Place the downloaded JSON file at:
```
app/src/main/res/raw/service_account.json
```
Use `service_account_template.json` at the project root as a reference for the expected format.

### 3. Configure local.properties
Copy `local.properties.template` to `local.properties` and fill in your values:
```properties
sdk.dir=/path/to/your/Android/sdk
GCP_PROJECT_ID=your-gcp-project-id
VERTEX_REGION=us-central1
GEMINI_MODEL=gemini-2.5-flash
```

### 4. Enable APIs in GCP Console
- [Cloud Speech-to-Text API](https://console.cloud.google.com/apis/library/speech.googleapis.com)
- [Vertex AI API](https://console.cloud.google.com/apis/library/aiplatform.googleapis.com)
