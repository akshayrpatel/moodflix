import { NextRequest, NextResponse } from "next/server";
import { GATEWAY_URL, getTokenFromCookie } from "@/lib/auth-cookies";

export async function POST(request: NextRequest) {
  let body: unknown;
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ results: [] }, { status: 200 });
  }

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };

  const token = await getTokenFromCookie();
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  let gatewayRes: Response;
  try {
    gatewayRes = await fetch(`${GATEWAY_URL}/api/search/batch`, {
      method: "POST",
      headers,
      body: JSON.stringify(body),
    });
  } catch (err) {
    console.error("[SEARCH/BATCH] Gateway unreachable:", err);
    return NextResponse.json({ results: [] }, { status: 200 });
  }

  if (!gatewayRes.ok) {
    console.error(`[SEARCH/BATCH] Failed | status=${gatewayRes.status}`);
    return NextResponse.json({ results: [] }, { status: 200 });
  }

  try {
    const data = await gatewayRes.json();
    return NextResponse.json(data);
  } catch (err) {
    console.error("[SEARCH/BATCH] Invalid response from gateway:", err);
    return NextResponse.json({ results: [] }, { status: 200 });
  }
}
