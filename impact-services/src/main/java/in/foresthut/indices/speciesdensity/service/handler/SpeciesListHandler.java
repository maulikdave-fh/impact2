package in.foresthut.indices.speciesdensity.service.handler;

import in.foresthut.impact.models.indices.speciesdensity.SpeciesListRequest;
import in.foresthut.impact.models.indices.speciesdensity.SpeciesListResponse;
import in.foresthut.indices.speciesdensity.validator.RequestValidator;
import in.foresthut.infra.MessageHandler;
import in.foresthut.infra.occurrence.repository.OccurrenceRepository;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public record SpeciesListHandler(SpeciesListRequest request,
                                 OccurrenceRepository occurrenceRepository,
                                 StreamObserver<SpeciesListResponse> responseObserver) implements MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(SpeciesListHandler.class);

    @Override
    public void handle() {
        // Validate createSiteRequest
        var status = RequestValidator.validateSpeciesListRequest(request);

        if (status.isPresent()) {
            logger.error(
                    "Validation failed: {}", status.get()
                                                   .getDescription());
            responseObserver.onError(status.get()
                                           .asRuntimeException());
            return;
        }

        List<String> speciesList = occurrenceRepository.speciesForS2Token(request().getCellToken(),
                                                                          request().getForYear());
        SpeciesListResponse response = SpeciesListResponse.newBuilder().addAllSpecies(speciesList).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
