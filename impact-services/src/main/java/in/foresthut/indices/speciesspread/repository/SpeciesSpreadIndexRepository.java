package in.foresthut.indices.speciesspread.repository;

import in.foresthut.indices.speciesdiversity.entity.SpeciesDiversityIndexDao;
import in.foresthut.indices.speciesspread.entity.SpeciesSpreadIndexDao;

public interface SpeciesSpreadIndexRepository {
    void add(SpeciesSpreadIndexDao speciesSpreadIndexDao);
    SpeciesSpreadIndexDao get(String siteId, int forYear);
    void delete(String siteId);
}
