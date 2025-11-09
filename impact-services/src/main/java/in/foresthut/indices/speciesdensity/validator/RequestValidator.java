package in.foresthut.indices.speciesdensity.validator;

import in.foresthut.impact.models.indices.speciesdensity.SpeciesDensityRequest;
import in.foresthut.impact.models.indices.speciesdensity.SpeciesListRequest;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Optional;

public class RequestValidator {
    private static final Logger logger = LoggerFactory.getLogger(RequestValidator.class);

    public static Optional<Status> validateSpeciesDensityIndexRequest(SpeciesDensityRequest speciesDensityRequest) {
        if (speciesDensityRequest == null)
            return Optional.of(Status.INVALID_ARGUMENT.withDescription("Species density cannot be null."));

        if (speciesDensityRequest.getSiteId() == null || speciesDensityRequest.getSiteId()
                                                                              .isBlank())
            return Optional.of(Status.INVALID_ARGUMENT.withDescription("Site id cannot be null or blank."));

        int year = speciesDensityRequest.getForYear();
        if (year < 1500 || year > LocalDate.now()
                                           .getYear()) return Optional.of(Status.INVALID_ARGUMENT.withDescription(
                "Invalid year " + year + ". Should be between 1500 and current year."));

        return Optional.empty();
    }

    public static Optional<Status> validateSpeciesListRequest(SpeciesListRequest request) {
        if (request == null)
            return Optional.of(Status.INVALID_ARGUMENT.withDescription("Species list request cannot be null."));

        if (request.getCellToken() == null || request
                .getCellToken()
                                                                              .isBlank())
            return Optional.of(Status.INVALID_ARGUMENT.withDescription("Cell Token cannot be null or blank."));

        int year = request.getForYear();
        if (year < 1500 || year > LocalDate.now()
                                           .getYear()) return Optional.of(Status.INVALID_ARGUMENT.withDescription(
                "Invalid year " + year + ". Should be between 1500 and current year."));

        return Optional.empty();

    }
}
