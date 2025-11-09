package in.foresthut.service;

import in.foresthut.impact.models.service.ImpactServiceGrpc;
import in.foresthut.impact.models.service.Request;
import in.foresthut.impact.models.service.Response;
import in.foresthut.restorationsite.repository.RestorationSiteRepository;
import in.foresthut.service.handler.StreamMessageHandler;
import in.foresthut.user.repository.UserRepository;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImpactService extends ImpactServiceGrpc.ImpactServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(ImpactService.class);

    private final UserRepository userRepository;
    private final RestorationSiteRepository restorationSiteRepository;

    public ImpactService(UserRepository userRepository, RestorationSiteRepository restorationSiteRepository) {
        this.userRepository = userRepository;
        this.restorationSiteRepository = restorationSiteRepository;
    }

    @Override
    public StreamObserver<Request> streamMessage(StreamObserver<Response> responseObserver) {
        return new StreamMessageHandler(userRepository, restorationSiteRepository, responseObserver);
    }

//    @Override
//    public void getSite(SiteId request, StreamObserver<Site> responseObserver) {
//        RestorationSiteDao restorationSiteDao = restorationSiteRepository.get(request.getSiteId());
//        responseObserver.onNext(SiteMapper.toSite(restorationSiteDao));
//        responseObserver.onCompleted();
//    }
//
//    @Override
//    public void deleteSite(SiteId request, StreamObserver<Empty> responseObserver) {
//        restorationSiteRepository.delete(request.getSiteId());
//    }

}
