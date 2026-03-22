import useSWR from "swr";

const fetcher = (url: string) => fetch(url).then((r) => r.json());

export function useAuth() {
  const { data, isLoading, mutate } = useSWR("/api/auth/me", fetcher, {
    revalidateOnFocus: true,
    dedupingInterval: 30000,
  });

  return {
    authenticated: data?.authenticated ?? false,
    username: data?.username ?? null,
    isLoading,
    mutate,
  };
}
