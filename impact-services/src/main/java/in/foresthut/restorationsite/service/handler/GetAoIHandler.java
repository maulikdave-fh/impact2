package in.foresthut.restorationsite.service.handler;

import in.foresthut.commons.s2geo.entity.S2CellTokenPlus;
import in.foresthut.impact.models.site.GetAoIRequest;
import in.foresthut.impact.models.site.GetAoIResponse;
import in.foresthut.infra.MessageHandler;
import in.foresthut.restorationsite.repository.RestorationSiteRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.locationtech.jts.io.WKBWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public record GetAoIHandler(GetAoIRequest request,
                            RestorationSiteRepository restorationSiteRepository,
                            StreamObserver<GetAoIResponse> responseObserver) implements MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(GetAoIHandler.class);

    @Override
    public void handle() {
        logger.info("Get AoI createSiteRequest for site Id {}", request.getSiteId());
        // Validate Request
        if (request.getSiteId() == null || request.getSiteId()
                                                  .isBlank()) {
            var errorResponse = Status.FAILED_PRECONDITION.withDescription("Invalid Site Id.");
            logger.error("Invalid site Id: '{}'", request.getSiteId());

            responseObserver.onError(errorResponse.asRuntimeException());
            return;
        }

        // Fetch from Db
        Map<String, Boolean> aoiBlocks = restorationSiteRepository.getAoI(request.getSiteId());

        if (aoiBlocks == null) {
            var errorResponse = Status.FAILED_PRECONDITION.withDescription("Site id not found.");
            logger.error("Site id '{}' not found.", request.getSiteId());

            responseObserver.onError(errorResponse.asRuntimeException());
            return;
        }
        logger.info("Found {} blocks for site '{}'", aoiBlocks.size(), request.getSiteId());
        for (var entryItem : aoiBlocks.entrySet()) {
            var polygonDetails = S2CellTokenPlus.cellDetails(entryItem.getKey());

            responseObserver.onNext(GetAoIResponse.newBuilder()
                                                  .setS2CellToken(entryItem.getKey())
                                                  .setIsInRestorationSite(entryItem.getValue())
                                                  .setWktPolygonString(polygonDetails.wktPolygonString())
                                                  .build());
        }
        responseObserver.onCompleted();
    }
}
