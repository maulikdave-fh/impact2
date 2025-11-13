package in.foresthut.restorationsite.mapper;

import in.foresthut.impact.models.site.AreaUnit;
import in.foresthut.impact.models.site.GetUnionAoIResponse;
import in.foresthut.impact.models.site.Site;
import in.foresthut.restorationsite.entity.RestorationSiteDao;

public class SiteMapper {
    public static in.foresthut.impact.models.site.Site toSite(RestorationSiteDao restorationSiteDao) {
        return Site.newBuilder()
                   .setSiteId(restorationSiteDao.siteId())
                   .setSiteName(restorationSiteDao.siteName())
                   .setWktPolygon(restorationSiteDao.wktPolygon())
                   .setArea(restorationSiteDao.area())
                   .setUnit(AreaUnit.HECTARE)
                   .setDistanceFromSeaInKm(restorationSiteDao.distanceFromSeaInKm())
                   .setMinElevationInM(restorationSiteDao.minElevationInMeter())
                   .setMaxElevationInM(restorationSiteDao.maxElevationInMeter())
//                   .setAoiUnion(GetUnionAoIResponse.newBuilder()
//                                                   .setAoiWktPolygonString(restorationSiteDao.aoIUnionWktPolygon())
//                                                   .build())
                   .build();
    }
}
