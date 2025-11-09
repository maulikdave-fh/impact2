package in.foresthut.indices.speciesdiversity.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.nirvighna.mongodb.commons.DatabaseConfig;
import in.foresthut.indices.speciesdiversity.entity.SpeciesDiversityIndexDao;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeciesDiversityIndexRepositoryImpl implements SpeciesDiversityIndexRepository {
    private static final Logger logger = LoggerFactory.getLogger(SpeciesDiversityIndexRepositoryImpl.class);

    private final MongoCollection<SpeciesDiversityIndexDao> speciesDiversityIndexDaoMongoCollection;

    public SpeciesDiversityIndexRepositoryImpl(DatabaseConfig databaseConfig){
        this.speciesDiversityIndexDaoMongoCollection = databaseConfig.database().getCollection("species_diversity", SpeciesDiversityIndexDao.class);
    }

    @Override
    public void add(SpeciesDiversityIndexDao speciesDiversityIndexDao) {
        this.speciesDiversityIndexDaoMongoCollection.insertOne(speciesDiversityIndexDao);
    }

    @Override
    public SpeciesDiversityIndexDao get(String siteId, int forYear) {
        Bson siteIdFilter = Filters.eq("siteId", siteId);
        Bson yearFilter = Filters.eq("forYear", forYear);

        Bson filters = Filters.and(siteIdFilter, yearFilter);

        return this.speciesDiversityIndexDaoMongoCollection.find(filters).first();
    }

    @Override
    public void delete(String siteId) {
        this.speciesDiversityIndexDaoMongoCollection.deleteMany(Filters.eq("siteId", siteId));
    }
}
