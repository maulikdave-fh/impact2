package in.foresthut.restorationsite.service.handler;

import in.foresthut.commons.s2geo.entity.S2CellTokenPlus;
import in.foresthut.impact.models.site.GetAoIRequest;
import in.foresthut.impact.models.site.GetAoIResponse;
import in.foresthut.impact.models.site.GetUnionAoIRequest;
import in.foresthut.impact.models.site.GetUnionAoIResponse;
import in.foresthut.infra.MessageHandler;
import in.foresthut.restorationsite.repository.RestorationSiteRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record GetAoIUnionHandler(GetUnionAoIRequest request,
                                 RestorationSiteRepository restorationSiteRepository,
                                 StreamObserver<GetUnionAoIResponse> responseObserver) implements MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(GetUnionAoIResponse.class);

    @Override
    public void handle() {
        logger.info("Get AoI createSiteRequest for site Id {}", request.getSiteId());
        // Validate Request
        if (request.getSiteId() == null || request.getSiteId()
                                                  .isBlank()) {
            var errorResponse = Status.FAILED_PRECONDITION.withDescription("Invalid Site Id.");
            logger.error("Invalid site Id: '{}'", request.getSiteId());

            responseObserver.onError(errorResponse.asRuntimeException());
            return;
        }

        // Fetch from Db
        Map<String, Boolean> aoiBlocks = restorationSiteRepository.getAoI(request.getSiteId());

        if (aoiBlocks == null || aoiBlocks.isEmpty()) {
            var errorResponse = Status.FAILED_PRECONDITION.withDescription("Site id not found.");
            logger.error("Site id '{}' not found.", request.getSiteId());

            responseObserver.onError(errorResponse.asRuntimeException());
            return;
        }

        String aoIUnionString = getAoIUnionWktString(aoiBlocks);

        if (aoIUnionString == null) {
            var errorResponse = Status.FAILED_PRECONDITION.withDescription("Site id not found.");
            logger.error("Site id '{}' not found.", request.getSiteId());

            responseObserver.onError(errorResponse.asRuntimeException());
            return;
        }

        responseObserver.onNext(GetUnionAoIResponse.newBuilder()
                                              .setAoiWktPolygonString(aoIUnionString)
                                              .build());
        responseObserver.onCompleted();
    }

    public static String getAoIUnionWktString(Map<String, Boolean> aoiBlocks) {

        if (aoiBlocks == null || aoiBlocks.isEmpty()) {
            return null;
        }

        List<Geometry> polygons = new ArrayList<>();
        WKTReader wktReader = new WKTReader();
        for (var block : aoiBlocks.keySet()) {
            try {
                polygons.add(wktReader.read(S2CellTokenPlus.cellDetails(block).wktPolygonString()));
            } catch (ParseException e) {
                logger.error("Error: ", e);
                throw new RuntimeException(e);
            }
        }
        GeometryFactory factory = new GeometryFactory();
        GeometryCollection geometryCollection = factory.createGeometryCollection(polygons.stream()
                                                                                         .toArray(Polygon[]::new));
        Geometry unionResult = geometryCollection.union();
        return unionResult.toText();
    }
}
