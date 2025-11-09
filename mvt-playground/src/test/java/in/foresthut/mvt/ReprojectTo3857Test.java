package in.foresthut.mvt;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.proj4j.*;

import static org.junit.jupiter.api.Assertions.*;

public class ReprojectTo3857Test {

    private static Geometry reprojectTo3857(Geometry geometry) {
        CRSFactory crsFactory = new CRSFactory();

        CoordinateReferenceSystem src = crsFactory.createFromParameters(
                "EPSG:4326",
                "+proj=longlat +datum=WGS84 +no_defs"
        );

        CoordinateReferenceSystem dst = crsFactory.createFromParameters(
                "EPSG:3857",
                "+proj=merc +lon_0=0 +k=1 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs"
        );

        CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
        CoordinateTransform transform = ctFactory.createTransform(src, dst);

        GeometryFactory factory = geometry.getFactory();
        Coordinate[] coords = geometry.getCoordinates();
        Coordinate[] newCoords = new Coordinate[coords.length];
        ProjCoordinate srcCoord = new ProjCoordinate();
        ProjCoordinate dstCoord = new ProjCoordinate();

        for (int i = 0; i < coords.length; i++) {
            srcCoord.x = coords[i].x;
            srcCoord.y = coords[i].y;
            transform.transform(srcCoord, dstCoord);
            newCoords[i] = new Coordinate(dstCoord.x, dstCoord.y);
        }

        if (geometry instanceof Polygon)
            return factory.createPolygon(newCoords);
        else if (geometry instanceof LineString)
            return factory.createLineString(newCoords);
        else if (geometry instanceof Point)
            return factory.createPoint(newCoords[0]);
        else
            return factory.createGeometry(geometry);
    }

    @Test
    public void testPointReprojection() throws Exception {
        String wkt = "POINT (73.5805054 18.2362594)";
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
        Geometry geom = new WKTReader(gf).read(wkt);

        Geometry projected = reprojectTo3857(geom);
        Coordinate c = projected.getCoordinate();

        System.out.printf("EPSG:4326 -> EPSG:3857: %.2f, %.2f%n", c.x, c.y);

        // Sanity check: expected 73°E ~ 8.19e6m, 18°N ~ 2.05e6m
        assertTrue(c.x > 8_000_000 && c.x < 8_300_000, "Unexpected X coordinate");
        assertTrue(c.y > 2_000_000 && c.y < 2_100_000, "Unexpected Y coordinate");
    }

    @Test
    public void testPolygonReprojection() throws Exception {
        String wkt = "POLYGON((73.580 18.236, 73.581 18.236, 73.581 18.237, 73.580 18.237, 73.580 18.236))";
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
        Geometry geom = new WKTReader(gf).read(wkt);

        Geometry projected = reprojectTo3857(geom);
        Envelope env = projected.getEnvelopeInternal();

        System.out.printf("Polygon envelope in meters: [%.1f, %.1f, %.1f, %.1f]%n",
                          env.getMinX(), env.getMaxX(), env.getMinY(), env.getMaxY());

        assertTrue(env.getWidth() > 100, "Projected polygon too small");
        assertTrue(env.getHeight() > 100, "Projected polygon too small");
    }
}
