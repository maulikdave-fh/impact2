package in.foresthut.occurrence.repository;

import in.foresthut.commons.occurrence.Occurrence;
import in.foresthut.commons.occurrence.entity.OccurrenceDao;
import in.foresthut.occurrence.processor.OccurrenceDataProcessor;

public interface OccurrenceRepository {
    void add(OccurrenceDao occurrence);
}
