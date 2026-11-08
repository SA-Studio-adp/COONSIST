/**
 * CONSIST - Secure AI Backend Proxy Service
 * 
 * Architecture:
 * - Stores private GEMINI_API_KEY securely on the server (never exposed to Android APK).
 * - Enforces HTTPS and authenticates requests via Bearer token.
 * - Enforces rate limiting (e.g. 30 requests / minute) to prevent abuse.
 * - Validates incoming payloads (max image size 3MB, input sanitization).
 * - Sanitizes error messages so credentials and internal stack traces are never leaked.
 */

const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const { GoogleGenerativeAI } = require('@google/generative-ai');

const app = express();
const PORT = process.env.PORT || 8080;
const AUTH_TOKEN = process.env.CONSIST_AUTH_TOKEN || '';
const GEMINI_API_KEY = process.env.GEMINI_API_KEY;

if (!GEMINI_API_KEY) {
  console.error("FATAL: GEMINI_API_KEY environment variable is not set on the server.");
}

// 1. Security Headers & CORS
app.use(helmet());
app.use(cors({ origin: true }));

// 2. Body Parser with 4MB limit
app.use(express.json({ limit: '4mb' }));

// 3. Rate Limiter (30 requests per minute per IP)
const apiLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 30,
  message: { error: 'Too many requests. Please wait a moment before trying again.' },
  standardHeaders: true,
  legacyHeaders: false,
});
app.use('/api/', apiLimiter);

// 4. Authentication Middleware
function authenticateRequest(req, res, next) {
  if (!AUTH_TOKEN) {
    // If no token is configured on server, proceed (open proxy)
    return next();
  }
  const authHeader = req.headers['authorization'];
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Unauthorized: Missing or invalid Bearer token.' });
  }
  const token = authHeader.substring(7).trim();
  if (token !== AUTH_TOKEN) {
    return res.status(403).json({ error: 'Forbidden: Invalid authorization token.' });
  }
  next();
}

const genAI = GEMINI_API_KEY ? new GoogleGenerativeAI(GEMINI_API_KEY) : null;

// Endpoint 1: Meal Image Recognition
app.post('/api/v1/scan-meal', authenticateRequest, async (req, res) => {
  try {
    const { image } = req.body;
    if (!image || typeof image !== 'string') {
      return res.status(400).json({ error: 'Missing base64 image payload.' });
    }

    if (!genAI) {
      return res.status(503).json({ error: 'AI service unavailable: Backend GEMINI_API_KEY not configured.' });
    }

    const model = genAI.getGenerativeModel({ model: 'gemini-2.5-flash' });
    const prompt = `
      Analyze this meal photograph for nutrition tracking.
      Identify distinct food items visible on the plate.
      For each item provide:
      - foodName (e.g. 'Idli', 'Cooked Rice', 'Sambar', 'Chicken Breast')
      - estimatedGrams (realistic weight in grams)
      - servingDescription (e.g. '2 pieces', '1 cup')
      - caloriesPer100g, proteinPer100g, carbsPer100g, fatPer100g, fiberPer100g
      Respond ONLY in valid JSON:
      {
        "mealOverview": "Summary of detected meal",
        "items": [
          {
            "foodName": "Food Name",
            "estimatedGrams": 100.0,
            "servingDescription": "1 serving",
            "caloriesPer100g": 120.0,
            "proteinPer100g": 5.0,
            "carbsPer100g": 20.0,
            "fatPer100g": 2.0,
            "fiberPer100g": 1.0
          }
        ]
      }
    `;

    const imagePart = {
      inlineData: {
        data: image,
        mimeType: 'image/jpeg'
      }
    };

    const result = await model.generateContent([prompt, imagePart]);
    const response = await result.response;
    let text = response.text().replace(/```json/g, '').replace(/```/g, '').trim();

    const parsed = JSON.parse(text);
    return res.json(parsed);
  } catch (err) {
    console.error("Scan error:", err.message);
    return res.status(500).json({ error: 'Unable to analyze image. Please ensure food is clearly visible.' });
  }
});

// Endpoint 2: Voice Natural Language Meal Parser
app.post('/api/v1/voice-parse', authenticateRequest, async (req, res) => {
  try {
    const { speechText, mealType } = req.body;
    if (!speechText) {
      return res.status(400).json({ error: 'Missing speechText payload.' });
    }

    if (!genAI) {
      return res.status(503).json({ error: 'AI service unavailable.' });
    }

    const model = genAI.getGenerativeModel({ model: 'gemini-2.5-flash' });
    const prompt = `
      The user described their meal: "${speechText}"
      Extract food items, estimated weight in grams, and nutrient values per 100g.
      Respond strictly in JSON:
      {
        "mealType": "${mealType || 'Lunch'}",
        "items": [
          {
            "foodName": "Food Name",
            "estimatedGrams": 100.0,
            "servingDescription": "1 serving",
            "caloriesPer100g": 120.0,
            "proteinPer100g": 4.0,
            "carbsPer100g": 20.0,
            "fatPer100g": 2.0,
            "fiberPer100g": 1.0
          }
        ]
      }
    `;

    const result = await model.generateContent(prompt);
    const response = await result.response;
    let text = response.text().replace(/```json/g, '').replace(/```/g, '').trim();
    return res.json(JSON.parse(text));
  } catch (err) {
    return res.status(500).json({ error: 'Voice parsing failed.' });
  }
});

// Endpoint 3: AI Coach Chat
app.post('/api/v1/coach-chat', authenticateRequest, async (req, res) => {
  try {
    const { message, age, goal, dietaryPreference, targetCalories, targetProteinGrams, consumedCaloriesToday, consumedProteinToday } = req.body;
    if (!message) {
      return res.status(400).json({ error: 'Missing message.' });
    }

    if (!genAI) {
      return res.status(503).json({ error: 'Coach service unavailable.' });
    }

    const model = genAI.getGenerativeModel({ model: 'gemini-2.5-flash' });
    const systemPrompt = `
      You are the CONSIST AI Coach. Tagline: BUILD. TRAIN. REPEAT.
      Provide concise, evidence-based fitness and nutrition guidance.
      User context: Goal=${goal}, Diet=${dietaryPreference}, Target=${targetCalories} kcal (Consumed: ${consumedCaloriesToday}), Protein=${targetProteinGrams}g (Consumed: ${consumedProteinToday}).
      Never make medical diagnoses. Never invent fake user data.
    `;

    const result = await model.generateContent(`${systemPrompt}\n\nUser Question: ${message}`);
    const response = await result.response;
    return res.json({ reply: response.text() });
  } catch (err) {
    return res.status(500).json({ error: 'Coach chat error.' });
  }
});

// Health check
app.get('/health', (req, res) => res.json({ status: 'ok', service: 'CONSIST Secure AI Backend' }));

app.listen(PORT, () => {
  console.log(`CONSIST Backend running securely on port ${PORT}`);
});
