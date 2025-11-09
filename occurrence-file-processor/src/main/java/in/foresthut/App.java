package in.foresthut;

import com.nirvighna.mongodb.commons.DatabaseConfig;
import com.nirvighna.redis.client.cuckoo.CuckooFilterClient;
import com.nirvighna.www.commons.config.AppConfig;
import in.foresthut.commons.continentality.Oceans;
import in.foresthut.commons.elevation.ElevationFinder;
import in.foresthut.geohash.repository.GeoHashRepository;
import in.foresthut.geohash.repository.GeoHashRepositoryImpl;
import in.foresthut.occurrence.processor.OccurrenceDataProcessor;
import in.foresthut.occurrence.repository.OccurrenceRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
//        CuckooFilterClient cuckooFilterClient = CuckooFilterClient.getInstance();
//        cuckooFilterClient.setup("geohash", 21500);

        GeoHashRepository geoHashRepository = new GeoHashRepositoryImpl(DatabaseConfig.getInstance());

        // 1. Populate Geohash Cuckoo filter
        // createGeoHashCuckooFilter(cuckooFilterClient, geoHashRepository);

        // 2.Start GBIF occurrence file processing
        logger.info("Starting GBIF occurrence file processor!");
        String filePath = AppConfig.getInstance()
                                   .get("gbif-occurrences-filepath");
        var threadPool = Executors.newFixedThreadPool(6);

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean head = true;
            while ((line = reader.readLine()) != null) {
                if (head) {
                    head = false;
                    continue;
                }
                threadPool.execute(new OccurrenceDataProcessor(
                        line, Oceans.getInstance(), ElevationFinder.getInstance(),
                        new OccurrenceRepositoryImpl(DatabaseConfig.getInstance()),
                        new GeoHashRepositoryImpl(DatabaseConfig.getInstance())));
            }
            threadPool.shutdown();
        } catch (IOException e) {
            logger.error("Error reading file {}", filePath, e);
            throw new RuntimeException(e);
        }
    }

    private static void createGeoHashCuckooFilter(CuckooFilterClient cuckooFilterClient,
                                                  GeoHashRepository geoHashRepository) {
        long geoHashCuckooFilterCount = cuckooFilterClient.itemCount("geohash");
        if (geoHashCuckooFilterCount == 0) {
            logger.info("Geohash cuckoo filter is empty");

            List<String> geoHashes = geoHashRepository.getAllGeoHashString();
            for (var geoHash : geoHashes) {
                cuckooFilterClient.add("geohash", geoHash);
            }
        } else {
            logger.info("Geohash cuckoo filter has {} items", geoHashCuckooFilterCount);
        }
    }
}
