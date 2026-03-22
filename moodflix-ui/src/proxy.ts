import { NextRequest, NextResponse } from "next/server";
import { COOKIE_NAME } from "@/lib/auth-cookies";

const PROTECTED_PATHS = ["/onboarding"];
const GUEST_ONLY_PATHS = ["/auth/login", "/auth/register"];

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const token = request.cookies.get(COOKIE_NAME)?.value;

  if (PROTECTED_PATHS.some((p) => pathname.startsWith(p)) && !token) {
    const url = request.nextUrl.clone();
    url.pathname = "/auth/login";
    return NextResponse.redirect(url);
  }

  if (GUEST_ONLY_PATHS.some((p) => pathname.startsWith(p)) && token) {
    const url = request.nextUrl.clone();
    url.pathname = "/dashboard";
    return NextResponse.redirect(url);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/onboarding/:path*", "/auth/login", "/auth/register"],
};
