package in.foresthut.restorationsite.repository;

import in.foresthut.restorationsite.entity.RestorationSiteDao;

import java.util.List;
import java.util.Map;

public interface RestorationSiteRepository {
    String add(RestorationSiteDao restorationSiteDao);
    RestorationSiteDao get(String siteId);
    List<RestorationSiteDao> getByUserId(String userId);
    void delete(String siteId);
    RestorationSiteDao update(RestorationSiteDao restorationSiteDao);
    Map<String, Boolean> getAoI(String siteId);
}
