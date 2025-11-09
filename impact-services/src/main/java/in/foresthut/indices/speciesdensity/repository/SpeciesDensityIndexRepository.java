package in.foresthut.indices.speciesdensity.repository;

import in.foresthut.indices.speciesdensity.entity.SpeciesDensityIndexDao;

public interface SpeciesDensityIndexRepository {
    void add(SpeciesDensityIndexDao speciesDensityIndexDao);
    SpeciesDensityIndexDao get(String siteId, int forYear);
    void delete(String siteId);
}
