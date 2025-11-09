package in.foresthut.restorationsite.service.handler;

import com.nirvighna.mongodb.commons.DatabaseConfig;
import in.foresthut.aoi.AoIProvider;
import in.foresthut.commons.continentality.Oceans;
import in.foresthut.commons.elevation.ElevationFinder;
import in.foresthut.commons.geometry.bioregion.WesternGhats;
import in.foresthut.impact.models.site.AreaUnit;
import in.foresthut.impact.models.site.CreateSiteRequest;
import in.foresthut.impact.models.site.CreateSiteResponse;
import in.foresthut.infra.MessageHandler;
import in.foresthut.infra.geometry.AreaCalculator;
import in.foresthut.infra.s2token.repository.S2CellTokenPlusRepo;
import in.foresthut.infra.s2token.repository.S2CellTokenPlusRepoImpl;
import in.foresthut.restorationsite.entity.RestorationSiteDao;
import in.foresthut.restorationsite.mapper.SiteMapper;
import in.foresthut.restorationsite.repository.RestorationSiteRepository;
import in.foresthut.restorationsite.validator.RequestValidator;
import in.foresthut.user.repository.UserRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public record CreateSiteHandler(CreateSiteRequest createSiteRequest,
                                UserRepository userRepository,
                                RestorationSiteRepository restorationSiteRepository,
                                StreamObserver<CreateSiteResponse> responseObserver,
                                AoIProvider aoIProvider) implements MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(CreateSiteHandler.class);
    private static final WKTReader wktReader = new WKTReader();

    @Override
    public void handle() {
        logger.info("Wkt polygon string {}", createSiteRequest.getWktPolygon());

        // Validate createSiteRequest
        var status = RequestValidator.validateCreateSiteRequest(createSiteRequest);

        if (status.isPresent()) {
            logger.error(
                    "Validation failed: {}", status.get()
                                                   .getDescription());
            responseObserver.onError(status.get()
                                           .asRuntimeException());
            return;
        }

        // 1. Check if user id is valid
        if (userRepository.get(createSiteRequest.getUserId()) == null) {
            var errorResponse = Status.FAILED_PRECONDITION.withDescription("Invalid User Id.");
            logger.error("Invalid user Id: {}", createSiteRequest.getUserId());

            responseObserver.onError(errorResponse.asRuntimeException());
            return;
        }

        // 1.5. Check if the site falls in the supported region
        if (!WesternGhats.getInstance()
                         .contains(createSiteRequest().getWktPolygon())) {
            var errorResponse =
                    Status.FAILED_PRECONDITION.withDescription("Restoration site falls outside the supported region.");
            logger.error("Restoration site falls outside the supported region.");

            responseObserver.onError(errorResponse.asRuntimeException());
            return;
        }

        RestorationSiteDao restorationSiteDao = createSite(createSiteRequest);
        logger.info(
                "Restoration site id '{}' with name '{}' created.", restorationSiteDao.siteId(),
                restorationSiteDao.siteName());

        // 4. Respond with newly created site
        var createSiteResponse = CreateSiteResponse.newBuilder()
                                                   .setSite(SiteMapper.toSite(restorationSiteDao))
                                                   .build();
        responseObserver.onNext(createSiteResponse);
        responseObserver.onCompleted();
    }

    public RestorationSiteDao createSite(CreateSiteRequest createSiteRequest) {
        // 2. Get AoI for the site
        var aoICellTokenStrings = aoIProvider.getAoI(createSiteRequest.getWktPolygon());

        logger.info("The site '{}' has {} AoI blocks", createSiteRequest.getSiteName(), aoICellTokenStrings.size());

        // 3. Create site in Db
        String siteId = UUID.randomUUID()
                            .toString();

        double distanceFromSea = Oceans.getInstance()
                                       .distance(getCenter(createSiteRequest.getWktPolygon()));
        double[] elevations = ElevationFinder.getInstance()
                                             .elevationOf(createSiteRequest.getWktPolygon());

        var restorationSiteDao = new RestorationSiteDao(
                siteId, createSiteRequest.getUserId(), createSiteRequest.getSiteName(),
                createSiteRequest.getWktPolygon(), AreaCalculator.area(createSiteRequest.getWktPolygon()),
                AreaUnit.HECTARE.toString(), aoICellTokenStrings, distanceFromSea, elevations[0], elevations[1], null);
        restorationSiteRepository.add(restorationSiteDao);
        return restorationSiteDao;
    }

    private Point getCenter(String wktPolygon) {
        Geometry polygon = null;
        try {
            polygon = wktReader.read(wktPolygon);
        } catch (ParseException e) {
            logger.error("Error parsing {}", wktPolygon, e);
            throw new RuntimeException(e);
        }
        return polygon.getCentroid();
    }
}
