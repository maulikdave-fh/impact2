package in.foresthut.repository;

import in.foresthut.commons.s2geo.entity.S2CellTokenPlus;

import java.util.List;

public interface S2CellTokenPlusRepo {
    void add(S2CellTokenPlus cellTokenPlus);
    void addMany(List<S2CellTokenPlus> tokenPlusList);
}
