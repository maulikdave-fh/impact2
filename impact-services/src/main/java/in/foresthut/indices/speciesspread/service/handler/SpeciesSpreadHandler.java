package in.foresthut.indices.speciesspread.service.handler;

import in.foresthut.impact.models.indices.speciesspread.AoIMetaData;
import in.foresthut.impact.models.indices.speciesspread.RestorationSiteMetaData;
import in.foresthut.impact.models.indices.speciesspread.SpeciesSpreadRequest;
import in.foresthut.impact.models.indices.speciesspread.SpeciesSpreadResponse;
import in.foresthut.indices.speciesspread.entity.SpeciesSpreadIndexDao;
import in.foresthut.indices.speciesspread.repository.SpeciesSpreadIndexRepository;
import in.foresthut.indices.speciesspread.validator.RequestValidator;
import in.foresthut.infra.MessageHandler;
import in.foresthut.infra.occurrence.repository.OccurrenceRepository;
import in.foresthut.restorationsite.entity.RestorationSiteDao;
import in.foresthut.restorationsite.repository.RestorationSiteRepository;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record SpeciesSpreadHandler(SpeciesSpreadRequest request,
                                   OccurrenceRepository occurrenceRepository,
                                   RestorationSiteRepository restorationSiteRepository,
                                   SpeciesSpreadIndexRepository speciesSpreadIndexRepository,
                                   StreamObserver<SpeciesSpreadResponse> responseObserver) implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(SpeciesSpreadHandler.class);

    @Override
    public void handle() {
        // Validate createSiteRequest
        var status = RequestValidator.validateSpeciesSpreadIndexRequest(request);

        if (status.isPresent()) {
            logger.error(
                    "Validation failed: {}", status.get()
                                                   .getDescription());
            responseObserver.onError(status.get()
                                           .asRuntimeException());
            return;
        }

        // if already exists in species diversity collection, return
        SpeciesSpreadIndexDao existingSpeciesSpread =
                speciesSpreadIndexRepository.get(request.getSiteId(), request.getForYear());
        if (existingSpeciesSpread != null) {
            var response = SpeciesSpreadResponse.newBuilder()
                                                .setSiteId(existingSpeciesSpread.siteId())
                                                .setForYear(existingSpeciesSpread.forYear())
                                                .setIndex(existingSpeciesSpread.index())
                                                .setRestorationSiteMetadata(RestorationSiteMetaData.newBuilder()
                                                                                                   .setTotalSpeciesCount(
                                                                                                           existingSpeciesSpread.restorationSiteMetaData()
                                                                                                                                .totalSpeciesCount())
                                                                                                   .putAllTaxaWiseSpeciesCount(
                                                                                                           existingSpeciesSpread.restorationSiteMetaData()
                                                                                                                                .taxaWiseSpeciesCount())
                                                                                                   .build())
                                                .setAoiMetadata(AoIMetaData.newBuilder()
                                                                           .setTotalSpeciesCount(
                                                                                   existingSpeciesSpread.aoIMetaData()
                                                                                                        .totalSpeciesCount())
                                                                           .putAllTaxaWiseSpeciesCount(
                                                                                   existingSpeciesSpread.aoIMetaData()
                                                                                                        .taxaWiseSpeciesCount())
                                                                           .build())
                                                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        RestorationSiteDao restorationSiteDao = restorationSiteRepository.get(request().getSiteId());

        SpeciesSpreadIndexCalculator.SpeciesSpreadIndexValueObj speciesSpreadIndexValueObj =
                new SpeciesSpreadIndexCalculator(
                        restorationSiteDao.wktPolygon(), restorationSiteDao.aoIUnionWktPolygon(),
                        request().getForYear(), occurrenceRepository).calculate();

        // Construct response
        RestorationSiteMetaData restorationSiteMetaData = RestorationSiteMetaData.newBuilder()
                                                                                 .setTotalSpeciesCount(
                                                                                         speciesSpreadIndexValueObj.restorationSiteMetaData()
                                                                                                                   .totalSpeciesCount())
                                                                                 .putAllTaxaWiseSpeciesCount(
                                                                                         speciesSpreadIndexValueObj.restorationSiteMetaData()
                                                                                                                   .taxaWiseSpeciesCount())
                                                                                 .build();

        AoIMetaData aoIMetaData = AoIMetaData.newBuilder()
                                             .setTotalSpeciesCount(speciesSpreadIndexValueObj.aoIMetaData()
                                                                                             .totalSpeciesCount())
                                             .putAllTaxaWiseSpeciesCount(speciesSpreadIndexValueObj.aoIMetaData()
                                                                                                   .taxaWiseSpeciesCount())
                                             .build();

        SpeciesSpreadResponse speciesSpreadResponse = SpeciesSpreadResponse.newBuilder()
                                                                           .setSiteId(request.getSiteId())
                                                                           .setForYear(request.getForYear())
                                                                           .setIndex(speciesSpreadIndexValueObj.index())
                                                                           .setRestorationSiteMetadata(
                                                                                   restorationSiteMetaData)
                                                                           .setAoiMetadata(aoIMetaData)
                                                                           .build();

        responseObserver.onNext(speciesSpreadResponse);
        responseObserver.onCompleted();
    }
}
