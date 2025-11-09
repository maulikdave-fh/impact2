package in.foresthut.aoi;

import ch.hsr.geohash.GeoHash;
import com.nirvighna.www.commons.config.AppConfig;
import in.foresthut.commons.geohash.entity.GeoHashPlus;
import in.foresthut.infra.geohash.repository.GeoHashRepository;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class AoI {
    private static final Logger logger = LoggerFactory.getLogger(AoI.class);
    private final static WKTReader wktReader = new WKTReader();
    private static final double DISTANCE_FROM_SEA_RANGE = Double.valueOf(AppConfig.getInstance()
                                                                                  .get("distance.from.sea.limit"));
    private static final double LATITUDE_RANGE = Double.valueOf(AppConfig.getInstance()
                                                                         .get("latitude.limit"));
    private final String wktPolygon;
    private final List<GeoHashPlus> aoiBlocks = new ArrayList<>();
    private final GeoHashRepository geoHashRepository;


    public AoI(String wktPolygon, GeoHashRepository geoHashRepository) {
        Objects.requireNonNull(wktPolygon, "Wkt polygon cannot be null.");
        if (wktPolygon.isBlank()) throw new IllegalArgumentException("Wkt polygon cannot be blank.");
        if (!validWkt(wktPolygon)) throw new IllegalArgumentException("Invalid Wkt polygon.");

        this.wktPolygon = wktPolygon;
        this.geoHashRepository = geoHashRepository;
    }

    public List<GeoHashPlus> getAoI() {
        // 1. Get genesis block
        GeoHash genesisBlock = getGenesisBlock();
        logger.info("Genesis block: {}", genesisBlock.toBase32());
        GeoHashPlus genesisGeoHashPlusBlock = geoHashRepository.get(genesisBlock.toBase32());
        if (genesisGeoHashPlusBlock != null) {
            aoiBlocks.add(geoHashRepository.get(genesisBlock.toBase32()));
        } else {
            logger.error("Genesis block {} not found", genesisBlock.toBase32());
            throw new RuntimeException("Genesis block {} not found.");
        }


        // 2. Get neighbouring blocks
        aoiBlocks.addAll(getNeighbours(genesisBlock));


        // 3. Get 10 more blocks with the same elevation range and distance from sea
        double genesisBlockDistanceFromSea = genesisGeoHashPlusBlock.distanceFromSeaInKM();
        double[] distanceFromSeaRange = {genesisBlockDistanceFromSea - DISTANCE_FROM_SEA_RANGE,
                                         genesisBlockDistanceFromSea + DISTANCE_FROM_SEA_RANGE};

        double[] latitudeRange = {genesisGeoHashPlusBlock.northWestY() + LATITUDE_RANGE,
                                  genesisGeoHashPlusBlock.southEastY() - LATITUDE_RANGE};

        double[] elevationRange =
                {genesisGeoHashPlusBlock.minElevationInMeters(), genesisGeoHashPlusBlock.maxElevationInMeters()};

        aoiBlocks.addAll(geoHashRepository.get(distanceFromSeaRange, elevationRange, latitudeRange));

        return aoiBlocks;
    }

    private List<GeoHashPlus> getNeighbours(GeoHash genesisBlock) {
        List<GeoHash> neighbourGeoHashes = Arrays.stream(genesisBlock.getAdjacent())
                                                 .toList();

        List<GeoHashPlus> neighbours = new ArrayList<>();

        for (var neighbourGeoHash : neighbourGeoHashes) {
            GeoHashPlus geoHashPlus = geoHashRepository.get(neighbourGeoHash.toBase32());
            if (geoHashPlus != null) {
                neighbours.add(geoHashPlus);
            }
        }
        return neighbours;
    }

    private GeoHash getGenesisBlock() {
        Point point = getCenter();
        var latitude = point.getY();
        var longitude = point.getX();
        return GeoHash.withCharacterPrecision(latitude, longitude, 5);
    }

    private Point getCenter() {
        Geometry polygon = null;
        try {
            polygon = wktReader.read(wktPolygon);
        } catch (ParseException e) {
            logger.error("Error parsing {}", wktPolygon, e);
            throw new RuntimeException(e);
        }
        return polygon.getCentroid();
    }

    private boolean validWkt(String wktPolygon) {
        try {
            return wktReader.read(wktPolygon)
                            .isValid();
        } catch (ParseException e) {
            return false;
        }
    }
}
