"use client";

import { FC } from "react";
import Link from "next/link";
import { useAuth } from "@/hooks/useAuth";

const MoodFlixLogo: FC = () => {
  const { authenticated } = useAuth();
  const href = authenticated ? "/dashboard" : "/";

  return (
    <Link
      href={href}
      className="flex flex-col items-center mb-10 select-none cursor-pointer"
    >
      {/* Main Logo with Entrance Animation */}
      <div className="animate__animated animate__fadeInDown">
        <h1 className="text-7xl md:text-9xl font-netflix-logo tracking-normal text-brand-red uppercase scale-y-110 md:scale-y-125">
          MoodFlix
        </h1>
      </div>

      {/* The Tagline Lockup */}
      <div className="relative flex items-center justify-center w-full mt-2 md:mt-4 overflow-hidden">
        {/* Decorative lines to "frame" the tagline */}
        <div className="hidden md:block h-px w-12 bg-linear-to-r from-transparent to-zinc-700 mx-4 animate__animated animate__fadeIn animate__delay-2s" />

        <p className="text-sm md:text-base font-mono tracking-[0.4em] text-zinc-400 uppercase animate__animated animate__fadeInUp animate__delay-2s">
          Your mood <span className="text-brand-red">/</span> Decoded
        </p>

        <div className="hidden md:block h-px w-12 bg-linear-to-l from-transparent to-zinc-700 mx-4 animate__animated animate__fadeIn animate__delay-2s" />
      </div>

      {/* Cinematic Floor Glow */}
      <div className="w-48 h-2 bg-brand-red blur-3xl opacity-20 -mt-2 animate__animated animate__fadeIn animate__delay-2s" />
    </Link>
  );
};

export default MoodFlixLogo;
