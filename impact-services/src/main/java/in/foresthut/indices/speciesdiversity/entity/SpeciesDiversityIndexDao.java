package in.foresthut.indices.speciesdiversity.entity;

import in.foresthut.impact.models.indices.speciesdensity.SpeciesDensityAoIBlockPlus;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record SpeciesDiversityIndexDao(String siteId,
                                       int forYear,
                                       double index,
                                       Map<String, Long> speciesCount) {
    public SpeciesDiversityIndexDao {
        Objects.requireNonNull(siteId, "Site id cannot be null.");
    }
}
