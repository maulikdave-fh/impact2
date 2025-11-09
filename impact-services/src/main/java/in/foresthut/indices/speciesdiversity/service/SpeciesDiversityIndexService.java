package in.foresthut.indices.speciesdiversity.service;

import in.foresthut.impact.models.indices.speciesdiversity.SpeciesDiversityIndexGrpc;
import in.foresthut.impact.models.indices.speciesdiversity.SpeciesDiversityRequest;
import in.foresthut.impact.models.indices.speciesdiversity.SpeciesDiversityResponse;
import in.foresthut.indices.speciesdiversity.repository.SpeciesDiversityIndexRepository;
import in.foresthut.indices.speciesdiversity.service.handler.SpeciesDiversityHandler;
import in.foresthut.infra.occurrence.repository.OccurrenceRepository;
import in.foresthut.restorationsite.repository.RestorationSiteRepository;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeciesDiversityIndexService extends SpeciesDiversityIndexGrpc.SpeciesDiversityIndexImplBase {
    private final static Logger logger = LoggerFactory.getLogger(SpeciesDiversityIndexService.class);

    private final OccurrenceRepository occurrenceRepository;
    private final RestorationSiteRepository restorationSiteRepository;
    private final SpeciesDiversityIndexRepository speciesDiversityIndexRepository;

    public SpeciesDiversityIndexService(OccurrenceRepository occurrenceRepository,
                                        RestorationSiteRepository restorationSiteRepository,
                                        SpeciesDiversityIndexRepository speciesDiversityIndexRepository) {
        this.occurrenceRepository = occurrenceRepository;
        this.restorationSiteRepository = restorationSiteRepository;
        this.speciesDiversityIndexRepository = speciesDiversityIndexRepository;
    }

    @Override
    public void get(SpeciesDiversityRequest request, StreamObserver<SpeciesDiversityResponse> responseObserver) {
        new SpeciesDiversityHandler(
                request, occurrenceRepository, restorationSiteRepository, speciesDiversityIndexRepository,
                responseObserver).handle();
    }
}
