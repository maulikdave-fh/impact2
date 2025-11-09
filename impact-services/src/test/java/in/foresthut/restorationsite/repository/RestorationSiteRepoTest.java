package in.foresthut.restorationsite.repository;

import com.nirvighna.mongodb.commons.DatabaseConfig;
import in.foresthut.impact.models.site.AreaUnit;
import in.foresthut.restorationsite.entity.RestorationSiteDao;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RestorationSiteRepoTest {
    private final String forestHut =
            "POLYGON ((73.5805054 18.2362594, 73.5803734 18.2361013, 73.5801464 18.235764, 73.5800897 18.235371, 73.580051 18.2351999, 73.5800175 18.2350338, 73.5800674 18.2346674, 73.5800905 18.2344901, 73.5800763 18.234334, 73.5800944 18.2341378, 73.5802172 18.2340208, 73.5802982 18.2339462, 73.5803941 18.2337463, 73.5804511 18.2337139, 73.580525 18.2336816, 73.5806198 18.2337052, 73.5807623 18.2337447, 73.5809229 18.2338173, 73.5809221 18.2360755, 73.5806839 18.2361839, 73.5805054 18.2362594))";

    @Test
    @Order(1)
    void test_createSite(){
        RestorationSiteDao restorationSiteDao = new RestorationSiteDao("siteId12", "userId12", "Forest Hut", forestHut, 2.2,
                                                                       AreaUnit.HECTARE.toString(), Map.of("abc", true, "def", false, "hig", false),
                                                                       56.6, 669, 670, null);

        var restorationSiteRepo = new RestorationSiteRepositoryImpl(DatabaseConfig.getInstance());
        restorationSiteRepo.add(restorationSiteDao);
    }

    @Test
    @Order(2)
    void test_getSite() {
        String siteId = "siteId12";
        var restorationSiteRepo = new RestorationSiteRepositoryImpl(DatabaseConfig.getInstance());
        var site = restorationSiteRepo.get(siteId);
        assertNotNull(site);
        assertNull(site.aoITokens());
    }

    @Test
    @Order(3)
    void test_getAoI() {
        String siteId = "siteId12";
        var restorationSiteRepo = new RestorationSiteRepositoryImpl(DatabaseConfig.getInstance());
        var aoIBlocks = restorationSiteRepo.getAoI(siteId);
        assertEquals(3, aoIBlocks.size());
    }

    @AfterAll
    static void tearDown() {
        var restorationSiteRepo = new RestorationSiteRepositoryImpl(DatabaseConfig.getInstance());
        restorationSiteRepo.delete("siteId12");
    }
}
