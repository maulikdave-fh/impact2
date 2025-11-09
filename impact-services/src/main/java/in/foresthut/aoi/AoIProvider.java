package in.foresthut.aoi;

import java.util.Map;

public interface AoIProvider {
    Map<String, Boolean> getAoI(String restorationSiteWktPolygonString);
    String getAoIUnion(String restorationSiteWktPolygonString);
}
