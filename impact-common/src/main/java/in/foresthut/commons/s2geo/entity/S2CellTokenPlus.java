package in.foresthut.commons.s2geo.entity;

import com.google.common.geometry.S2Cell;
import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2Point;
import in.foresthut.commons.continentality.Oceans;
import in.foresthut.commons.elevation.ElevationFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record S2CellTokenPlus(String cellToken,
                              String wktPolygon,
                              double centerLat,
                              double centerLng,
                              double minElevationInMeters,
                              double maxElevationInMeters,
                              double distanceFromSeaInKM) {
    private static final Logger logger = LoggerFactory.getLogger(S2CellTokenPlus.class);
    private static final GeometryFactory geometryFactory = new GeometryFactory();
    private static final ElevationFinder elevationFinder = ElevationFinder.getInstance();
    private static final Oceans oceans = Oceans.getInstance();


    public static S2CellTokenPlus toS2TokenPlus(String cellToken) {
        try {
            logger.debug("Processing {} ....", cellToken);
            var polygonDetails = cellDetails(cellToken);
            logger.debug("Received polygon details {}", polygonDetails);
            Point point =
                    geometryFactory.createPoint(new Coordinate(polygonDetails.centerLong, polygonDetails.centerLat));

            final double[] elevations;
            elevations = elevationFinder.elevationOf(polygonDetails.wktPolygonString);
            logger.debug("Found elevations {}", elevations);
            final double distanceFromSea = oceans.distance(point);
            logger.debug("Found distance from sea {}km", distanceFromSea);
            return new S2CellTokenPlus(
                    cellToken, polygonDetails.wktPolygonString, polygonDetails.centerLat(), polygonDetails.centerLong(),
                    elevations[0], elevations[1], distanceFromSea);
        } catch (Exception e) {
            logger.error("Error: ", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts an S2 cell token to a WKT Polygon string.
     *
     * @return A WKT Polygon string representing the S2 cell token. Plus center point of the cell
     */
    public static PolygonDetails cellDetails(String s2CellToken) {

        // Step 1: Create an S2Cell from the S2CellId
        S2CellId s2CellId = S2CellId.fromToken(s2CellToken);
        S2Cell s2Cell = new S2Cell(s2CellId);

        S2LatLng center = s2CellId.toLatLng();

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

        return new PolygonDetails(center.latDegrees(), center.lngDegrees(), wktBuilder.toString());
    }

    public record PolygonDetails(double centerLat,
                          double centerLong,
                          String wktPolygonString) {
    }
}
