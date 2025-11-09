package in.foresthut.mvt;

import com.wdtinc.mapbox_vector_tile.VectorTile;
import com.wdtinc.mapbox_vector_tile.adapt.jts.*;
import com.wdtinc.mapbox_vector_tile.build.*;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.proj4j.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;


public class MvtGenerator {

    /** Debug helper: render the tile’s MVT geometry into a PNG */
    private void renderTilePreview(List<Geometry> mvtGeoms, int tileExtent, String outputPath) throws Exception {
        int width = tileExtent, height = tileExtent;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();

        // Background white
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);

        g2.setColor(Color.LIGHT_GRAY);
        g2.drawRect(0, 0, width - 1, height - 1);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 24));
        g2.drawString("z=12", 10, 30);


        // Draw polygon outlines
        g2.setColor(Color.RED);
        g2.setStroke(new BasicStroke(2));

        for (Geometry geom : mvtGeoms) {
            if (!(geom instanceof Polygon)) continue;
            Polygon poly = (Polygon) geom;
            Coordinate[] coords = poly.getExteriorRing().getCoordinates();
            int[] xs = new int[coords.length];
            int[] ys = new int[coords.length];
            for (int i = 0; i < coords.length; i++) {
                xs[i] = (int) coords[i].x;
                ys[i] = height - (int) coords[i].y; // flip Y axis
            }
            g2.drawPolygon(xs, ys, coords.length);
        }

        g2.dispose();
        ImageIO.write(img, "png", new File(outputPath));
        System.out.println("🖼️  Tile preview written to: " + outputPath);
    }


    /** Reproject from EPSG:4326 → EPSG:3857 */
    private static Geometry reprojectTo3857(Geometry g) {
        CRSFactory crsFactory = new CRSFactory();
        CoordinateReferenceSystem src = crsFactory.createFromParameters("EPSG:4326", "+proj=longlat +datum=WGS84 +no_defs");
        CoordinateReferenceSystem dst = crsFactory.createFromParameters("EPSG:3857",
                                                                        "+proj=merc +a=6378137 +b=6378137 +lat_ts=0 +lon_0=0 +x_0=0 +y_0=0 "
                                                                        + "+k=1 +units=m +nadgrids=@null +no_defs");
        CoordinateTransform ct = new CoordinateTransformFactory().createTransform(src, dst);

        GeometryFactory f = g.getFactory();
        Coordinate[] c = g.getCoordinates();
        Coordinate[] nc = new Coordinate[c.length];
        ProjCoordinate s = new ProjCoordinate(), d = new ProjCoordinate();
        for (int i=0;i<c.length;i++) {
            s.x=c[i].x; s.y=c[i].y;
            ct.transform(s,d);
            nc[i]=new Coordinate(d.x,d.y);
        }
        if (g instanceof Polygon) return f.createPolygon(nc);
        if (g instanceof LineString) return f.createLineString(nc);
        if (g instanceof Point) return f.createPoint(nc[0]);
        return f.createGeometry(g);
    }

    /** Compute tile bounds in EPSG:3857 */
    private static Envelope getTileBounds(int x, int y, int z) {
        double n = Math.pow(2.0, z);
        double tileSize = 40075016.68557849 / n;
        double minX = -20037508.342789244 + x * tileSize;
        double maxX = -20037508.342789244 + (x + 1) * tileSize;
        double maxY = 20037508.342789244 - y * tileSize;
        double minY = 20037508.342789244 - (y + 1) * tileSize;
        return new Envelope(minX, maxX, minY, maxY);
    }

    /** Main entry */
    public byte[] generateMvt(List<String> wktString, int zoom, int x, int y, String layerName) throws Exception {
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
        List<Geometry> geometries = new ArrayList<>();

        for (String wkt : wktString) {
            Geometry geom = new WKTReader(gf).read(wkt);
            geometries.add(reprojectTo3857(geom));
        }

        // Compute tile envelope
        Envelope tileEnv = getTileBounds(x, y, zoom);

        // Clip & transform geometry into 0–4096 coordinate space
        MvtLayerParams layerParams = new MvtLayerParams(4096, 256);
        TileGeomResult tileGeom = JtsAdapter.createTileGeom(
                geometries, tileEnv, geometries.get(0).getFactory(), layerParams, g -> true);

        // Skip if geometry is completely outside tile
        if (tileGeom.mvtGeoms.isEmpty()) {
            System.out.printf("🟡 Skipping empty tile z=%d x=%d y=%d (no overlap)%n", zoom, x, y);
            return null;
        }

//        renderTilePreview(tileGeom.mvtGeoms, layerParams.extent,
//                          "target/preview_z" + zoom + "_x" + x + "_y" + y + ".png");

        // Build features
        MvtLayerProps layerProps = new MvtLayerProps();
        IUserDataConverter userDataConverter = new UserDataKeyValueMapConverter();
        List<VectorTile.Tile.Feature> features = JtsAdapter.toFeatures(
                tileGeom.mvtGeoms, layerProps, userDataConverter);

        // Build layer
        VectorTile.Tile.Layer.Builder layerBuilder = MvtLayerBuild.newLayerBuilder(layerName, layerParams);
        layerBuilder.addAllFeatures(features);
        MvtLayerBuild.writeProps(layerBuilder, layerProps);
        layerBuilder.setName(layerName);
        layerBuilder.setVersion(2);
        layerBuilder.setExtent(layerParams.extent);

        // Build tile
        VectorTile.Tile.Builder tileBuilder = VectorTile.Tile.newBuilder();
        tileBuilder.addLayers(layerBuilder.build());

        VectorTile.Tile tileMvt = tileBuilder.build();
        return tileMvt.toByteArray();
    }
}
