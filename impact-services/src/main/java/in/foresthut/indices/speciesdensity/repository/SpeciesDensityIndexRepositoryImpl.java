package in.foresthut.indices.speciesdensity.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.nirvighna.mongodb.commons.DatabaseConfig;
import in.foresthut.indices.speciesdensity.entity.SpeciesDensityIndexDao;
import in.foresthut.indices.speciesdensity.service.SpeciesDensityIndexService;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeciesDensityIndexRepositoryImpl implements SpeciesDensityIndexRepository {
    private static final Logger logger = LoggerFactory.getLogger(SpeciesDensityIndexService.class);

    private final MongoCollection<SpeciesDensityIndexDao> speciesDensityIndexCollection;

    public SpeciesDensityIndexRepositoryImpl(DatabaseConfig databaseConfig){
        this.speciesDensityIndexCollection = databaseConfig.database().getCollection("species_density", SpeciesDensityIndexDao.class);
    }

    @Override
    public void add(SpeciesDensityIndexDao speciesDensityIndexDao) {
        this.speciesDensityIndexCollection.insertOne(speciesDensityIndexDao);
    }

    @Override
    public SpeciesDensityIndexDao get(String siteId, int forYear) {
        Bson siteIdFilter = Filters.eq("siteId", siteId);
        Bson yearFilter = Filters.eq("forYear", forYear);

        Bson filters = Filters.and(siteIdFilter, yearFilter);

        return this.speciesDensityIndexCollection.find(filters).first();
    }

    @Override
    public void delete(String siteId) {
        this.speciesDensityIndexCollection.deleteMany(Filters.eq("siteId", siteId));
    }
}
