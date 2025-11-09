package in.foresthut.indices.speciesdensity.service;

import in.foresthut.impact.models.indices.speciesdensity.*;
import in.foresthut.indices.speciesdensity.repository.SpeciesDensityIndexRepository;
import in.foresthut.indices.speciesdensity.service.handler.SpeciesDensityRequestHandler;
import in.foresthut.indices.speciesdensity.service.handler.SpeciesListHandler;
import in.foresthut.infra.occurrence.repository.OccurrenceRepository;
import in.foresthut.restorationsite.repository.RestorationSiteRepository;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeciesDensityIndexService extends SpeciesDensityIndexGrpc.SpeciesDensityIndexImplBase {
    private final static Logger logger = LoggerFactory.getLogger(SpeciesDensityIndexService.class);

    private final OccurrenceRepository occurrenceRepository;
    private final RestorationSiteRepository restorationSiteRepository;
    private final SpeciesDensityIndexRepository speciesDensityIndexRepository;

    public SpeciesDensityIndexService(OccurrenceRepository occurrenceRepository, RestorationSiteRepository restorationSiteRepository, SpeciesDensityIndexRepository speciesDensityIndexRepository) {
        this.occurrenceRepository = occurrenceRepository;
        this.restorationSiteRepository = restorationSiteRepository;
        this.speciesDensityIndexRepository = speciesDensityIndexRepository;
    }

    @Override
    public void get(SpeciesDensityRequest request, StreamObserver<SpeciesDensityResponse> responseObserver) {
        new SpeciesDensityRequestHandler(request, occurrenceRepository, restorationSiteRepository, speciesDensityIndexRepository, responseObserver).handle();
    }

    @Override
    public void getSpeciesList(SpeciesListRequest request, StreamObserver<SpeciesListResponse> responseObserver) {
       new SpeciesListHandler(request, occurrenceRepository, responseObserver).handle();
    }
}
