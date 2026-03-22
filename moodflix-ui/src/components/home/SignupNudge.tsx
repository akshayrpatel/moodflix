"use client";

import { FC } from "react";
import { X } from "lucide-react";

const GUEST_SEARCH_KEY = "moodflix_guest_searches";
const NUDGE_THRESHOLD = 3;
const GATE_THRESHOLD = 10;

export const getGuestSearchCount = (): number => {
  if (typeof window === "undefined") return 0;
  return parseInt(localStorage.getItem(GUEST_SEARCH_KEY) || "0", 10);
};

export const incrementGuestSearchCount = (): number => {
  if (typeof window === "undefined") return 0;
  const next = getGuestSearchCount() + 1;
  localStorage.setItem(GUEST_SEARCH_KEY, String(next));
  return next;
};

interface Props {
  count: number;
  onDismiss?: () => void;
}

const SignupNudge: FC<Props> = ({ count, onDismiss }) => {
  if (count < NUDGE_THRESHOLD) return null;

  // Full-screen gate at 10 searches — blocks all further searches
  if (count >= GATE_THRESHOLD) {
    return (
      <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/80 backdrop-blur-sm p-6 animate__animated animate__fadeIn">
        <div className="max-w-md w-full bg-white/5 border border-white/20 rounded-2xl backdrop-blur-md shadow-2xl p-10 text-center">
          <h2 className="text-2xl md:text-3xl font-netflix-logo tracking-wide text-white uppercase mb-4">
            You&apos;re hooked
          </h2>
          <p className="text-sm text-zinc-400 leading-relaxed mb-8">
            You&apos;ve used all your free searches. Create an account to keep
            going and unlock recommendations built around your taste.
          </p>
          <div className="flex flex-col items-center gap-4">
            <a
              href="/auth/register"
              className="inline-block text-sm font-medium bg-brand-red hover:bg-brand-dark text-white px-8 py-3 rounded-full uppercase tracking-widest transition-colors"
            >
              Sign Up - It&apos;s Free
            </a>
            <p className="text-xs font-mono tracking-widest text-zinc-500 uppercase">
              Already a user?{" "}
              <a
                href="/auth/login"
                className="text-brand-red hover:text-brand-dark transition-colors"
              >
                Login
              </a>
            </p>
          </div>
        </div>
      </div>
    );
  }

  // Inline nudge between 3 and 10 — same card aesthetic, dismissible
  return (
    <div className="w-full max-w-md mx-auto px-4 animate__animated animate__fadeInUp">
      <div className="relative bg-white/5 border border-white/20 rounded-2xl backdrop-blur-md shadow-2xl p-8 text-center">
        {onDismiss && (
          <button
            onClick={onDismiss}
            className="absolute top-3 right-3 text-zinc-500 hover:text-zinc-300 transition-colors"
            aria-label="Dismiss"
          >
            <X size={16} strokeWidth={1.5} />
          </button>
        )}
        <h3 className="text-lg md:text-xl font-netflix-logo tracking-wide text-white uppercase mb-3">
          Enjoying MoodFlix?
        </h3>
        <p className="text-sm text-zinc-400 leading-relaxed mb-6">
          Sign up to save your picks and tune recommendations to your taste.
        </p>
        <div className="flex flex-col items-center gap-3">
          <a
            href="/auth/register"
            className="inline-block text-sm font-medium bg-brand-red hover:bg-brand-dark text-white px-8 py-3 rounded-full uppercase tracking-widest transition-colors"
          >
            Sign Up - It&apos;s Free
          </a>
          <p className="text-xs font-mono tracking-widest text-zinc-500 uppercase">
            Already a user?{" "}
            <a
              href="/auth/login"
              className="text-brand-red hover:text-brand-dark transition-colors"
            >
              Login
            </a>
          </p>
        </div>
      </div>
    </div>
  );
};

export default SignupNudge;
