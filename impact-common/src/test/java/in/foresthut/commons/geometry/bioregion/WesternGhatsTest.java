package in.foresthut.commons.geometry.bioregion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class WesternGhatsTest {
    @Test
    void test() {
        double lat =  18.235203512739993;
        double lon = 73.58054480866926;

        assertTrue(WesternGhats.getInstance().contains(lat, lon));

    }

    @Test
    void testPolygon() {
        String forestHut = "POLYGON ((73.58264389438973 18.237123765924977, 73.58298721714364 18.233781439701175, 73.58012047267239 18.23407491469577, 73.58001747479848 18.237237893127656, 73.58001747479848 18.237237893127656, 73.58264389438973 18.237123765924977))";

        assertTrue(WesternGhats.getInstance().contains(forestHut));
    }
}
