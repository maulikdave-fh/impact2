package in.foresthut.user.validator;

import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class RequestValidator {
    private static final Logger logger = LoggerFactory.getLogger(RequestValidator.class);

    public static Optional<Status> validateSignInRequest(in.foresthut.impact.models.user.SignInRequest request) {
//        if (createSiteRequest.getUser() == null)
//            return Optional.of(Status.INVALID_ARGUMENT.withDescription("User cannot be null."));

//        if (createSiteRequest.getUser()
//                   .getId() == null || createSiteRequest.getUser()
//                                              .getId()
//                                              .isBlank())
//            return Optional.of(Status.INVALID_ARGUMENT.withDescription("User id cannot be null or blank."));
//
//        if (createSiteRequest.getUser()
//                   .getName() == null || createSiteRequest.getUser()
//                                                .getName()
//                                                .isBlank())
//            return Optional.of(Status.INVALID_ARGUMENT.withDescription("User name cannot be null or blank."));
//
//        if (createSiteRequest.getUser()
//                   .getEmail() == null || createSiteRequest.getUser()
//                                                 .getEmail()
//                                                 .isBlank())
//            return Optional.of(Status.INVALID_ARGUMENT.withDescription("User email cannot be null or blank."));

        if (request.getIdToken() == null || request.getIdToken()
                                                   .isBlank())
            return Optional.of(Status.INVALID_ARGUMENT.withDescription("Id token cannot be null or blank."));

        return Optional.empty();
    }
}
