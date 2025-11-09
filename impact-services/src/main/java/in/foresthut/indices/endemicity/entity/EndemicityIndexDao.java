package in.foresthut.indices.endemicity.entity;

import java.util.Map;
import java.util.Objects;

public record EndemicityIndexDao(String siteId, double index,
                                 Map<String, Double> SpeciesWiseEndemicity) {

    public EndemicityIndexDao {
        Objects.requireNonNull(siteId, "Site id cannot be null.");
        if (siteId.isBlank()) throw new IllegalArgumentException("Site id cannot be blank.");
    }
}
