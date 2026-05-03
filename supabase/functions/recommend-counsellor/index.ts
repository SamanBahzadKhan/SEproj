/**
 * AI counsellor recommendation + LUMS guidance.
 *
 * Secrets: GROQ_API_KEY, FIREBASE_PROJECT_ID, FIREBASE_API_KEY
 *
 * Reads counsellor profiles from Firestore `counselors` (same collection as the Android app).
 * Ensure Firestore rules allow this access path for your deployment (e.g. public read on counsellor directory profiles).
 *
 * Deploy: supabase functions deploy recommend-counsellor
 */

const FALLBACK_ANSWER =
  "I'm having trouble right now. Please try again in a moment, or speak directly with a counsellor at LUMS.";

const CRISIS_KEYWORDS = [
  "suicide",
  "kill myself",
  "self harm",
  "self-harm",
  "end my life",
  "want to die",
  "hurt myself",
  "overdose",
];

const DIAGNOSIS_KEYWORDS = [
  "do i have",
  "diagnose",
  "am i depressed",
  "am i anxious",
  "what disorder",
  "mental illness",
];

function checkSafety(query: string): string | null {
  const lower = query.toLowerCase();

  if (CRISIS_KEYWORDS.some((k) => lower.includes(k))) {
    return "I'm concerned about what you've shared. Please reach out to emergency support immediately. LUMS Counselling: +92-42-3560-8000. Umang helpline (Pakistan): 0317-4288665. You are not alone.";
  }

  if (DIAGNOSIS_KEYWORDS.some((k) => lower.includes(k))) {
    return "I'm not able to diagnose conditions — only a licensed professional can do that. I can help you find the right counsellor at LUMS to speak with. Would you like a recommendation?";
  }

  return null;
}

const LUMS_OFFICES = [
  {
    problems: ["transcript", "degree", "grades", "cgpa", "result", "academic record"],
    office: "Registrar's Office",
    location: "Main Academic Block, Ground Floor",
    contact: "registrar@lums.edu.pk",
  },
  {
    problems: ["fee", "scholarship", "financial aid", "payment", "tuition"],
    office: "Financial Aid & Bursar Office",
    location: "Student Services Block",
    contact: "finaid@lums.edu.pk",
  },
  {
    problems: ["hostel", "dorm", "accommodation", "room", "residential"],
    office: "Residential Life Office",
    location: "Student Affairs Building",
    contact: "residential@lums.edu.pk",
  },
  {
    problems: [
      "counselling",
      "mental health",
      "stress",
      "anxiety",
      "depression",
      "therapy",
      "emotional",
    ],
    office: "CAPs — Counselling & Psychological Services",
    location: "Student Affairs Building, 2nd Floor",
    contact: "caps@lums.edu.pk",
  },
  {
    problems: ["career", "job", "internship", "placement", "cv", "resume"],
    office: "Career Services Office",
    location: "SDSB Building",
    contact: "careers@lums.edu.pk",
  },
  {
    problems: ["harassment", "abuse", "complaint", "discrimination", "misconduct"],
    office: "Office of Student Affairs — Conduct & Discipline",
    location: "Student Affairs Building",
    contact: "studentaffairs@lums.edu.pk",
  },
  {
    problems: ["visa", "international", "foreign student", "immigration"],
    office: "International Students Office",
    location: "Main Academic Block",
    contact: "international@lums.edu.pk",
  },
  {
    problems: ["medical", "sick", "doctor", "health", "clinic", "pharmacy"],
    office: "LUMS Health Centre",
    location: "Near Main Gate",
    contact: "health@lums.edu.pk",
  },
  {
    problems: ["course", "enrollment", "add drop", "withdrawal", "semester"],
    office: "Office of the Registrar — Academic Affairs",
    location: "Main Academic Block",
    contact: "registrar@lums.edu.pk",
  },
  {
    problems: ["library", "book", "research", "journal", "database"],
    office: "Khizra & Waqar Younis Library",
    location: "Central Campus",
    contact: "library@lums.edu.pk",
  },
];

function findLUMSOffice(query: string): (typeof LUMS_OFFICES)[0] | null {
  const lower = query.toLowerCase();
  for (const entry of LUMS_OFFICES) {
    if (entry.problems.some((p) => lower.includes(p))) {
      return entry;
    }
  }
  return null;
}

function docIdFromResourceName(name: string): string {
  const parts = name.split("/");
  return parts[parts.length - 1] ?? "";
}

function parseNumberField(fields: Record<string, unknown>, key: string): number {
  const wrap = fields[key] as Record<string, string | undefined> | undefined;
  if (!wrap) return 0;
  if ("doubleValue" in wrap && wrap.doubleValue != null) {
    return Number(wrap.doubleValue);
  }
  if ("integerValue" in wrap && wrap.integerValue != null) {
    return parseInt(String(wrap.integerValue), 10);
  }
  return 0;
}

function parseString(fields: Record<string, unknown>, key: string): string {
  const wrap = fields[key] as Record<string, string | undefined> | undefined;
  return wrap?.stringValue ?? "";
}

