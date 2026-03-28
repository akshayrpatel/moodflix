import { NextResponse } from "next/server";
import { decodeJwt } from "jose";
import { getTokenFromCookie, clearAuthCookie } from "@/lib/auth-cookies";

export async function GET() {
  try {
    const token = await getTokenFromCookie();

    if (!token) {
      return NextResponse.json({ authenticated: false });
    }

    const payload = decodeJwt(token);
    const now = Math.floor(Date.now() / 1000);

    if (payload.exp && payload.exp < now) {
      // Token expired — clean up the stale cookie so the browser
      // stops sending it on every request
      await clearAuthCookie();
      return NextResponse.json({ authenticated: false });
    }

    return NextResponse.json({
      authenticated: true,
      username: payload.sub,
    });
  } catch (err) {
    // Malformed token — clear it and treat as unauthenticated
    console.error("[AUTH/ME] Failed to decode token:", err);
    await clearAuthCookie();
    return NextResponse.json({ authenticated: false });
  }
}
