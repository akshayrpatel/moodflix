# MoodFlix UI

Next.js frontend with a BFF (Backend for Frontend) security pattern. All API communication with the gateway routes through server-side Next.js routes - the browser never talks to backend services directly.

## Tech Stack

- Next.js 16 / React 19 / TypeScript 5
- Tailwind CSS 4 (ESM config, @import syntax)
- SWR for auth state management
- Lucide React for icons

## Pages

### Landing

Hero section with a search bar and value proposition cards for guests. Demonstrates the search experience without requiring signup.

### Dashboard

The main experience for authenticated users. Displays search results with Lumi's AI pick, four personalized discover rows, and persistent search bar. Content cards show mood tags, ratings, and metadata.

### Auth

Register and login flows. Registration automatically logs the user in and redirects to onboarding. Login redirects to the dashboard.

### Onboarding

A 4-step mood quiz that captures the user's taste profile. Selections are sent to the gateway, which computes a 384-dim taste vector that shapes every future interaction.

## Guest Experience

Guests get 10 free searches with progressive engagement:

- First 3 searches are uninterrupted
- A signup nudge appears after the 3rd search
- A full-screen gate appears at 10 searches with an option to return home

This lets users experience real value before asking for any commitment.

## Security

The frontend implements a strict BFF pattern:

- **JWT in httpOnly cookie** - The token is set by the server-side route handler after login. Browser JavaScript has no access to it.
- **Server-side API proxy** - Every API call from the browser goes to a Next.js server route, which attaches the JWT from the cookie and forwards to the gateway. The browser never knows the gateway exists.
- **Route protection** - Middleware intercepts navigation and redirects unauthenticated users away from protected pages.

## Engineering Decisions

- **BFF over direct API calls** - Eliminates an entire class of token theft vulnerabilities. The JWT never appears in localStorage, sessionStorage, or any client-side JavaScript. Worth the added complexity of server-side route handlers.
- **SWR for auth state** - Lightweight alternative to a full state management library. Auth status is fetched once and cached, with automatic revalidation on focus. No Redux, no context providers, no boilerplate.
- **Progressive guest gating** - Users who search 3+ times have demonstrated intent. That is the right moment to ask for signup, not on first visit.
