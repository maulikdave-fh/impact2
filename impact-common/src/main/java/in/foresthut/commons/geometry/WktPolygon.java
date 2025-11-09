package in.foresthut.commons.geometry;

import com.mongodb.client.model.geojson.PolygonCoordinates;
import com.mongodb.client.model.geojson.Position;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.geometry.jts.JTS;
import org.geotools.measure.Measure;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.util.LineStringExtracter;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.operation.polygonize.Polygonizer;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import si.uom.SI;

import java.util.*;
import java.util.stream.Collectors;


/**
 * WKT Polygon and related operations
 *
 * @author Maulik Dave
 */

public class WktPolygon {
    private static final double MAX_AREA = 0.05d;
    private final static Logger logger = LoggerFactory.getLogger(WktPolygon.class);
    private final static WKTReader wktReader;

    static {
        wktReader = new WKTReader();
    }

    private final Geometry poly;

    /**
     * Expects a valid WKT (Well Known Text) Polygon
     *
     * @param wktPolygon a valid WKT polygon
     */
    public WktPolygon(String wktPolygon) {
        Objects.requireNonNull(wktPolygon, "WKT polygon cannot be null");
        if (wktPolygon.isBlank()) throw new IllegalArgumentException("WKT polygon cannot be blank");

        try {
            this.poly = wktReader.read(wktPolygon);
            if (!poly.isValid()) throw new IllegalArgumentException("Invalid wkt polygon");
        } catch (ParseException ex) {
            final String traceId = UUID.randomUUID().toString();
            logger.error("[Error-Trace-Id] {} Invalid wkt polygon {}", traceId, wktPolygon, ex);
            throw new RuntimeException("Invalid wkt polygon: ", ex);
        }
    }

    public static boolean isValid(String wktPolygon) {
        try {
            return new WKTReader().read(wktPolygon).isValid();
        } catch (ParseException ex) {
            final String traceId = UUID.randomUUID().toString();
            logger.error("[Error-Trace-Id] {} Invalid wkt polygon {}", traceId, wktPolygon, ex);
            throw new RuntimeException("Invalid wkt polygon: ", ex);
        }
    }

    /**
     * Returns pre-configured optimal max area of individual splits
     *
     * @return pre-configured max area
     */
    public static double getMaxArea() {
        return MAX_AREA;
    }

    /**
     * Combines multiple polygons into one.
     *
     * @param polygons list of polygons in WKT format
     * @param simplify true if combined polygon to be simplified, false otherwise
     * @return A combined polygon in wkt format
     */
    public static String combine(List<String> polygons, boolean simplify) {
        GeoJsonReader geoJsonReader = new GeoJsonReader();
        GeometryFactory geoFac = new GeometryFactory();
        List<Geometry> geometries = new ArrayList<>();

        for (var polygon : polygons) {
            Geometry geometry;
            try {
                geometry = geoJsonReader.read(polygon);
                geometries.add(geometry);
            } catch (ParseException e) {
                String traceId = UUID.randomUUID().toString();
                logger.error("[Error-Trace-Id] {} Error while reading polygon {}", traceId, polygon, e);
                throw new RuntimeException("Error while reading polygon", e);
            }
        }

        GeometryCollection geometryCollection = (GeometryCollection) geoFac.buildGeometry(geometries);
        var combined = geometryCollection.union();
        if (!simplify) return combined.toText();

        // Define the precision model (e.g., round to the nearest 1 decimal place)
        PrecisionModel precisionModel = new PrecisionModel(10.0); // 10.0 means rounding to 1 decimal place

        // Create a GeometryPrecisionReducer
        GeometryPrecisionReducer reducer = new GeometryPrecisionReducer(precisionModel);
        reducer.setRemoveCollapsedComponents(true); // Remove collapsed components
        reducer.setPointwise(false); // Allow topology rebuilding

        // Reduce the geometry
        // Make it CCW
        Geometry reducedPolygon = reducer.reduce(combined).reverse();
        return reducedPolygon.toText();
    }

    /**
     * Provides intersection between two polygons
     *
     * @param otherWktPolygon WktPolygon
     * @return intersection between two WktPolygons
     */
    public String intersectionWith(WktPolygon otherWktPolygon) {
        Geometry otherWktPoly = otherWktPolygon.poly;
        if (!otherWktPoly.isValid()) throw new IllegalArgumentException("Wkt polygon is not valid");
        var intersection = this.poly.intersection(otherWktPoly).toText();
        return intersection.equals("POLYGON EMPTY") ? "" : intersection;
    }

    public List<List<Double>> coordinates() {
        List<List<Double>> coordinates = new ArrayList<>();
        for (var coordinate : poly.getCoordinates()) {
            coordinates.add(new ArrayList<>(Arrays.asList(coordinate.x, coordinate.y)));
        }
        return coordinates;
    }

