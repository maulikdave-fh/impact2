package in.foresthut.occurrence.processor;


import ch.hsr.geohash.GeoHash;
import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2LatLng;
import com.nirvighna.www.commons.config.AppConfig;
import in.foresthut.commons.continentality.Oceans;
import in.foresthut.commons.elevation.ElevationFinder;
import in.foresthut.commons.geometry.bioregion.WesternGhats;
import in.foresthut.commons.occurrence.Occurrence;
import in.foresthut.commons.occurrence.entity.OccurrenceDao;
import in.foresthut.commons.occurrence.mapper.OccurrenceMapper;
import in.foresthut.commons.occurrence.util.IconicTaxaFinder;
import in.foresthut.geohash.repository.GeoHashRepository;
import in.foresthut.occurrence.repository.OccurrenceRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;

public class OccurrenceDataProcessor implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(OccurrenceDataProcessor.class);
    private final static AppConfig config = AppConfig.getInstance();
    private static final GeometryFactory geometryFactory = new GeometryFactory();
    private static Oceans oceans;
    private static ElevationFinder elevationFinder;
    private static OccurrenceRepository occurrenceRepository;
    private static GeoHashRepository geoHashRepository;
    private final String occurrenceRawLine;

    public OccurrenceDataProcessor(String occurrenceRaw, Oceans oceans, ElevationFinder elevationFinder,
                                   OccurrenceRepository occurrenceRepository, GeoHashRepository geoHashRepository) {
        this.occurrenceRawLine = occurrenceRaw;
        OccurrenceDataProcessor.oceans = oceans;
        OccurrenceDataProcessor.elevationFinder = elevationFinder;
        OccurrenceDataProcessor.occurrenceRepository = occurrenceRepository;
        OccurrenceDataProcessor.geoHashRepository = geoHashRepository;
    }

    /*
                0 - GBIF Occurrence Id
                5 - Modified On
                15 - Dataset Name
                24 - Recorded By
                62 - Event Date
                97 - Latitude
                98 - Longitude
                99 - Elevation
                156 - kingdom,
                157 - phylum
                158 - _class
                159 - order
                160 - superFamily
                161 - family
                162 - subFamily
                163 - tribe
                164 - subTribe
                165 - genus
                167 - subGenus
                201 - Species Name
                215 - Country
                217 - State
                219 - District
                221 - Location Name String - County
                222 - IUCN code
         */
    private static OccurrenceDao toOccurrenceDao(String[] fields) {
        try {
            // Coordinates
            double longitude = Double.valueOf(fields[98]);
            double latitude = Double.valueOf(fields[97]);

            // Elevation
            double elevation = ElevationFinder.getInstance()
                                              .elevationOf(latitude, longitude);

            // Iconic Taxa
            String iconicTaxa = IconicTaxaFinder.iconicTaxaOf(fields[156], fields[157], fields[158], fields[159]);

            // Distance From Sea
            Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
            double distanceFromSeaInKm = oceans.distance(point);

            // GeoHash String
//            String geoHashString = GeoHash.withCharacterPrecision(latitude, longitude, 5)
//                                          .toBase32();

            // S2 Geometry cell token
            String cellToken = s2CellToken(latitude, longitude, 16);

            var occurrence = new Occurrence(
                    Long.valueOf(fields[0]), fields[201], fields[156], fields[157], fields[158], fields[159],
                    fields[160], fields[161], fields[162], fields[163], fields[164], fields[165], fields[167],
                    iconicTaxa, fields[222], Double.valueOf(fields[97]), Double.valueOf(fields[98]), elevation,
                    distanceFromSeaInKm, dateParser(fields[62]), dateParser(fields[5]), fields[15], fields[24],
                    cellToken, fields[215], fields[217], fields[219], fields[221]);
            return OccurrenceMapper.toOccurrenceDao(occurrence);
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
        return null;
    }

    private static boolean validData(String[] fields) {
        return isValid(fields[15]) && isValid(fields[201]) && isValid(fields[97]) && isValid(fields[98]) && isValid(
                fields[62]);
    }

    private static boolean isValid(String str) {
        return str != null && !str.isBlank();
    }

    private static LocalDateTime dateParser(String date) {
        LocalDateTime result = null;

        if (date.matches("\\d{4}")) {
            date = date + "-01-01";
            result = LocalDateTime.of(LocalDate.parse(date), LocalTime.of(0, 0, 0));
        } else if (date.matches("\\d{4}-\\d{1,2}")) {
            date = date + "-01";
            result = LocalDateTime.of(LocalDate.parse(date), LocalTime.of(0, 0, 0));
        } else if (date.contains("/")) {
            date = date.substring(0, date.indexOf('/'));
            result = LocalDateTime.of(LocalDate.parse(date), LocalTime.of(0, 0, 0));
        } else if (date.matches("\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{1,2}:\\d{1,2}\\.\\d{3}\\+\\d{2}:\\d{2}")) {
            date = date.substring(0, date.indexOf('.'));
            result = LocalDateTime.parse(date);
        } else if (date.matches("\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{1,2}Z")) {
            date = date.substring(0, date.length() - 1) + ":00Z";
            result = Instant.parse(date)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
        } else if (date.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
            result = LocalDateTime.of(LocalDate.parse(date), LocalTime.of(0, 0, 0));
        } else if (date.contains("Z") || date.contains("z")) {
            result = Instant.parse(date)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
        } else {
            result = LocalDateTime.parse(date);
        }
        return result;
    }

    private static String s2CellToken(double latitude, double longitude, int level) {
        S2LatLng location = S2LatLng.fromDegrees(latitude, longitude);
        S2CellId cellId = S2CellId.fromLatLng(location)
                                  .parent(level);
        return cellId.toToken();
    }

    @Override
    public void run() {
        String[] fields = occurrenceRawLine.split("\t");

        if (fields.length > 222 && validData(fields)) {
            // Check if the coordinates fall in supported geohash
            double latitude = Double.valueOf(fields[97]);
            double longitude = Double.valueOf(fields[98]);
            // if (isSupportedGeoHash(latitude, longitude)) {
            if (isSupported(latitude, longitude)) {
                //logger.info("Geohash exists for ({}, {}).", latitude, longitude);
                // Extract necessary fields from the data to create Observation object
                OccurrenceDao occurrence = toOccurrenceDao(fields);

                if (occurrence != null) {
                    occurrenceRepository.add(occurrence);
                }
            }
        }
    }

    private boolean isSupportedGeoHash(double latitude, double longitude) {
//        return cuckooFilterClient.exists("geohash", GeoHash.withCharacterPrecision(latitude, longitude, 5)
//                                                           .toBase32());
        String geoHash = GeoHash.withCharacterPrecision(latitude, longitude, 5)
                                .toBase32();

        return geoHashRepository.exists(geoHash);
    }

    private boolean isSupported(double latitude, double longitude) {
        return WesternGhats.getInstance()
                           .contains(latitude, longitude);
    }
}

