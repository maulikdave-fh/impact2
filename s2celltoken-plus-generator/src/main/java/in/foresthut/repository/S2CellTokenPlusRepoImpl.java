package in.foresthut.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.InsertOneModel;
import com.nirvighna.mongodb.commons.DatabaseConfig;
import in.foresthut.commons.s2geo.entity.S2CellTokenPlus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class S2CellTokenPlusRepoImpl implements S2CellTokenPlusRepo {
    private static final Logger logger = LoggerFactory.getLogger(S2CellTokenPlusRepoImpl.class);

    private final MongoCollection<S2CellTokenPlus> tokenPlusMongoCollection;

    private static final InsertManyOptions options = new InsertManyOptions().ordered(false);


    public S2CellTokenPlusRepoImpl(DatabaseConfig databaseConfig) {
        this.tokenPlusMongoCollection = databaseConfig.database()
                                                      .getCollection("cell_token", S2CellTokenPlus.class);
    }

    @Override
    public void add(S2CellTokenPlus cellTokenPlus) {
        var result = this.tokenPlusMongoCollection.insertOne(cellTokenPlus);
//        logger.debug("Added {} to db", cellTokenPlus);
    }

    @Override
    public void addMany(List<S2CellTokenPlus> tokenPlusList) {
        //var result = this.tokenPlusMongoCollection.insertMany(tokenPlusList, options);
        List<InsertOneModel<S2CellTokenPlus>> writes = tokenPlusList.stream()
                                                                    .map(d -> new InsertOneModel<>(d))
                                                                    .collect(Collectors.toList())
                                                                    .reversed();
        this.tokenPlusMongoCollection.bulkWrite(writes, new BulkWriteOptions().ordered(false));
        //logger.info("Added {} items to db", tokenPlusList.size());
    }
}
