package in.foresthut.infra.occurrence.repository;

import in.foresthut.commons.geometry.bioregion.Bioregion;

import java.util.List;
import java.util.Map;

public interface OccurrenceRepository {
    long speciesCountForS2Token(String cellToken, int year);

    long speciesCountForPolygon(String wktPolygonString, int year);

    List<String> speciesForS2Token(String cellToken, int year);

    Map<String, Long> speciesOccurrencesForPolygon(String wktPolygonString, int year);

    long speciesCountByTaxaGroup(String iconicTaxa, String wktPolygonString, int forYear);

    List<String> getTaxaGroups(String wktPolygon, int forYear);

    long numberOfTaxaGroups(String wktPolygon, int forYear);

    List<String> getDistinctSpecies(String wktPolygon);

    long speciesOccurrencesForPolygon(String species, String wktPolygon);
}
