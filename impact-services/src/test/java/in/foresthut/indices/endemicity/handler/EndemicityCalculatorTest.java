package in.foresthut.indices.endemicity.handler;

import com.nirvighna.mongodb.commons.DatabaseConfig;
import in.foresthut.commons.geometry.bioregion.Bioregion;
import in.foresthut.commons.geometry.bioregion.WesternGhats;
import in.foresthut.indices.endemicity.service.handler.EndemicityIndexCalculator;
import in.foresthut.infra.gbif.GBIFClient;
import in.foresthut.infra.occurrence.repository.OccurrenceRepository;
import in.foresthut.infra.occurrence.repository.OccurrenceRepositoryImpl;
import org.junit.jupiter.api.Test;

public class EndemicityCalculatorTest {
    private final String forestHut =
            "POLYGON ((73.5805054 18.2362594, 73.5803734 18.2361013, 73.5801464 18.235764, 73.5800897 18.235371, 73.580051 18.2351999, 73.5800175 18.2350338, 73.5800674 18.2346674, 73.5800905 18.2344901, 73.5800763 18.234334, 73.5800944 18.2341378, 73.5802172 18.2340208, 73.5802982 18.2339462, 73.5803941 18.2337463, 73.5804511 18.2337139, 73.580525 18.2336816, 73.5806198 18.2337052, 73.5807623 18.2337447, 73.5809229 18.2338173, 73.5809221 18.2360755, 73.5806839 18.2361839, 73.5805054 18.2362594))";

    @Test
    void test() {
        Bioregion westernGhats = WesternGhats.getInstance();

        DatabaseConfig databaseConfig = DatabaseConfig.getInstance();
        OccurrenceRepository occurrenceRepository = new OccurrenceRepositoryImpl(databaseConfig);

        EndemicityIndexCalculator endemicityIndexCalculator =
                new EndemicityIndexCalculator(
                        westernGhats, "Ocyceros griseus", occurrenceRepository, null, GBIFClient.getInstance());

        endemicityIndexCalculator.calculate();
    }
}
