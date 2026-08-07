package edu.eci.arsw.blacklist;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Laboratory implementation: students must complete this class using Java 21 virtual threads.
 */
public final class VirtualThreadBlackListSearch implements BlackListSearch {
    private final List<BlackListProvider> providers;

    public VirtualThreadBlackListSearch(List<BlackListProvider> providers) {
        this.providers = List.copyOf(Objects.requireNonNull(providers, "providers"));
    }

    @Override
    public SearchResult search(String ipAddress, int alarmThreshold) {
        Instant start = Instant.now();
        AtomicInteger alarms = new AtomicInteger(0);
        AtomicInteger checked = new AtomicInteger(0);
        List<Integer> matchingProviderIds = new CopyOnWriteArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            for (BlackListProvider provider : providers) {
                
                executor.submit(() -> {
                    if (alarms.get() >= alarmThreshold) {
                        return; 
                    }

                    checked.incrementAndGet();

                    if (provider.isBlacklisted(ipAddress)) {
                        matchingProviderIds.add(provider.id());
                        alarms.incrementAndGet();
                    }
                });
            }
        } 
        
        Instant end = Instant.now();
        Duration elapsed = Duration.between(start, end);

        return new SearchResult(ipAddress, matchingProviderIds, checked.get(), elapsed);
    }
}
