package in.foresthut.restorationsite.validator;

import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class RequestValidator {
    private static final Logger logger = LoggerFactory.getLogger(RequestValidator.class);

    public static Optional<Status> validateCreateSiteRequest(in.foresthut.impact.models.site.CreateSiteRequest request) {
        if (request.getUserId() == null || request.getUserId().isBlank())
            return Optional.of(Status.INVALID_ARGUMENT.withDescription("User id cannot be null or blank."));

        if (request.getSiteName() == null || request.getSiteName().isBlank())
            return Optional.of(Status.INVALID_ARGUMENT.withDescription("Site name cannot be null or blank."));

        if (request.getWktPolygon() == null || request.getWktPolygon().isBlank())
            return Optional.of(Status.INVALID_ARGUMENT.withDescription("Wkt polygon cannot be null or blank."));

        return Optional.empty();
    }
}
