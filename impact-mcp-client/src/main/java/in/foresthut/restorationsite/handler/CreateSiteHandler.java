package in.foresthut.restorationsite.handler;

import in.foresthut.aoi.AoI;
import in.foresthut.commons.geohash.entity.GeoHashPlus;
import in.foresthut.impact.models.service.CreateSiteRequest;
import in.foresthut.impact.models.service.CreateSiteResponse;
import in.foresthut.impact.models.service.Error;
import in.foresthut.impact.models.service.Response;
import in.foresthut.infra.geohash.repository.GeoHashRepository;
import in.foresthut.infra.geohash.repository.GeoHashRepositoryImpl;
import in.foresthut.restorationsite.entity.RestorationSiteDao;
import in.foresthut.restorationsite.repository.RestorationSiteRepository;
import in.foresthut.restorationsite.validator.RequestValidator;
import in.foresthut.service.handler.MessageHandler;
import in.foresthut.user.repository.UserRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public record CreateSiteHandler(CreateSiteRequest createSiteRequest,
                                UserRepository userRepository,
                                RestorationSiteRepository restorationSiteRepository,
                                StreamObserver<Response> responseObserver) implements MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(CreateSiteHandler.class);


    @Override
    public void handle() {
        // Validate request
        var status = RequestValidator.validateCreateSiteRequest(createSiteRequest);

        if (status.isPresent()) {
            logger.error(
                    "Validation failed: {}", status.get()
                                                   .getDescription());
            var error = Error.newBuilder()
                             .setCode(status.get()
                                            .getCode()
                                            .name())
                             .setMessage(status.get()
                                               .getDescription())
                             .build();
            responseObserver.onNext(Response.newBuilder()
                                            .setError(error)
                                            .build());
            return;
        }

        // 1. Check if user id is valid
        if (userRepository.get(createSiteRequest.getUserId()) == null) {
            var errorResponse = Status.FAILED_PRECONDITION.withDescription("Invalid User Id.");
            logger.error("Invalid user Id: {}", createSiteRequest.getUserId());

            var error = Error.newBuilder()
                             .setCode(errorResponse
                                              .getCode()
                                              .name())
                             .setMessage(errorResponse
                                                 .getDescription())
                             .build();
            responseObserver.onNext(Response.newBuilder()
                                            .setError(error)
                                            .build());
            return;
        }

        // 2. Get AoI for the site
        GeoHashRepository geoHashRepository = new GeoHashRepositoryImpl();
        AoI aoI = new AoI(createSiteRequest.getWktPolygon(), geoHashRepository);
        List<GeoHashPlus> aoIGeoHashPlusList = aoI.getAoI();

        List<String> aoIGeoHashStrings = aoIGeoHashPlusList.stream()
                                                           .map(GeoHashPlus::geoHashString)
                                                           .toList();

        // 3. Create site in Db
        String siteId = UUID.randomUUID()
                            .toString();
        var restorationSiteDao = new RestorationSiteDao(
                siteId, createSiteRequest.getUserId(), createSiteRequest.getSiteName(),
                createSiteRequest.getWktPolygon(), aoIGeoHashStrings);
        restorationSiteRepository.add(restorationSiteDao);

        // 4. Respond with site id
        var createSiteResponse = CreateSiteResponse.newBuilder()
                                                   .setSiteId(siteId)
                                                   .build();
        responseObserver.onNext(Response.newBuilder()
                                        .setCreateSiteResponse(createSiteResponse)
                                        .build());
    }
}
