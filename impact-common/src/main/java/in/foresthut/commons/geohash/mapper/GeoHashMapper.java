package in.foresthut.commons.geohash.mapper;

import ch.hsr.geohash.BoundingBox;
import ch.hsr.geohash.GeoHash;
import org.locationtech.jts.geom.*;

public class GeoHashMapper {
    public static Geometry toGeometry(String geoHashString){
        try {
            var geoHash = GeoHash.fromGeohashString(geoHashString);
            BoundingBox boundingBox = geoHash.getBoundingBox();

            double northWestX = boundingBox.getNorthWestCorner()
                                           .getLongitude();
            double northWestY = boundingBox.getNorthWestCorner()
                                           .getLatitude();
            double southEastX = boundingBox.getSouthEastCorner()
                                           .getLongitude();
            double southEastY = boundingBox.getSouthEastCorner()
                                           .getLatitude();

            // The four corner coordinates of the rectangle
            Coordinate[] coordinates = new Coordinate[]{new Coordinate(northWestX, northWestY), // Northwest
                                                        new Coordinate(southEastX, northWestY), // Northeast
                                                        new Coordinate(southEastX, southEastY), // Southeast
                                                        new Coordinate(northWestX, southEastY), // Southwest
                                                        new Coordinate(northWestX, northWestY)
                                                        // Close the ring by repeating the start point
            };

            // Create the LinearRing and Polygon
            final GeometryFactory geometryFactory = new GeometryFactory();
            LinearRing linearRing = geometryFactory.createLinearRing(coordinates);
            Polygon polygon = geometryFactory.createPolygon(linearRing);
            return polygon;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
