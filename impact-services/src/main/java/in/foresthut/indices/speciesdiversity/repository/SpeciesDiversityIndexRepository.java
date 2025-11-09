package in.foresthut.indices.speciesdiversity.repository;

import in.foresthut.indices.speciesdiversity.entity.SpeciesDiversityIndexDao;

public interface SpeciesDiversityIndexRepository {
    void add(SpeciesDiversityIndexDao speciesDensityIndexDao);
    SpeciesDiversityIndexDao get(String siteId, int forYear);
    void delete(String siteId);
}
