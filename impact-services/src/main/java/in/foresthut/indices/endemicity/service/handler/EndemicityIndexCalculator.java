package in.foresthut.indices.endemicity.service.handler;

import in.foresthut.commons.geometry.bioregion.Bioregion;
import in.foresthut.infra.endemicity.repository.EndemicityRepository;
import in.foresthut.infra.gbif.GBIFClient;
import in.foresthut.infra.occurrence.repository.OccurrenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record EndemicityIndexCalculator(Bioregion bioregion,
                                        String speciesName,
                                        OccurrenceRepository occurrenceRepository,
                                        EndemicityRepository endemicityRepository,
                                        GBIFClient gbifClient) {

    private static final Logger logger = LoggerFactory.getLogger(EndemicityIndexCalculator.class);

    public EndemicityIndexValueObj calculate() {
        long bioregionOccurrenceCount =
                occurrenceRepository.speciesOccurrencesForPolygon(speciesName, bioregion().getWKTString());

        long globalCount = gbifClient.observations(speciesName, null);
        double endemicity = (double) bioregionOccurrenceCount / globalCount;
        return new EndemicityIndexValueObj(
                bioregion().code(), speciesName, endemicity, bioregionOccurrenceCount, globalCount);
    }

    public record EndemicityIndexValueObj(String bioregionCode,
                                          String speciesName,
                                          double index,
                                          long bioregionOccurrenceCount,
                                          long worldOccurrenceCount) {

    }
}
