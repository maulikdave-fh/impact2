package in.foresthut.aoi;

import com.google.common.geometry.*;
import com.nirvighna.www.commons.config.AppConfig;
import in.foresthut.commons.elevation.ElevationFinder;
import in.foresthut.commons.s2geo.entity.S2CellTokenPlus;
import in.foresthut.infra.s2token.repository.S2CellTokenPlusRepo;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class S2GeometryBasedAoIProvider implements AoIProvider {
    private static final Logger logger = LoggerFactory.getLogger(S2GeometryBasedAoIProvider.class);
    private final static WKTReader wktReader = new WKTReader();
    private static final double DISTANCE_FROM_SEA_RANGE = Double.valueOf(AppConfig.getInstance()
                                                                                  .get("distance.from.sea.limit"));
    private static final double LATITUDE_RANGE = Double.valueOf(AppConfig.getInstance()
                                                                         .get("latitude.limit"));

    private static final double ELEVATION_RANGE = Double.valueOf(AppConfig.getInstance().get("elevation.limit"));
    private final S2CellTokenPlusRepo cellTokenRepo;


    public S2GeometryBasedAoIProvider(S2CellTokenPlusRepo cellTokenRepo) {
        this.cellTokenRepo = cellTokenRepo;
    }

    @Override
    public Map<String, Boolean> getAoI(String wktPolygon) {
        final List<S2CellTokenPlus> aoiBlocks = new ArrayList<>();
        if (!validWkt(wktPolygon)) throw new IllegalArgumentException("Invalid Wkt polygon.");

        // 1. Get genesis block
        com.google.common.geometry.S2CellId genesisBlock = getGenesisBlock(wktPolygon);
        logger.info("Genesis block: {}", genesisBlock.toToken());
        S2CellTokenPlus genesisCellTokenPlus = cellTokenRepo.get(genesisBlock.toToken());
        if (genesisCellTokenPlus == null) {
            logger.error("Genesis block {} not found", genesisBlock.toToken());
            throw new RuntimeException("Genesis block {} not found.");
        }


        // 2. Get all blocks with the same elevation range and distance from sea
        double genesisBlockDistanceFromSea = genesisCellTokenPlus.distanceFromSeaInKM();
        double[] distanceFromSeaRange = {genesisBlockDistanceFromSea - DISTANCE_FROM_SEA_RANGE <=  0 ? 0.1 : genesisBlockDistanceFromSea - DISTANCE_FROM_SEA_RANGE,
                                         genesisBlockDistanceFromSea + DISTANCE_FROM_SEA_RANGE};

        double[] latitudeRange = {genesisCellTokenPlus.centerLat() + LATITUDE_RANGE,
                                  genesisCellTokenPlus.centerLat() - LATITUDE_RANGE};

//        double[] elevationRange =
//                {genesisCellTokenPlus.minElevationInMeters() - ELEVATION_RANGE, genesisCellTokenPlus.maxElevationInMeters() + ELEVATION_RANGE};

        double[] elevationRange = ElevationFinder.getInstance()
                                             .elevationOf(wktPolygon);
        elevationRange = new double[]{elevationRange[0] - ELEVATION_RANGE <= 0 ? 0.1 : elevationRange[0] - ELEVATION_RANGE, elevationRange[1] + ELEVATION_RANGE};

        aoiBlocks.addAll(cellTokenRepo.get(distanceFromSeaRange, elevationRange, latitudeRange));
        logger.info("Found {} AoI blocks for polygon '{}'.", aoiBlocks.size(), wktPolygon);
        Map<String, Boolean> result = new HashMap<>();
        S2Polygon restorationSitePolygon = toS2Polygon(wktPolygon);
        for (var aoiBlock : aoiBlocks) {
            S2CellId cellId = S2CellId.fromToken(aoiBlock.cellToken());
            S2Cell cell = new S2Cell(cellId);

            if (restorationSitePolygon.mayIntersect(cell)) {
                result.put(aoiBlock.cellToken(), true);
            } else {
                result.put(aoiBlock.cellToken(), false);
            }
        }

        return result;
    }

    @Override
    public String getAoIUnion(String restorationSiteWktPolygonString) {
        var aoIBlocks = getAoI(restorationSiteWktPolygonString);
        List<Geometry> polygons = new ArrayList<>();
        for (var block : aoIBlocks.keySet()) {
            try {
                polygons.add(wktReader.read(block));
            } catch (ParseException e) {
                logger.error("Error: ", e);
                throw new RuntimeException(e);
            }
        }
        GeometryFactory factory = new GeometryFactory();
        GeometryCollection geometryCollection = factory.createGeometryCollection(polygons.stream()
                                                                                         .toArray(Polygon[]::new));
        Geometry unionResult = geometryCollection.union();
        System.out.println(unionResult.toText());
        return unionResult.toText();
    }

    public S2CellId getGenesisBlock(String wktPolygon) {
        Point point = getCenter(wktPolygon);
        var latitude = point.getY();
        var longitude = point.getX();
        S2LatLng location = S2LatLng.fromDegrees(latitude, longitude);
        return S2CellId.fromLatLng(location)
                       .parent(16);
    }

    private Point getCenter(String wktPolygon) {
        Geometry polygon = null;
        try {
            polygon = wktReader.read(wktPolygon);
        } catch (ParseException e) {
            logger.error("Error parsing {}", wktPolygon, e);
            throw new RuntimeException(e);
        }
        return polygon.getCentroid();
    }

    private boolean validWkt(String wktPolygon) {
        try {
            return wktReader.read(wktPolygon)
                            .isValid();
        } catch (ParseException e) {
            return false;
        }
    }

    private S2Polygon toS2Polygon(String wktString) {
        // 1. Parse the WKT string into a JTS Geometry object
        Geometry jtsGeometry = null;
        try {
            jtsGeometry = wktReader.read(wktString);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }


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