    public List<PolygonCoordinates> coordinatesForMultiPolygon() {
        List<PolygonCoordinates> list = new ArrayList<>();

        if (poly instanceof MultiPolygon multiPolygon) {
            for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);
                list.add(convertPolygon(polygon));
            }
        } else if (poly instanceof Polygon polygon) {
            list.add(convertPolygon(polygon));
        } else {
            throw new IllegalArgumentException("Unsupported geometry type: " + poly.getGeometryType());
        }

        return list;
    }

    private static PolygonCoordinates convertPolygon(Polygon polygon) {
        List<List<Position>> holes = new ArrayList<>();
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            LineString ring = polygon.getInteriorRingN(i);
            holes.add(toCoordinateList(ring));
        }

        PolygonCoordinates pc = new PolygonCoordinates(toCoordinateList(polygon.getExteriorRing()),holes);
        return pc;
    }

    private static List<Position> toCoordinateList(LineString ring) {
        List<Position> coords = new ArrayList<>();
        Coordinate[] jtsCoords = ring.getCoordinates();
        for (Coordinate c : jtsCoords) {
            coords.add(new Position(c.getX(), c.getY()));
        }
        return coords;
    }

    /**
     * For point in polygon check.
     *
     * @param latitude  latitude
     * @param longitude longitude
     * @return true if the coordinate is falls inside the polygon
     */
    public boolean contains(double latitude, double longitude) {
        Coordinate point = new Coordinate(longitude, latitude, 0);
        Geometry location = new GeometryFactory().createPoint(point);
        return this.poly.contains(location);
    }

    public boolean isPartOf(WktPolygon outerWktPoly) {
        return outerWktPoly.poly.contains(poly);
    }

    /**
     * Splits the polygon into multiple polygons. Each smaller polygon will be no
     * greater than MAX_AREA
     *
     * @return list of polygons in WKT format
     */
    public List<String> split() {
        List<Geometry> polys = split(poly, MAX_AREA);
        return polys.stream().map(Geometry::toText).collect(Collectors.toList());
    }

    public List<String> split(double maxArea) {
        List<Geometry> polys = split(poly, maxArea);
        return polys.stream().map(Geometry::toText).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return poly.toString();
    }

    private List<Geometry> split(Geometry inputPoly, double maxArea) {
        List<Geometry> result = new ArrayList<>();
        if (inputPoly.getArea() <= maxArea) {
            // GBIF needs CCW (Counter ClockWise) polygon.
            // Make polygon CCW before adding
            result.add(inputPoly.reverse());
            return result;
        }
        Geometry nodedLinework = inputPoly.getBoundary().union(bisector(inputPoly));
        Geometry[] polys = polygonize(nodedLinework);

        // Only keep polygons which are inside the input
        List<Geometry> output = new ArrayList<>();
        for (Geometry poly : polys) {
            if (inputPoly.contains(poly.getInteriorPoint())) {
                output.add(poly);
            }
        }

        for (var poly : output) {
            result.addAll(split(poly, maxArea));
        }
        return result;
    }

    private Geometry bisector(Geometry targetPolygon) {
        Point center = targetPolygon.getCentroid();
        double width = targetPolygon.getEnvelopeInternal().getWidth();
        double height = targetPolygon.getEnvelopeInternal().getHeight();
        Coordinate c0 = null;
        Coordinate c1 = null;
        if (width >= height) {
            c0 = new Coordinate(center.getX(), targetPolygon.getEnvelopeInternal().getMaxY());
            c1 = new Coordinate(center.getX(), targetPolygon.getEnvelopeInternal().getMinY());
        } else {
            c0 = new Coordinate(targetPolygon.getEnvelopeInternal().getMaxX(), center.getY());
            c1 = new Coordinate(targetPolygon.getEnvelopeInternal().getMinX(), center.getY());
        }
        return new GeometryFactory().createLineString(new Coordinate[]{c0, c1});
    }

    private Geometry[] polygonize(Geometry geometry) {
        List<?> lines = LineStringExtracter.getLines(geometry);
        Polygonizer polygonizer = new Polygonizer();
        polygonizer.add(lines);
        Collection<?> polys = polygonizer.getPolygons();
        return GeometryFactory.toPolygonArray(polys);
    }

    public double areaInDegrees() {
        return poly.getArea();
    }

    public double areaInHectares() {
        try {
            Point centroid = poly.getCentroid();
            String autoCode = "AUTO:42001," + centroid.getX() + "," + centroid.getY();
            CoordinateReferenceSystem auto = CRS.decode(autoCode);
            MathTransform transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, auto);
            Geometry projed = JTS.transform(poly, transform);
            var measure = new Measure(projed.getArea(), SI.SQUARE_METRE);
            return measure.doubleValue() * 0.0001;
        } catch (Exception ex) {
            final String traceId = UUID.randomUUID().toString();
            logger.error("[Error-Trace-Id] {} Error while calculating area of a polygon", traceId, ex);
            throw new RuntimeException("Error while calculating area of a polygon",  ex);
        }
    }

    public Geometry getGeometry() {
        return poly;
    }
}
