package in.foresthut.restorationsite.service.handler;

import in.foresthut.impact.models.site.GetSiteRequest;
import in.foresthut.impact.models.site.GetSiteResponse;
import in.foresthut.infra.MessageHandler;
import in.foresthut.restorationsite.entity.RestorationSiteDao;
import in.foresthut.restorationsite.mapper.SiteMapper;
import in.foresthut.restorationsite.repository.RestorationSiteRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record GetSiteHandler(GetSiteRequest getSiteRequest,
                             RestorationSiteRepository restorationSiteRepository,
                             StreamObserver<GetSiteResponse> responseObserver) implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(GetSiteHandler.class);

    @Override
    public void handle() {
        // Validate Request
        if (getSiteRequest.getSiteId() == null || getSiteRequest.getSiteId().isBlank()) {
            var errorResponse = Status.FAILED_PRECONDITION.withDescription("Invalid Site Id.");
            logger.error("Invalid site Id: '{}'", getSiteRequest.getSiteId());

            responseObserver.onError(errorResponse.asRuntimeException());
            return;
        }

        // Fetch from Db
        RestorationSiteDao restorationSiteDao = restorationSiteRepository.get(getSiteRequest.getSiteId());

        if (restorationSiteDao == null) {
            var errorResponse = Status.FAILED_PRECONDITION.withDescription("Site id not found.");
            logger.error("Site id '{}' not found.", getSiteRequest.getSiteId());

            responseObserver.onError(errorResponse.asRuntimeException());
            return;
        }

        // Return
        responseObserver.onNext(GetSiteResponse.newBuilder()
                                               .setSite(SiteMapper.toSite(restorationSiteDao))
                                               .build());
        responseObserver.onCompleted();
    }
}
