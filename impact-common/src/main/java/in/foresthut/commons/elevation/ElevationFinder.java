package in.foresthut.commons.elevation;

import com.nirvighna.www.commons.config.AppConfig;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.Position2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;

public class ElevationFinder {
    private static final Logger logger = LoggerFactory.getLogger(ElevationFinder.class);

    private static ElevationFinder instance;
    private static GridCoverage2D demCoverageS10E060, demCoverageN10E060;
    private static Geometry demS10E060Polygon, demN10E060Polygon;
    private static GeometryFactory geometryFactory = new GeometryFactory();
//    private static Raster S10E060tiffRaster, N10E060tiffRaster;
//    private static GridGeometry2D S10E060GridGeometry, N10E060GridGeometry;

    private ElevationFinder() {
        Hints.putSystemDefault(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);

        ClassLoader classLoader = getClass().getClassLoader();
        File tiffFileS10E060 = new File(AppConfig.getInstance().get("GMTED2010S10E060_150_tiff_path"));

        File tiffFileN10E060 = new File(AppConfig.getInstance().get("GMTED2010N10E060_150_tiff_path"));

        GeoTiffReader reader = null;
        try {
            reader = new GeoTiffReader(tiffFileS10E060);
            demCoverageS10E060 = reader.read(null);
            demS10E060Polygon = convertBoundsToGeometry(demCoverageS10E060.getEnvelope2D());
//            S10E060tiffRaster = demCoverageS10E060.getRenderedImage().getData();
//            S10E060GridGeometry = demCoverageS10E060.getGridGeometry();

            reader = new GeoTiffReader(tiffFileN10E060);
            demCoverageN10E060 = reader.read(null);
            demN10E060Polygon = convertBoundsToGeometry(demCoverageN10E060.getEnvelope2D());
//            N10E060tiffRaster = demCoverageN10E060.getRenderedImage().getData();
//            N10E060GridGeometry = demCoverageN10E060.getGridGeometry();
        } catch (IOException e) {
            logger.error("Error: ", e);
            throw new RuntimeException(e);
        }
        Runtime.getRuntime()
               .addShutdownHook(new Thread(() -> {
                   if (demCoverageS10E060 != null) demCoverageS10E060.dispose(false);
                   if (demCoverageN10E060 != null) demCoverageN10E060.dispose(false);
               }));
    }

    public synchronized static ElevationFinder getInstance() {
        if (instance == null) instance = new ElevationFinder();
        return instance;
    }

    public double elevationOf(double latitude, double longitude) {
        Coordinate coordinate = new Coordinate(longitude, latitude);
        Point point = geometryFactory.createPoint(coordinate);

        CoordinateReferenceSystem wgs84 = DefaultGeographicCRS.WGS84;
        Position2D posWorld = new Position2D(wgs84, longitude, latitude); // longitude supplied first

        try {
            if (point.within(demN10E060Polygon) || point.intersects(demN10E060Polygon)) {
                Raster N10E060tiffRaster = demCoverageN10E060.getRenderedImage().getData();
                GridGeometry2D N10E060GridGeometry = demCoverageN10E060.getGridGeometry();

                GridCoordinates2D posGrid = N10E060GridGeometry.worldToGrid(posWorld);
                // sample tiff data with at pixel coordinate
                double[] rasterData = new double[1];
                N10E060tiffRaster.getPixel(posGrid.x, posGrid.y, rasterData);
                return rasterData[0];
            } else if (point.within(demS10E060Polygon) || point.intersects(demS10E060Polygon)) {
                Raster S10E060tiffRaster = demCoverageS10E060.getRenderedImage().getData();
                GridGeometry2D S10E060GridGeometry = demCoverageS10E060.getGridGeometry();

                GridCoordinates2D posGrid = S10E060GridGeometry.worldToGrid(posWorld);
                // sample tiff data with at pixel coordinate
                double[] rasterData = new double[1];
                S10E060tiffRaster.getPixel(posGrid.x, posGrid.y, rasterData);
                return rasterData[0];
            } else  {
                return 0d;
            }
        }catch (Exception e) {
            logger.error("Error: ", e);
            return 0d;
        }
    }

