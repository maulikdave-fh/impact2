package in.foresthut.restorationsite.entity;

import java.util.Map;
import java.util.Objects;

public record RestorationSiteDao(String siteId,
                                 String userId,
                                 String siteName,
                                 String wktPolygon,
                                 double area,
                                 String areaUnit,
                                 Map<String, Boolean> aoITokens,
                                 double distanceFromSeaInKm,
                                 double minElevationInMeter,
                                 double maxElevationInMeter,
                                 String aoIUnionWktPolygon
                            ) {
    public RestorationSiteDao {
        Objects.requireNonNull(siteId, "Site id cannot be null.");
        Objects.requireNonNull(userId, "User id cannot be null.");
        Objects.requireNonNull(siteName, "Site name cannot be null.");
        Objects.requireNonNull(wktPolygon, "Wkt polygon cannot be null.");
        //Objects.requireNonNull(aoITokens, "AoI tokens cannot be null.");
        Objects.requireNonNull(areaUnit, "Unit of area cannot be null.");

        if (area <= 0) throw new IllegalArgumentException("Area cannot be less or equal to zero.");

        //  if (aoITokens.isEmpty()) throw new IllegalArgumentException("AoI tokens list cannot be empty.");
    }
}
