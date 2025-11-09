package in.foresthut.infra.occurrence.repository;

import com.nirvighna.mongodb.commons.DatabaseConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class OccurrenceRepositoryTest {
    private final String forestHut =
            "POLYGON ((73.5805054 18.2362594, 73.5803734 18.2361013, 73.5801464 18.235764, 73.5800897 18.235371, 73.580051 18.2351999, 73.5800175 18.2350338, 73.5800674 18.2346674, 73.5800905 18.2344901, 73.5800763 18.234334, 73.5800944 18.2341378, 73.5802172 18.2340208, 73.5802982 18.2339462, 73.5803941 18.2337463, 73.5804511 18.2337139, 73.580525 18.2336816, 73.5806198 18.2337052, 73.5807623 18.2337447, 73.5809229 18.2338173, 73.5809221 18.2360755, 73.5806839 18.2361839, 73.5805054 18.2362594))";

    @Test
    void testSpeciesCount() {
        String s2CellToken = "3bc2848b7";
        var occurrenceRepo = new OccurrenceRepositoryImpl(DatabaseConfig.getInstance());

        long speciesCount = occurrenceRepo.speciesCountForS2Token(s2CellToken, 2022);
        System.out.println(speciesCount);
        assertTrue(speciesCount > 0);
    }

    @Test
    void testSpeciesOccurrences() {
        var occurrenceRepo = new OccurrenceRepositoryImpl(DatabaseConfig.getInstance());
        System.out.println(occurrenceRepo.speciesOccurrencesForPolygon(forestHut, 2023));
    }

    @Test
    void testSpeciesCountForForestHut() {
        var occurrenceRepo = new OccurrenceRepositoryImpl(DatabaseConfig.getInstance());
        long speciesCount = occurrenceRepo.speciesCountForPolygon(forestHut, 2023);
        System.out.println("Species count " + speciesCount);
        assertTrue(speciesCount > 0);
    }

    @Test
    void testTaxaGroupCountForForestHut() {
        var occurrenceRepo = new OccurrenceRepositoryImpl(DatabaseConfig.getInstance());
        long taxaGroupCount = occurrenceRepo.numberOfTaxaGroups(forestHut, 2022);
        System.out.println("Taxa group count " + taxaGroupCount);
        assertTrue(taxaGroupCount > 0);
    }

    @Test
    void testSpeciesCountForIconicTaxa() {
        var occurrenceRepo = new OccurrenceRepositoryImpl(DatabaseConfig.getInstance());
        String iconicTaxa = "Insecta";
        int forYear = 2023;
        long speciesCountForTaxa = occurrenceRepo.speciesCountByTaxaGroup(iconicTaxa, forestHut, forYear);
        System.out.printf("There were %d species observed in taxa '%s' for year %d.\n", speciesCountForTaxa,
                          iconicTaxa, forYear);
        assertTrue(speciesCountForTaxa >= 0);
    }
}
