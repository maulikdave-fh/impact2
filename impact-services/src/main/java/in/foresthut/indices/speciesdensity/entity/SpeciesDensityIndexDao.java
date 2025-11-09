package in.foresthut.indices.speciesdensity.entity;

import in.foresthut.impact.models.indices.speciesdensity.SpeciesDensityAoIBlockPlus;

import java.util.List;
import java.util.Objects;

public record SpeciesDensityIndexDao(String siteId,
                                     int forYear,
                                     double index,
                                     List<SpeciesDensityAoIBlockPlus> aoiBlocks) {
    public SpeciesDensityIndexDao {
        Objects.requireNonNull(siteId, "Site id cannot be null.");

        if (aoiBlocks == null || aoiBlocks.isEmpty())
            throw new IllegalArgumentException("AoI block " + "list cannot " + "be empty or " + "null.");
    }
}
