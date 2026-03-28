import { NextRequest, NextResponse } from "next/server";
import { GATEWAY_URL, parseGatewayError } from "@/lib/auth-cookies";

export async function POST(request: NextRequest) {
  // 1. Parse request body
  let body: { username?: string; email?: string; password?: string };
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ error: "Invalid request" }, { status: 400 });
  }

  // 2. Field validation — fail fast before hitting the gateway
  if (!body.username?.trim() || !body.email?.trim() || !body.password) {
    return NextResponse.json(
      { error: "All fields are required" },
      { status: 400 },
    );
  }

  // 3. Forward to gateway
  let gatewayRes: Response;
  try {
    gatewayRes = await fetch(`${GATEWAY_URL}/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
  } catch (err) {
    console.error("[REGISTER] Gateway unreachable:", err);
    return NextResponse.json(
      { error: "Unable to reach registration service. Please try again." },
      { status: 503 },
    );
  }

  // 4. Handle gateway errors
  if (!gatewayRes.ok) {
    const message = await parseGatewayError(gatewayRes, "Registration failed");
    console.error(
      `[REGISTER] Failed | status=${gatewayRes.status} message="${message}"`,
    );

    // 409 = duplicate username or email — user can fix this
    if (gatewayRes.status === 409) {
      return NextResponse.json(
        { error: "An account with that username or email already exists" },
        { status: 409 },
      );
    }

    return NextResponse.json(
      { error: "Something went wrong. Please try again." },
      { status: 502 },
    );
  }

  return NextResponse.json({ success: true });
}
