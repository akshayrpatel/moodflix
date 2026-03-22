"use client";

import ContentRow from "@/components/content/ContentRow";
import SignupNudge, {
  getGuestSearchCount,
  incrementGuestSearchCount,
} from "@/components/home/SignupNudge";
import ValueProposition from "@/components/home/ValueProposition";
import MoodFlixLogo from "@/components/logo/MoodflixLogo";
import LumiSection from "@/components/lumi/LumiSection";
import SearchSection from "@/components/search/SearchSection";
import { useAuth } from "@/hooks/useAuth";
import { useDiscover } from "@/hooks/useDiscover";
import { useSearch } from "@/hooks/useSearch";
import { authService } from "@/services/auth";
import { ContentItem } from "@/types";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { FC, useEffect, useState } from "react";

const FALLBACK_ROWS = [
  { key: "vibe", title: "Your Vibe Tonight" },
  { key: "because", title: "Because You Watched" },
  { key: "people", title: "People Like You Enjoyed" },
  { key: "hidden", title: "Hidden Gems For You" },
];

const DashboardPage: FC = () => {
  const router = useRouter();
  const { authenticated, isLoading: authLoading, mutate } = useAuth();
  const { query, syncQuery, triggerSearch, searchStatus, searchResults } =
    useSearch();
  const { rows: discoverRows, isLoading: discoverLoading } = useDiscover();

  const [guestCount, setGuestCount] = useState<number>(() =>
    getGuestSearchCount(),
  );
  const [nudgeDismissed, setNudgeDismissed] = useState(false);
  const [bootstrapped, setBootstrapped] = useState(false);

  const showLumi = searchResults.length > 0 && searchStatus === "completed";
  const lumiItem: ContentItem =
    searchResults.find(
      (item) => item.analysis && item.analysis.trim().length > 0,
    ) || searchResults[0];

  const isGuest = !authLoading && !authenticated;

  const handleSearch = async (val?: string) => {
    const target = (val ?? query).trim();
    if (!target) return;

    if (isGuest) {
      // Hard gate — don't hit the backend if already maxed out
      if (guestCount >= 10) return;
      const next = incrementGuestSearchCount();
      setGuestCount(next);
    }
    await triggerSearch(target);
  };

  // Auto-run initial search from sessionStorage on first load
  useEffect(() => {
    if (bootstrapped || authLoading) return;
    const pending = sessionStorage.getItem("moodflix_pending_query");
    if (pending) {
      sessionStorage.removeItem("moodflix_pending_query");
      syncQuery(pending);
      handleSearch(pending);
    }
    setBootstrapped(true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authLoading, bootstrapped]);

  const handleSignOut = async () => {
    await authService.logout();
    mutate();
    router.push("/");
  };

  if (authLoading) return null;

  return (
    <div className="flex flex-col w-full min-h-screen bg-background">
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

        <div className="absolute top-6 right-6 z-20 flex items-center gap-3">
          {isGuest ? (
            <>
              <Link
                href="/auth/login"
                className="text-sm font-medium text-zinc-300 hover:text-white transition-colors"
              >
                Login
              </Link>
              <Link
                href="/auth/register"
                className="text-sm font-medium bg-brand-red hover:bg-brand-dark text-white px-5 py-2 rounded-full uppercase tracking-widest transition-colors"
              >
                Sign Up
              </Link>
            </>
          ) : (
            <button
              onClick={handleSignOut}
              className="text-sm font-medium bg-white/10 hover:bg-white/20 border border-white/20 text-white px-5 py-2 rounded-full uppercase tracking-widest transition-colors"
            >
              Sign out
            </button>
          )}
        </div>

        <div className="relative z-10">
          <MoodFlixLogo />
          <SearchSection
            currentQuery={query}
            onQueryChange={syncQuery}
            onTriggerSearch={handleSearch}
            isSearching={searchStatus === "searching"}
          />
        </div>
      </div>

      {/* Guest signup nudge — inline when idle (count>=3), value prop below */}
      {isGuest && searchStatus === "idle" && !nudgeDismissed && (
        <SignupNudge
          count={guestCount}
          onDismiss={() => setNudgeDismissed(true)}
        />
      )}

      {/* Full-screen gate when maxed out — overrides everything */}
      {isGuest && guestCount >= 10 && <SignupNudge count={guestCount} />}

      {showLumi && (
        <div className="w-full max-w-3xl mx-auto px-4 animate__animated animate__fadeInUp">
          <LumiSection
            recommendedContentItem={lumiItem}
            searchStatus={searchStatus}
          />
        </div>
      )}

      {(searchStatus !== "idle" ||
        (authenticated && searchStatus === "idle")) && (
        <div className="px-4 md:px-10 lg:px-20 mt-10 pb-20">
          {searchStatus !== "idle" && (
            <ContentRow
              title={`Results for "${query}"`}
              content_items={searchResults}
              isLoading={searchStatus === "searching"}
            />
          )}

          {/* Personalized rows — authenticated users only */}
          {authenticated && searchStatus === "idle" && (
            <>
              {discoverLoading || discoverRows.length === 0
                ? FALLBACK_ROWS.map((row) => (
                    <ContentRow
                      key={row.key}
                      title={row.title}
                      content_items={[]}
                      isLoading={true}
                    />
                  ))
                : discoverRows.map((row) => (
                    <ContentRow
                      key={row.key}
                      title={row.title}
                      content_items={row.items}
                      isLoading={false}
                    />
                  ))}
            </>
          )}
        </div>
      )}

      {/* Guest idle state — value prop + CTA, nudge stacks above */}
      {isGuest && searchStatus === "idle" && (
        <>
          <ValueProposition />
          <div className="w-full mt-auto pt-24 pb-16 text-center border-t border-white/5">
            <p className="text-2xl md:text-3xl font-netflix-logo tracking-wide text-white uppercase">
              Your next favorite movie
            </p>
            <p className="text-sm font-mono tracking-[0.4em] text-zinc-500 uppercase mt-2">
              is a mood away
            </p>
            <Link
              href="/auth/register"
              className="inline-block mt-8 text-sm font-medium bg-brand-red hover:bg-brand-dark text-white px-8 py-3 rounded-full uppercase tracking-widest transition-colors"
            >
              Get Started
            </Link>
            <p className="text-xs text-zinc-400 mt-10 font-mono tracking-wider">
              MoodFlix · Built with AI · 2026
            </p>
          </div>
        </>
      )}
    </div>
  );
};

export default DashboardPage;
