package in.foresthut.mvt;

import com.mongodb.client.MongoCollection;
import com.nirvighna.mongodb.commons.DatabaseConfig;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;

import java.util.ArrayList;
import java.util.List;

public class MvtTest {

    private final static String forestHut =
            "POLYGON ((73.5805054 18.2362594, 73.5803734 18.2361013, 73.5801464 18.235764, 73.5800897 18.235371, 73.580051 18.2351999, 73.5800175 18.2350338, 73.5800674 18.2346674, 73.5800905 18.2344901, 73.5800763 18.234334, 73.5800944 18.2341378, 73.5802172 18.2340208, 73.5802982 18.2339462, 73.5803941 18.2337463, 73.5804511 18.2337139, 73.580525 18.2336816, 73.5806198 18.2337052, 73.5807623 18.2337447, 73.5809229 18.2338173, 73.5809221 18.2360755, 73.5806839 18.2361839, 73.5805054 18.2362594))";

    @Test
    void test() throws Exception {
        DatabaseConfig databaseConfig = DatabaseConfig.getInstance();
        MongoCollection<Document> s2Cells = databaseConfig.database()
                                                          .getCollection("cell_token");

        var cells = s2Cells.find().limit(10000);
        List<String> aoIBlocks = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (var cell : cells){
            aoIBlocks.add(cell.getString("wktPolygon"));
            sb.append(cell.getString("cellToken")).append(",");
        }

        System.out.println("AoI blocks " + aoIBlocks.size());

        int zoom = 10;

        var site = new WKTReader().read(aoIBlocks.get(0));

        int[] tile = printTileFor(site.getCentroid().getX(), site.getCentroid().getY(), zoom);
        int x = tile[0];
        int y = tile[1];

        MvtGenerator mvtGenerator = new MvtGenerator();
        S3Uploader s3Uploader = new S3Uploader("impact3-vector-tiles");

        for (int i = x-1; i <= x+1; i++)
            for (int j = y-1; j <= y+1; j++) {
                byte[] mvtBytes = mvtGenerator.generateMvt(aoIBlocks, zoom, i, j, "polygons");
                s3Uploader.upload(i, j, zoom, mvtBytes);
            }
        //printTileFor(73.5805, 18.2362, 12);
    }

    private static int[] printTileFor(double lon, double lat, int z) {
        int x = (int) Math.floor((lon + 180.0) / 360.0 * (1 << z));
        int y = (int) Math.floor(
                (1.0 - Math.log(Math.tan(Math.toRadians(lat)) + 1.0 / Math.cos(Math.toRadians(lat))) / Math.PI)
                / 2.0 * (1 << z)
        );
        System.out.printf("Tile for lon %.6f, lat %.6f, zoom %d => x=%d, y=%d%n", lon, lat, z, x, y);
        return new int[]{x, y};
    }

}
