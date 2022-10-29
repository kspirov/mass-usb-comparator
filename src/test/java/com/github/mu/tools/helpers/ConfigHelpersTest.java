package com.github.mu.tools.helpers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConfigHelpersTest {


    @Test
    void testMasterFilename() {
        ConfigHelpers copyController = new ConfigHelpers("1",
                                                         "1:{2}/{-4},2:{2}/{-4}/ext",
                                                         "", ".csv",
                                                         "zero-result,ext",
                                                         "lost+found,System Volume Information");
        assertTrue(copyController.isMasterFileName("x.csv"));
        assertFalse(copyController.isMasterFileName("x.csvs"));
        assertFalse(copyController.isMasterFileName("xzero-result.csv"));
        assertFalse(copyController.isMasterFileName("ext.csv"));
    }
}
