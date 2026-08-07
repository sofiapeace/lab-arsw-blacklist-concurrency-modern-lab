package edu.eci.arsw.blacklist;

import java.time.Duration;
import java.time.Instant; 
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Laboratory implementation: students must complete this class.
 */
public final class FixedPoolBlackListSearch implements BlackListSearch {
    private final List<BlackListProvider> providers;
    private final int poolSize;

    public FixedPoolBlackListSearch(List<BlackListProvider> providers, int poolSize) {
        this.providers = List.copyOf(Objects.requireNonNull(providers, "providers"));
        if (poolSize <= 0) {
            throw new IllegalArgumentException("poolSize must be greater than zero");
        }
        this.poolSize = poolSize;
    }

    @Override
    public SearchResult search(String ipAddress, int alarmThreshold) {
        Instant start = Instant.now();
        
        AtomicInteger alarms = new AtomicInteger(0);
        AtomicInteger checked = new AtomicInteger(0);
        List<Integer> matchingProviderIds = new CopyOnWriteArrayList<>(); 
        
        int totalProviders = providers.size();
        
        if (totalProviders == 0) {
            return new SearchResult(ipAddress, matchingProviderIds, 0, Duration.between(start, Instant.now()));
        }

        int actualThreads = Math.min(poolSize, totalProviders);
        int chunkSize = totalProviders / actualThreads;
        
        CountDownLatch latch = new CountDownLatch(actualThreads);

        try (ExecutorService executor = Executors.newFixedThreadPool(poolSize)) {
            for (int i = 0; i < actualThreads; i++) {
                final int startIndex = i * chunkSize;
                final int endIndex = (i == actualThreads - 1) ? totalProviders : (i + 1) * chunkSize;

                executor.submit(() -> {
                    try {
                        for (int j = startIndex; j < endIndex; j++) {

                            if (alarms.get() >= alarmThreshold) {
                                break;
                            }

                            BlackListProvider provider = providers.get(j);
                            checked.incrementAndGet();

                            if (provider.isBlacklisted(ipAddress)) {
                                matchingProviderIds.add(provider.id());
                                alarms.incrementAndGet();
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        Instant end = Instant.now();

        return new SearchResult(ipAddress, matchingProviderIds, checked.get(), Duration.between(start, end));
    }
}
