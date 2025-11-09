package in.foresthut.indices.speciesspread.entity;

import java.util.Map;
import java.util.Objects;

public record SpeciesSpreadIndexDao(String siteId,
                                    int forYear,
                                    double index,
                                    RestorationSiteMetaData restorationSiteMetaData,
                                    AoIMetaData aoIMetaData) {
    public SpeciesSpreadIndexDao {
        Objects.requireNonNull(siteId, "Site id cannot be null.");
    }

    public record RestorationSiteMetaData(long totalSpeciesCount,
                                          Map<String, Long> taxaWiseSpeciesCount) {

    }

    public record AoIMetaData(long totalSpeciesCount,
                              Map<String, Long> taxaWiseSpeciesCount) {

    }
}
