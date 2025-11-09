package in.foresthut.infra.endemicity.entity;

import java.time.LocalDateTime;

public record Endemicity(String speciesName,
                         String bioregion,
                         long bioregionOccurrenceCount,
                         long worldOccurrenceCount,
                         double index,
                         LocalDateTime calculatedOn) {
}
