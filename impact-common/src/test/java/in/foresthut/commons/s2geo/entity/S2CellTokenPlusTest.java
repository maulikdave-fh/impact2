package in.foresthut.commons.s2geo.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class S2CellTokenPlusTest {

    @Test
    void test() {
        String cellToken = "3ba66d081";

        var s2CellPlush = S2CellTokenPlus.toS2TokenPlus(cellToken);
        assertEquals(cellToken, s2CellPlush.cellToken());
    }
}
