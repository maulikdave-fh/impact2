package in.foresthut.indices.speciesdensity.service.handler;

import in.foresthut.impact.models.indices.speciesdensity.SpeciesDensityAoIBlockPlus;
import in.foresthut.impact.models.indices.speciesdensity.SpeciesDensityRequest;
import in.foresthut.impact.models.indices.speciesdensity.SpeciesDensityResponse;
import in.foresthut.indices.speciesdensity.entity.SpeciesDensityIndexDao;
import in.foresthut.indices.speciesdensity.repository.SpeciesDensityIndexRepository;
import in.foresthut.indices.speciesdensity.validator.RequestValidator;
import in.foresthut.infra.MessageHandler;
import in.foresthut.infra.occurrence.repository.OccurrenceRepository;
import in.foresthut.restorationsite.repository.RestorationSiteRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record SpeciesDensityRequestHandler(SpeciesDensityRequest request,
                                           OccurrenceRepository occurrenceRepository,
                                           RestorationSiteRepository restorationSiteRepository,
                                           SpeciesDensityIndexRepository speciesDensityIndexRepository,
                                           StreamObserver<SpeciesDensityResponse> responseObserver)
        implements MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(SpeciesDensityRequestHandler.class);

    @Override
    public void handle() {
        // Validate request
        var status = RequestValidator.validateSpeciesDensityIndexRequest(request);

        if (status.isPresent()) {
            logger.error(
                    "Validation failed: {}", status.get()
                                                   .getDescription());
            responseObserver.onError(status.get()
                                           .asRuntimeException());
            return;
        }

        // if already exists in species density collection, return
        SpeciesDensityIndexDao existingSpeciesDensity =
                speciesDensityIndexRepository.get(request.getSiteId(), request.getForYear());
        if (existingSpeciesDensity != null) {
            var response = SpeciesDensityResponse.newBuilder()
                                                 .setSiteId(existingSpeciesDensity.siteId())
                                                 .setForYear(existingSpeciesDensity.forYear())
                                                 .setIndex(existingSpeciesDensity.index())
                                                 .addAllAoiBlockPlus(existingSpeciesDensity.aoiBlocks()
                                                                                           .stream()
                                                                                           .map(block -> SpeciesDensityAoIBlockPlus.newBuilder()
                                                                                                                                   .setSpeciesCount(
                                                                                                                                           block.speciesCount)
                                                                                                                                   .putAllAoiBlock(
                                                                                                                                           block.aoIBlock)
                                                                                                                                   .build())
                                                                                           .toList())
                                                 .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        // get all AoI blocks of the site
        Map<String, Boolean> aoIBlocks = restorationSiteRepository.getAoI(request.getSiteId());
        if (aoIBlocks == null) {
            var errorResponse = Status.FAILED_PRECONDITION.withDescription("Site id not found.");
            logger.error("Site id '{}' not found.", request.getSiteId());

            responseObserver.onError(errorResponse.asRuntimeException());
            return;
        }
        logger.info("The site {} has {} AoI blocks", request.getSiteId(), aoIBlocks.size());
        // Calculate the index
        var speciesDensityValueObj = calculateSpeciesDensity(aoIBlocks);

        // Return the index
        var response = SpeciesDensityResponse.newBuilder()
                                             .setSiteId(request.getSiteId())
                                             .setForYear(request.getForYear())
                                             .setIndex(speciesDensityValueObj.speciesDensityIndexDao.index())
                                             .addAllAoiBlockPlus(
                                                     speciesDensityValueObj.speciesDensityIndexDao.aoiBlocks()
                                                                                                  .stream()
                                                                                                  .map(block -> SpeciesDensityAoIBlockPlus.newBuilder()
                                                                                                                                          .setSpeciesCount(
                                                                                                                                                  block.speciesCount)
                                                                                                                                          .putAllAoiBlock(
                                                                                                                                                  block.aoIBlock)
                                                                                                                                          .build())
                                                                                                  .toList())
                                             .setMaxSpeciesCount(speciesDensityValueObj.maxSpeciesCount)
                                             .setMinSpeciesCount(speciesDensityValueObj.minSpeciesCount)
                                             .setAvgSpeciesCount(speciesDensityValueObj.avgSpeciesCount)
                                             .setRestorationSiteAvgSpeciesCount(
                                                     speciesDensityValueObj.restorationSiteAvgSpeciesCount)
                                             .build();

        speciesDensityIndexRepository.add(speciesDensityValueObj.speciesDensityIndexDao);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public SpeciesDensityValueObj calculateSpeciesDensity(Map<String, Boolean> aoIBlocks) {
        // get species count for each AoI blocks of the site
        List<SpeciesDensityAoIPlus> restorationSiteAoIBlockPlus = new ArrayList<>();
        List<SpeciesDensityAoIPlus> aoIRegionAoIBlockPlus = new ArrayList<>();

        long maxSpeciesCount = Integer.MIN_VALUE;
        long minSpeciesCount = Integer.MAX_VALUE;

        int restorationSiteSpeciesCountTotal = 0;
        int aoISpeciesCountTotal = 0;

        logger.info("Number of blocks {}", aoIBlocks.size());
        for (var entrySet : aoIBlocks.entrySet()) {
            long speciesCount = occurrenceRepository.speciesCountForS2Token(entrySet.getKey(), request().getForYear());

            aoISpeciesCountTotal += speciesCount;

            var speciesDensityAoIBlockPlus =
                    new SpeciesDensityAoIPlus(Map.of(entrySet.getKey(), entrySet.getValue()), speciesCount);

            if (entrySet.getValue()) {
                restorationSiteAoIBlockPlus.add(speciesDensityAoIBlockPlus);

                restorationSiteSpeciesCountTotal += speciesCount;

            } else {
                aoIRegionAoIBlockPlus.add(speciesDensityAoIBlockPlus);

                // Find AoI blocks with max and min species counts for non-restoration site blocks
                if (speciesCount > maxSpeciesCount) {
                    maxSpeciesCount = speciesCount;
                }

                if (speciesCount < minSpeciesCount) {
                    minSpeciesCount = speciesCount;
                }
            }
        }
        logger.info("Restoration site blocks {}", restorationSiteAoIBlockPlus);

        logger.info("Max count {}", maxSpeciesCount);
        logger.info("Min count {}", minSpeciesCount);
        logger.info("Restoration site total count {}", restorationSiteSpeciesCountTotal);
        // Find average of restoration site blocks
        double restorationSiteAvgSpeciesCount =
                (double) restorationSiteSpeciesCountTotal / restorationSiteAoIBlockPlus.size();

        logger.info("Restoration site avg count {}", restorationSiteAvgSpeciesCount);

        // Calculate the index
        if (restorationSiteAvgSpeciesCount > maxSpeciesCount) maxSpeciesCount = (int) restorationSiteAvgSpeciesCount;

        double speciesDensityIndex =
                (double) ((int) restorationSiteAvgSpeciesCount - minSpeciesCount) / (maxSpeciesCount - minSpeciesCount);

        logger.info("Species Density {}", speciesDensityIndex);
        // Construct the dao
        List<SpeciesDensityAoIPlus> allBlocks = new ArrayList<>();
        allBlocks.addAll(aoIRegionAoIBlockPlus);
        allBlocks.addAll(restorationSiteAoIBlockPlus);

        SpeciesDensityIndexDao speciesDensityIndexDao =
                new SpeciesDensityIndexDao(
                        request().getSiteId(), request().getForYear(), speciesDensityIndex,
                                           allBlocks);
        return new SpeciesDensityValueObj(
                speciesDensityIndexDao, maxSpeciesCount, minSpeciesCount,
                                          aoISpeciesCountTotal / aoIBlocks.size(), restorationSiteAvgSpeciesCount);
    }

    public record SpeciesDensityValueObj(SpeciesDensityIndexDao speciesDensityIndexDao,
                                         long maxSpeciesCount,
                                         long minSpeciesCount,
                                         int avgSpeciesCount,
                                         double restorationSiteAvgSpeciesCount) {

    }

    public record SpeciesDensityAoIPlus(Map<String, Boolean> aoIBlock,
                                        long speciesCount) {

    }
}
