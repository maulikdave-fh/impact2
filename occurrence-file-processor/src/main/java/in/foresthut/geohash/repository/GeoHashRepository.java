package in.foresthut.geohash.repository;

import java.util.List;

public interface GeoHashRepository {
    List<String> getAllGeoHashString();
    boolean exists(String geoHashString);
}
