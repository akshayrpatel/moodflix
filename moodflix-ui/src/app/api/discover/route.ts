import { NextResponse } from "next/server";
import { GATEWAY_URL, getTokenFromCookie } from "@/lib/auth-cookies";

export async function GET() {
  const token = await getTokenFromCookie();
  if (!token) {
    return NextResponse.json({ rows: [] }, { status: 200 });
  }

  let gatewayRes: Response;
  try {
    gatewayRes = await fetch(`${GATEWAY_URL}/api/discover`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
    });
  } catch (err) {
    console.error("[DISCOVER] Gateway unreachable:", err);
    return NextResponse.json({ rows: [] }, { status: 200 });
  }

  if (!gatewayRes.ok) {
    console.error(`[DISCOVER] Failed | status=${gatewayRes.status}`);
    return NextResponse.json({ rows: [] }, { status: 200 });
  }

  try {
    const data = await gatewayRes.json();
    return NextResponse.json(data);
  } catch (err) {
    console.error("[DISCOVER] Invalid response from gateway:", err);
    return NextResponse.json({ rows: [] }, { status: 200 });
  }
}
