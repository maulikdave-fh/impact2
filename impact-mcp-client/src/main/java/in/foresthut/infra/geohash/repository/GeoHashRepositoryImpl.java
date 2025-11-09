package in.foresthut.infra.geohash.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.nirvighna.mongodb.commons.DatabaseConfig;
import in.foresthut.commons.geohash.entity.GeoHashPlus;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GeoHashRepositoryImpl implements GeoHashRepository {
    private static final Logger logger = LoggerFactory.getLogger(GeoHashRepositoryImpl.class);
    private final MongoCollection<GeoHashPlus> geoHashPlusMongoCollection;

    public GeoHashRepositoryImpl() {
        this.geoHashPlusMongoCollection = DatabaseConfig.getInstance().database()
                                                        .getCollection("geohash", GeoHashPlus.class);
    }

    @Override
    public GeoHashPlus get(String geoHashString) {
        Bson filter = Filters.eq("geoHashString", geoHashString);
        return this.geoHashPlusMongoCollection.find(filter)
                                              .first();
    }

    @Override
    public List<GeoHashPlus> get(double[] distanceFromSeaLimits, double[] elevationLimits, double[] latitudeLimits) {
        Bson distanceFromSeaFilter = Filters.and(
                Filters.gte("distanceFromSeaInKM", distanceFromSeaLimits[0]),
                Filters.lte("distanceFromSeaInKM", distanceFromSeaLimits[1])
        );

        Bson elevationFilter = Filters.and(
                Filters.gte("maxElevationInMeters", elevationLimits[0]),
                Filters.lte("minElevationInMeters", elevationLimits[1])
        );

        Bson latitudeFilter = Filters.and(
                Filters.lte("northWestY", latitudeLimits[0]),
                Filters.gte("southEastY", latitudeLimits[1])
        );

        Bson allFilters = Filters.and(distanceFromSeaFilter, elevationFilter, latitudeFilter);

        List<GeoHashPlus> result = new ArrayList<>();
        this.geoHashPlusMongoCollection.find(allFilters).into(result);
        return result;
    }
}
