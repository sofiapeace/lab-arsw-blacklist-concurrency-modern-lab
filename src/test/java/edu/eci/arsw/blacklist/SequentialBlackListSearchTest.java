package edu.eci.arsw.blacklist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class SequentialBlackListSearchTest {

    @Test
    void shouldConsultAllProvidersAndReturnDeterministicMatches() {
        List<BlackListProvider> providers = ProviderFactory.create(100, false);
        BlackListSearch search = new SequentialBlackListSearch(providers);

        SearchResult first = search.search("202.24.34.55", 5);
        SearchResult second = search.search("202.24.34.55", 5);

        assertEquals(100, first.consultedProviders());
        assertEquals(first.matchingProviderIds(), second.matchingProviderIds());
        assertFalse(first.matchingProviderIds().isEmpty());
    }

    @Test
    void shouldReturnEquivalentResultsAcrossAllStrategies() {
        List<BlackListProvider> providers = ProviderFactory.create(1000, false);
        String targetIp = "202.24.34.55";
        int threshold = 50; 

        BlackListSearch sequential = new SequentialBlackListSearch(providers);
        BlackListSearch fixedPool = new FixedPoolBlackListSearch(providers, 8);
        BlackListSearch virtualThreads = new VirtualThreadBlackListSearch(providers);

        SearchResult seqResult = sequential.search(targetIp, threshold);
        SearchResult fixedResult = fixedPool.search(targetIp, threshold);
        SearchResult virtualResult = virtualThreads.search(targetIp, threshold);

        assertEquals(seqResult.matchingProviderIds().size(), fixedResult.matchingProviderIds().size(), 
                "El pool fijo no encontró la misma cantidad de servidores maliciosos");
        assertEquals(seqResult.matchingProviderIds().size(), virtualResult.matchingProviderIds().size(),
                "Los hilos virtuales no encontraron la misma cantidad de servidores maliciosos");

        assertEquals(new HashSet<>(seqResult.matchingProviderIds()), new HashSet<>(fixedResult.matchingProviderIds()),
                "Los IDs encontrados por el pool fijo no coinciden con la ejecución secuencial");
        assertEquals(new HashSet<>(seqResult.matchingProviderIds()), new HashSet<>(virtualResult.matchingProviderIds()),
                "Los IDs encontrados por los hilos virtuales no coinciden con la ejecución secuencial");
    }

    @Test
    void shouldShortCircuitAndConsultFewerProviders() {

        List<BlackListProvider> providers = ProviderFactory.create(1000, false);
        String targetIp = "202.24.34.55";
        int threshold = 5; 

        BlackListSearch fixedPool = new FixedPoolBlackListSearch(providers, 4);
        BlackListSearch virtualThreads = new VirtualThreadBlackListSearch(providers);

        SearchResult fixedResult = fixedPool.search(targetIp, threshold);
        SearchResult virtualResult = virtualThreads.search(targetIp, threshold);

        assertTrue(fixedResult.matchingProviderIds().size() <= threshold, 
                "El pool fijo no respetó el cortocircuito");
        assertTrue(virtualResult.matchingProviderIds().size() <= threshold, 
                "Los hilos virtuales no respetaron el cortocircuito");
    }
}