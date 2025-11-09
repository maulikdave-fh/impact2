package in.foresthut.commons.geohash;

import ch.hsr.geohash.GeoHash;
import in.foresthut.commons.geohash.entity.GeoHashPlus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GeoHashPlusTest {

    @Test
    void testToGeoHashPlus () {
        String geoHashString = "tegg0";
        var geoHash = GeoHash.fromGeohashString(geoHashString);

        var geoHashPlus = GeoHashPlus.toGeoHashPlus(geoHash);

        assertEquals(geoHashString, geoHashPlus.geoHashString());
    }
}
