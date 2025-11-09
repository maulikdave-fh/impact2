package in.foresthut.indices.speciesdiversity.handler;

import com.nirvighna.mongodb.commons.DatabaseConfig;
import in.foresthut.aoi.S2GeometryBasedAoIProvider;
import in.foresthut.impact.models.indices.speciesdensity.SpeciesDensityRequest;
import in.foresthut.impact.models.indices.speciesdiversity.SpeciesDiversityRequest;
import in.foresthut.impact.models.site.CreateSiteRequest;
import in.foresthut.indices.speciesdensity.repository.SpeciesDensityIndexRepositoryImpl;
import in.foresthut.indices.speciesdensity.service.handler.SpeciesDensityRequestHandler;
import in.foresthut.indices.speciesdiversity.repository.SpeciesDiversityIndexRepositoryImpl;
import in.foresthut.indices.speciesdiversity.service.handler.SpeciesDiversityHandler;
import in.foresthut.infra.occurrence.repository.OccurrenceRepositoryImpl;
import in.foresthut.infra.s2token.repository.S2CellTokenPlusRepoImpl;
import in.foresthut.restorationsite.repository.RestorationSiteRepositoryImpl;
import in.foresthut.restorationsite.service.handler.CreateSiteHandler;
import in.foresthut.user.repository.UserRepositoryImpl;
import org.junit.jupiter.api.Test;

public class SpeciesDiversityHandlerTest {
    private final static String forestHut =
            "POLYGON ((73.5805054 18.2362594, 73.5803734 18.2361013, 73.5801464 18.235764, 73.5800897 18.235371, 73.580051 18.2351999, 73.5800175 18.2350338, 73.5800674 18.2346674, 73.5800905 18.2344901, 73.5800763 18.234334, 73.5800944 18.2341378, 73.5802172 18.2340208, 73.5802982 18.2339462, 73.5803941 18.2337463, 73.5804511 18.2337139, 73.580525 18.2336816, 73.5806198 18.2337052, 73.5807623 18.2337447, 73.5809229 18.2338173, 73.5809221 18.2360755, 73.5806839 18.2361839, 73.5805054 18.2362594))";


    @Test
    void test() {
        String siteId = createSite();

        DatabaseConfig databaseConfig = DatabaseConfig.getInstance();
        SpeciesDiversityRequest request = SpeciesDiversityRequest.newBuilder()
                                                               .setSiteId(siteId)
                                                               .setForYear(2022)
                                                               .build();

        SpeciesDiversityHandler requestHandler = new SpeciesDiversityHandler(
                request, new OccurrenceRepositoryImpl(databaseConfig),
                new RestorationSiteRepositoryImpl(databaseConfig),
                new SpeciesDiversityIndexRepositoryImpl(databaseConfig), null);

        var response  =requestHandler.calculateIndex(request);

        System.out.println(response);

        new RestorationSiteRepositoryImpl(databaseConfig).delete(siteId);
    }

    public static String createSite() {
        DatabaseConfig databaseConfig = DatabaseConfig.getInstance();
        CreateSiteRequest request = CreateSiteRequest.newBuilder()
                                                     .setUserId("12")
                                                     .setSiteName("Forest Hut")
                                                     .setWktPolygon(forestHut)
                                                     .build();
        var createSiteHandler = new CreateSiteHandler(
                request, new UserRepositoryImpl(databaseConfig), new RestorationSiteRepositoryImpl(databaseConfig),
                null, new S2GeometryBasedAoIProvider(new S2CellTokenPlusRepoImpl(databaseConfig)));

        var site = createSiteHandler.createSite(request);
        return site.siteId();
    }
}
