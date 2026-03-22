"use client";

import { FC, useState } from "react";
import { useRouter } from "next/navigation";
import { mutate } from "swr";
import {
  CloudRain,
  Zap,
  Laugh,
  Tornado,
  Flame,
  Bomb,
  Lightbulb,
  User,
  Heart,
  Users,
  PartyPopper,
  Popcorn,
  Target,
  Repeat,
  Waves,
  LucideIcon,
} from "lucide-react";
import { authService } from "@/services/auth";
import MoodFlixLogo from "@/components/logo/MoodflixLogo";

type Option = { value: string; icon: LucideIcon; label: string };

const MOODS: Option[] = [
  { value: "NEED_TO_FEEL", icon: CloudRain, label: "Feel Something Deep" },
  { value: "EDGE_OF_SEAT", icon: Zap, label: "Edge of My Seat" },
  { value: "JUST_LAUGH", icon: Laugh, label: "Just Laugh" },
  { value: "MIND_BLOWN", icon: Tornado, label: "Blow My Mind" },
  { value: "QUIET_BEAUTIFUL", icon: Flame, label: "Quiet & Beautiful" },
  { value: "PURE_CHAOS", icon: Bomb, label: "Pure Chaos" },
  { value: "LEARN_SOMETHING", icon: Lightbulb, label: "Learn Something" },
];

const COMPANY: Option[] = [
  { value: "SOLO", icon: User, label: "Just Me" },
  { value: "DATE_NIGHT", icon: Heart, label: "Date Night" },
  { value: "FAMILY", icon: Users, label: "Family" },
  { value: "FRIENDS", icon: PartyPopper, label: "Friends" },
];

const COMMITMENT: Option[] = [
  { value: "CASUAL", icon: Popcorn, label: "Casual" },
  { value: "FOCUSED", icon: Target, label: "Fully In It" },
  { value: "COMFORT_REWATCH", icon: Repeat, label: "Comfort Rewatch" },
  { value: "EPIC", icon: Waves, label: "Epic Night" },
];

const TIRED_OF = [
  { value: "SUPERHEROES", label: "Superheroes" },
  { value: "SEQUELS", label: "Sequels & Remakes" },
  { value: "HAPPY_ENDINGS", label: "Happy Endings" },
  { value: "AMERICAN_SETTINGS", label: "American Settings" },
  { value: "CGI_EVERYTHING", label: "CGI Everything" },
  { value: "SLOW_BURNS", label: "Slow Burns" },
];

const STEP_TITLES = [
  {
    heading: "How are you feeling tonight?",
    sub: "Pick the vibe that matches your mood.",
  },
  {
    heading: "Who are you watching with?",
    sub: "This shapes the kind of experience we find.",
  },
  { heading: "How deep are you going?", sub: "Be honest." },
  {
    heading: "What have you had enough of?",
    sub: "Select any that apply — or just skip ahead.",
  },
];