    public double[] elevationOf(String wktPolygon) {
        Geometry polygon = null;
        CoordinateReferenceSystem crs = null;
        // target polygon
        try {
            polygon = new WKTReader().read(wktPolygon);
            // Define a Coordinate Reference System (CRS), e.g., WGS84.
            crs = CRS.decode("EPSG:4326");


            // Create a ReferencedEnvelope with the desired coordinates and CRS.
            // For example, a bounding box from (10, 20) to (30, 40).
            ReferencedEnvelope cropEnvelope = new ReferencedEnvelope();
            cropEnvelope.setCoordinateReferenceSystem(crs);
            cropEnvelope.setBounds(JTS.toEnvelope(polygon));

            CoverageProcessor processor = CoverageProcessor.getInstance();


//        System.out.println(convertBoundsToWKT(demCoverageS10E060.getEnvelope2D()));
//        System.out.println(convertBoundsToWKT(demCoverageN10E060.getEnvelope2D()));
//        System.out.println(polygon.toText());

            if (polygon.within(demS10E060Polygon)) {
                return getElevations(processor, cropEnvelope, demCoverageS10E060);
            } else if (polygon.within(demN10E060Polygon)) {
                return getElevations(processor, cropEnvelope, demCoverageN10E060);
            } else {
                var intersection = polygon.intersection(demS10E060Polygon);

                ReferencedEnvelope cropIntersectionEnvelope = new ReferencedEnvelope();
                cropIntersectionEnvelope.setCoordinateReferenceSystem(crs);
                cropIntersectionEnvelope.setBounds(JTS.toEnvelope(intersection));

                return getElevations(processor, cropIntersectionEnvelope, demCoverageS10E060);

            }
        } catch (Exception e) {
            logger.error("Error: ", e);
            throw new RuntimeException(e);
        }
    }

    private static double[] getElevations(CoverageProcessor processor, ReferencedEnvelope cropEnvelope,
                                          GridCoverage2D demCoverage) {
        // Manually create the operation and parameters
        ParameterValueGroup paramN10E060 = processor.getOperation("CoverageCrop")
                                                    .getParameters();

        paramN10E060.parameter("Source")
                    .setValue(demCoverage);
        paramN10E060.parameter("Envelope")
                    .setValue(cropEnvelope);

        // Execute the operation
        GridCoverage2D clippedCoverage = (GridCoverage2D) processor.doOperation(paramN10E060);

        Raster raster = clippedCoverage.getRenderedImage()
                                       .getData();

        double minElevation = Double.POSITIVE_INFINITY;
        double maxElevation = Double.NEGATIVE_INFINITY;

        for (int y = raster.getMinY(); y < raster.getMinY() + raster.getHeight(); y++) {
            for (int x = raster.getMinX(); x < raster.getMinX() + raster.getWidth(); x++) {
                double elevation = raster.getSampleDouble(x, y, 0); // Assuming single band DEM

                // Handle NoData values if necessary (e.g., if a specific value represents
                // NoData)
                // if (elevation != NO_DATA_VALUE) {
                if (elevation < minElevation) {
                    minElevation = elevation;
                }
                if (elevation > maxElevation) {
                    maxElevation = elevation;
                }
                // }
            }

        }
        raster = null;
        //logger.debug("Min elevation: {}m Max elevation: {}m", minElevation, maxElevation);
        return new double[]{minElevation, maxElevation};

    }

    private static String convertBoundsToWKT(ReferencedEnvelope bounds) {
        // Get the coordinates of the bounds
        Coordinate[] coordinates = new Coordinate[5];
        coordinates[0] = new Coordinate(bounds.getMinX(), bounds.getMinY());
        coordinates[1] = new Coordinate(bounds.getMaxX(), bounds.getMinY());
        coordinates[2] = new Coordinate(bounds.getMaxX(), bounds.getMaxY());
        coordinates[3] = new Coordinate(bounds.getMinX(), bounds.getMaxY());
        coordinates[4] = new Coordinate(bounds.getMinX(), bounds.getMinY()); // Close the polygon

        // Create a LinearRing from the coordinates
        // A LinearRing is a closed LineString that defines the boundary of a Polygon
        org.locationtech.jts.geom.LinearRing shell = geometryFactory.createLinearRing(coordinates);

        // Create the Polygon
        Polygon polygon = geometryFactory.createPolygon(shell, null); // No holes

        // Use WKTWriter to convert the Polygon to a WKT string
        WKTWriter wktWriter = new WKTWriter();
        return wktWriter.write(polygon);
    }

    private static Geometry convertBoundsToGeometry(ReferencedEnvelope bounds) {
        // Get the coordinates of the bounds
        Coordinate[] coordinates = new Coordinate[5];
        coordinates[0] = new Coordinate(bounds.getMinX(), bounds.getMinY());
        coordinates[1] = new Coordinate(bounds.getMaxX(), bounds.getMinY());
        coordinates[2] = new Coordinate(bounds.getMaxX(), bounds.getMaxY());
        coordinates[3] = new Coordinate(bounds.getMinX(), bounds.getMaxY());
        coordinates[4] = new Coordinate(bounds.getMinX(), bounds.getMinY()); // Close the polygon

        // Create a LinearRing from the coordinates
        // A LinearRing is a closed LineString that defines the boundary of a Polygon
        org.locationtech.jts.geom.LinearRing shell = geometryFactory.createLinearRing(coordinates);

        // Create the Polygon
        Polygon polygon = geometryFactory.createPolygon(shell, null); // No holes
        return polygon;
    }
}
