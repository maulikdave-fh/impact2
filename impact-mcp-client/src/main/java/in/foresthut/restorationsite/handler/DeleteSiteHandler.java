package in.foresthut.restorationsite.handler;

import in.foresthut.impact.models.service.DeleteSiteRequest;
import in.foresthut.impact.models.service.DeleteSiteResponse;
import in.foresthut.impact.models.service.Error;
import in.foresthut.impact.models.service.Response;
import in.foresthut.restorationsite.mapper.SiteMapper;
import in.foresthut.restorationsite.repository.RestorationSiteRepository;
import in.foresthut.service.handler.MessageHandler;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record DeleteSiteHandler(DeleteSiteRequest deleteSiteRequest,
                                RestorationSiteRepository restorationSiteRepository,
                                StreamObserver<Response> responseObserver) implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(DeleteSiteHandler.class);

    @Override
    public void handle() {
        // 1. Validate request
        if (deleteSiteRequest == null || deleteSiteRequest.getSiteId() == null || deleteSiteRequest.getSiteId()
                                                                                                   .isBlank()) {
            var errorResponse = Status.FAILED_PRECONDITION.withDescription("Invalid Request.");
            logger.error("Invalid request: {}", deleteSiteRequest);

            var error = Error.newBuilder()
                             .setCode(errorResponse.getCode()
                                                   .name())
                             .setMessage(errorResponse.getDescription())
                             .build();
            responseObserver.onNext(Response.newBuilder()
                                            .setError(error)
                                            .build());
            return;
        }

        // 2. Delete site
        var userId = restorationSiteRepository.get(deleteSiteRequest.getSiteId())
                                              .userId();
        if (userId != null) {
            restorationSiteRepository.delete(deleteSiteRequest.getSiteId());
        } else {
            var errorResponse = Status.INVALID_ARGUMENT.withDescription("Invalid Request.");
            logger.error("Invalid request: {}", deleteSiteRequest);

            var error = Error.newBuilder()
                             .setCode(errorResponse.getCode()
                                                   .name())
                             .setMessage(errorResponse.getDescription())
                             .build();
            responseObserver.onNext(Response.newBuilder()
                                            .setError(error)
                                            .build());
            return;
        }

        // 3. Send response
        var siteDaos = restorationSiteRepository.getByUserId(userId);
        var sites = siteDaos.stream()
                            .map(SiteMapper::toSite)
                            .toList();
        var deleteSiteResponse = DeleteSiteResponse.newBuilder()
                                                   .addAllSite(sites)
                                                   .build();
        responseObserver.onNext(Response.newBuilder()
                                        .setDeleteSiteResponse(deleteSiteResponse)
                                        .build());
    }
}
