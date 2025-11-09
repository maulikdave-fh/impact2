package in.foresthut.indices.endemicity.repository;

import in.foresthut.indices.endemicity.entity.EndemicityIndexDao;

public interface EndemicityIndexRepository {
    void add(EndemicityIndexDao endemicityIndexDao);
    EndemicityIndexDao get(String siteId);
    void delete(String siteId);
}
