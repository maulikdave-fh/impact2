package in.foresthut.commons.elevation;

import in.foresthut.commons.geohash.mapper.GeoHashMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ElevationFinderTest {
    private static ElevationFinder elevationFinder;

    @BeforeAll
    static void setup(){
        elevationFinder = ElevationFinder.getInstance();
    }

    @Test
    void testPointElevation1() {
        double lat =  18.235203512739993;
        double lon = 73.58054480866926;

        assertTrue(elevationFinder.elevationOf(lat, lon) > 0);

    }

    @Test
    void testPointElevation2() {
        double lat = 17.47234;
        double lon = 73.897769;

        assertTrue(elevationFinder.elevationOf(lat, lon) > 0);
    }


    @Test
    void testSouthGeoHash() {
        String geoHashString = "t9qze"; //t9x3n
        assertTrue(elevationFinder.elevationOf(GeoHashMapper.toGeometry(geoHashString).toText())[0] > 0);
    }

    @Test
    void testNorthGeoHash() {
        String geoHashString = "tek43";
        assertTrue(elevationFinder.elevationOf(GeoHashMapper.toGeometry(geoHashString).toText())[0] > 0);
    }

    @Test
    void testNullElevation() {
        String geoHashString = "t9z0c";
        assertTrue(elevationFinder.elevationOf(GeoHashMapper.toGeometry(geoHashString).toText())[0] > 0);
    }

}
