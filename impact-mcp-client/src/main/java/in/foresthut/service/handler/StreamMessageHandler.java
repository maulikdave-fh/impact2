package in.foresthut.service.handler;

import in.foresthut.impact.models.service.Request;
import in.foresthut.impact.models.service.Response;
import in.foresthut.restorationsite.handler.CreateSiteHandler;
import in.foresthut.restorationsite.handler.DeleteSiteHandler;
import in.foresthut.restorationsite.repository.RestorationSiteRepository;
import in.foresthut.user.repository.UserRepository;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record StreamMessageHandler(UserRepository userRepository,
                                   RestorationSiteRepository restorationSiteRepository,
                                   StreamObserver<Response> responseObserver) implements StreamObserver<Request> {
    private static final Logger logger = LoggerFactory.getLogger(StreamMessageHandler.class);

    @Override
    public void onNext(Request request) {
        switch (request.getObjectCase()) {
            case CREATE_SITE_REQUEST ->
                    new CreateSiteHandler(
                            request.getCreateSiteRequest(), userRepository, restorationSiteRepository,
                            responseObserver).handle();

            case DELETE_SITE_REQUEST -> new DeleteSiteHandler(
                    request.getDeleteSiteRequest(), restorationSiteRepository,
                                                              responseObserver).handle();


        }
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error("Error: ", throwable);
    }

    @Override
    public void onCompleted() {
        responseObserver.onCompleted();
    }

}
