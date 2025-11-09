package in.foresthut.restorationsite.service.handler;

import in.foresthut.impact.models.site.DeleteSiteRequest;
import in.foresthut.impact.models.site.DeleteSiteResponse;
import in.foresthut.indices.endemicity.repository.EndemicityIndexRepository;
import in.foresthut.indices.speciesdensity.repository.SpeciesDensityIndexRepository;
import in.foresthut.indices.speciesdiversity.repository.SpeciesDiversityIndexRepository;
import in.foresthut.indices.speciesspread.repository.SpeciesSpreadIndexRepository;
import in.foresthut.infra.MessageHandler;
import in.foresthut.restorationsite.mapper.SiteMapper;
import in.foresthut.restorationsite.repository.RestorationSiteRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record DeleteSiteHandler(DeleteSiteRequest deleteSiteRequest,
                                RestorationSiteRepository restorationSiteRepository,
                                SpeciesDensityIndexRepository speciesDensityIndexRepository,
                                SpeciesDiversityIndexRepository speciesDiversityIndexRepository,
                                SpeciesSpreadIndexRepository speciesSpreadIndexRepository,
                                EndemicityIndexRepository endemicityIndexRepository,
                                StreamObserver<DeleteSiteResponse> responseObserver) implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(DeleteSiteHandler.class);

    @Override
    public void handle() {
        // 1. Validate createSiteRequest
        if (deleteSiteRequest == null || deleteSiteRequest.getSiteId() == null || deleteSiteRequest.getSiteId()
                                                                                                   .isBlank()) {
            var errorResponse = Status.FAILED_PRECONDITION.withDescription("Invalid Request.");
            logger.error("Invalid createSiteRequest: {}", deleteSiteRequest);

            responseObserver.onError(errorResponse.asRuntimeException());
            return;
        }

        // 2. Delete site
        var userId = restorationSiteRepository.get(deleteSiteRequest.getSiteId())
                                              .userId();
        if (userId != null) {
            restorationSiteRepository.delete(deleteSiteRequest.getSiteId());

            // Delete saved indices as well
            speciesDensityIndexRepository.delete(deleteSiteRequest.getSiteId());
            speciesDiversityIndexRepository.delete(deleteSiteRequest.getSiteId());
            speciesSpreadIndexRepository.delete(deleteSiteRequest.getSiteId());
            endemicityIndexRepository.delete(deleteSiteRequest.getSiteId());
        } else {
            var errorResponse = Status.INVALID_ARGUMENT.withDescription("Invalid Request.");
            logger.error("Invalid createSiteRequest: {}", deleteSiteRequest);

            responseObserver.onError(errorResponse.asRuntimeException());
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
        responseObserver.onNext(deleteSiteResponse);
        responseObserver.onCompleted();
    }
}
