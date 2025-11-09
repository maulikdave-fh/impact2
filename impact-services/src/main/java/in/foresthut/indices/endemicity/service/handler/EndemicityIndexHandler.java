package in.foresthut.indices.endemicity.service.handler;

import in.foresthut.commons.geometry.bioregion.Bioregion;
import in.foresthut.commons.geometry.bioregion.WesternGhats;
import in.foresthut.impact.models.indices.endemicity.EndemicityRequest;
import in.foresthut.impact.models.indices.endemicity.EndemicityResponse;
import in.foresthut.indices.endemicity.entity.EndemicityIndexDao;
import in.foresthut.indices.endemicity.repository.EndemicityIndexRepository;
import in.foresthut.indices.endemicity.validator.RequestValidator;
import in.foresthut.infra.MessageHandler;
import in.foresthut.infra.endemicity.entity.Endemicity;
import in.foresthut.infra.endemicity.repository.EndemicityRepository;
import in.foresthut.infra.gbif.GBIFClient;
import in.foresthut.infra.occurrence.repository.OccurrenceRepository;
import in.foresthut.restorationsite.repository.RestorationSiteRepository;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record EndemicityIndexHandler(EndemicityRequest request,
                                     RestorationSiteRepository restorationSiteRepository,
                                     OccurrenceRepository occurrenceRepository,
                                     EndemicityIndexRepository endemicityIndexRepository,
                                     EndemicityRepository endemicityRepository,
                                     GBIFClient gbifClient,
                                     StreamObserver<EndemicityResponse> responseObserver) implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(EndemicityIndexHandler.class);

    @Override
    public void handle() {
        // Validate request
        var status = RequestValidator.validateEndemicityIndexRequest(request);

        if (status.isPresent()) {
            logger.error(
                    "Validation failed: {}", status.get()
                                                   .getDescription());
            responseObserver.onError(status.get()
                                           .asRuntimeException());
            return;
        }

        // if already exists in species density collection, return
        EndemicityIndexDao existingEndemicityIndex = endemicityIndexRepository.get(request.getSiteId());
        if (existingEndemicityIndex != null) {
            var response = EndemicityResponse.newBuilder()
                                             .setSiteId(existingEndemicityIndex.siteId())
                                             .setIndex(existingEndemicityIndex.index())
                                             .putAllSpeciesLevelEndemicity(
                                                     existingEndemicityIndex.SpeciesWiseEndemicity())
                                             .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        // Get restoration site
        var site = restorationSiteRepository.get(request.getSiteId());

        // Get all restoration site species
        List<String> siteSpecies = occurrenceRepository.getDistinctSpecies(site.wktPolygon());

        // Calculate endemicity for each species and store it
        double sumOfEndemicity = 0;
        Map<String, Double> speciesWiseEndemicity = new HashMap<>();
        // FIXME: remove bioregion hardcoding
        Bioregion bioregion = WesternGhats.getInstance();
        for (var species : siteSpecies) {
            EndemicityIndexCalculator endemicityIndexCalculator =
                    new EndemicityIndexCalculator(
                            bioregion, species, occurrenceRepository, endemicityRepository, gbifClient);

            var endemicityValueObj = endemicityIndexCalculator.calculate();
            endemicityRepository.add(new Endemicity(
                    species, bioregion.code(), endemicityValueObj.bioregionOccurrenceCount(),
                    endemicityValueObj.worldOccurrenceCount(), endemicityValueObj.index(),
                    LocalDateTime.now(ZoneId.of("Asia/Kolkata"))));
            sumOfEndemicity += endemicityValueObj.index();
            speciesWiseEndemicity.put(species, endemicityValueObj.index());
        }

        // Calculate site wide endemicity & store in db
        double index = sumOfEndemicity / siteSpecies.size();

        endemicityIndexRepository.add(new EndemicityIndexDao(request.getSiteId(), index, speciesWiseEndemicity));

        // Prepare response
        EndemicityResponse response = EndemicityResponse.newBuilder()
                                                        .setSiteId(request.getSiteId())
                                                        .setIndex(index)
                                                        .putAllSpeciesLevelEndemicity(speciesWiseEndemicity)
                                                        .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
