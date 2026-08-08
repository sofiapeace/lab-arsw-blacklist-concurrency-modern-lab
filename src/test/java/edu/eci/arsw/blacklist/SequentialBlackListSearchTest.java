package edu.eci.arsw.blacklist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the mandatory result contract: every strategy performs a complete
 * scan and produces results equivalent to the sequential baseline.
 * All tests run with simulated I/O disabled.
 */
class SequentialBlackListSearchTest {

    private static final String TARGET_IP = "202.24.34.55";
    private static final int PROVIDER_COUNT = 100;
    private static final int ALARM_THRESHOLD = 5;

    private static List<BlackListProvider> providers;
    private static SearchResult baseline;

    @BeforeAll
    static void createBaseline() {
        providers = ProviderFactory.create(PROVIDER_COUNT, false);
        baseline = new SequentialBlackListSearch(providers).search(TARGET_IP, ALARM_THRESHOLD);
    }

    @Test
    void sequentialSearchIsDeterministic() {
        SearchResult second = new SequentialBlackListSearch(providers).search(TARGET_IP, ALARM_THRESHOLD);

        assertEquals(PROVIDER_COUNT, baseline.consultedProviders());
        assertEquals(baseline.matchingProviderIds(), second.matchingProviderIds());
        assertFalse(baseline.matchingProviderIds().isEmpty());
    }

    @Test
    void fixedPoolOf2ThreadsMatchesSequentialBaseline() {
        assertEquivalentToBaseline(new FixedPoolBlackListSearch(providers, 2));
    }

    @Test
    void fixedPoolOf4ThreadsMatchesSequentialBaseline() {
        assertEquivalentToBaseline(new FixedPoolBlackListSearch(providers, 4));
    }

    @Test
    void fixedPoolOf8ThreadsMatchesSequentialBaseline() {
        assertEquivalentToBaseline(new FixedPoolBlackListSearch(providers, 8));
    }

    @Test
    void virtualThreadsMatchSequentialBaseline() {
        assertEquivalentToBaseline(new VirtualThreadBlackListSearch(providers));
    }

    @Test
    void everyStrategyConsultsAllProviders() {
        assertEquals(PROVIDER_COUNT, baseline.consultedProviders());
        assertEquals(PROVIDER_COUNT,
                new FixedPoolBlackListSearch(providers, 4).search(TARGET_IP, ALARM_THRESHOLD).consultedProviders());
        assertEquals(PROVIDER_COUNT,
                new VirtualThreadBlackListSearch(providers).search(TARGET_IP, ALARM_THRESHOLD).consultedProviders());
    }

    @Test
    void matchingProviderIdsContainNoDuplicates() {
        List<Integer> fixedIds =
                new FixedPoolBlackListSearch(providers, 8).search(TARGET_IP, ALARM_THRESHOLD).matchingProviderIds();
        List<Integer> virtualIds =
                new VirtualThreadBlackListSearch(providers).search(TARGET_IP, ALARM_THRESHOLD).matchingProviderIds();

        assertEquals(new HashSet<>(fixedIds).size(), fixedIds.size(),
                "El pool fijo devolvió IDs duplicados");
        assertEquals(new HashSet<>(virtualIds).size(), virtualIds.size(),
                "Los hilos virtuales devolvieron IDs duplicados");
    }

    @Test
    void matchingProviderIdsAreInAscendingOrder() {
        assertAscending(new FixedPoolBlackListSearch(providers, 8).search(TARGET_IP, ALARM_THRESHOLD));
        assertAscending(new VirtualThreadBlackListSearch(providers).search(TARGET_IP, ALARM_THRESHOLD));
    }

    @Test
    void fixedPoolRejectsNonPositivePoolSize() {
        assertThrows(IllegalArgumentException.class, () -> new FixedPoolBlackListSearch(providers, 0));
        assertThrows(IllegalArgumentException.class, () -> new FixedPoolBlackListSearch(providers, -3));
    }

    private static void assertEquivalentToBaseline(BlackListSearch strategy) {
        SearchResult result = strategy.search(TARGET_IP, ALARM_THRESHOLD);

        assertEquals(baseline.matchingProviderIds(), result.matchingProviderIds(),
                "Los IDs encontrados no coinciden con la ejecución secuencial");
        assertEquals(baseline.consultedProviders(), result.consultedProviders(),
                "La estrategia no consultó todos los proveedores");
        assertEquals(baseline.isTrustworthy(ALARM_THRESHOLD), result.isTrustworthy(ALARM_THRESHOLD),
                "La clasificación de confianza no coincide con la ejecución secuencial");
    }

    private static void assertAscending(SearchResult result) {
        List<Integer> ids = result.matchingProviderIds();
        for (int i = 1; i < ids.size(); i++) {
            assertTrue(ids.get(i - 1) < ids.get(i),
                    "Los IDs no están en orden ascendente: " + ids);
        }
    }
}
