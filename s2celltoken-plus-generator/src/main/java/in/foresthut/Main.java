package in.foresthut;

import com.google.common.geometry.*;
import com.nirvighna.mongodb.commons.DatabaseConfig;
import in.foresthut.commons.geometry.bioregion.WesternGhats;
import in.foresthut.producer.TerrestrialS2GeoTokenProducer;
import in.foresthut.repository.S2CellTokenPlusRepo;
import in.foresthut.repository.S2CellTokenPlusRepoImpl;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final int MONGODB_INSERT_BATCH_SIZE = 2000;
    private static final int RECORDS_PER_RUN = 2000000;
    private static final int RUN_CYCLE = 6;

    public static void main(String[] args) {

        try {
            List<String> supportedCellTokens = getCellTokens();
            logger.info("Generated {} cell tokens", supportedCellTokens.size());

            S2CellTokenPlusRepo cellTokenPlusRepo = new S2CellTokenPlusRepoImpl(DatabaseConfig.getInstance());

            CountDownLatch latch = new CountDownLatch(RECORDS_PER_RUN);

            var producer = new TerrestrialS2GeoTokenProducer(cellTokenPlusRepo, latch);

            // Producers
            int startingIndex = (RECORDS_PER_RUN * (RUN_CYCLE - 1));
            int next =  startingIndex + MONGODB_INSERT_BATCH_SIZE;
            for (int i = startingIndex; i < (RECORDS_PER_RUN * RUN_CYCLE) && i <= supportedCellTokens.size(); i += MONGODB_INSERT_BATCH_SIZE) {
                var subList = supportedCellTokens.subList(i, next);
                producer.startProducing(subList);
                next = next + MONGODB_INSERT_BATCH_SIZE < supportedCellTokens.size() ? next + MONGODB_INSERT_BATCH_SIZE
                                                                                     : supportedCellTokens.size();
            }
            logger.info("Latch down {}", latch.getCount());
            latch.await();
            logger.info("Latch down {}", latch.getCount());
            if (latch.getCount() == 0) {
                logger.info("Exiting...");
                System.exit(0);
            }
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
    }

    private static List<String> getCellTokens() throws ParseException {
        List<String> tokens = new ArrayList<>();

        // 1. Get buffered polygon of Western Ghats
        Geometry bufferedPolygonWesternGhats = WesternGhats.getInstance()
                                                           .bufferPolygon();

        // 2. Convert it to S2Polygon
        S2Polygon s2Polygon = toS2Polygon(bufferedPolygonWesternGhats.toText());

        // 3. Find level 16 cells in the S2Polygon
        S2RegionCoverer coverer = S2RegionCoverer.builder()
                                                 .setMinLevel(16)
                                                 .setMaxLevel(16)
                                                 .setMaxCells(Integer.MAX_VALUE)
                                                 .build();
        S2CellUnion union = coverer.getCovering(s2Polygon);

        for (var cellId : union.cellIds()) {
            var cell = new S2Cell(cellId);

            if (s2Polygon.mayIntersect(cell)) {
                if (cellId.level() <= 16) {
                    var children = cellId.childrenAtLevel(16);
                    for (var child : children) {
                          tokens.add(child.toToken());
                    }
                }
            }
        }

        return tokens;
    }

    private static S2Polygon toS2Polygon(String wktString) throws ParseException {
        // 1. Parse the WKT string into a JTS Geometry object
        WKTReader wktReader = new WKTReader(new GeometryFactory());
        Geometry jtsGeometry = wktReader.read(wktString);

//        if (!(jtsGeometry instanceof org.locationtech.jts.geom.Polygon) || !(jtsGeometry instanceof org.locationtech.jts.geom.MultiPolygon)) {
//            throw new IllegalArgumentException("WKT string is not a Polygon: " + wktString);
//        }
        //org.locationtech.jts.geom.Polygon jtsPolygon = (org.locationtech.jts.geom.Polygon) jtsGeometry;

        // 2. Extract coordinates and build an S2Loop for the outer shell
        List<S2Point> shellPoints = new ArrayList<>();
        for (Coordinate coord : jtsGeometry.getCoordinates()) {
            // System.out.println(coord);
            S2LatLng s2LatLng = S2LatLng.fromDegrees(coord.y, coord.x);
            shellPoints.add(s2LatLng.toPoint());
        }
        S2Loop shellLoop = new S2Loop(shellPoints);
        shellLoop.normalize();
        // System.out.println(shellLoop);

        // Optional: Handle inner holes if needed. This example focuses on the outer
        // shell.
        // For holes, you would create additional S2Loop objects from the interior rings
        // and pass them to the S2Polygon constructor.

        // 3. Create the S2Polygon from the S2Loop
        return new S2Polygon(shellLoop);
    }

}
