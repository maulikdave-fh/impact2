package in.foresthut.commons.continentality;

import in.foresthut.commons.geohash.mapper.GeoHashMapper;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OceansTest {
    @Test
    public void testPolygon_whenEntirelyInOcean_shouldReturnTrue() {
        // Arrange
        String geoHash = "te7twu";
        var polygonInOcean = toGeometry(geoHash);

        // Act
        var actualResult = Oceans.getInstance().inOcean(polygonInOcean);

        // Assert
        assertTrue(actualResult);
    }

    @Test
    public void testPolygon_whenPartiallyInOcean_shouldReturnFalse() {
        // Arrange
        String geoHash = "te7g8";
        var polygonInOcean = toGeometry(geoHash);

        // Act
        var actualResult = Oceans.getInstance().inOcean(polygonInOcean);

        // Assert
        assertFalse(actualResult);
    }

    @Test
    public void testPolygon_whenEntirelyOnLand_shouldReturnFalse() {
        // Arrange
        String geoHash = "tek7es";
        var polygonInOcean = toGeometry(geoHash);

        // Act
        var actualResult = Oceans.getInstance().inOcean(polygonInOcean);

        // Assert
        assertFalse(actualResult);
    }

    private Geometry toGeometry(String geoHashString) {
        return GeoHashMapper.toGeometry(geoHashString);
    }
}