function parseBool(fields: Record<string, unknown>, key: string): boolean {
  const wrap = fields[key] as Record<string, boolean | undefined> | undefined;
  return wrap?.booleanValue === true;
}

/** Safe for LLM prompt — rating may be string/unknown from Firestore or maps. */
function formatRatingOneDecimal(rating: unknown): string {
  const n = Number(rating);
  if (!Number.isFinite(n)) return "0.0";
  return n.toFixed(1);
}

async function fetchCounsellors(
  firebaseProjectId: string,
  firebaseApiKey: string,
): Promise<any[]> {
  if (!firebaseProjectId || !firebaseApiKey) {
    console.warn("Firestore env missing; returning no counsellors");
    return [];
  }

  const url =
    `https://firestore.googleapis.com/v1/projects/${firebaseProjectId}/databases/(default)/documents/counselors?key=${
      encodeURIComponent(firebaseApiKey)
    }`;

  console.log("Fetching counsellors from Firestore...");

  let res: Response;
  try {
    res = await fetch(url);
  } catch (e) {
    console.error("Firestore fetch network error:", e);
    return [];
  }

  let data: any;
  try {
    data = await res.json();
  } catch (e) {
    console.error("Firestore JSON parse error:", e);
    return [];
  }

  if (!res.ok) {
    console.error("Firestore fetch failed:", JSON.stringify(data));
    return [];
  }

  if (!data.documents || !Array.isArray(data.documents)) {
    console.warn("No documents found in counselors collection");
    return [];
  }

  console.log(`Fetched ${data.documents.length} raw documents`);

  const counsellors = data.documents
    .map((doc: { name: string; fields?: Record<string, unknown> }) => {
      const id = docIdFromResourceName(doc.name);
      const f = doc.fields ?? {};
      const role = parseString(f, "role").toUpperCase();
      const deleted = parseBool(f, "isDeleted");
      const accepting = parseBool(f, "isAcceptingClients");
      const rating = parseNumberField(f, "rating");
      const reviewCount = Math.round(parseNumberField(f, "ratingCount"));

      return {
        counselorId: id,
        name: parseString(f, "name") || "Unknown",
        specialization: parseString(f, "specialization"),
        department: parseString(f, "department"),
        rating,
        reviewCount,
        acceptingClients: accepting,
        about: parseString(f, "bio"),
        role,
        isDeleted: deleted,
      };
    })
    .filter((c: any) =>
      !c.isDeleted &&
      c.acceptingClients &&
      (c.role === "COUNSELOR" || c.role === "COUNSELLOR" || c.role === "")
    );

  console.log(`Filtered to ${counsellors.length} active accepting counsellors`);
  return counsellors;
}

function rankCounsellors(query: string, counsellors: any[]): any[] {
  const lower = query.toLowerCase();

  const keywords = [
    "anxiety",
    "stress",
    "depression",
    "trauma",
    "grief",
    "anger",
    "relationship",
    "academic",
    "family",
    "career",
    "sleep",
    "adhd",
    "addiction",
    "self esteem",
    "confidence",
    "loneliness",
    "burnout",
  ];

  const queryKeywords = keywords.filter((k) => lower.includes(k));

  const scored = counsellors.map((c) => {
    let score = 0;
    const specLower = (c.specialization ?? "").toLowerCase();

    for (const kw of queryKeywords) {
      if (specLower.includes(kw)) score += 4;
    }

    const ratingN = Number(c.rating);
    const reviewsN = Number(c.reviewCount);
    score += (Number.isFinite(ratingN) ? ratingN : 0) * 2;
    score += Math.min((Number.isFinite(reviewsN) ? reviewsN : 0) * 0.1, 2);

    return { ...c, score };
  });

  return scored.sort((a, b) => b.score - a.score).slice(0, 3);
}

/** Groq chat completions — never throws; returns fallback text on any failure. */
async function callGroq(prompt: string, groqApiKey: string): Promise<string> {
  const key = groqApiKey?.trim();
  if (!key) {
    console.warn("GROQ_API_KEY missing");
    return FALLBACK_ANSWER;
  }

  try {
    const res = await fetch("https://api.groq.com/openai/v1/chat/completions", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${key}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        // llama3-8b-8192 was retired; see https://console.groq.com/docs/deprecations
        model: "llama-3.1-8b-instant",
        messages: [
          {
            role: "system",
            content:
              "You are a safe university counselling assistant. You must never diagnose or give medical advice.",
          },
          {
            role: "user",
            content: prompt,
          },
        ],
        temperature: 0.3,
        max_completion_tokens: 300,
      }),
    });

    let data: any;
    try {
      data = await res.json();
    } catch {
      console.error("Groq response not JSON, status:", res.status);
      return FALLBACK_ANSWER;
    }

    if (!res.ok) {
      console.error("Groq API error:", res.status, JSON.stringify(data).slice(0, 800));
      return FALLBACK_ANSWER;
    }

    const content = data?.choices?.[0]?.message?.content;
    if (typeof content !== "string" || !content.trim()) {
      console.error("Groq unexpected payload:", JSON.stringify(data).slice(0, 800));
      return FALLBACK_ANSWER;
    }

    return content.trim();
  } catch (e) {
    console.error("Groq request failed:", e);
    return FALLBACK_ANSWER;
  }
}

