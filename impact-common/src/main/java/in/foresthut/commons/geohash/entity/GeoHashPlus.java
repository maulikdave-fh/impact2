package in.foresthut.commons.geohash.entity;

import ch.hsr.geohash.BoundingBox;
import ch.hsr.geohash.GeoHash;
import in.foresthut.commons.continentality.Oceans;
import in.foresthut.commons.elevation.ElevationFinder;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public record GeoHashPlus(String geoHashString,
                          String wktPolygon,
                          double northWestX,
                          double northWestY,
                          double southEastX,
                          double southEastY,
                          double minElevationInMeters,
                          double maxElevationInMeters,
                          double distanceFromSeaInKM) {
    private static final Logger logger = LoggerFactory.getLogger(GeoHashPlus.class);
    private static final ExecutorService threadPool = Executors.newVirtualThreadPerTaskExecutor();
    private static final GeometryFactory geometryFactory = new GeometryFactory();
    private static final WKTWriter wktWriter = new WKTWriter();

    public static GeoHashPlus toGeoHashPlus(GeoHash geoHash) {
        try {
            final Oceans oceans = Oceans.getInstance();

            logger.debug("Processing {} ....", geoHash.toBase32());
            var polygonDetails = getWktPolygon(geoHash);
            logger.debug("Received polygon {}", polygonDetails);
            Point point = geometryFactory.createPoint(new Coordinate(
                    geoHash.getOriginatingPoint()
                           .getLongitude(), geoHash.getOriginatingPoint()
                                                   .getLatitude()));

            final double[] elevations;
            elevations = ElevationFinder.getInstance().elevationOf(polygonDetails.get(0)
                                                                   .toString());
            logger.debug("Found elevations {}", elevations);
            final double distanceFromSea = oceans.distance(point);
            logger.debug("Found distance from sea {}km", distanceFromSea);
            return new GeoHashPlus(
                    geoHash.toBase32(), polygonDetails.get(0)
                                                      .toString(), Double.valueOf(polygonDetails.get(1)
                                                                                                .toString()),
                    Double.valueOf(polygonDetails.get(2)
                                                 .toString()), Double.valueOf(polygonDetails.get(3)
                                                                                            .toString()),
                    Double.valueOf(polygonDetails.get(4)
                                                 .toString()), elevations[0], elevations[1], distanceFromSea);
        } catch (Exception e) {
            logger.error("Error: ", e);
            throw new RuntimeException(e);
        }
    }

    private static List<Object> getWktPolygon(GeoHash geoHash) {
        BoundingBox boundingBox = geoHash.getBoundingBox();

        double northWestX = boundingBox.getNorthWestCorner()
                                       .getLongitude();
        double northWestY = boundingBox.getNorthWestCorner()
                                       .getLatitude();
        double southEastX = boundingBox.getSouthEastCorner()
                                       .getLongitude();
        double southEastY = boundingBox.getSouthEastCorner()
                                       .getLatitude();

        List<Object> result = new ArrayList<>();
        result.add(createWktPolygon(northWestX, northWestY, southEastX, southEastY));
        result.add(northWestX);
        result.add(northWestY);
        result.add(southEastX);
        result.add(southEastY);
        return result;
    }

    private static String createWktPolygon(double northwestX, double northwestY, double southeastX, double southeastY) {

        // The four corner coordinates of the rectangle
        Coordinate[] coordinates = new Coordinate[]{new Coordinate(northwestX, northwestY), // Northwest
                                                    new Coordinate(southeastX, northwestY), // Northeast
                                                    new Coordinate(southeastX, southeastY), // Southeast
                                                    new Coordinate(northwestX, southeastY), // Southwest
                                                    new Coordinate(northwestX, northwestY)
                                                    // Close the ring by repeating the start point
        };

        // Create the LinearRing and Polygon
        LinearRing linearRing = geometryFactory.createLinearRing(coordinates);
        Polygon polygon = geometryFactory.createPolygon(linearRing);

        // Convert the Polygon to a WKT string
        return wktWriter.write(polygon);
    }

}
