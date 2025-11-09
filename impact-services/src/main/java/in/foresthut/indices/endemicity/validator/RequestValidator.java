package in.foresthut.indices.endemicity.validator;

import in.foresthut.impact.models.indices.endemicity.EndemicityRequest;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Optional;

public class RequestValidator {
    private static final Logger logger = LoggerFactory.getLogger(RequestValidator.class);

    public static Optional<Status> validateEndemicityIndexRequest(EndemicityRequest endemicityRequest) {
        if (endemicityRequest == null)
            return Optional.of(Status.INVALID_ARGUMENT.withDescription("Endemicity index request cannot be null."));

        if (endemicityRequest.getSiteId() == null || endemicityRequest.getSiteId()
                                                                      .isBlank())
            return Optional.of(Status.INVALID_ARGUMENT.withDescription("Site id cannot be null or blank."));

        return Optional.empty();
    }
}
