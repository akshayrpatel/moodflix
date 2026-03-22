const GUEST_SEARCH_KEY = "moodflix_guest_searches";
const MAX_GUEST_SEARCHES = 10;

export const authService = {
  async register(
    username: string,
    email: string,
    password: string,
  ): Promise<void> {
    const res = await fetch("/api/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, email, password }),
    });
    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      throw new Error(data.error || "Registration failed");
    }
  },

  async login(username: string, password: string): Promise<void> {
    const res = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password }),
    });
    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      throw new Error(data.error || "Invalid credentials");
    }
    // Cookie is set by the API route server-side. Nothing to do here.
  },

  async logout(): Promise<void> {
    try {
      await fetch("/api/auth/logout", { method: "POST" });
    } catch {
      // Network error during logout — cookie will expire naturally.
      // Don't block the user from navigating away.
    }
  },

  async getAuthStatus(): Promise<{
    authenticated: boolean;
    username?: string;
  }> {
    try {
      const res = await fetch("/api/auth/me");
      if (!res.ok) return { authenticated: false };
      return await res.json();
    } catch {
      // Network error or server down — treat as unauthenticated.
      // The page will work in guest mode instead of crashing.
      return { authenticated: false };
    }
  },

  async completeOnboarding(moodSelection: {
    mood: string;
    company: string;
    commitment: string;
    tiredOf: string[];
  }): Promise<void> {
    const res = await fetch("/api/onboarding/complete", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(moodSelection),
    });
    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      throw new Error(data.error || "Onboarding failed");
    }
  },

  // Guest search limit — stays in localStorage (UX feature, not auth state)

  getGuestSearchCount(): number {
    if (typeof window === "undefined") return 0;
    return parseInt(localStorage.getItem(GUEST_SEARCH_KEY) || "0", 10);
  },

  incrementGuestSearch(): number {
    const count = this.getGuestSearchCount() + 1;
    localStorage.setItem(GUEST_SEARCH_KEY, String(count));
    return count;
  },

  canGuestSearch(): boolean {
    return this.getGuestSearchCount() < MAX_GUEST_SEARCHES;
  },
};
