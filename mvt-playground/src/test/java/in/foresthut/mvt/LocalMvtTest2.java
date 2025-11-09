package in.foresthut.mvt;

import com.wdtinc.mapbox_vector_tile.adapt.jts.MvtEncoder;
import com.wdtinc.mapbox_vector_tile.adapt.jts.UserDataKeyValueMapConverter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.model.JtsLayer;
import com.wdtinc.mapbox_vector_tile.adapt.jts.model.JtsMvt;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerParams;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.proj4j.*;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class LocalMvtTest2 {

    public static void main(String[] args) throws Exception {
        System.out.println("Loaded JTS from: " +
                           org.locationtech.jts.geom.Geometry.class
                                   .getProtectionDomain()
                                   .getCodeSource()
                                   .getLocation());


        // Simple polygon near Pune
        String wkt = "POLYGON((73.580 18.236, 73.581 18.236, 73.581 18.237, 73.580 18.237, 73.580 18.236))";

        GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
        Geometry geom = new WKTReader(gf).read(wkt);

        // Reproject to EPSG:3857
        Geometry geom3857 = reprojectTo3857(geom);

        // ---- Build MVT structures ----
        String layerName = "polygons";
        MvtLayerParams layerParams = new MvtLayerParams(); // extent=4096, buffer=256
        UserDataKeyValueMapConverter userDataConverter = new UserDataKeyValueMapConverter();

        // Wrap geometry into a JtsLayer -> JtsMvt
        JtsLayer layer = new JtsLayer(layerName, Collections.singletonList(geom3857));
        JtsMvt mvt = new JtsMvt(Collections.singletonList(layer));

        // Encode to MVT bytes
        byte[] bytes = MvtEncoder.encode(mvt, layerParams, userDataConverter);

        // ---- Write file ----
        Path out = Path.of("target/sample_simple.mvt");
        try (FileOutputStream fos = new FileOutputStream(out.toFile())) {
            fos.write(bytes);
        }
        System.out.println("✅ wrote " + bytes.length + " bytes to " + out.toAbsolutePath());
    }


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
}

