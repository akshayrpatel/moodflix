import { cookies } from "next/headers";
import { decodeJwt } from "jose";

export const COOKIE_NAME = "moodflix_token";
export const GATEWAY_URL = process.env.GATEWAY_URL || "http://localhost:8080";

export async function setAuthCookie(token: string) {
  const cookieStore = await cookies();
  const decoded = decodeJwt(token);
  const maxAge = decoded.exp
    ? decoded.exp - Math.floor(Date.now() / 1000)
    : 86400;

  cookieStore.set(COOKIE_NAME, token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: Math.max(maxAge, 0),
  });
}

export async function clearAuthCookie() {
  const cookieStore = await cookies();
  cookieStore.delete(COOKIE_NAME);
}

export async function getTokenFromCookie(): Promise<string | null> {
  const cookieStore = await cookies();
  return cookieStore.get(COOKIE_NAME)?.value ?? null;
}

/**
 * Extracts a user-safe error message from a gateway response.
 *
 * The gateway (Spring WebFlux) returns errors as JSON:
 *   { "status": 401, "error": "Unauthorized", "message": "Invalid credentials", ... }
 *
 * We extract only the "message" field. If parsing fails, we return the
 * fallback — never the raw response body, which could leak internal details
 * like paths, timestamps, or class names.
 */
export async function parseGatewayError(
  res: Response,
  fallback: string,
): Promise<string> {
  try {
    const body = await res.text();
    const json = JSON.parse(body);
    // Spring's "message" field contains the human-readable reason
    if (json.message && typeof json.message === "string") {
      return json.message;
    }
    return fallback;
  } catch {
    return fallback;
  }
}
