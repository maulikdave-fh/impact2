package in.foresthut.user.validator;

import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class RequestValidator {
    private static final Logger logger = LoggerFactory.getLogger(RequestValidator.class);

    public static Optional<Status> validateSignInRequest(in.foresthut.impact.models.user.SignInRequest request) {
        if (request.getUser() == null)
            return Optional.of(Status.INVALID_ARGUMENT.withDescription("User cannot be null."));

        if (request.getUser()
                   .getId() == null || request.getUser()
                                              .getId()
                                              .isBlank())
            return Optional.of(Status.INVALID_ARGUMENT.withDescription("User id cannot be null or blank."));

        if (request.getUser()
                   .getName() == null || request.getUser()
                                                .getName()
                                                .isBlank())
            return Optional.of(Status.INVALID_ARGUMENT.withDescription("User name cannot be null or blank."));

        if (request.getUser()
                   .getEmail() == null || request.getUser()
                                                 .getEmail()
                                                 .isBlank())
            return Optional.of(Status.INVALID_ARGUMENT.withDescription("User email cannot be null or blank."));

        if (request.getUser()
                   .getIdToken() == null || request.getUser()
                                                   .getIdToken()
                                                   .isBlank())
            return Optional.of(Status.INVALID_ARGUMENT.withDescription("Id token cannot be null or blank."));

        return Optional.empty();
    }
}
