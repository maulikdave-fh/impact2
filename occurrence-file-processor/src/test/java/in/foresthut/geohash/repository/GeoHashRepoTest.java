package in.foresthut.geohash.repository;

import com.nirvighna.mongodb.commons.DatabaseConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeoHashRepoTest {

    @Test
    void testExists_whenExists() {
        assertTrue(new GeoHashRepositoryImpl(DatabaseConfig.getInstance()).exists("tegf1"));
    }

    @Test
    void testExists_whenDoesNotExist() {
        assertFalse(new GeoHashRepositoryImpl(DatabaseConfig.getInstance()).exists("tega1"));
    }
}
