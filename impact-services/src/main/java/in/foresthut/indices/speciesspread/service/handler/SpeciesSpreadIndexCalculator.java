package in.foresthut.indices.speciesspread.service.handler;

import in.foresthut.indices.speciesspread.entity.SpeciesSpreadIndexDao;
import in.foresthut.infra.occurrence.repository.OccurrenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public record SpeciesSpreadIndexCalculator(String restorationSitePolygon,
                                           String aoIPolygon,
                                           int forYear,
                                           OccurrenceRepository occurrenceRepository) {
    private static final Logger logger = LoggerFactory.getLogger(SpeciesSpreadIndexCalculator.class);

    private static final Map<String, Double> IMPACT_COEFFICIENTS = new HashMap<>();

    static {
        IMPACT_COEFFICIENTS.put("Actinopterygii", 0.6);
        IMPACT_COEFFICIENTS.put("Amphibia", 0.7);
        IMPACT_COEFFICIENTS.put("Arachnida", 0.4);
        IMPACT_COEFFICIENTS.put("Aves", 0.7);
        IMPACT_COEFFICIENTS.put("Fungi", 0.4);
        IMPACT_COEFFICIENTS.put("Insecta", 0.4);
        IMPACT_COEFFICIENTS.put("Mammalia", 0.9);
        IMPACT_COEFFICIENTS.put("Mollusca", 0.4);
        IMPACT_COEFFICIENTS.put("Other", 0.5);
        IMPACT_COEFFICIENTS.put("Plantae", 0.6);
        IMPACT_COEFFICIENTS.put("Squamata", 0.7);
    }

    public SpeciesSpreadIndexValueObj calculate() {
        double index = 0.0, sumOfTaxaGroupSkew = 0.0;

        long nSE = occurrenceRepository.speciesCountForPolygon(aoIPolygon, forYear);
        long nSR = occurrenceRepository.speciesCountForPolygon(restorationSitePolygon, forYear);


        Map<String, Long> restorationSiteTaxaWiseSpeciesCount = new HashMap<>();
        Map<String, Long> aoITaxaWiseSpeciesCount = new HashMap<>();

        for (var iconicTaxa : IMPACT_COEFFICIENTS.keySet()) {
            long nSTgE = occurrenceRepository.speciesCountByTaxaGroup(iconicTaxa, aoIPolygon, forYear);
            long nSTgR = occurrenceRepository.speciesCountByTaxaGroup(iconicTaxa, restorationSitePolygon, forYear);

            double taxaGroupSkew = 0;
            if (nSR != 0 && nSTgR != 0)
                taxaGroupSkew =
                    IMPACT_COEFFICIENTS.get(iconicTaxa) * Math.abs((nSTgE / (double) nSE) - (nSTgR / (double) nSR));

            sumOfTaxaGroupSkew += taxaGroupSkew;

            restorationSiteTaxaWiseSpeciesCount.put(iconicTaxa, nSTgR);
            aoITaxaWiseSpeciesCount.put(iconicTaxa, nSTgE);
        }

        long nTgE = occurrenceRepository.numberOfTaxaGroups(aoIPolygon, forYear);
        index = nTgE != 0 && nSR != 0 ? sumOfTaxaGroupSkew / nTgE : 0;
        return new SpeciesSpreadIndexValueObj(
                index, forYear,
                new SpeciesSpreadIndexDao.RestorationSiteMetaData(nSR, restorationSiteTaxaWiseSpeciesCount),
                new SpeciesSpreadIndexDao.AoIMetaData(nSE, aoITaxaWiseSpeciesCount));

    }

    public record SpeciesSpreadIndexValueObj(double index,
                                             int forYear,
                                             SpeciesSpreadIndexDao.RestorationSiteMetaData restorationSiteMetaData,
                                             SpeciesSpreadIndexDao.AoIMetaData aoIMetaData) {
    }

}
