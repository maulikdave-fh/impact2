package in.foresthut.infra.geohash.repository;

import in.foresthut.commons.geohash.entity.GeoHashPlus;

import java.util.List;

public interface GeoHashRepository {
    GeoHashPlus get(String geoHashString);
    List<GeoHashPlus> get(double[] distanceFromSeaLimits,
                          double[] elevationLimits,
                          double[] latitudeLimits);
}
