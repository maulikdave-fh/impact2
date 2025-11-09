package in.foresthut.infra.s2token.repository;

import in.foresthut.commons.s2geo.entity.S2CellTokenPlus;

import java.util.List;

public interface S2CellTokenPlusRepo {
    S2CellTokenPlus get(String cellToken);

    List<S2CellTokenPlus> get(double[] distanceFromSeaLimits,
                              double[] elevationLimits,
                              double[] latitudeLimits);
}
