package in.foresthut.service;

import com.nirvighna.mongodb.commons.DatabaseConfig;
import in.foresthut.aoi.AoI;
import in.foresthut.infra.geohash.repository.GeoHashRepository;
import in.foresthut.infra.geohash.repository.GeoHashRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AoITest {
    private final String forestHut =
            "POLYGON ((73.5805054 18.2362594, 73.5803734 18.2361013, 73.5801464 18.235764, 73.5800897 18.235371, 73.580051 18.2351999, 73.5800175 18.2350338, 73.5800674 18.2346674, 73.5800905 18.2344901, 73.5800763 18.234334, 73.5800944 18.2341378, 73.5802172 18.2340208, 73.5802982 18.2339462, 73.5803941 18.2337463, 73.5804511 18.2337139, 73.580525 18.2336816, 73.5806198 18.2337052, 73.5807623 18.2337447, 73.5809229 18.2338173, 73.5809221 18.2360755, 73.5806839 18.2361839, 73.5805054 18.2362594))";

    @Test
    void testForestHut_shouldReturnAtleastNineBlocks() throws ParseException {
        GeoHashRepository geoHashRepository = new GeoHashRepositoryImpl(DatabaseConfig.getInstance());

        AoI aoi = new AoI(forestHut, geoHashRepository);
        var actualResult = aoi.getAoI();

        System.out.println("Number of blocks: " + actualResult.size());
        WKTReader reader = new WKTReader(new GeometryFactory());
        List<Polygon> polygons = new ArrayList<>();
        for (var block : actualResult) {
            polygons.add((Polygon) reader.read(block.wktPolygon()));
        }

        GeometryFactory factory = new GeometryFactory();
        GeometryCollection geometryCollection = factory.createGeometryCollection(polygons.stream()
                                                                                         .toArray(Polygon[]::new));
        Geometry unionResult = geometryCollection.union();
        System.out.println(unionResult.toText());
        assertTrue(actualResult.size() >= 9);
    }
}
