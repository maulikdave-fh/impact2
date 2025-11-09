package in.foresthut.geohash.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.nirvighna.mongodb.commons.DatabaseConfig;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GeoHashRepositoryImpl implements GeoHashRepository {
    private static final Logger logger = LoggerFactory.getLogger(GeoHashRepositoryImpl.class);

    private static MongoCollection<Document> geoHashPlusCollection = null;

    public GeoHashRepositoryImpl(DatabaseConfig databaseConfig) {
        this.geoHashPlusCollection = databaseConfig.database()
                                                   .getCollection("geohash");
    }

    public static void main(String[] args) {
        var result = new GeoHashRepositoryImpl(DatabaseConfig.getInstance()).getAllGeoHashString();
        logger.info("Number of records: {}", result.size());
    }

    @Override
    public List<String> getAllGeoHashString() {
        // Define the fields to include in the response
        Bson projection = Projections.fields(Projections.include("geoHashString"), Projections.excludeId()
                                             // Exclude the _id field (optional)
        );


        List<String> geoHashes = new ArrayList<>();

        geoHashPlusCollection.find()
                             .projection(projection)
                             .forEach(doc -> geoHashes.add(doc.getString("geoHashString")));

        return geoHashes;
    }

    @Override
    public boolean exists(String geoHashString) {
        Bson filter = Filters.eq("geoHashString", geoHashString);
        return geoHashPlusCollection.countDocuments(filter) > 0;
    }
}