const OnboardingPage: FC = () => {
  const router = useRouter();
  const [step, setStep] = useState(0);
  const [mood, setMood] = useState("");
  const [company, setCompany] = useState("");
  const [commitment, setCommitment] = useState("");
  const [tiredOf, setTiredOf] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [animating, setAnimating] = useState(false);

  const goToStep = (next: number) => {
    setAnimating(true);
    setTimeout(() => {
      setStep(next);
      setAnimating(false);
    }, 200);
  };

  const toggleTiredOf = (value: string) => {
    setTiredOf((prev) =>
      prev.includes(value) ? prev.filter((v) => v !== value) : [...prev, value],
    );
  };

  const handleSubmit = async () => {
    setLoading(true);
    setError("");
    try {
      await authService.completeOnboarding({
        mood,
        company,
        commitment,
        tiredOf,
      });
      await mutate("/api/auth/me");
      router.push("/dashboard");
    } catch {
      setError("Something went wrong. Please try again.");
      setLoading(false);
    }
  };

  const optionClass = (selected: boolean) =>
    `flex items-center gap-3 px-4 py-3 rounded-xl border transition-all text-left cursor-pointer
        ${
          selected
            ? "border-brand-red bg-brand-red/10 text-white"
            : "border-white/10 bg-white/5 text-zinc-300 hover:border-white/30 hover:bg-white/10"
        }`;

  return (
    <div className="flex flex-col w-full min-h-screen bg-background">
      {/* Hero — same as landing page */}
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
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 flex flex-col items-center px-4 pb-20">
        <div className="w-full max-w-lg">
          {/* Progress bar — fixed, never moves */}
          <div className="flex gap-2 mb-8">
            {STEP_TITLES.map((_, i) => (
              <div
                key={i}
                className={`h-0.5 flex-1 rounded-full transition-all duration-500 ${
                  i <= step ? "bg-brand-red" : "bg-white/10"
                }`}
              />
            ))}
          </div>

          {/* Step heading — fixed height, fades between steps */}
          <div
            className={`transition-opacity duration-200 ${
              animating ? "opacity-0" : "opacity-100"
            }`}
          >
            <h2 className="text-2xl font-bold text-white mb-1">
              {STEP_TITLES[step].heading}
            </h2>
            <p className="text-zinc-400 text-sm mb-8">
              {STEP_TITLES[step].sub}
            </p>

            {/* Step 1 — Mood */}
            {step === 0 && (
              <div className="grid grid-cols-2 gap-3">
                {MOODS.map((m) => (
                  <button
                    key={m.value}
                    onClick={() => {
                      setMood(m.value);
                      goToStep(1);
                    }}
                    className={optionClass(mood === m.value)}
                  >
                    <m.icon size={22} strokeWidth={1.5} className="shrink-0" />
                    <span className="text-sm font-medium">{m.label}</span>
                  </button>
                ))}
              </div>
            )}

            {/* Step 2 — Company */}
            {step === 1 && (
              <div className="grid grid-cols-2 gap-3">
                {COMPANY.map((c) => (
                  <button
                    key={c.value}
                    onClick={() => {
                      setCompany(c.value);
                      goToStep(2);
                    }}
                    className={optionClass(company === c.value)}
                  >
                    <c.icon size={22} strokeWidth={1.5} className="shrink-0" />
                    <span className="text-sm font-medium">{c.label}</span>
                  </button>
                ))}
              </div>
            )}

            {/* Step 3 — Commitment */}
            {step === 2 && (
              <div className="grid grid-cols-2 gap-3">
                {COMMITMENT.map((c) => (
                  <button
                    key={c.value}
                    onClick={() => {
                      setCommitment(c.value);
                      goToStep(3);
                    }}
                    className={optionClass(commitment === c.value)}
                  >
                    <c.icon size={22} strokeWidth={1.5} className="shrink-0" />
                    <span className="text-sm font-medium">{c.label}</span>
                  </button>
                ))}
              </div>
            )}

            {/* Step 4 — Tired Of */}
            {step === 3 && (
              <>
                <div className="flex flex-wrap gap-2 mb-8">
                  {TIRED_OF.map((t) => (
                    <button
                      key={t.value}
                      onClick={() => toggleTiredOf(t.value)}
                      className={`px-4 py-2 rounded-full text-sm border transition-all
                                                ${
                                                  tiredOf.includes(t.value)
                                                    ? "border-brand-red bg-brand-red/10 text-white"
                                                    : "border-white/10 bg-white/5 text-zinc-400 hover:border-white/30"
                                                }`}
                    >
                      {t.label}
                    </button>
                  ))}
                </div>

                {error && (
                  <p className="text-brand-red text-sm mb-4">{error}</p>
                )}

                <button
                  onClick={handleSubmit}
                  disabled={loading}
                  className="w-full py-4 bg-brand-red hover:bg-brand-dark text-white font-bold
                                               rounded-full transition-all disabled:opacity-50
                                               tracking-widest uppercase text-sm font-mono"
                >
                  {loading ? "Setting up your profile..." : "Find My Movies →"}
                </button>
              </>
            )}

            {/* Back button — always at the same position */}
            {step > 0 && (
              <button
                onClick={() => goToStep(step - 1)}
                className="mt-6 text-xs font-mono text-zinc-500 hover:text-zinc-300 transition-colors tracking-widest uppercase"
              >
                ← Back
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default OnboardingPage;
