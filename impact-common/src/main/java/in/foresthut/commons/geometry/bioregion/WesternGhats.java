package in.foresthut.commons.geometry.bioregion;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class WesternGhats implements Bioregion {
    private static final String code = "IM2";
    private static final String name = "Indian Tropical Coastal Forests";
    private static Geometry polygon, bufferPolygon;
    private static WKTReader wktReader;
    private static WesternGhats instance;
    private String wktPolygonString = null;

    private WesternGhats() {
        wktReader = new WKTReader();
        wktPolygonString = getWKTString();
        try {
            polygon = wktReader.read(wktPolygonString);
            //Buffered area - around 20km+
            bufferPolygon = polygon.buffer(0.2);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized static WesternGhats getInstance() {
        if (instance == null) instance = new WesternGhats();
        return instance;
    }

    public Geometry polygon() {
        return polygon;
    }

    public Geometry bufferPolygon() {
        return bufferPolygon;
    }

    public boolean contains(double latitude, double longitude) {
        if (polygon.isValid()) {
            GeometryFactory geometryFactory = new GeometryFactory();
            Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));

            return bufferPolygon.contains(point);
        }
        return false;
    }

    public boolean contains(String innerPolygonWktString) {
        try {
            var innerPolygon = wktReader.read(innerPolygonWktString);
            return polygon.contains(innerPolygon);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String getWKTString() {

        // Example of loading from a file (requires error handling)
        try (InputStream inputStream = WesternGhats.class.getClassLoader()
                                                         .getResourceAsStream("im2.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line)
                  .append("\n");
            }
            String longString = sb.toString();
            return longString;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
