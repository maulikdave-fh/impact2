package in.foresthut.restorationsite.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.nirvighna.mongodb.commons.DatabaseConfig;
import in.foresthut.restorationsite.entity.RestorationSiteDao;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RestorationSiteRepositoryImpl implements RestorationSiteRepository {
    private final static Logger logger = LoggerFactory.getLogger(RestorationSiteRepositoryImpl.class);

    private final MongoCollection<RestorationSiteDao> restorationSiteMongoCollection;

    public RestorationSiteRepositoryImpl() {
        this.restorationSiteMongoCollection = DatabaseConfig.getInstance()
                                                            .database()
                                                            .getCollection(
                                                                    "restoration_sites",
                                                                    RestorationSiteDao.class);
    }

    @Override
    public String add(RestorationSiteDao restorationSiteDao) {
        restorationSiteMongoCollection.insertOne(restorationSiteDao);
        return restorationSiteDao.siteId();
    }

    @Override
    public RestorationSiteDao get(String siteId) {
        Bson filter = Filters.eq("siteId", siteId);
        return restorationSiteMongoCollection.find(filter)
                                             .first();
    }

    @Override
    public List<RestorationSiteDao> getByUserId(String userId) {
        List<RestorationSiteDao> userSites = new ArrayList<>();
        Bson filter = Filters.eq("userId", userId);
        restorationSiteMongoCollection.find(filter)
                                      .into(userSites);
        return userSites;
    }

    @Override
    public void delete(String siteId) {
        Bson filter = Filters.eq("siteId", siteId);
        restorationSiteMongoCollection.deleteOne(filter);
    }

    @Override
    public RestorationSiteDao update(RestorationSiteDao restorationSiteDao) {
        Bson filter = Filters.eq("siteId", restorationSiteDao.siteId());
        Bson updateName = Updates.set("siteName", restorationSiteDao.siteName());
        Bson updateWktPolygon = Updates.set("wktPolygon", restorationSiteDao.wktPolygon());
        Bson updateAoI = Updates.set("aoIGeoHashes", restorationSiteDao.aoIGeoHashes());

        List<Bson> updateList = List.of(updateName, updateWktPolygon, updateAoI);

        restorationSiteMongoCollection.updateOne(filter, updateList);

        return get(restorationSiteDao.siteId());
    }
}
