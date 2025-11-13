package in.foresthut;

import com.nirvighna.grpc.commons.server.GrpcServer;
import com.nirvighna.mongodb.commons.DatabaseConfig;
import com.nirvighna.redis.client.map.KeyValuePairHolder;
import in.foresthut.aoi.AoIProvider;
import in.foresthut.aoi.S2GeometryBasedAoIProvider;
import in.foresthut.indices.endemicity.repository.EndemicityIndexRepository;
import in.foresthut.indices.endemicity.repository.EndemicityIndexRepositoryImpl;
import in.foresthut.indices.endemicity.service.EndemicityIndexService;
import in.foresthut.indices.speciesdensity.repository.SpeciesDensityIndexRepository;
import in.foresthut.indices.speciesdensity.repository.SpeciesDensityIndexRepositoryImpl;
import in.foresthut.indices.speciesdensity.service.SpeciesDensityIndexService;
import in.foresthut.indices.speciesdiversity.repository.SpeciesDiversityIndexRepository;
import in.foresthut.indices.speciesdiversity.repository.SpeciesDiversityIndexRepositoryImpl;
import in.foresthut.indices.speciesdiversity.service.SpeciesDiversityIndexService;
import in.foresthut.indices.speciesspread.repository.SpeciesSpreadIndexRepository;
import in.foresthut.indices.speciesspread.repository.SpeciesSpreadIndexRepositoryImpl;
import in.foresthut.indices.speciesspread.service.SpeciesSpreadIndexService;
import in.foresthut.infra.endemicity.repository.EndemicityRepository;
import in.foresthut.infra.endemicity.repository.EndemicityRepositoryImpl;
import in.foresthut.infra.gbif.GBIFClient;
import in.foresthut.infra.occurrence.repository.OccurrenceRepository;
import in.foresthut.infra.occurrence.repository.OccurrenceRepositoryImpl;
import in.foresthut.infra.s2token.repository.S2CellTokenPlusRepo;
import in.foresthut.infra.s2token.repository.S2CellTokenPlusRepoImpl;
import in.foresthut.infra.security.AuthenticationInterceptor;
import in.foresthut.restorationsite.repository.RestorationSiteRepository;
import in.foresthut.restorationsite.repository.RestorationSiteRepositoryImpl;
import in.foresthut.restorationsite.service.RestorationSiteService;
import in.foresthut.user.repository.UserRepository;
import in.foresthut.user.repository.UserRepositoryImpl;
import in.foresthut.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        // Dependencies
        var databaseConfig = DatabaseConfig.getInstance();
        final UserRepository userRepository = new UserRepositoryImpl(databaseConfig);
        final OccurrenceRepository occurrenceRepository = new OccurrenceRepositoryImpl(databaseConfig);
        final SpeciesDensityIndexRepository speciesDensityIndexRepository =
                new SpeciesDensityIndexRepositoryImpl(databaseConfig);
        final SpeciesDiversityIndexRepository speciesDiversityIndexRepository =
                new SpeciesDiversityIndexRepositoryImpl(databaseConfig);
        final SpeciesSpreadIndexRepository speciesSpreadIndexRepository =
                new SpeciesSpreadIndexRepositoryImpl(databaseConfig);
        final EndemicityIndexRepository endemicityIndexRepository = new EndemicityIndexRepositoryImpl(databaseConfig);
        final EndemicityRepository endemicityRepository = new EndemicityRepositoryImpl(databaseConfig);
        final GBIFClient gbifClient = GBIFClient.getInstance();
        final RestorationSiteRepository restorationSiteRepository = new RestorationSiteRepositoryImpl(databaseConfig);
        final S2CellTokenPlusRepo cellTokenPlusRepo = new S2CellTokenPlusRepoImpl(databaseConfig);
        final AoIProvider aoIProvider = new S2GeometryBasedAoIProvider(cellTokenPlusRepo);
        final KeyValuePairHolder sessionRegistry = KeyValuePairHolder.getInstance();

        // Construct Grpc server
        final GrpcServer server = GrpcServer.create(
                List.of(new AuthenticationInterceptor()), List.of(
                        new UserService(userRepository, sessionRegistry),
                        new RestorationSiteService(userRepository, restorationSiteRepository, aoIProvider),
                        new SpeciesDensityIndexService(
                                occurrenceRepository, restorationSiteRepository,
                                speciesDensityIndexRepository),
                        new SpeciesDiversityIndexService(
                                occurrenceRepository, restorationSiteRepository,
                                speciesDiversityIndexRepository),
                        new SpeciesSpreadIndexService(
                                occurrenceRepository, restorationSiteRepository,
                                speciesSpreadIndexRepository),
                        new EndemicityIndexService(
                                occurrenceRepository, restorationSiteRepository, endemicityIndexRepository,
                                endemicityRepository, gbifClient)));

        server.start();
        server.await();
    }
}
