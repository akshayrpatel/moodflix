import { NextResponse } from "next/server";
import { clearAuthCookie } from "@/lib/auth-cookies";

export async function POST() {
  try {
    await clearAuthCookie();
  } catch (err) {
    // Cookie deletion failed — log but don't block the logout.
    // The cookie will expire naturally via maxAge.
    console.error("[LOGOUT] Failed to clear cookie:", err);
  }
  return NextResponse.json({ success: true });
}
