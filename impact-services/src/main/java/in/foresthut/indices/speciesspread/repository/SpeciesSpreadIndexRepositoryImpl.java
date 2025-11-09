package in.foresthut.indices.speciesspread.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.nirvighna.mongodb.commons.DatabaseConfig;
import in.foresthut.indices.speciesdiversity.entity.SpeciesDiversityIndexDao;
import in.foresthut.indices.speciesspread.entity.SpeciesSpreadIndexDao;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeciesSpreadIndexRepositoryImpl implements SpeciesSpreadIndexRepository {
    private static final Logger logger = LoggerFactory.getLogger(SpeciesSpreadIndexRepositoryImpl.class);

    private final MongoCollection<SpeciesSpreadIndexDao> speciesSpreadIndexDaoMongoCollection;

    public SpeciesSpreadIndexRepositoryImpl(DatabaseConfig databaseConfig){
        this.speciesSpreadIndexDaoMongoCollection = databaseConfig.database().getCollection("species_spread",
                                                                                            SpeciesSpreadIndexDao.class);
    }

    @Override
    public void add(SpeciesSpreadIndexDao speciesSpreadIndexDao) {
        this.speciesSpreadIndexDaoMongoCollection.insertOne(speciesSpreadIndexDao);
    }

    @Override
    public SpeciesSpreadIndexDao get(String siteId, int forYear) {
        Bson siteIdFilter = Filters.eq("siteId", siteId);
        Bson yearFilter = Filters.eq("forYear", forYear);

        Bson filters = Filters.and(siteIdFilter, yearFilter);

        return this.speciesSpreadIndexDaoMongoCollection.find(filters).first();
    }

    @Override
    public void delete(String siteId) {
        this.speciesSpreadIndexDaoMongoCollection.deleteMany(Filters.eq("siteId", siteId));
    }
}
