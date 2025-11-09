package in.foresthut.restorationsite.entity;

import java.util.List;
import java.util.Objects;

public record RestorationSiteDao(String siteId,
                                 String userId,
                                 String siteName,
                                 String wktPolygon,
                                 List<String> aoIGeoHashes) {
    public RestorationSiteDao {
        Objects.requireNonNull(siteId, "Site id cannot be null.");
        Objects.requireNonNull(userId, "User id cannot be null.");
        Objects.requireNonNull(siteName, "Site name cannot be null.");
        Objects.requireNonNull(wktPolygon, "Wkt polygon cannot be null.");
        Objects.requireNonNull(aoIGeoHashes, "AoI geohashes cannot be null.");

        if (aoIGeoHashes.isEmpty()) throw new IllegalArgumentException("AoI geohashes list cannot be empty.");
    }
}
