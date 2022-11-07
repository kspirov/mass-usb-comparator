package com.github.mu.tools.interactive.controllers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.mu.tools.interactive.controllers.ArchiveOpsController;
import com.github.mu.tools.interactive.model.InteractiveModeStatus;

class CopyControllerTest {


    @Test
    void testFindBaseFolderForPartition() {
        ArchiveOpsController
                abstractFileOpsControlller = new ArchiveOpsController(new InteractiveModeStatus(),
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      "1",
                                                                      "1:{2}/{-4},2:{2}/{-4}/ext",
                                                                      "lost+found,System Volume Information");
        String s1 = abstractFileOpsControlller.findDestinationFolderForPartition("x", "014000028.csv", "1");
        assertThat(s1).endsWith("01/014000028");
        String s2 = abstractFileOpsControlller.findDestinationFolderForPartition("x", "014000028.csv", "2");
        assertThat(s2).endsWith("01/014000028/ext");

    }

}