package edu.eci.arsw.blacklist;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Laboratory implementation: consults every provider concurrently using
 * Java 21 virtual threads, one independent task per provider. Performs a
 * complete scan of the provider list and returns matches in ascending order.
 */
public final class VirtualThreadBlackListSearch implements BlackListSearch {
    private final List<BlackListProvider> providers;

    public VirtualThreadBlackListSearch(List<BlackListProvider> providers) {
        this.providers = List.copyOf(Objects.requireNonNull(providers, "providers"));
    }

    @Override
    public SearchResult search(String ipAddress, int alarmThreshold) {
        Objects.requireNonNull(ipAddress, "ipAddress");
        if (alarmThreshold <= 0) {
            throw new IllegalArgumentException("alarmThreshold must be greater than zero");
        }

        long startedAt = System.nanoTime();
        List<Integer> matches = new ArrayList<>();
        int consulted = 0;

        List<Future<Integer>> futures = new ArrayList<>(providers.size());
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (BlackListProvider provider : providers) {
                futures.add(executor.submit(
                        () -> provider.isBlacklisted(ipAddress) ? provider.id() : null));
            }
            for (Future<Integer> future : futures) {
                Integer matchingId = waitForResult(future);
                consulted++;
                if (matchingId != null) {
                    matches.add(matchingId);
                }
            }
        }

        Collections.sort(matches);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);
        return new SearchResult(ipAddress, matches, consulted, elapsed);
    }

    private static Integer waitForResult(Future<Integer> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Search interrupted while waiting for providers", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Provider consultation failed", e.getCause());
        }
    }
}
