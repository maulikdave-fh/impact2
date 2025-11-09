import com.nirvighna.mongodb.commons.DatabaseConfig;
import in.foresthut.consumer.TerrestrialGeoHashChildrenConsumer;
import in.foresthut.producer.TerrestrialGeoHashChildrenProducer;
import in.foresthut.repository.GeoHashPlusRepo;
import in.foresthut.repository.GeoHashPlusRepoImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {

        try {
            BlockingQueue<String> queue = new ArrayBlockingQueue<>(5);
//            String[] geoHashPrefixes = {"tegez", "tdkz", "tek4"};
        String[] geoHashPrefixes =
                {"tegs", "tegu", "tege", "tegg", "tegd", "tegf", "teg9", "tegc", "teg8", "tegb", "teex", "teez",
                 "teewz", "teey", "teev", "teeu", "teeg", "teeex", "teeer", "teeep", "teef", "teec", "teeb", "teedz",
                 "teedx", "teedq", "teedr", "teedn", "teedp", "tee9y", "tee9z", "tee9t", "tee9w", "tee9x", "tee9m",
                 "tee9q", "tee9r", "tee9j", "tee9n", "tee9p", "tee8v", "tee8y", "tee8z", "tee8t", "tee8w", "tee8x",
                 "tee8m", "tee8q", "tee8r", "tee8n", "tee8p", "teu", "tev", "tes", "tet", "te7x", "te7z", "te7w",
                 "te7y", "te7t", "te7v", "te7s", "te7u", "te7g", "te7f", "te7c", "te7b", "tek", "tem", "te5z", "te5y",
                 "te5v", "te5u", "teh", "tej",

                 "tdu", "tdv", "tdy", "tdsx", "tdsz", "tdsw", "tdsy", "tdst", "tdsv", "tdsu", "tdsg", "tdsf", "tdsc",
                 "tdsb", "tdsrz", "tdsrw", "tdsrx", "tdsrq", "tdsrr", "tdssu", "tdssv", "tdssy", "tdssz", "tdsst",
                 "tdssw", "tdssx", "tdssp", "tdssr", "tdssn", "tdssp", "tdsey", "tdsez", "tdt", "tdw", "tdkzz",
                 "tdkzx", "tdm","tdq", "tdj", "tdn",

                 "t9vz", "t9vy", "t9vv", "t9y", "t9z", "t9w", "t9x", "t9qx", "t9qz", "t9qy", "t9rp", "t9rn", "t9rj",
                 "t9rr", "t9rqb", "t9rqc", "t9rq8", "t9rq2", "t9rqc", "t9rq9"};
            int geoHashStringLength = 5;

            List<Thread> threads = new ArrayList<>();

            // Consumer
            GeoHashPlusRepo geoHashPlusRepo = new GeoHashPlusRepoImpl(DatabaseConfig.getInstance());
            var consumer = new TerrestrialGeoHashChildrenConsumer(queue, geoHashPrefixes.length, geoHashPlusRepo);
            threads.add(consumer.startConsuming());

            // Producers
            for (var geoHashPrefix : geoHashPrefixes) {
                var producer = new TerrestrialGeoHashChildrenProducer(queue, geoHashPrefix, geoHashStringLength);
                threads.add(producer.startProducing());
            }

            for (var thread : threads) {
                thread.join();
            }
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
    }
}
