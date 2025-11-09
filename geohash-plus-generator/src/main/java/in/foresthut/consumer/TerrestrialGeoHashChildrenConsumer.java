package in.foresthut.consumer;

import ch.hsr.geohash.GeoHash;
import in.foresthut.commons.geohash.entity.GeoHashPlus;
import in.foresthut.producer.TerrestrialGeoHashChildrenProducer;
import in.foresthut.repository.GeoHashPlusRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TerrestrialGeoHashChildrenConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TerrestrialGeoHashChildrenConsumer.class);
    private static final ExecutorService threadPool = Executors.newVirtualThreadPerTaskExecutor();
    private final BlockingQueue<String> terrestrialGeoHashChildrenQueue;
    private final int numberOfPoisonPills;
    private final GeoHashPlusRepo geoHashPlusRepo;

    public TerrestrialGeoHashChildrenConsumer(BlockingQueue<String> terrestrialGeoHashChildrenQueue,
                                              int numberOfPoisonPills, GeoHashPlusRepo geoHashPlusRepo) {
        this.terrestrialGeoHashChildrenQueue = terrestrialGeoHashChildrenQueue;
        this.numberOfPoisonPills = numberOfPoisonPills;
        this.geoHashPlusRepo = geoHashPlusRepo;
    }

    public Thread startConsuming() {
        return Thread.ofVirtual()
                     .start(new Runnable() {
                         @Override
                         public void run() {
                             String lastConsumed = "";
                             int poisonPillsReceived = 0;
                             while (poisonPillsReceived < numberOfPoisonPills) {
                                 try {
                                     lastConsumed = terrestrialGeoHashChildrenQueue.take();
                                     logger.debug("Terrestrial Geohash Queue size {}", terrestrialGeoHashChildrenQueue.size());
                                     if (lastConsumed.equals(TerrestrialGeoHashChildrenProducer.POISON_PILL)) {
                                         poisonPillsReceived++;
                                         logger.info(
                                                 "{} poison pill(s) received out of {}", poisonPillsReceived,
                                                 numberOfPoisonPills);
                                     } else {
                                         logger.debug("Terrestrial geohash {} received.", lastConsumed);
                                         var geoHashPlus =
                                                 GeoHashPlus.toGeoHashPlus(GeoHash.fromGeohashString(lastConsumed));
                                         geoHashPlusRepo.add(geoHashPlus);
                                         logger.debug("Added to database - Geohash plus: {}", geoHashPlus);
                                     }
                                 } catch (Exception e) {
                                     logger.error("Error: ", e);
                                 }
                             }
                         }
                     });
    }
}