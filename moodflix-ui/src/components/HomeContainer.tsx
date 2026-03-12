"use client";

import { FC, useState } from "react";
import { useRouter } from "next/navigation";
import { MessageCircle, Sparkles, Fingerprint } from "lucide-react";

import MoodFlixLogo from "./logo/MoodflixLogo";
import SearchSection from "./search/SearchSection";
import { useAuth } from "@/hooks/useAuth";
import { authService } from "@/services/auth";

const HomeContainer: FC = () => {
  const router = useRouter();
  const { authenticated, isLoading, mutate } = useAuth();
  const [query, setQuery] = useState("");

  const handleSearch = (val: string) => {
    const trimmed = val.trim();
    if (!trimmed) return;
    sessionStorage.setItem("moodflix_pending_query", trimmed);
    router.push("/dashboard");
  };

  const handleSignOut = async () => {
    await authService.logout();
    mutate();
    router.refresh();
  };

  return (
    <div className="flex flex-col w-full min-h-screen min-w-xl bg-background">
      {/* Auth nav */}
      {!isLoading &&
        (authenticated ? (
          <div className="absolute top-0 right-0 z-50 flex items-center gap-3 p-6">
            <a
              href="/dashboard"
              className="text-sm font-medium text-zinc-300 hover:text-white transition-colors"
            >
              Dashboard
            </a>
            <button
              onClick={handleSignOut}
              className="text-sm font-medium bg-white/10 hover:bg-white/20 border border-white/20 text-white px-5 py-2 rounded-full uppercase tracking-widest transition-colors"
            >
              Sign out
            </button>
          </div>
        ) : (
          <div className="absolute top-0 right-0 z-50 flex items-center gap-3 p-6">
            <a
              href="/auth/login"
              className="text-sm font-medium text-zinc-300 hover:text-white transition-colors"
            >
              Login
            </a>
            <a
              href="/auth/register"
              className="text-sm font-medium bg-brand-red hover:bg-brand-dark text-white px-5 py-2 rounded-full transition-colors"
            >
              Sign Up
            </a>
          </div>
        ))}

      <div className="relative w-full pt-40 pb-20 overflow-hidden">
        <div
          className="absolute inset-0 z-0 opacity-20 pointer-events-none"
          style={{
            backgroundImage: `url('/images/hero-bg.jpg')`,
            backgroundSize: "cover",
            backgroundPosition: "center",
          }}
        />
        <div className="absolute inset-0 z-0 bg-linear-to-b from-transparent via-background/50 to-background" />

        <div className="relative z-10">
          <MoodFlixLogo />
          <SearchSection
            currentQuery={query}
            onQueryChange={setQuery}
            onTriggerSearch={handleSearch}
            isSearching={false}
          />
        </div>
      </div>

      {/* Value Proposition Section */}
      <div className="w-full max-w-4xl mx-auto px-6 py-20">
        <div className="text-center mb-12">
          <h2 className="text-2xl md:text-3xl font-netflix-logo tracking-wide text-white uppercase">
            Not sure what to watch?
          </h2>
          <p className="text-sm md:text-base text-zinc-500 mt-3 max-w-lg mx-auto">
            Skip the endless scrolling. Just tell us how you feel — we'll handle
            the rest.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
          <div
            className="bg-white/5 border border-white/20 rounded-2xl backdrop-blur-md shadow-2xl p-8 text-center animate__animated animate__fadeInUp"
            style={{
              animationDelay: `0s`,
              animationFillMode: "both",
            }}
          >
            <div className="text-3xl mb-4">
              <MessageCircle
                className="mx-auto mb-4 text-zinc-400"
                size={28}
                strokeWidth={1.5}
              />
            </div>
            <h3 className="text-sm font-mono tracking-[0.3em] text-zinc-400 uppercase mb-3 h-10 flex items-center justify-center">
              Describe Your Mood
            </h3>
            <p className="text-sm text-zinc-500 leading-relaxed">
              Type something like &quot;I want something cozy&quot; or
              &quot;feeling like a villain&quot; — no need for titles or genres.
            </p>
          </div>

          <div
            className="bg-white/5 border border-white/20 rounded-2xl backdrop-blur-md shadow-2xl p-8 text-center animate__animated animate__fadeInUp"
            style={{
              animationDelay: `0.1s`,
              animationFillMode: "both",
            }}
          >
            <div className="text-3xl mb-4">
              <Sparkles
                className="mx-auto mb-4 text-zinc-400"
                size={28}
                strokeWidth={1.5}
              />
            </div>
            <h3 className="text-sm font-mono tracking-[0.3em] text-zinc-400 uppercase mb-3 h-10 flex items-center justify-center">
              Get AI Matches
            </h3>
            <p className="text-sm text-zinc-500 leading-relaxed">
              Our engine maps your vibe to movies using mood analysis and
              semantic search — not just keywords.
            </p>
          </div>

          <div
            className="bg-white/5 border border-white/20 rounded-2xl backdrop-blur-md shadow-2xl p-8 text-center animate__animated animate__fadeInUp"
            style={{
              animationDelay: `0.2s`,
              animationFillMode: "both",
            }}
          >
            <div className="text-3xl mb-4">
              <Fingerprint
                className="mx-auto mb-4 text-zinc-400"
                size={28}
                strokeWidth={1.5}
              />
            </div>
            <h3 className="text-sm font-mono tracking-[0.3em] text-zinc-400 uppercase mb-3 h-10 flex items-center justify-center">
              Build Your Taste
            </h3>
            <p className="text-sm text-zinc-500 leading-relaxed">
              Sign up and the more you search, the better it gets. Your results
              become uniquely yours over time.
            </p>
          </div>
        </div>
      </div>

      {/* Footer */}
      <div className="w-full mt-auto pt-24 pb-16 text-center border-t border-white/5">
        <p className="text-2xl md:text-3xl font-netflix-logo tracking-wide text-white uppercase">
          Your next favorite movie
        </p>
        <p className="text-sm font-mono tracking-[0.4em] text-zinc-500 uppercase mt-2">
          is a mood away
        </p>

        {!isLoading && !authenticated && (
          <a
            href="/auth/register"
            className="inline-block mt-8 text-sm font-medium bg-brand-red hover:bg-brand-dark text-white px-8 py-3 rounded-full uppercase tracking-widest transition-colors"
          >
            Get Started
          </a>
        )}

        <p className="text-xs text-zinc-400 mt-10 font-mono tracking-wider">
          MoodFlix · Built with AI · 2026
        </p>
      </div>
    </div>
  );
};

export default HomeContainer;
