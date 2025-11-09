package in.foresthut.repository;

import com.mongodb.client.MongoCollection;
import com.nirvighna.mongodb.commons.DatabaseConfig;
import in.foresthut.commons.geohash.entity.GeoHashPlus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoHashPlusRepoImpl implements GeoHashPlusRepo {
    private static final Logger logger = LoggerFactory.getLogger(GeoHashPlusRepoImpl.class);

    private final MongoCollection<GeoHashPlus> geoHashPlusCollection;

    public GeoHashPlusRepoImpl(DatabaseConfig databaseConfig) {
        this.geoHashPlusCollection = databaseConfig.database()
                                                   .getCollection("geohash", GeoHashPlus.class);
    }


    @Override
    public void add(GeoHashPlus geoHashPlus) {
        logger.debug("Adding {} to db", geoHashPlus);
        var result = this.geoHashPlusCollection.insertOne(geoHashPlus);
    }
}
