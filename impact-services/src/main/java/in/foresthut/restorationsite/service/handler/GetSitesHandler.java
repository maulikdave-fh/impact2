package in.foresthut.restorationsite.service.handler;

import in.foresthut.impact.models.site.GetSitesRequest;
import in.foresthut.impact.models.site.GetSitesResponse;
import in.foresthut.impact.models.site.SiteDetails;
import in.foresthut.infra.MessageHandler;
import in.foresthut.restorationsite.entity.RestorationSiteDao;
import in.foresthut.restorationsite.repository.RestorationSiteRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public record GetSitesHandler(GetSitesRequest request,
                              RestorationSiteRepository restorationSiteRepository,
                              StreamObserver<GetSitesResponse> responseObserver) implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(GetSitesHandler.class);

    @Override
    public void handle() {
        // Validate Request
        if (request.getUserId() == null || request.getUserId()
                                                  .isBlank()) {
            var errorResponse = Status.FAILED_PRECONDITION.withDescription("Invalid User Id.");
            logger.error("Invalid user Id: '{}'", request.getUserId());

            responseObserver.onError(errorResponse.asRuntimeException());
            return;
        }

        List<RestorationSiteDao> userSites = restorationSiteRepository.getByUserId(request.getUserId());

        List<SiteDetails> siteDetails = userSites.stream()
                                                 .map(site -> SiteDetails.newBuilder()
                                                                         .setSiteId(site.siteId())
                                                                         .setSiteName(site.siteName())
                                                                         .setWktPolygon(site.wktPolygon())
                                                                         .build())
                                                 .toList();

        responseObserver.onNext(GetSitesResponse.newBuilder().addAllSites(siteDetails).build());
        responseObserver.onCompleted();
    }
}
