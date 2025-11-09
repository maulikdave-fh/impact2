package in.foresthut.commons.occurrence.mapper;

import in.foresthut.commons.occurrence.Occurrence;
import in.foresthut.commons.occurrence.entity.OccurrenceDao;
import org.bson.Document;

import java.util.List;

public class OccurrenceMapper {
    public static OccurrenceDao toOccurrenceDao(Occurrence occurrence) {
        Document geoJsonPoint = new Document("type", "Point").append(
                "coordinates",
                List.of(
                        occurrence.decimalLongitude(),
                        occurrence.decimalLatitude()));

        return new OccurrenceDao(
                occurrence.key(), occurrence.species(), occurrence.kingdom(), occurrence.phylum(), occurrence._class(),
                occurrence.order(), occurrence.superFamily(), occurrence.family(), occurrence.subFamily(),
                occurrence.tribe(), occurrence.subTribe(), occurrence.genus(), occurrence.subGenus(),
                occurrence.iconicTaxa(), occurrence.iucnRedListCategory(), occurrence.decimalLatitude(),
                occurrence.decimalLongitude(), geoJsonPoint, occurrence.elevationInMeters(),
                occurrence.distanceFromSeaInKm(), occurrence.eventDate(), occurrence.modified(),
                occurrence.datasetName(), occurrence.recordedBy(), occurrence.geoHashString(), occurrence.country(),
                occurrence.state(), occurrence.district(), occurrence.county());
    }

    public static Occurrence toOccurrence(OccurrenceDao occurrenceDao) {
        return new Occurrence(
                occurrenceDao.key(), occurrenceDao.species(), occurrenceDao.kingdom(), occurrenceDao.phylum(),
                occurrenceDao._class(), occurrenceDao.order(), occurrenceDao.superFamily(), occurrenceDao.family(),
                occurrenceDao.subFamily(), occurrenceDao.tribe(), occurrenceDao.subTribe(), occurrenceDao.genus(),
                occurrenceDao.subGenus(), occurrenceDao.iconicTaxa(), occurrenceDao.iucnRedListCategory(),
                occurrenceDao.decimalLatitude(), occurrenceDao.decimalLongitude(), occurrenceDao.elevationInMeters(),
                occurrenceDao.distanceFromSeaInKm(), occurrenceDao.eventDate(), occurrenceDao.modified(),
                occurrenceDao.datasetName(), occurrenceDao.recordedBy(), occurrenceDao.s2CellToken(),
                occurrenceDao.country(), occurrenceDao.state(), occurrenceDao.district(), occurrenceDao.county());
    }
}
