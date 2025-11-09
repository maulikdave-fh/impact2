package in.foresthut.restorationsite.service;

import com.nirvighna.mongodb.commons.DatabaseConfig;
import in.foresthut.aoi.AoIProvider;
import in.foresthut.impact.models.site.*;
import in.foresthut.indices.endemicity.repository.EndemicityIndexRepository;
import in.foresthut.indices.endemicity.repository.EndemicityIndexRepositoryImpl;
import in.foresthut.indices.speciesdensity.repository.SpeciesDensityIndexRepository;
import in.foresthut.indices.speciesdensity.repository.SpeciesDensityIndexRepositoryImpl;
import in.foresthut.indices.speciesdiversity.repository.SpeciesDiversityIndexRepository;
import in.foresthut.indices.speciesdiversity.repository.SpeciesDiversityIndexRepositoryImpl;
import in.foresthut.indices.speciesspread.repository.SpeciesSpreadIndexRepository;
import in.foresthut.indices.speciesspread.repository.SpeciesSpreadIndexRepositoryImpl;
import in.foresthut.restorationsite.repository.RestorationSiteRepository;
import in.foresthut.restorationsite.service.handler.*;
import in.foresthut.user.repository.UserRepository;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestorationSiteService extends RestorationSiteServiceGrpc.RestorationSiteServiceImplBase {
    private final static Logger logger = LoggerFactory.getLogger(RestorationSiteService.class);

    private final UserRepository userRepository;
    private final RestorationSiteRepository restorationSiteRepository;
    private final AoIProvider aoIProvider;

    public RestorationSiteService(UserRepository userRepository, RestorationSiteRepository restorationSiteRepository,
                                  AoIProvider aoIProvider) {
        this.userRepository = userRepository;
        this.restorationSiteRepository = restorationSiteRepository;
        this.aoIProvider = aoIProvider;
    }

    @Override
    public void createSite(CreateSiteRequest request, StreamObserver<CreateSiteResponse> responseObserver) {
        new CreateSiteHandler(
                request, userRepository, restorationSiteRepository, responseObserver, aoIProvider).handle();
    }

    @Override
    public void uploadSiteMap(SiteMapFileUploadRequest request, StreamObserver<CreateSiteResponse> responseObserver) {
        super.uploadSiteMap(request, responseObserver);
    }

    @Override
    public void getSite(GetSiteRequest request, StreamObserver<GetSiteResponse> responseObserver) {
        new GetSiteHandler(request, restorationSiteRepository, responseObserver).handle();
    }

    @Override
    public void getSites(GetSitesRequest request, StreamObserver<GetSitesResponse> responseObserver) {
        new GetSitesHandler(request, restorationSiteRepository, responseObserver).handle();
    }

    @Override
    public void deleteSite(DeleteSiteRequest request, StreamObserver<DeleteSiteResponse> responseObserver) {
        DatabaseConfig databaseConfig = DatabaseConfig.getInstance();
        SpeciesDensityIndexRepository speciesDensityIndexRepository =
                new SpeciesDensityIndexRepositoryImpl(databaseConfig);
        SpeciesDiversityIndexRepository speciesDiversityIndexRepository =
                new SpeciesDiversityIndexRepositoryImpl(databaseConfig);
        SpeciesSpreadIndexRepository speciesSpreadIndexRepository =
                new SpeciesSpreadIndexRepositoryImpl(databaseConfig);
        EndemicityIndexRepository endemicityIndexRepository =
                new EndemicityIndexRepositoryImpl(databaseConfig);

        new DeleteSiteHandler(
                request, restorationSiteRepository, speciesDensityIndexRepository, speciesDiversityIndexRepository,
                speciesSpreadIndexRepository, endemicityIndexRepository, responseObserver).handle();
    }

    @Override
    public void updateSite(UpdateSiteRequest request, StreamObserver<UpdateSiteResponse> responseObserver) {
        // TODO: Delete saved indices as well
        super.updateSite(request, responseObserver);
    }

    @Override
    public void getAoI(GetAoIRequest request, StreamObserver<GetAoIResponse> responseObserver) {
        new GetAoIHandler(request, restorationSiteRepository, responseObserver).handle();
    }

    @Override
    public void getAoIUnion(GetUnionAoIRequest request, StreamObserver<GetUnionAoIResponse> responseObserver) {
        new GetAoIUnionHandler(request, restorationSiteRepository, responseObserver).handle();
    }

    @Override
    public void createSiteStreaming(CreateSiteRequest request, StreamObserver<SiteResponseV2> responseObserver) {
        new CreateSiteStreamingResponseHandler(
                request, userRepository, restorationSiteRepository, responseObserver,
                                               aoIProvider).handle();
    }
}
