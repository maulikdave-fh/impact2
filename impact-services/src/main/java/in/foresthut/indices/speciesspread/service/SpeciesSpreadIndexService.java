package in.foresthut.indices.speciesspread.service;

import in.foresthut.impact.models.indices.speciesspread.SpeciesSpreadIndexGrpc;
import in.foresthut.impact.models.indices.speciesspread.SpeciesSpreadRequest;
import in.foresthut.impact.models.indices.speciesspread.SpeciesSpreadResponse;
import in.foresthut.indices.speciesdiversity.repository.SpeciesDiversityIndexRepository;
import in.foresthut.indices.speciesdiversity.service.SpeciesDiversityIndexService;
import in.foresthut.indices.speciesspread.repository.SpeciesSpreadIndexRepository;
import in.foresthut.indices.speciesspread.service.handler.SpeciesSpreadHandler;
import in.foresthut.infra.occurrence.repository.OccurrenceRepository;
import in.foresthut.restorationsite.repository.RestorationSiteRepository;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeciesSpreadIndexService extends SpeciesSpreadIndexGrpc.SpeciesSpreadIndexImplBase {
    private final static Logger logger = LoggerFactory.getLogger(SpeciesSpreadIndexService.class);

    private final OccurrenceRepository occurrenceRepository;
    private final RestorationSiteRepository restorationSiteRepository;
    private final SpeciesSpreadIndexRepository speciesSpreadIndexRepository;

    public SpeciesSpreadIndexService(OccurrenceRepository occurrenceRepository,
                                        RestorationSiteRepository restorationSiteRepository,
                                        SpeciesSpreadIndexRepository speciesSpreadIndexRepository) {
        this.occurrenceRepository = occurrenceRepository;
        this.restorationSiteRepository = restorationSiteRepository;
        this.speciesSpreadIndexRepository = speciesSpreadIndexRepository;
    }

    @Override
    public void get(SpeciesSpreadRequest request, StreamObserver<SpeciesSpreadResponse> responseObserver) {
        new SpeciesSpreadHandler(request, occurrenceRepository, restorationSiteRepository,
                                        speciesSpreadIndexRepository, responseObserver).handle();
    }
}
