package in.foresthut.infra.s2token.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.nirvighna.mongodb.commons.DatabaseConfig;
import in.foresthut.commons.s2geo.entity.S2CellTokenPlus;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class S2CellTokenPlusRepoImpl implements S2CellTokenPlusRepo {
    private static final Logger logger = LoggerFactory.getLogger(S2CellTokenPlusRepoImpl.class);

    private final MongoCollection<S2CellTokenPlus> tokenPlusMongoCollection;

    public S2CellTokenPlusRepoImpl(DatabaseConfig databaseConfig) {
        this.tokenPlusMongoCollection = databaseConfig.database()
                                                      .getCollection("cell_token", S2CellTokenPlus.class);
    }

    @Override
    public S2CellTokenPlus get(String cellToken) {
        Bson filter = Filters.eq("cellToken", cellToken);
        return this.tokenPlusMongoCollection.find(filter)
                                              .first();
    }

    @Override
    public List<S2CellTokenPlus> get(double[] distanceFromSeaLimits, double[] elevationLimits, double[] latitudeLimits) {
        Bson distanceFromSeaFilter = Filters.and(
                Filters.gte("distanceFromSeaInKM", distanceFromSeaLimits[0]),
                Filters.lte("distanceFromSeaInKM", distanceFromSeaLimits[1])
        );

        Bson elevationFilter = Filters.and(
                Filters.gte("minElevationInMeters", elevationLimits[0]),
                Filters.lte("maxElevationInMeters", elevationLimits[1])
        );

        Bson latitudeFilter = Filters.and(
                Filters.lte("centerLat", latitudeLimits[0]),
                Filters.gte("centerLat", latitudeLimits[1])
        );

        Bson allFilters = Filters.and(distanceFromSeaFilter, elevationFilter, latitudeFilter);

        List<S2CellTokenPlus> result = new ArrayList<>();
        this.tokenPlusMongoCollection.find(allFilters).into(result);
        logger.info("Found {} AoI blocks", result.size());
        return result;
    }
}
