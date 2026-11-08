# CONSIST - Secure AI Backend Architecture & Deployment Guide

This backend proxy service secures the CONSIST AI features (Meal Photo Scanner, Voice Logging NLP, and AI Coach) by preventing client-side API key leakage.

## Security Principles Implemented

1. **Zero Client-Side Keys**: The private `GEMINI_API_KEY` is kept exclusively on the server as an environment variable and is never compiled into the Android APK.
2. **Encrypted Transport (HTTPS)**: All client-to-server communication requires HTTPS. The Android client actively rejects plain `http://` URLs.
3. **Authentication**: Requests are gated behind an optional `Authorization: Bearer <token>`.
4. **Rate Limiting & Payload Bounds**: Prevents API quota exhaustion via Express rate limiting (30 requests/minute/IP) and 4MB max body limits.
5. **Data Minimization**: Only the photograph or query is sent; passwords, authentication tokens, and irrelevant profile details are never transmitted.
6. **No Fake Fallbacks**: If the server fails to analyze an image or is unreachable, the Android client explicitly informs the user and invites them to log manually.

## Deployment Instructions

### Option 1: Google Cloud Run (Recommended)

1. Clone or copy `backend/`:
   ```bash
   cd backend
   npm install express @google/generative-ai cors helmet express-rate-limit
   ```
2. Build and deploy to Google Cloud Run:
   ```bash
   gcloud run deploy consist-backend \
     --source . \
     --platform managed \
     --region us-central1 \
     --allow-unauthenticated \
     --set-env-vars GEMINI_API_KEY="YOUR_GEMINI_KEY",CONSIST_AUTH_TOKEN="YOUR_SECRET_TOKEN"
   ```
3. Copy the generated HTTPS service URL (e.g., `https://consist-backend-xyz.a.run.app`).

### Option 2: Node.js / Docker Server

```dockerfile
FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install --production
COPY . .
EXPOSE 8080
ENV PORT=8080
CMD ["node", "server.js"]
```

## Android App Configuration

1. In the CONSIST Android app, navigate to **Profile > Manage Privacy & Backend Settings**.
2. Enable **AI Cloud Processing Consent**.
3. Enter your deployed HTTPS backend URL (e.g. `https://consist-backend-xyz.a.run.app`).
4. (Optional) Enter your `CONSIST_AUTH_TOKEN` matching the server's Bearer token.
5. Tap **Save Settings**.
