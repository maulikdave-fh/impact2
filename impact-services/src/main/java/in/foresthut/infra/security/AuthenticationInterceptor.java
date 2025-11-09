package in.foresthut.infra.security;

import com.nirvighna.redis.client.map.KeyValuePairHolder;
import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class AuthenticationInterceptor implements ServerInterceptor {
    public static final Context.Key<String> USER_ID_KEY = Context.key("user-id");
    public static final Context.Key<String> SESSION_TOKEN_KEY = Context.key("session-token");
    public static final Context.Key<String> API_KEY = Context.key("api-key");

    private static final Metadata.Key<String> USER_ID_METATDATA_KEY =
            Metadata.Key.of("user-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> SESSION_TOKEN_METATDATA_KEY =
            Metadata.Key.of("session-token", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> API_KEY_METADATA_KEY =
            Metadata.Key.of("api-key", Metadata.ASCII_STRING_MARSHALLER);

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationInterceptor.class);

    private static final List<String> WEB_CHANNEL_WHITE_LIST_METHODS = List.of("user.UserService/SignIn");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {

        var userId = headers.get(USER_ID_METATDATA_KEY);
        var sessionToken = headers.get(SESSION_TOKEN_METATDATA_KEY);
        var apiKey = headers.get(API_KEY_METADATA_KEY);

        // Get the full method name, e.g., "your.package.YourService/YourMethod"
        String fullMethodName = serverCall.getMethodDescriptor()
                                          .getFullMethodName();
        if (userId != null && sessionToken != null && !userId.isBlank() && !sessionToken.isBlank()) {
            if (!WEB_CHANNEL_WHITE_LIST_METHODS.contains(fullMethodName)) {
                // Check if session token is valid
                final KeyValuePairHolder sessionRegistry = KeyValuePairHolder.getInstance();
                if (sessionRegistry.isValid(userId, sessionToken)) {
                    //logger.info("deviceId: {} is an active device.", deviceId);
                    var ctx = toContext(userId, sessionToken);

                    if (Objects.nonNull(ctx)) {
                        // Forward the call to the next interceptor / service
                        logger.info("User {} is authenticated.", userId);
                        return Contexts.interceptCall(ctx, serverCall, headers, next);
                    }
                    logger.error("User {} is not authenticated.", userId);
                    return close(serverCall, headers, Status.UNAUTHENTICATED.withDescription("Invalid Context"));
                } else {
                    logger.error("User {} is not authenticated.", userId);
                    return close(serverCall, headers, Status.UNAUTHENTICATED.withDescription("Authentication Failed."));
                }
            } else {
                //logger.error("No device id found in the metadata.");
                logger.error("Missing metadata");
                return close(serverCall, headers, Status.UNAUTHENTICATED.withDescription("Authentication Failed."));
            }
        } else if (apiKey != null && !apiKey.isBlank()) {
            return close(serverCall, headers, Status.NOT_FOUND.withDescription("Not implemented"));
        } else if (WEB_CHANNEL_WHITE_LIST_METHODS.contains(fullMethodName)) {
            return next.startCall(serverCall, headers);
        }else {
            return close(serverCall, headers, Status.UNAUTHENTICATED.withDescription("Missing api-key or " +
                                                                                     "session-token"));
        }
    }

    // Put use id in the current context
    private Context toContext(String... values) {
        return Context.current()
                      .withValues(USER_ID_KEY, values[0], SESSION_TOKEN_KEY, values[1]);
    }

    private <ReqT, RespT> ServerCall.Listener<ReqT> close(ServerCall<ReqT, RespT> serverCall, Metadata metadata,
                                                          Status status) {
        serverCall.close(status, metadata);
        return new ServerCall.Listener<ReqT>() {
        };
    }
}
