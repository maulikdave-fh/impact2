package in.foresthut.occurrence.repository;

import com.mongodb.client.MongoCollection;
import com.nirvighna.mongodb.commons.DatabaseConfig;
import in.foresthut.commons.occurrence.entity.OccurrenceDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OccurrenceRepositoryImpl implements OccurrenceRepository {
    private static final Logger logger = LoggerFactory.getLogger(OccurrenceRepositoryImpl.class);

    private static MongoCollection<OccurrenceDao> occurrenceCollection;

    public OccurrenceRepositoryImpl(DatabaseConfig databaseConfig) {
        occurrenceCollection = databaseConfig.database()
                                                  .getCollection("occurrence", OccurrenceDao.class);
    }


    @Override
    public void add(OccurrenceDao occurrence) {
        occurrenceCollection.insertOne(occurrence);
    }
}
