package in.foresthut.aoi;

import ch.hsr.geohash.GeoHash;
import com.google.api.gax.rpc.UnimplementedException;
import com.nirvighna.www.commons.config.AppConfig;
import in.foresthut.commons.geohash.entity.GeoHashPlus;
import in.foresthut.infra.geohash.repository.GeoHashRepository;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class GeoHashBasedAoIProvider implements AoIProvider {
    private static final Logger logger = LoggerFactory.getLogger(GeoHashBasedAoIProvider.class);
    private final static WKTReader wktReader = new WKTReader();
    private static final double DISTANCE_FROM_SEA_RANGE = Double.valueOf(AppConfig.getInstance()
                                                                                  .get("distance.from.sea.limit"));
    private static final double LATITUDE_RANGE = Double.valueOf(AppConfig.getInstance()
                                                                         .get("latitude.limit"));
    private final GeoHashRepository geoHashRepository;


    public GeoHashBasedAoIProvider(GeoHashRepository geoHashRepository) {
        this.geoHashRepository = geoHashRepository;
    }

    @Override
    public Map<String, Boolean> getAoI(String wktPolygon) {
        if (!validWkt(wktPolygon)) throw new IllegalArgumentException("Invalid Wkt polygon.");

        // 1. Get genesis block
        GeoHash genesisBlock = getGenesisBlock(wktPolygon);
        logger.info("Genesis block: {}", genesisBlock.toBase32());
        GeoHashPlus genesisGeoHashPlusBlock = geoHashRepository.get(genesisBlock.toBase32());

        List<GeoHashPlus> aoiBlocks =  new ArrayList<>();
        if (genesisGeoHashPlusBlock == null) {
            logger.error("Genesis block {} not found", genesisBlock.toBase32());
            throw new RuntimeException("Genesis block {} not found.");
        } else {
            aoiBlocks.add(genesisGeoHashPlusBlock);
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

        Map<String, Boolean> result = new HashMap<>();
        Geometry restorationSitePolygon = toPolygon(wktPolygon);
        for (var aoiBlock : aoiBlocks) {
            var aoiBlockPolygon = toPolygon(aoiBlock.wktPolygon());
            if (restorationSitePolygon.intersects(aoiBlockPolygon)) {
                result.put(aoiBlock.geoHashString(), true);
            } else {
                result.put(aoiBlock.geoHashString(), false);
            }
        }

        return result;
    }

    @Override
    public String getAoIUnion(String restorationSiteWktPolygonString) {
        throw new UnsupportedOperationException();
    }

    private Geometry toPolygon(String wktString) {
        try {
            return wktReader.read(wktString);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
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

    public GeoHash getGenesisBlock(String wktPolygon) {
        Point point = getCenter(wktPolygon);
        var latitude = point.getY();
        var longitude = point.getX();
        return GeoHash.withCharacterPrecision(latitude, longitude, 5);
    }

    private Point getCenter(String wktPolygon) {
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
