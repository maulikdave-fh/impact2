package in.foresthut.indices.endemicity.service;

import in.foresthut.impact.models.indices.endemicity.EndemicityIndexGrpc;
import in.foresthut.impact.models.indices.endemicity.EndemicityRequest;
import in.foresthut.impact.models.indices.endemicity.EndemicityResponse;
import in.foresthut.indices.endemicity.repository.EndemicityIndexRepository;
import in.foresthut.indices.endemicity.service.handler.EndemicityIndexHandler;
import in.foresthut.infra.endemicity.repository.EndemicityRepository;
import in.foresthut.infra.gbif.GBIFClient;
import in.foresthut.infra.occurrence.repository.OccurrenceRepository;
import in.foresthut.restorationsite.repository.RestorationSiteRepository;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndemicityIndexService extends EndemicityIndexGrpc.EndemicityIndexImplBase {
    private final static Logger logger = LoggerFactory.getLogger(EndemicityIndexService.class);

    private final OccurrenceRepository occurrenceRepository;
    private final RestorationSiteRepository restorationSiteRepository;
    private final EndemicityIndexRepository endemicityIndexRepository;
    private final EndemicityRepository endemicityRepository;
    private final GBIFClient gbifClient;

    public EndemicityIndexService(OccurrenceRepository occurrenceRepository,
                                  RestorationSiteRepository restorationSiteRepository,
                                  EndemicityIndexRepository endemicityIndexRepository,
                                  EndemicityRepository endemicityRepository, GBIFClient gbifClient) {
        this.occurrenceRepository = occurrenceRepository;
        this.restorationSiteRepository = restorationSiteRepository;
        this.endemicityIndexRepository = endemicityIndexRepository;
        this.endemicityRepository = endemicityRepository;
        this.gbifClient = gbifClient;
    }

    @Override
    public void get(EndemicityRequest request, StreamObserver<EndemicityResponse> responseObserver) {
        new EndemicityIndexHandler(
                request, restorationSiteRepository, occurrenceRepository, endemicityIndexRepository,
                endemicityRepository, gbifClient, responseObserver).handle();
    }
}
