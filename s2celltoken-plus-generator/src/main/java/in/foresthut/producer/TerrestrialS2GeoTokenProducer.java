package in.foresthut.producer;

import in.foresthut.commons.s2geo.entity.S2CellTokenPlus;
import in.foresthut.repository.S2CellTokenPlusRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class TerrestrialS2GeoTokenProducer {
    private static final Logger logger = LoggerFactory.getLogger(TerrestrialS2GeoTokenProducer.class);
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(4);
    private static final ExecutorService dbThreadPool = Executors.newVirtualThreadPerTaskExecutor();//Executors.newFixedThreadPool(2);
    private final S2CellTokenPlusRepo cellTokenPlusRepo;
    private final CountDownLatch latch;

    public TerrestrialS2GeoTokenProducer(S2CellTokenPlusRepo cellTokenPlusRepo, CountDownLatch latch) {
        this.cellTokenPlusRepo = cellTokenPlusRepo;
        this.latch = latch;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            threadPool.shutdown();
            dbThreadPool.shutdown();
        }));
    }

    public void startProducing(List<String> cellTokens) {
        threadPool.submit(() -> {
            try {
                List<S2CellTokenPlus> cellTokenPlusList = new ArrayList<>();

                for (var cellToken : cellTokens) {
                    cellTokenPlusList.add(S2CellTokenPlus.toS2TokenPlus(cellToken));
                }

                dbThreadPool.submit(() -> {
                    cellTokenPlusRepo.addMany(cellTokenPlusList);
                    IntStream.rangeClosed(0, cellTokens.size())
                             .forEach(i -> {
                                 latch.countDown();
                             });
                });
                logger.info("Latch {}", latch.getCount());
            } catch (Exception e) {
                logger.error("Error: ", e);
                throw new RuntimeException(e);
            }
        });
    }
}
