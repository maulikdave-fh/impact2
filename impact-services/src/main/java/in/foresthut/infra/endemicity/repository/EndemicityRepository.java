package in.foresthut.infra.endemicity.repository;

import in.foresthut.infra.endemicity.entity.Endemicity;

public interface EndemicityRepository {
    void add(Endemicity endemicity);
    Endemicity get(String speciesName, String bioregion);
}
