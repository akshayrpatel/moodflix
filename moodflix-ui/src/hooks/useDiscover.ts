import useSWR from "swr";
import { fetchDiscover, DiscoverRow } from "@/services/discover";

export const useDiscover = () => {
  const { data, error, isLoading, mutate } = useSWR<DiscoverRow[]>(
    "discover",
    fetchDiscover,
    {
      revalidateOnFocus: false,
      dedupingInterval: 60_000,
    },
  );

  return {
    rows: data ?? [],
    isLoading,
    error,
    refresh: mutate,
  };
};
