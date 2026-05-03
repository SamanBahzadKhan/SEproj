/// <reference types="https://esm.sh/@supabase/functions-js/src/edge-runtime.d.ts" />

/**
 * Google Meet via Calendar API (service account JWT).
 *
 * Secrets (Supabase Dashboard → Project Settings → Edge Functions → Secrets):
 * - GOOGLE_CLIENT_EMAIL — service account email (e.g. meet-service@....iam.gserviceaccount.com)
 * - GOOGLE_PRIVATE_KEY — full PEM; may be pasted as one line with \n escapes (handled below)
 * - CALENDAR_ID — calendar id (often your Gmail: cs360proj@gmail.com)
 */

import { decode as decodeBase64 } from "https://deno.land/std@0.224.0/encoding/base64.ts";

function normalizePrivateKeyPem(raw: string | undefined): string {
  if (!raw || !raw.trim()) {
    throw new Error(
      "GOOGLE_PRIVATE_KEY is missing. Add it under Edge Function secrets.",
    );
  }
  let s = raw.trim();
  if (
    (s.startsWith("\"") && s.endsWith("\"")) ||
    (s.startsWith("'") && s.endsWith("'"))
  ) {
    s = s.slice(1, -1).trim();
  }
  // Dashboard / JSON paste often stores newlines as the two characters \ and n
  if (s.includes("\\n")) {
    s = s.replace(/\\n/g, "\n");
  }
  return s;
}

function pemToArrayBuffer(pem: string): ArrayBuffer {
  const b64 = pem
    .replace(/-----BEGIN RSA PRIVATE KEY-----/g, "")
    .replace(/-----END RSA PRIVATE KEY-----/g, "")
    .replace(/-----BEGIN PRIVATE KEY-----/g, "")
    .replace(/-----END PRIVATE KEY-----/g, "")
    .replace(/\r?\n/g, "")
    .replace(/\s/g, "");

  try {
    const bytes = decodeBase64(b64);
    return bytes.buffer.slice(
      bytes.byteOffset,
      bytes.byteOffset + bytes.byteLength,
    );
  } catch {
    throw new Error(
      "GOOGLE_PRIVATE_KEY could not be decoded. Paste the full PEM from the service account JSON " +
        "(private_key field), or ensure escaped \\n newlines are preserved.",
    );
  }
}

function base64UrlEncode(input: string | ArrayBuffer): string {
  const str = typeof input === "string"
    ? input
    : String.fromCharCode(...new Uint8Array(input));

  return btoa(str)
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}

async function createJWT(): Promise<string> {
  const pemRaw = Deno.env.get("GOOGLE_PRIVATE_KEY");
  const pem = normalizePrivateKeyPem(pemRaw ?? undefined);

  const header = {
    alg: "RS256",
    typ: "JWT",
  };

  const now = Math.floor(Date.now() / 1000);

  const payload = {
    iss: Deno.env.get("GOOGLE_CLIENT_EMAIL"),
    scope: "https://www.googleapis.com/auth/calendar",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  };

  if (!payload.iss) {
    throw new Error("GOOGLE_CLIENT_EMAIL secret is missing.");
  }

  const encoder = new TextEncoder();

  const unsignedJWT =
    base64UrlEncode(JSON.stringify(header)) + "." +
    base64UrlEncode(JSON.stringify(payload));

  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToArrayBuffer(pem),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );

  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    encoder.encode(unsignedJWT),
  );

  return unsignedJWT + "." + base64UrlEncode(signature);
}

async function getAccessToken(): Promise<string> {
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: await createJWT(),
    }),
  });

  const data = await res.json();
  if (!res.ok) {
    console.error("oauth token error", data);
    throw new Error(
      typeof data.error_description === "string"
        ? data.error_description
        : (data.error ?? "OAuth token request failed"),
    );
  }
  return data.access_token as string;
}

/** Canonical Meet extraction: hangoutLink first, then conferenceData.entryPoints (video preferred). */
function extractMeetLink(event: Record<string, unknown>): string | null {
  const hangout = event["hangoutLink"];
  if (typeof hangout === "string" && hangout.length > 0) {
    return hangout;
  }

  const cd = event["conferenceData"] as Record<string, unknown> | undefined;
  const eps = cd?.["entryPoints"] as
    | Array<{ entryPointType?: string; uri?: string }>
    | undefined;

  if (eps && eps.length > 0) {
    const video = eps.find((e) => e.entryPointType === "video") ?? eps[0];
    if (video?.uri && typeof video.uri === "string") {
      return video.uri;
    }
  }

  return null;
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, {
      status: 204,
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Headers":
          "authorization, x-client-info, apikey, content-type",
      },
    });
  }

  try {
    const { studentEmail, counselorEmail, start, end } = await req.json();

    const accessToken = await getAccessToken();
    const calendarId = Deno.env.get("CALENDAR_ID");
    if (!calendarId) {
      throw new Error("CALENDAR_ID secret is missing.");
    }

    // Query params (Calendar API): conference creation + attendee notifications for Meet conference.
    const calendarUrl =
      `https://www.googleapis.com/calendar/v3/calendars/${
        encodeURIComponent(calendarId)
      }/events?conferenceDataVersion=1&sendUpdates=all`;

    const eventBody = {
      summary: "Counseling Session",
      start: { dateTime: start },
      end: { dateTime: end },
      attendees: [
        { email: studentEmail },
        { email: counselorEmail },
      ],
      conferenceData: {
        createRequest: {
          requestId: crypto.randomUUID(),
          conferenceSolutionKey: {
            type: "hangoutsMeet",
          },
        },
      },
    };

    const res = await fetch(calendarUrl, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(eventBody),
    });

    const data = (await res.json()) as Record<string, unknown>;

    console.log("CALENDAR RESPONSE:", JSON.stringify(data, null, 2));

    if (!res.ok) {
      console.error("calendar insert error", data);
      return Response.json(
        {
          error: (data.error as { message?: string } | undefined)?.message ??
            "Calendar API error",
          details: data,
        },
        { status: 502 },
      );
    }

    const event = data;
    const meetLink = extractMeetLink(event);
    const calendarEventId = typeof event.id === "string" ? event.id : undefined;

    if (!meetLink) {
      console.warn(
        "Event created but no Meet link — check conferenceData, calendar Meet support, and SA permissions.",
        { calendarEventId },
      );
      return Response.json(
        {
          meetLink: null,
          error:
            "Calendar accepted the event but returned no Meet link. Ensure this calendar supports Google Meet, the service account can edit it, and conferenceData appears in logs.",
          calendarEventId,
          conferenceData: event.conferenceData,
        },
        {
          status: 200,
          headers: { "Access-Control-Allow-Origin": "*" },
        },
      );
    }

    return Response.json(
      { meetLink, calendarEventId },
      {
        headers: { "Access-Control-Allow-Origin": "*" },
      },
    );
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e);
    console.error(msg);
    return Response.json({ error: msg }, { status: 500 });
  }
});
