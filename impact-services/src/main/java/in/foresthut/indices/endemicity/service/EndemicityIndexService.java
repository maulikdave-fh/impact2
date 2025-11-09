package in.foresthut.indices.endemicity.service;

import in.foresthut.impact.models.indices.endemicity.EndemicityIndexGrpc;
import in.foresthut.impact.models.indices.endemicity.EndemicityRequest;
import in.foresthut.impact.models.indices.endemicity.EndemicityResponse;
import io.grpc.stub.StreamObserver;

public class EndemicityIndexService extends EndemicityIndexGrpc.EndemicityIndexImplBase {
    @Override
    public void get(EndemicityRequest request, StreamObserver<EndemicityResponse> responseObserver) {
        super.get(request, responseObserver);
    }
}
