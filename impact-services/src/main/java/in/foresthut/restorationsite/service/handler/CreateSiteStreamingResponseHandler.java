package in.foresthut.restorationsite.service.handler;

import com.nirvighna.mongodb.commons.DatabaseConfig;
import in.foresthut.aoi.AoIProvider;
import in.foresthut.commons.continentality.Oceans;
import in.foresthut.commons.elevation.ElevationFinder;
import in.foresthut.commons.geometry.bioregion.WesternGhats;
import in.foresthut.impact.models.site.*;
import in.foresthut.infra.MessageHandler;
import in.foresthut.infra.geometry.AreaCalculator;
import in.foresthut.infra.s2token.repository.S2CellTokenPlusRepo;
import in.foresthut.infra.s2token.repository.S2CellTokenPlusRepoImpl;
import in.foresthut.restorationsite.entity.RestorationSiteDao;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public record CreateSiteStreamingResponseHandler(CreateSiteRequest createSiteRequest,
                                                 UserRepository userRepository,
                                                 RestorationSiteRepository restorationSiteRepository,
                                                 StreamObserver<SiteResponseV2> responseObserver,
                                                 AoIProvider aoIProvider) implements MessageHandler {


    private static final Logger logger = LoggerFactory.getLogger(CreateSiteStreamingResponseHandler.class);
    private static final WKTReader wktReader = new WKTReader();

    private static final ExecutorService threadPool = Executors.newVirtualThreadPerTaskExecutor();

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

        RestorationSiteDao restorationSiteDao = createSite(createSiteRequest, responseObserver);
        logger.info(
                "Restoration site id '{}' with name '{}' created.", restorationSiteDao.siteId(),
                restorationSiteDao.siteName());

        responseObserver.onCompleted();
    }

    public RestorationSiteDao createSite(CreateSiteRequest createSiteRequest,
                                         StreamObserver<SiteResponseV2> responseObserver) {
        CountDownLatch countDownLatch = new CountDownLatch(4);

        // 1. Calculate site area and respond
        AtomicReference<Double> siteArea = new AtomicReference<>((double) 0);
        threadPool.submit(() -> {
            siteArea.set(AreaCalculator.area(createSiteRequest.getWktPolygon()));
            var siteAreaResponse = SiteArea.newBuilder()
                                           .setArea(siteArea.get())
                                           .setUnit(AreaUnit.HECTARE)
                                           .build();
            responseObserver.onNext(SiteResponseV2.newBuilder()
                                                  .setSiteArea(siteAreaResponse)
                                                  .build());
            countDownLatch.countDown();
        });

        // 2. Calculate distance from sea and respond
        AtomicReference<Double> distanceFromSea = new AtomicReference<>((double) 0);
        threadPool.submit(() -> {
            distanceFromSea.set(Oceans.getInstance()
                                      .distance(getCenter(createSiteRequest.getWktPolygon())));

            var distanceFromSeaResponse = SiteDistanceFromSea.newBuilder()
                                                             .setDistanceFromSeaInKm(distanceFromSea.get())
                                                             .build();
            responseObserver.onNext(SiteResponseV2.newBuilder()
                                                  .setDistanceFromSea(distanceFromSeaResponse)
                                                  .build());
            countDownLatch.countDown();
        });

        // 3. Calculate min and max elevations and respond
        AtomicReference<Double> minElevation = new AtomicReference<>((double) 0);
        AtomicReference<Double> maxElevation = new AtomicReference<>((double) 0);

        threadPool.submit(() -> {
            double[] elevations = ElevationFinder.getInstance()
                                                 .elevationOf(createSiteRequest.getWktPolygon());
            minElevation.set(elevations[0]);
            maxElevation.set(elevations[1]);

            var elevationResponse = SiteElevation.newBuilder()
                                                 .setMinElevationInM(minElevation.get())
                                                 .setMaxElevationInM(maxElevation.get())
                                                 .build();
            responseObserver.onNext(SiteResponseV2.newBuilder()
                                                  .setElevation(elevationResponse)
                                                  .build());
            countDownLatch.countDown();
        });

        // 4. Get AoI union and respond
        AtomicReference<Map<String, Boolean>> aoICellTokenStrings = new AtomicReference<>(new HashMap<>());
        AtomicReference<String> aoIUnion = new AtomicReference<>(new String());
        threadPool.submit(() -> {
            aoICellTokenStrings.set(aoIProvider.getAoI(createSiteRequest.getWktPolygon()));

            logger.info("The site '{}' has {} AoI blocks", createSiteRequest.getSiteName(),
                        aoICellTokenStrings.get().size());

            aoIUnion.set(GetAoIUnionHandler.getAoIUnionWktString(aoICellTokenStrings.get()));

            var aoIResponse = GetUnionAoIResponse.newBuilder().setAoiWktPolygonString(aoIUnion.get()).build();
            responseObserver.onNext(SiteResponseV2.newBuilder().setAoiUnion(aoIResponse).build());
            countDownLatch.countDown();
        });

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            logger.error("Error: ", e);
            throw new RuntimeException(e);
        }

        // 5. Generate site Id & respond with site details
        String siteId = UUID.randomUUID()
                            .toString();

        var siteDetails = SiteDetails.newBuilder()
                                     .setSiteId(siteId)
                                     .setSiteName(createSiteRequest().getSiteName())
                                     .setWktPolygon(createSiteRequest.getWktPolygon())
                                     .build();
        responseObserver.onNext(SiteResponseV2.newBuilder()
                                              .setSiteDetails(siteDetails)
                                              .build());
        var restorationSiteDao = new RestorationSiteDao(
                siteId, createSiteRequest.getUserId(), createSiteRequest.getSiteName(),
                createSiteRequest.getWktPolygon(), siteArea.get(), AreaUnit.HECTARE.toString(),
                aoICellTokenStrings.get(), distanceFromSea.get(), minElevation.get(),
                maxElevation.get(), aoIUnion.get());
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
