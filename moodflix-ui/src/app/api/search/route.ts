import { NextRequest, NextResponse } from "next/server";
import { GATEWAY_URL, getTokenFromCookie } from "@/lib/auth-cookies";

export async function POST(request: NextRequest) {
  let body: { query?: string };
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ results: [] }, { status: 200 });
  }

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };

  // If user is logged in, forward JWT so gateway can personalize results
  const token = await getTokenFromCookie();
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  let gatewayRes: Response;
  try {
    gatewayRes = await fetch(`${GATEWAY_URL}/api/search`, {
      method: "POST",
      headers,
      body: JSON.stringify({ query: body.query ?? "" }),
    });
  } catch (err) {
    // Gateway is down — return empty results instead of crashing.
    // Search is non-critical; the user can retry.
    console.error("[SEARCH] Gateway unreachable:", err);
    return NextResponse.json({ results: [] }, { status: 200 });
  }

  if (!gatewayRes.ok) {
    console.error(
      `[SEARCH] Failed | status=${gatewayRes.status} query="${body.query}"`,
    );
    return NextResponse.json({ results: [] }, { status: 200 });
  }

  try {
    const data = await gatewayRes.json();
    return NextResponse.json(data);
  } catch (err) {
    console.error("[SEARCH] Invalid response from gateway:", err);
    return NextResponse.json({ results: [] }, { status: 200 });
  }
}
