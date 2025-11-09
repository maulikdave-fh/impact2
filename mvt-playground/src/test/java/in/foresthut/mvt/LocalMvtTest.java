package in.foresthut.mvt;

import com.wdtinc.mapbox_vector_tile.adapt.jts.JtsAdapter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.TileGeomResult;
import com.wdtinc.mapbox_vector_tile.adapt.jts.UserDataKeyValueMapConverter;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTReader;
import com.wdtinc.mapbox_vector_tile.VectorTile;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerBuild;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerParams;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerProps;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class LocalMvtTest {

    @Test
    public void generateSampleMvt() throws Exception {
        // Example WKT polygon around Pune, India
        String wkt = "POLYGON((" +
                     "73.580 18.236," +
                     "73.581 18.236," +
                     "73.581 18.237," +
                     "73.580 18.237," +
                     "73.580 18.236))";

        int zoom = 12, x = 3368, y = 1860;
        String layerName = "polygons";

        // Step 1: Read geometry
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        Geometry geom = new WKTReader(geometryFactory).read(wkt);

        // Step 2: Reproject to EPSG:3857 (Web Mercator)
        Geometry geom3857 = reprojectTo3857(geom);

        Coordinate centroidLL = geom.getCentroid().getCoordinate(); // use the original lon/lat geometry
        TileIndex tile = tileForLonLat(centroidLL.x, centroidLL.y, zoom);


        // Step 3: Compute tile bounds in EPSG:3857
        Envelope tileEnv = getTileBoundsNew(tile.x,  tile.y, zoom);

        System.out.println("geom3857 envelope: " + geom3857.getEnvelopeInternal());
        System.out.println("tile envelope: " + tileEnv);


        // Step 4: Clip + transform geometry into tile coordinates
        MvtLayerParams layerParams = new MvtLayerParams(); // default extent 4096
        List<Geometry> geoms = Collections.singletonList(geom3857);
        TileGeomResult tileGeom = JtsAdapter.createTileGeom(geoms, tileEnv, geom3857.getFactory(), layerParams, g -> true);

        System.out.println("TileGeom mvtGeoms size: " + tileGeom.mvtGeoms.size());

        if (tileGeom.mvtGeoms.isEmpty()) {
            System.out.println("⚠️ No geometry survived clipping — tile will be empty!");
        }



        // Step 5: Build MVT layer
        VectorTile.Tile.Builder tileBuilder = VectorTile.Tile.newBuilder();
        MvtLayerProps layerProps = new MvtLayerProps();
        VectorTile.Tile.Layer.Builder layerBuilder = MvtLayerBuild.newLayerBuilder(layerName, layerParams);

        List<VectorTile.Tile.Feature> features = JtsAdapter.toFeatures(
                tileGeom.mvtGeoms, layerProps, new UserDataKeyValueMapConverter()
        );

        System.out.println("Converted features count: " + features.size());

        layerBuilder.addAllFeatures(features);
        MvtLayerBuild.writeProps(layerBuilder, layerProps);
        layerBuilder.setName(layerName);
        layerBuilder.setVersion(2);
        layerBuilder.setExtent(layerParams.extent);

        System.out.println("Layer features added: " + layerBuilder.getFeaturesCount());

        MvtLayerBuild.writeProps(layerBuilder, layerProps);
        tileBuilder.addLayers(layerBuilder.build());
        System.out.println("Tile layers count: " + tileBuilder.getLayersCount());


        // Step 6: Serialize to file
        byte[] mvtBytes = tileBuilder.build().toByteArray();
        Path outputPath = Path.of("target/sample_12_2884_1837.mvt");
        Files.createDirectories(outputPath.getParent());
        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
            fos.write(mvtBytes);
        }

        System.out.println("✅ MVT tile written to: " + outputPath.toAbsolutePath());
        System.out.println("Size: " + mvtBytes.length + " bytes");
    }

    /** ---- Helpers ---- **/

    private static Envelope getTileBounds(int x, int y, int zoom) {
        double totalTiles = Math.pow(2, zoom);
        double earthCircumference = 40075016.6855787; // meters
        double tileWidth = earthCircumference / totalTiles;
        double tileHeight = earthCircumference / totalTiles;

        double minX = -earthCircumference / 2 + x * tileWidth;
        double maxX = minX + tileWidth;
        double maxY = earthCircumference / 2 - y * tileHeight;
        double minY = maxY - tileHeight;

        return new Envelope(minX, maxX, minY, maxY);
    }

    private static Geometry reprojectTo3857(Geometry geometry) {
        org.locationtech.proj4j.CRSFactory crsFactory = new org.locationtech.proj4j.CRSFactory();

        var src = crsFactory.createFromParameters("EPSG:4326", "+proj=longlat +datum=WGS84 +no_defs");
        //var dst = crsFactory.createFromParameters("EPSG:3857", "+proj=merc +datum=WGS84 +units=m +no_defs");
        var dst = crsFactory.createFromParameters("EPSG:3857",
                                                  "+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 "
                                                  + "+x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +no_defs");

        var ctFactory = new org.locationtech.proj4j.CoordinateTransformFactory();
        var transform = ctFactory.createTransform(src, dst);

        GeometryFactory factory = geometry.getFactory();
        Coordinate[] coords = geometry.getCoordinates();
        Coordinate[] newCoords = new Coordinate[coords.length];
        var srcCoord = new org.locationtech.proj4j.ProjCoordinate();
        var dstCoord = new org.locationtech.proj4j.ProjCoordinate();

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

    private static Envelope getTileBoundsNew(int x, int y, int z) {
        double n = Math.pow(2.0, z);
        double tileSize = 40075016.68557849 / n; // total Web Mercator width in meters

        double minX = -20037508.342789244 + x * tileSize;
        double maxX = -20037508.342789244 + (x + 1) * tileSize;
        double maxY = 20037508.342789244 - y * tileSize;
        double minY = 20037508.342789244 - (y + 1) * tileSize;

        return new Envelope(minX, maxX, minY, maxY);
    }

    /** Simple data class to hold tile indices */
    public static class TileIndex {
        public final int x;
        public final int y;
        public final int z;

        public TileIndex(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public String toString() {
            return String.format("TileIndex{z=%d, x=%d, y=%d}", z, x, y);
        }
    }

    /** Converts lon/lat to XYZ tile index (Web Mercator / Slippy Map convention) */
    public static TileIndex tileForLonLat(double lon, double lat, int zoom) {
        int x = (int) Math.floor((lon + 180.0) / 360.0 * (1 << zoom));
        int y = (int) Math.floor(
                (1.0 - Math.log(Math.tan(Math.toRadians(lat)) + 1.0 / Math.cos(Math.toRadians(lat))) / Math.PI)
                / 2.0 * (1 << zoom)
        );
        return new TileIndex(x, y, zoom);
    }


}

