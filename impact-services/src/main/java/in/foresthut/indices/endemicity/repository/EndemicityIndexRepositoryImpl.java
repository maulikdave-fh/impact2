package in.foresthut.indices.endemicity.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.nirvighna.mongodb.commons.DatabaseConfig;
import in.foresthut.indices.endemicity.entity.EndemicityIndexDao;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndemicityIndexRepositoryImpl implements EndemicityIndexRepository{
    private static final Logger logger = LoggerFactory.getLogger(EndemicityIndexRepositoryImpl.class);

    private final MongoCollection<EndemicityIndexDao> endemicityIndexDaoMongoCollection;
    public EndemicityIndexRepositoryImpl(DatabaseConfig databaseConfig){
        endemicityIndexDaoMongoCollection = databaseConfig.database()
                                                          .getCollection("endemicity_index", EndemicityIndexDao.class);
    }

    @Override
    public void add(EndemicityIndexDao endemicityIndexDao) {
        this.endemicityIndexDaoMongoCollection.insertOne(endemicityIndexDao);
    }

    @Override
    public EndemicityIndexDao get(String siteId) {
        Bson siteIdFilter = Filters.eq("siteId", siteId);

        Bson filters = Filters.and(siteIdFilter);
        return this.endemicityIndexDaoMongoCollection.find(filters).first();
    }

    @Override
    public void delete(String siteId) {
        this.endemicityIndexDaoMongoCollection.deleteMany(Filters.eq("siteId", siteId));
    }
}
