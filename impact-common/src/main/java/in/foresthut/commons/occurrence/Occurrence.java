package in.foresthut.commons.occurrence;

import java.time.LocalDateTime;

public record Occurrence(long key,
                         String species,
                         String kingdom,
                         String phylum,
                         String _class,
                         String order,
                         String superFamily,
                         String family,
                         String subFamily,
                         String tribe,
                         String subTribe,
                         String genus,
                         String subGenus,
                         String iconicTaxa,
                         String iucnRedListCategory,
                         double decimalLatitude,
                         double decimalLongitude,
                         double elevationInMeters,
                         double distanceFromSeaInKm,
                         LocalDateTime eventDate,
                         LocalDateTime modified,
                         String datasetName,
                         String recordedBy,
                         String geoHashString,
                         String country,
                         String state,
                         String district,
                         String county) {
}
