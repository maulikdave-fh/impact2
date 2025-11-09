package in.foresthut.commons.continentality;

import com.nirvighna.www.commons.config.AppConfig;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Oceans {

    private static final Logger logger = LoggerFactory.getLogger(Oceans.class);
    private static Oceans instance;
    private static Geometry oceanPolygon;
    private DataStore dataStore = null;
    private SimpleFeatureCollection features = null;
    private SimpleFeatureIterator featureIterator = null;

    private Oceans() {
        // Connect to the shapefile data store
        Map<String, Object> params = new HashMap<>();


        try {
            ClassLoader classLoader = getClass().getClassLoader();
            File shpFile = new File(AppConfig.getInstance().get("ne_10m_ocean_shp_path"));

            params.put(
                    "url", shpFile.toURI()
                                  .toURL());
            dataStore = DataStoreFinder.getDataStore(params);

            // Read the features
            String typeName = dataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
            features = featureSource.getFeatures();
            featureIterator = features.features();
            var feature = featureIterator.next();
            oceanPolygon = (Geometry) feature.getDefaultGeometry();
            if (!oceanPolygon.isValid()) {
                oceanPolygon = GeometryFixer.fix(oceanPolygon);
            }
        } catch (IOException e) {
            logger.error("Error: ", e);
            throw new RuntimeException(e);
        } finally {
            if (dataStore != null && featureIterator != null) {
                featureIterator.close();
                dataStore.dispose();
            }
        }

        Runtime.getRuntime()
               .addShutdownHook(new Thread(() -> {
                   if (dataStore != null) {
                       featureIterator.close();
                       dataStore.dispose();
                       logger.info("Datastore disposed successfully.");
                   }
               }));
    }

    public static synchronized Oceans getInstance() {
        if (instance == null) instance = new Oceans();
        return instance;
    }

    public boolean inOcean(Geometry polygon) {
        return polygon.within(oceanPolygon);
    }

    public double distance(Point point) {
        // Check if the point is inside any ocean polygon
//        if (features.contains(point)) {
//            // Point is inside the sea, distance is 0. But you could return negative for
//            // clarity.
//            return -1.0;
//        }

        // If not in the sea, find the nearest distance
        double distance = point.distance(oceanPolygon);
        return (distance * 60 * 1.1515) * 1.609344;
    }

}