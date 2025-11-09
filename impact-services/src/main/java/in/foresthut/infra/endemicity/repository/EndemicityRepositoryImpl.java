package in.foresthut.infra.endemicity.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.nirvighna.mongodb.commons.DatabaseConfig;
import in.foresthut.infra.endemicity.entity.Endemicity;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndemicityRepositoryImpl implements EndemicityRepository {
    private static final Logger logger = LoggerFactory.getLogger(EndemicityRepositoryImpl.class);

    private final MongoCollection<Endemicity> endemicityMongoCollection;

    public EndemicityRepositoryImpl(DatabaseConfig databaseConfig) {
        this.endemicityMongoCollection = databaseConfig.database()
                                                       .getCollection("endemicity", Endemicity.class);
    }

    @Override
    public void add(Endemicity endemicity) {
        this.endemicityMongoCollection.insertOne(endemicity);
    }

    @Override
    public Endemicity get(String speciesName, String bioregion) {
        Bson speciesFilter = Filters.eq("speciesName", speciesName);
        Bson bioregionFilter = Filters.eq("bioregion", bioregion);

        Bson combinedFilter = Filters.and(speciesFilter, bioregionFilter);

        Bson sortByDateFilter = Sorts.descending("calculatedOn");

        return this.endemicityMongoCollection.find(combinedFilter)
                                             .sort(sortByDateFilter)
                                             .limit(1)
                                             .first();
    }
}
