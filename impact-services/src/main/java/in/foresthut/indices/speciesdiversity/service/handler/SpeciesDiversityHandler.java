package in.foresthut.indices.speciesdiversity.service.handler;

import in.foresthut.impact.models.indices.speciesdiversity.SpeciesDiversityRequest;
import in.foresthut.impact.models.indices.speciesdiversity.SpeciesDiversityResponse;
import in.foresthut.indices.speciesdiversity.entity.SpeciesDiversityIndexDao;
import in.foresthut.indices.speciesdiversity.repository.SpeciesDiversityIndexRepository;
import in.foresthut.indices.speciesdiversity.validator.RequestValidator;
import in.foresthut.infra.MessageHandler;
import in.foresthut.infra.occurrence.repository.OccurrenceRepository;
import in.foresthut.restorationsite.entity.RestorationSiteDao;
import in.foresthut.restorationsite.repository.RestorationSiteRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public record SpeciesDiversityHandler(SpeciesDiversityRequest request,
                                      OccurrenceRepository occurrenceRepository,
                                      RestorationSiteRepository restorationSiteRepository,
                                      SpeciesDiversityIndexRepository speciesDiversityIndexRepository,
                                      StreamObserver<SpeciesDiversityResponse> responseObserver)
        implements MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(SpeciesDiversityHandler.class);

    @Override
    public void handle() {
        // Validate createSiteRequest
        var status = RequestValidator.validateSpeciesDiversityRequest(request);

        if (status.isPresent()) {
            logger.error(
                    "Validation failed: {}", status.get()
                                                   .getDescription());
            responseObserver.onError(status.get()
                                           .asRuntimeException());
            return;
        }

        // if already exists in species diversity collection, return
        SpeciesDiversityIndexDao existingSpeciesDiversity =
                speciesDiversityIndexRepository.get(request.getSiteId(), request.getForYear());
        if (existingSpeciesDiversity != null) {
            var response = SpeciesDiversityResponse.newBuilder()
                                                   .setSiteId(existingSpeciesDiversity.siteId())
                                                   .setForYear(existingSpeciesDiversity.forYear())
                                                   .setIndex(existingSpeciesDiversity.index())
                                                   .putAllSpeciesCount(existingSpeciesDiversity.speciesCount())
                                                   .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        var speciesDiversityIndexDao = calculateIndex(request);

        if (speciesDiversityIndexDao == null) {
            var errorResponse = Status.FAILED_PRECONDITION.withDescription("Site id not found.");
            logger.error(
                    "Failed to retrieve restoration site for site id '{}'",request.getSiteId());
            responseObserver.onError(errorResponse.asRuntimeException());
            return;
        }

        logger.info(
                "Species Diversity Index for site '{}' is {}", request.getSiteId(),
                speciesDiversityIndexDao.index());
        speciesDiversityIndexRepository.add(speciesDiversityIndexDao);

        SpeciesDiversityResponse response = SpeciesDiversityResponse.newBuilder()
                                                                    .setSiteId(speciesDiversityIndexDao.siteId())
                                                                    .setForYear(speciesDiversityIndexDao.forYear())
                                                                    .setIndex(speciesDiversityIndexDao.index())
                                                                    .putAllSpeciesCount(
                                                                            speciesDiversityIndexDao.speciesCount())
                                                                    .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public SpeciesDiversityIndexDao calculateIndex(SpeciesDiversityRequest request) {
        RestorationSiteDao restorationSiteDao = restorationSiteRepository.get(request.getSiteId());

        if (restorationSiteDao == null) {
            return null;
        }

        Map<String, Long> speciesCount =
                occurrenceRepository.speciesOccurrencesForPolygon(
                        restorationSiteDao.wktPolygon(), request().getForYear());


        long totalSpeciesOccurrences = speciesCount.values()
                                                   .stream()
                                                   .count();


        double speciesDiversityIndex = 0;

        for (var entryItem : speciesCount.entrySet()) {
            var pi = entryItem.getValue() / (double) totalSpeciesOccurrences;
            var lnPi = Math.log(pi);
            speciesDiversityIndex += (pi * lnPi);
        }

        speciesDiversityIndex *= -1;

        SpeciesDiversityIndexDao speciesDiversityIndexDao =
                new SpeciesDiversityIndexDao(
                        request.getSiteId(), request.getForYear(), speciesDiversityIndex,
                                             speciesCount);

        return speciesDiversityIndexDao;
    }


}
