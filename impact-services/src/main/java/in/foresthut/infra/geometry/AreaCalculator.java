package in.foresthut.infra.geometry;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.geometry.jts.JTS;
import org.geotools.measure.Measure;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import si.uom.SI;

public class AreaCalculator {
    private final static Logger logger = LoggerFactory.getLogger(AreaCalculator.class);

    public static double area(String wktPolygon) {
        GeometryFactory geometryFact = new GeometryFactory();
        WKTReader wktReader = new WKTReader(geometryFact);
        try {
            Geometry site = wktReader.read(wktPolygon);

            Point centroid = site.getCentroid();
            String autoCode = "AUTO:42001," + centroid.getX() + "," + centroid.getY();
            CoordinateReferenceSystem auto = CRS.decode(autoCode);
            MathTransform transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, auto);
            Geometry projected = JTS.transform(site, transform);
            var measure = new Measure(projected.getArea(), SI.SQUARE_METRE);
            return (measure.doubleValue() / 10000d);
        } catch (Exception ex) {
            logger.error("Error while calculating area for {}", wktPolygon, ex);
            return 0.0d;
        }
    }
}
