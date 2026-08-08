package edu.eci.arsw.blacklist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Runs a benchmark for one strategy from command-line arguments.
 *
 * <p>Usage: {@code <strategy> <ipAddress> <simulateIo> <warmups> <measuredRuns> [poolSize]}
 * where strategy is SEQUENTIAL, FIXED, or VIRTUAL. FIXED requires poolSize.
 */
public final class BenchmarkRunner {
    private static final int PROVIDER_COUNT = 100;
    private static final int ALARM_THRESHOLD = 5;

    private BenchmarkRunner() {
    }

    public static void main(String[] args) {
        if (args.length < 5) {
            printUsageAndExit();
        }

        String strategyName = args[0].toUpperCase(Locale.ROOT);
        String ipAddress = args[1];
        boolean simulateIo = Boolean.parseBoolean(args[2]);
        int warmups = parsePositiveOrZero(args[3], "warmups");
        int measuredRuns = parsePositive(args[4], "measuredRuns");

        int poolSize = 0;
        if (strategyName.equals("FIXED")) {
            if (args.length < 6) {
                System.err.println("FIXED strategy requires a poolSize argument.");
                printUsageAndExit();
            }
            poolSize = parsePositive(args[5], "poolSize");
        }

        List<BlackListProvider> providers = ProviderFactory.create(PROVIDER_COUNT, simulateIo);
        BlackListSearch search = createStrategy(strategyName, providers, poolSize);

        SearchResult expected = new SequentialBlackListSearch(providers).search(ipAddress, ALARM_THRESHOLD);

        String scenario = simulateIo ? "IO" : "NO_IO";
        String poolLabel = strategyName.equals("FIXED") ? String.valueOf(poolSize) : "-";

        System.out.printf("Configuration: strategy=%s scenario=%s ip=%s warmups=%d runs=%d poolSize=%s%n",
                strategyName, scenario, ipAddress, warmups, measuredRuns, poolLabel);

        for (int i = 0; i < warmups; i++) {
            search.search(ipAddress, ALARM_THRESHOLD);
        }

        List<Double> elapsedMillis = new ArrayList<>(measuredRuns);
        System.out.println("csv,scenario,strategy,pool_size,run,elapsed_ms,matches,consulted_providers");
        for (int run = 1; run <= measuredRuns; run++) {
            SearchResult result = search.search(ipAddress, ALARM_THRESHOLD);
            verifyEquivalence(expected, result, run);

            double ms = result.elapsed().toNanos() / 1_000_000.0;
            elapsedMillis.add(ms);
            System.out.printf(Locale.ROOT, "csv,%s,%s,%s,%d,%.3f,%d,%d%n",
                    scenario, strategyName, poolLabel, run, ms,
                    result.matchingProviderIds().size(), result.consultedProviders());
        }

        double min = elapsedMillis.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = elapsedMillis.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double avg = elapsedMillis.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        System.out.printf(Locale.ROOT, "Summary: strategy=%s scenario=%s pool=%s avg=%.3f ms min=%.3f ms max=%.3f ms "
                        + "matches=%d consulted=%d trustworthy=%s%n",
                strategyName, scenario, poolLabel, avg, min, max,
                expected.matchingProviderIds().size(), expected.consultedProviders(),
                expected.isTrustworthy(ALARM_THRESHOLD));
    }

    private static BlackListSearch createStrategy(String name, List<BlackListProvider> providers, int poolSize) {
        return switch (name) {
            case "SEQUENTIAL" -> new SequentialBlackListSearch(providers);
            case "FIXED" -> new FixedPoolBlackListSearch(providers, poolSize);
            case "VIRTUAL" -> new VirtualThreadBlackListSearch(providers);
            default -> {
                System.err.println("Unknown strategy: " + name);
                printUsageAndExit();
                yield null;
            }
        };
    }

    private static void verifyEquivalence(SearchResult expected, SearchResult actual, int run) {
        if (!expected.matchingProviderIds().equals(actual.matchingProviderIds())
                || expected.consultedProviders() != actual.consultedProviders()) {
            throw new IllegalStateException(
                    "Run " + run + " produced a result different from the sequential baseline. "
                            + "Expected " + expected.matchingProviderIds()
                            + " but was " + actual.matchingProviderIds());
        }
    }

    private static int parsePositive(String value, String name) {
        int parsed = parseInt(value, name);
        if (parsed <= 0) {
            System.err.println(name + " must be greater than zero.");
            printUsageAndExit();
        }
        return parsed;
    }

    private static int parsePositiveOrZero(String value, String name) {
        int parsed = parseInt(value, name);
        if (parsed < 0) {
            System.err.println(name + " must not be negative.");
            printUsageAndExit();
        }
        return parsed;
    }

    private static int parseInt(String value, String name) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println(name + " must be an integer, got: " + value);
            printUsageAndExit();
            return -1;
        }
    }

    private static void printUsageAndExit() {
        System.err.println("Usage: <strategy> <ipAddress> <simulateIo> <warmups> <measuredRuns> [poolSize]");
        System.err.println("  strategy: SEQUENTIAL | FIXED | VIRTUAL (FIXED requires poolSize)");
        System.err.println("Examples:");
        System.err.println("  mvn exec:java -Dexec.args=\"SEQUENTIAL 202.24.34.55 true 2 5\"");
        System.err.println("  mvn exec:java -Dexec.args=\"FIXED 202.24.34.55 true 2 5 4\"");
        System.err.println("  mvn exec:java -Dexec.args=\"VIRTUAL 202.24.34.55 true 2 5\"");
        System.exit(1);
    }
}
