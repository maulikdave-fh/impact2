package in.foresthut.indices.speciesspread.validator;

import in.foresthut.impact.models.indices.speciesspread.SpeciesSpreadRequest;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Optional;

public class RequestValidator {
    private static final Logger logger = LoggerFactory.getLogger(RequestValidator.class);

    public static Optional<Status> validateSpeciesSpreadIndexRequest(SpeciesSpreadRequest request) {
        if (request == null)
            return Optional.of(Status.INVALID_ARGUMENT.withDescription("Species spread request cannot be null."));

        if (request.getSiteId() == null || request.getSiteId()
                                                  .isBlank())
            return Optional.of(Status.INVALID_ARGUMENT.withDescription("Site id cannot be null or blank."));

        int year = request.getForYear();
        if (year < 1500 || year > LocalDate.now()
                                           .getYear()) return Optional.of(Status.INVALID_ARGUMENT.withDescription(
                "Invalid year " + year + ". Should be between 1500 and current year."));

        return Optional.empty();
    }
}
