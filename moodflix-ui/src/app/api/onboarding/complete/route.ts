import { NextRequest, NextResponse } from "next/server";
import {
  GATEWAY_URL,
  getTokenFromCookie,
  clearAuthCookie,
  parseGatewayError,
} from "@/lib/auth-cookies";

export async function POST(request: NextRequest) {
  const token = await getTokenFromCookie();
  if (!token) {
    return NextResponse.json(
      { error: "Please sign in to continue" },
      { status: 401 },
    );
  }

  // 1. Parse request body
  let body: Record<string, unknown>;
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ error: "Invalid request" }, { status: 400 });
  }

  // 2. Forward to gateway
  let gatewayRes: Response;
  try {
    gatewayRes = await fetch(`${GATEWAY_URL}/onboarding/complete`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(body),
    });
  } catch (err) {
    console.error("[ONBOARDING] Gateway unreachable:", err);
    return NextResponse.json(
      { error: "Something went wrong. Please try again." },
      { status: 503 },
    );
  }

  // 3. Handle gateway errors
  if (!gatewayRes.ok) {
    const message = await parseGatewayError(gatewayRes, "Onboarding failed");
    console.error(
      `[ONBOARDING] Failed | status=${gatewayRes.status} message="${message}"`,
    );

    // 401 = token expired or invalid during onboarding
    if (gatewayRes.status === 401) {
      await clearAuthCookie();
      return NextResponse.json(
        { error: "Your session has expired. Please sign in again." },
        { status: 401 },
      );
    }

    return NextResponse.json(
      { error: "Something went wrong. Please try again." },
      { status: 502 },
    );
  }

  return NextResponse.json({ success: true });
}
