"use client";

import { FC } from "react";
import { MessageCircle, Sparkles, Fingerprint } from "lucide-react";

const CARDS = [
  {
    icon: MessageCircle,
    title: "Describe Your Mood",
    body: 'Type something like "I want something cozy" or "feeling like a villain" — no need for titles or genres.',
  },
  {
    icon: Sparkles,
    title: "Get AI Matches",
    body: "Our engine maps your vibe to movies using mood analysis and semantic search — not just keywords.",
  },
  {
    icon: Fingerprint,
    title: "Build Your Taste",
    body: "Sign up and the more you search, the better it gets. Your results become uniquely yours over time.",
  },
];

const ValueProposition: FC = () => {
  return (
    <div className="w-full max-w-4xl mx-auto px-6 py-20">
      <div className="text-center mb-12">
        <h2 className="text-2xl md:text-3xl font-netflix-logo tracking-wide text-white uppercase">
          Not sure what to watch?
        </h2>
        <p className="text-sm md:text-base text-zinc-500 mt-3 max-w-lg mx-auto">
          Skip the endless scrolling. Just tell us how you feel — we&apos;ll
          handle the rest.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        {CARDS.map(({ icon: Icon, title, body }, index) => (
          <div
            key={title}
            className="bg-white/5 border border-white/20 rounded-2xl backdrop-blur-md shadow-2xl p-8 text-center animate__animated animate__fadeInUp"
            style={{
              animationDelay: `${index * 0.1}s`,
              animationFillMode: "both",
            }}
          >
            <div className="text-3xl mb-4">
              <Icon
                className="mx-auto mb-4 text-zinc-400"
                size={28}
                strokeWidth={1.5}
              />
            </div>
            <h3 className="text-sm font-mono tracking-[0.3em] text-zinc-400 uppercase mb-3 h-10 flex items-center justify-center">
              {title}
            </h3>
            <p className="text-sm text-zinc-500 leading-relaxed">{body}</p>
          </div>
        ))}
      </div>
    </div>
  );
};

export default ValueProposition;
