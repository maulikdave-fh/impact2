package in.foresthut.aoi;

import com.google.common.geometry.S2Cell;
import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2Point;
import com.nirvighna.mongodb.commons.DatabaseConfig;
import in.foresthut.infra.s2token.repository.S2CellTokenPlusRepo;
import in.foresthut.infra.s2token.repository.S2CellTokenPlusRepoImpl;
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

public class AoIS2Test {
    private final String forestHut =
            "POLYGON ((73.5805054 18.2362594, 73.5803734 18.2361013, 73.5801464 18.235764, 73.5800897 18.235371, 73.580051 18.2351999, 73.5800175 18.2350338, 73.5800674 18.2346674, 73.5800905 18.2344901, 73.5800763 18.234334, 73.5800944 18.2341378, 73.5802172 18.2340208, 73.5802982 18.2339462, 73.5803941 18.2337463, 73.5804511 18.2337139, 73.580525 18.2336816, 73.5806198 18.2337052, 73.5807623 18.2337447, 73.5809229 18.2338173, 73.5809221 18.2360755, 73.5806839 18.2361839, 73.5805054 18.2362594))";

    private final String mumbaiBlock =
            "POLYGON ((72.79541015625 18.9129638671875, 72.806396484375 18.9129638671875, 72.806396484375 18.907470703125, 72.79541015625 18.907470703125, 72.79541015625 18.9129638671875))";

    @Test
    void testForestHut_shouldReturnAtleastNineBlocks() throws ParseException {
        S2CellTokenPlusRepo cellTokenRepo = new S2CellTokenPlusRepoImpl(DatabaseConfig.getInstance());

        AoIProvider aoi = new S2GeometryBasedAoIProvider(cellTokenRepo);
        var actualResult = aoi.getAoI(forestHut);

        System.out.println("Number of blocks: " + actualResult.size());
        WKTReader reader = new WKTReader(new GeometryFactory());
        List<Polygon> polygons = new ArrayList<>();
        for (var block : actualResult.keySet()) {
            polygons.add((Polygon) reader.read(toWktString(block)));
        }

        GeometryFactory factory = new GeometryFactory();
        GeometryCollection geometryCollection = factory.createGeometryCollection(polygons.stream()
                                                                                         .toArray(Polygon[]::new));
        Geometry unionResult = geometryCollection.union();
        System.out.println(unionResult.toText());
        assertTrue(actualResult.size() >= 9);
    }

    @Test
    void testMumbaiBlock() throws ParseException {
        S2CellTokenPlusRepo cellTokenRepo = new S2CellTokenPlusRepoImpl(DatabaseConfig.getInstance());

        AoIProvider aoi = new S2GeometryBasedAoIProvider(cellTokenRepo);
        var actualResult = aoi.getAoI(mumbaiBlock);


        System.out.println("Number of blocks: " + actualResult.size());
        WKTReader reader = new WKTReader(new GeometryFactory());
        List<Polygon> polygons = new ArrayList<>();
        for (var block : actualResult.keySet()) {
            polygons.add((Polygon) reader.read(toWktString(block)));
        }

        GeometryFactory factory = new GeometryFactory();
        GeometryCollection geometryCollection = factory.createGeometryCollection(polygons.stream()
                                                                                         .toArray(Polygon[]::new));
        Geometry unionResult = geometryCollection.union();
        System.out.println(unionResult.toText());
        assertTrue(actualResult.size() >= 9);

    }

    void testAoIUnion() {

    }

    private String toWktString(String s2CellToken) {
        // Step 1: Create an S2Cell from the S2CellId
        S2Cell s2Cell = new S2Cell(S2CellId.fromToken(s2CellToken));

        // A StringBuilder is more efficient for building the WKT string
        StringBuilder wktBuilder = new StringBuilder("POLYGON((");

        // Step 2 and 3: Get each vertex and convert it to lat/lng degrees
        // S2 stores vertices in a counter-clockwise (CCW) order, which is
        // the standard for WKT representation of outer rings.
        for (int i = 0; i < 4; i++) {
            S2Point vertex = s2Cell.getVertex(i);
            S2LatLng s2LatLng = new S2LatLng(vertex);
            wktBuilder.append(s2LatLng.lngDegrees());
            wktBuilder.append(" ");
            wktBuilder.append(s2LatLng.latDegrees());
            wktBuilder.append(",");
        }

        // Step 4: Close the polygon loop by repeating the first vertex
        S2Point firstVertex = s2Cell.getVertex(0);
        S2LatLng firstS2LatLng = new S2LatLng(firstVertex);
        wktBuilder.append(firstS2LatLng.lngDegrees());
        wktBuilder.append(" ");
        wktBuilder.append(firstS2LatLng.latDegrees());

        // Finalize the WKT string
        wktBuilder.append("))");

        return wktBuilder.toString();

    }
}
