package in.foresthut.producer;

import in.foresthut.commons.continentality.Oceans;
import in.foresthut.commons.geohash.mapper.GeoHashMapper;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

public class TerrestrialGeoHashChildrenProducer {
    private static final Logger logger = LoggerFactory.getLogger(TerrestrialGeoHashChildrenProducer.class);

    private final BlockingQueue<String> terrestrialGeoHashChildrenQueue;
    private final String geoHashPrefix;
    private final int targetGeoHashLength;

    private static final char[] chars = "0123456789bcdefghjkmnpqrstuvwxyz".toCharArray();
    public static final String POISON_PILL = "DONE";

    public TerrestrialGeoHashChildrenProducer(BlockingQueue<String> terrestrialGeoHashChildrenQueue, String geoHashPrefix,
                                              int targetGeoHashLength) {
        this.terrestrialGeoHashChildrenQueue = terrestrialGeoHashChildrenQueue;
        this.geoHashPrefix = geoHashPrefix;
        this.targetGeoHashLength = targetGeoHashLength;
    }

    public Thread startProducing() {
        return Thread.ofVirtual().name("virtual-" + geoHashPrefix).start(new Runnable() {
            @Override
            public void run() {
                produceTerrestrialGeoHashChildren(chars, targetGeoHashLength - geoHashPrefix.length());
                try {
                    terrestrialGeoHashChildrenQueue.put(POISON_PILL);
                } catch (InterruptedException e) {
                    logger.error("Error: ", e);
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void produceTerrestrialGeoHashChildren(char[] set, String prefix, int n, int k) {
        // Base case: k is 0,
        // print prefix
        if (k == 0) {
            try {
                String geoHashString = geoHashPrefix + prefix;
                    Geometry geoHashPolygon = GeoHashMapper.toGeometry(geoHashString);
                if (!Oceans.getInstance().inOcean(geoHashPolygon)) {
                    terrestrialGeoHashChildrenQueue.put(geoHashString);
                    logger.debug("Terrestrial Geohash Queue size {}", terrestrialGeoHashChildrenQueue.size());
      //              logger.info("Produced geohash {}", geoHashString);
                } else {
                    //logger.info("{} is in ocean", geoHashString);
                }
            } catch (InterruptedException e) {
                logger.error("Error: ", e);
                throw new RuntimeException(e);
            }
            return;
        }

        // One by one add all characters
        // from set and recursively
        // call for k equals to k-1
        for (int i = 0; i < n; ++i) {

            // Next character of input added
            String newPrefix = prefix + set[i];

            // k is decreased, because
            // we have added a new character
            produceTerrestrialGeoHashChildren(set, newPrefix, n, k - 1);
        }
    }

    private void produceTerrestrialGeoHashChildren(char[] set, int k) {
        int n = set.length;
        produceTerrestrialGeoHashChildren(set, "", n, k);
    }
}
