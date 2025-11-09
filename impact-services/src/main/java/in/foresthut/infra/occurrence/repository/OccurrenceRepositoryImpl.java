package in.foresthut.infra.occurrence.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.geojson.MultiPolygon;
import com.mongodb.client.model.geojson.PolygonCoordinates;
import com.nirvighna.mongodb.commons.DatabaseConfig;
import in.foresthut.commons.geometry.WktPolygon;
import in.foresthut.commons.geometry.bioregion.Bioregion;
import in.foresthut.commons.occurrence.entity.OccurrenceDao;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OccurrenceRepositoryImpl implements OccurrenceRepository {
    private static final Logger logger = LoggerFactory.getLogger(OccurrenceRepositoryImpl.class);

    private final MongoCollection<OccurrenceDao> occurrenceCollection;

    public OccurrenceRepositoryImpl(DatabaseConfig databaseConfig) {
        this.occurrenceCollection = databaseConfig.database()
                                                  .getCollection("occurrence", OccurrenceDao.class);
    }

    @Override
    public long speciesCountForS2Token(String cellToken, int forYear) {
        return speciesForS2Token(cellToken, forYear).size();
    }

    @Override
    public long speciesCountForPolygon(String wktPolygonString, int year) {
        var coordinates = coordinates(wktPolygonString);
        Bson combinedFilter = getFilter(year, coordinates);
        var distinctSpecies = occurrenceCollection.distinct("species", combinedFilter, String.class);
        return distinctSpecies.into(new ArrayList<>())
                              .size();
    }

    @Override
    public List<String> speciesForS2Token(String cellToken, int year) {
        Bson combinedFilter = getFilter(cellToken, year);
        var distinctSpecies = occurrenceCollection.distinct("species", combinedFilter, String.class);
        return distinctSpecies.into(new ArrayList<>());
    }

    @Override
    public Map<String, Long> speciesOccurrencesForPolygon(String wktPolygonString, int year) {
        List<PolygonCoordinates> coordinates = coordinates(wktPolygonString);
        Bson combinedFilter = getFilter(year, coordinates);

        var distinctSpecies = occurrenceCollection.distinct("species", combinedFilter, String.class);

        Map<String, Long> result = new HashMap<>();

        for (var species : distinctSpecies) {
            Bson filter = getFilter(species, year, coordinates);
            long speciesCount = occurrenceCollection.countDocuments(filter);

            result.put(species, speciesCount);
        }
        return result;
    }

    @Override
    public long speciesCountByTaxaGroup(String iconicTaxa, String wktPolygonString, int forYear) {
        var coordinates = coordinates(wktPolygonString);
        Bson combinedFilter = getFilter(forYear, coordinates);
        Bson moreFilter = Filters.and(combinedFilter, Filters.eq("iconicTaxa", iconicTaxa));
        var speciesForTaxa = occurrenceCollection.distinct("species", moreFilter, String.class);
        return speciesForTaxa.into(new ArrayList<>())
                             .size();
    }

    @Override
    public List<String> getTaxaGroups(String wktPolygon, int forYear) {
        var coordinates = coordinates(wktPolygon);
        Bson combinedFilter = getFilter(forYear, coordinates);
        var distinctTaxa = occurrenceCollection.distinct("iconicTaxa", combinedFilter, String.class);
        return distinctTaxa.into(new ArrayList<>());
    }

    @Override
    public long numberOfTaxaGroups(String wktPolygon, int forYear) {
        return getTaxaGroups(wktPolygon, forYear).size();
    }

    @Override
    public List<String> getDistinctSpecies(String wktPolygon) {
        List<PolygonCoordinates> coordinates = coordinates(wktPolygon);
        com.mongodb.client.model.geojson.MultiPolygon geometry = new MultiPolygon(coordinates);

        Bson filterOnPolygon = Filters.geoWithin("location", geometry);


        var distinctSpecies = occurrenceCollection.distinct("species", filterOnPolygon, String.class);

        return distinctSpecies.into(new ArrayList<>());
    }

    @Override
    public long speciesOccurrencesForPolygon(String species, String wktPolygon) {
        var coordinates = coordinates(wktPolygon);
        Bson filter = getFilter(species, coordinates);
        return occurrenceCollection.countDocuments(filter);
    }



    private Bson getFilter(String cellToken, int forYear) {
        Bson geohashFilter = Filters.eq("s2CellToken", cellToken);
        var startDate = LocalDateTime.of(forYear, 1, 1, 0, 0);
        var endDate = LocalDateTime.of(forYear, 12, 31, 23, 59);
        Bson startDateFilter = Filters.gte("eventDate", startDate);
        Bson endDateFilter = Filters.lte("eventDate", endDate);
        return Filters.and(geohashFilter, startDateFilter, endDateFilter);
    }

    private Bson getFilter(int forYear, List<PolygonCoordinates> coordinates) {
        //     com.mongodb.client.model.geojson.Polygon geometry = new com.mongodb.client.model.geojson.Polygon(coordinates);
        com.mongodb.client.model.geojson.MultiPolygon geometry = new MultiPolygon(coordinates);

        Bson filterOnPolygon = Filters.geoWithin("location", geometry);

        var startDate = LocalDateTime.of(forYear, 1, 1, 0, 0);
        var endDate = LocalDateTime.of(forYear, 12, 31, 23, 59);
        Bson startDateFilter = Filters.gte("eventDate", startDate);
        Bson endDateFilter = Filters.lte("eventDate", endDate);
        return Filters.and(filterOnPolygon, startDateFilter, endDateFilter);
    }

    private Bson getFilter(String species, int forYear, List<PolygonCoordinates> coordinates) {
        //com.mongodb.client.model.geojson.Polygon geometry = new com.mongodb.client.model.geojson.Polygon(coordinates);
        com.mongodb.client.model.geojson.MultiPolygon geometry = new MultiPolygon(coordinates);

        Bson filterOnPolygon = Filters.geoWithin("location", geometry);

        Bson speciesFilter = Filters.eq("species", species);

        var startDate = LocalDateTime.of(forYear, 1, 1, 0, 0);
        var endDate = LocalDateTime.of(forYear, 12, 31, 23, 59);
        Bson startDateFilter = Filters.gte("eventDate", startDate);
        Bson endDateFilter = Filters.lte("eventDate", endDate);
        return Filters.and(filterOnPolygon, speciesFilter, startDateFilter, endDateFilter);
    }

    private Bson getFilter(String species, List<PolygonCoordinates> coordinates) {
        //com.mongodb.client.model.geojson.Polygon geometry = new com.mongodb.client.model.geojson.Polygon(coordinates);
        com.mongodb.client.model.geojson.MultiPolygon geometry = new MultiPolygon(coordinates);

        Bson filterOnPolygon = Filters.geoWithin("location", geometry);

        Bson speciesFilter = Filters.eq("species", species);

        return Filters.and(filterOnPolygon, speciesFilter);
    }


    private List<PolygonCoordinates> coordinates(String wktPolygon) {
        WktPolygon poly = new WktPolygon(wktPolygon);
        return poly.coordinatesForMultiPolygon();
    }
}
