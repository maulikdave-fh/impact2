package in.foresthut.user.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.nirvighna.www.commons.config.AppConfig;
import in.foresthut.impact.models.user.GetUserRequest;
import in.foresthut.impact.models.user.User;
import in.foresthut.user.entity.UserDao;
import in.foresthut.user.mapper.UserMapper;
import in.foresthut.user.repository.UserRepository;
import in.foresthut.user.validator.RequestValidator;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.UUID;

public class UserService extends in.foresthut.impact.models.user.UserServiceGrpc.UserServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final String WEB_CLIENT_ID = AppConfig.getInstance()
                                                         .get("google.web.client.id");
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @Override
    public void signIn(in.foresthut.impact.models.user.SignInRequest request,
                       StreamObserver<in.foresthut.impact.models.user.SignInResponse> responseObserver) {
        // Validate request
        var status = RequestValidator.validateSignInRequest(request);

        if (status.isPresent()) {
            logger.error(
                    "Validation failed: {}", status.get()
                                                   .getDescription());
            responseObserver.onError(status.get()
                                           .asRuntimeException());
            return;
        }

        // Verify id_token - if verified generate session token
        if (!isValidIdToken(request.getUser()
                                   .getIdToken())) {
            var errorResponse = Status.UNAUTHENTICATED.withDescription("Authentication Failed.");
            logger.error(
                    "Authentication Failed for : {}", request.getUser()
                                                             .getId());
            responseObserver.onError(errorResponse.asRuntimeException());
            return;
        }

        String sessionToken = UUID.randomUUID()
                                  .toString();

        // If user does not exist in database
        if (userRepository.get(request.getUser()
                                      .getId()) == null) {
            var userDao = UserMapper.toUserDao(request.getUser()
                                                      .getIdToken());
            if (userDao != null) {
                userRepository.add(UserMapper.toUserDao(request.getUser()
                                                               .getIdToken()));
            } else {
                var errorResponse = Status.UNAUTHENTICATED.withDescription("Authentication Failed.");
                logger.error(
                        "Authentication Failed for : {}", request.getUser()
                                                                 .getId());
                responseObserver.onError(errorResponse.asRuntimeException());
                return;
            }
        }

        responseObserver.onNext(in.foresthut.impact.models.user.SignInResponse.newBuilder()
                                                                              .setSessionToken(sessionToken)
                                                                              .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getUser(GetUserRequest request, StreamObserver<User> responseObserver) {
        UserDao userDao = userRepository.get(request.getUserId());
        responseObserver.onNext(UserMapper.toUser(userDao));
        responseObserver.onCompleted();
    }

    private boolean isValidIdToken(String idTokenString) {
        HttpTransport transport = new NetHttpTransport();
        JsonFactory jsonFactory = new GsonFactory();
//        JsonFactory jsonFactory =
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                // Specify the WEB_CLIENT_ID of the app that accesses the backend:
                .setAudience(Collections.singletonList(WEB_CLIENT_ID))
                // Or, if multiple clients access the backend:
                //.setAudience(Arrays.asList(WEB_CLIENT_ID_1, WEB_CLIENT_ID_2, WEB_CLIENT_ID_3))
                .build();

        // (Receive idTokenString by HTTPS POST)

        GoogleIdToken idToken = null;
        try {
            idToken = verifier.verify(idTokenString);
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }

        //            GoogleIdToken.Payload payload = idToken.getPayload();
        // Print user identifier. This ID is unique to each Google Account, making it suitable for
        // use as a primary key during account lookup. Email is not a good choice because it can be
        // changed by the user.
        //            String userId = payload.getSubject();
        //
        //            // Get profile information from payload
        //            String email = payload.getEmail();
        //            boolean emailVerified = payload.getEmailVerified();
        //            String name = (String) payload.get("name");
        //            String pictureUrl = (String) payload.get("picture");
        //            String locale = (String) payload.get("locale");
        //            String familyName = (String) payload.get("family_name");
        //            String givenName = (String) payload.get("given_name");
        return idToken != null;
    }
}
