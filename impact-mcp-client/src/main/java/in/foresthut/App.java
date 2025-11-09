package in.foresthut;

import com.nirvighna.grpc.commons.server.GrpcServer;
import in.foresthut.restorationsite.repository.RestorationSiteRepository;
import in.foresthut.restorationsite.repository.RestorationSiteRepositoryImpl;
import in.foresthut.service.ImpactService;
import in.foresthut.user.repository.UserRepository;
import in.foresthut.user.repository.UserRepositoryImpl;
import in.foresthut.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        final UserRepository userRepository = new UserRepositoryImpl();
        final RestorationSiteRepository restorationSiteRepository = new RestorationSiteRepositoryImpl();

        final GrpcServer server = GrpcServer.create(
                List.of(),
                List.of(new UserService(userRepository), new ImpactService(userRepository, restorationSiteRepository)));
        server.start();
        server.await();
    }
}