function filterResponse(response: string): string {
  const bannedPhrases = [
    "you have",
    "you are diagnosed",
    "you suffer from",
    "you should take medication",
    "prescription",
    "you definitely have",
  ];

  const lower = response.toLowerCase();
  if (bannedPhrases.some((p) => lower.includes(p))) {
    return "I can help you find the right support at LUMS. Based on what you've shared, I'd recommend speaking with one of our counsellors. Would you like me to suggest someone?";
  }

  return response;
}

function jsonSuccess(body: Record<string, unknown>): Response {
  return Response.json(body, {
    status: 200,
    headers: { "Access-Control-Allow-Origin": "*" },
  });
}

Deno.serve(async (req) => {
  console.log("FUNCTION ENTERED");

  const GROQ_API_KEY = Deno.env.get("GROQ_API_KEY") ?? "";
  const FIREBASE_PROJECT_ID = Deno.env.get("FIREBASE_PROJECT_ID") ?? "";
  const FIREBASE_API_KEY = Deno.env.get("FIREBASE_API_KEY") ?? "";

  console.log("ENV CHECK:", {
    hasGroq: !!GROQ_API_KEY,
    hasFbProject: !!FIREBASE_PROJECT_ID,
    hasFbKey: !!FIREBASE_API_KEY,
  });

  if (req.method === "OPTIONS") {
    return new Response("ok", {
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Headers":
          "authorization, x-client-info, apikey, content-type",
      },
    });
  }

  try {
    let body: Record<string, unknown>;
    try {
      body = await req.json() as Record<string, unknown>;
    } catch {
      return jsonSuccess({
        answer: "Please send a valid JSON body with a \"query\" field.",
        isCrisis: false,
        recommendedCounsellors: [],
        officeInfo: null,
      });
    }

    const query = String(body.query ?? "").trim();
    console.log("Query received:", query);

    if (!query) {
      return jsonSuccess({
        answer: "Please ask me something and I'll do my best to help.",
        isCrisis: false,
        recommendedCounsellors: [],
        officeInfo: null,
      });
    }

    const safetyResponse = checkSafety(query);
    if (safetyResponse) {
      console.log("Safety/diagnosis rule matched, returning fixed response");
      return jsonSuccess({
        answer: safetyResponse,
        isCrisis: true,
        recommendedCounsellors: [],
        officeInfo: null,
      });
    }

    const office = findLUMSOffice(query);
    console.log("Office match:", office?.office ?? "none");

    const counsellors = await fetchCounsellors(FIREBASE_PROJECT_ID, FIREBASE_API_KEY);
    const topCounsellors = rankCounsellors(query, counsellors);
    console.log("Top counsellors:", topCounsellors.map((c: any) => c.name));

    const systemPrompt = `<s>[INST] You are a supportive university counselling assistant at LUMS.

STRICT RULES:
- Never diagnose any condition
- Never give medical advice
- Never recommend medication
- If someone seems in crisis, refer to professional help only
- Be warm, empathetic, and concise
- Maximum 150 words in your response
- Only recommend counsellors from the provided data

User said: "${query}"

${
      office
        ? `Relevant LUMS office:
Name: ${office.office}
Location: ${office.location}
Contact: ${office.contact}`
        : ""
    }

${
      topCounsellors.length > 0
        ? `Available counsellors that may help:
${
          topCounsellors.map((c, i) =>
            `${i + 1}. ${c.name} — Specializes in: ${c.specialization}, Rating: ${
              formatRatingOneDecimal(c.rating)
            }/5 (${c.reviewCount} reviews)`
          ).join("\n")
        }`
        : "No counsellors currently available matching this query."
    }

Give a warm, helpful response. If recommending a counsellor, briefly explain why they match. Do not invent information. [/INST]`;

    const llmRaw = await callGroq(systemPrompt, GROQ_API_KEY);
    const llmResponse = filterResponse(llmRaw);

    return jsonSuccess({
      answer: llmResponse,
      recommendedCounsellors: topCounsellors.map((c: any) => ({
        counselorId: String(c.counselorId ?? ""),
        name: String(c.name ?? ""),
        specialization: String(c.specialization ?? ""),
        rating: Number.isFinite(Number(c.rating)) ? Number(c.rating) : 0,
        reviewCount: Math.round(Number.isFinite(Number(c.reviewCount)) ? Number(c.reviewCount) : 0),
      })),
      officeInfo: office ?? null,
      isCrisis: false,
    });
  } catch (error) {
    const errMsg = error instanceof Error ? error.message : String(error);
    const errStack = error instanceof Error ? error.stack : "";
    console.error("Unhandled error:", errMsg, errStack, error);
    return jsonSuccess({
      answer: FALLBACK_ANSWER,
      isCrisis: false,
      recommendedCounsellors: [],
      officeInfo: null,
    });
  }
});
