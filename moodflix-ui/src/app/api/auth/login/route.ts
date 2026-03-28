import { NextRequest, NextResponse } from "next/server";
import {
  GATEWAY_URL,
  setAuthCookie,
  parseGatewayError,
} from "@/lib/auth-cookies";

export async function POST(request: NextRequest) {
  // 1. Parse request body — fails if body is empty or not JSON
  let body: { username?: string; password?: string };
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ error: "Invalid request" }, { status: 400 });
  }

  // 2. Basic field presence check (gateway would reject too, but
  //    we fail fast to avoid an unnecessary round-trip)
  if (!body.username?.trim() || !body.password) {
    return NextResponse.json(
      { error: "Username and password are required" },
      { status: 400 },
    );
  }

  // 3. Forward to gateway
  let gatewayRes: Response;
  try {
    gatewayRes = await fetch(`${GATEWAY_URL}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
  } catch (err) {
    // Gateway is down, DNS failed, connection refused, etc.
    console.error("[LOGIN] Gateway unreachable:", err);
    return NextResponse.json(
      { error: "Unable to reach authentication service. Please try again." },
      { status: 503 },
    );
  }

  // 4. Handle gateway error responses (401 bad creds, 400 validation, etc.)
  if (!gatewayRes.ok) {
    const message = await parseGatewayError(gatewayRes, "Login failed");
    console.error(
      `[LOGIN] Failed | status=${gatewayRes.status} message="${message}"`,
    );
    // For 401 (bad credentials), return 401 so the frontend knows it's an auth error.
    // For anything else (500, 400), return a generic message — don't expose internals.
    if (gatewayRes.status === 401) {
      return NextResponse.json(
        { error: "Incorrect username or password" },
        { status: 401 },
      );
    }
    return NextResponse.json(
      { error: "Something went wrong. Please try again." },
      { status: 502 },
    );
  }

  // 5. Set the httpOnly cookie with the JWT
  try {
    const token = await gatewayRes.text();
    await setAuthCookie(token);
  } catch (err) {
    // Gateway returned a 200 but the body isn't a valid JWT
    console.error("[LOGIN] Failed to set auth cookie:", err);
    return NextResponse.json(
      { error: "Something went wrong. Please try again." },
      { status: 500 },
    );
  }

  return NextResponse.json({ success: true });
}
