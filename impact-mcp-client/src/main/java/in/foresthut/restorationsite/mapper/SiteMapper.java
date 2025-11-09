package in.foresthut.restorationsite.mapper;

import in.foresthut.restorationsite.entity.RestorationSiteDao;

public class SiteMapper {
    public static in.foresthut.impact.models.service.Site toSite(RestorationSiteDao restorationSiteDao) {
        return in.foresthut.impact.models.service.Site.newBuilder()
                                                   .setSiteId(restorationSiteDao.siteId())
                                                   .setSiteName(restorationSiteDao.siteName())
                                                   .setWktPolygon(restorationSiteDao.wktPolygon())
                                                   .build();
    }
}
