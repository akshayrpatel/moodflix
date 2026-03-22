"use client";

import { FC, useState, FormEvent } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { mutate } from "swr";
import { authService } from "@/services/auth";
import MoodFlixLogo from "@/components/logo/MoodflixLogo";

const LoginPage: FC = () => {
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await authService.login(username, password);
      await mutate("/api/auth/me");
      router.push("/dashboard");
    } catch {
      setError("Invalid username or password");
    } finally {
      setLoading(false);
    }
  };

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

      {/* Form */}
      <div className="flex-1 flex flex-col items-center px-4 pb-20">
        <div className="w-full max-w-md animate__animated animate__fadeInUp">
          <h2 className="text-2xl font-bold text-white mb-1">Welcome back</h2>
          <p className="text-zinc-400 text-sm mb-8">
            No account yet?{" "}
            <Link
              href="/auth/register"
              className="text-brand-red hover:underline"
            >
              Create one
            </Link>
          </p>

          {error && (
            <div className="mb-6 px-4 py-3 rounded-xl bg-brand-red/10 border border-brand-red/30 text-brand-red text-sm">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="flex flex-col gap-5">
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-mono tracking-widest text-zinc-400 uppercase">
                Username
              </label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                placeholder="akshay"
                className="w-full py-4 px-6 bg-white/5 border border-white/20 rounded-full text-white text-sm
                                           focus:outline-none focus:ring-2 focus:ring-brand-red focus:bg-white/10
                                           placeholder:text-zinc-600 transition-all"
              />
            </div>

            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-mono tracking-widest text-zinc-400 uppercase">
                Password
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                placeholder="••••••••"
                className="w-full py-4 px-6 bg-white/5 border border-white/20 rounded-full text-white text-sm
                                           focus:outline-none focus:ring-2 focus:ring-brand-red focus:bg-white/10
                                           placeholder:text-zinc-600 transition-all"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="mt-2 w-full py-4 bg-brand-red hover:bg-brand-dark text-white font-bold
                                       rounded-full transition-all disabled:opacity-50 disabled:cursor-not-allowed
                                       tracking-widest uppercase text-sm font-mono"
            >
              {loading ? "Signing in..." : "Sign In"}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
