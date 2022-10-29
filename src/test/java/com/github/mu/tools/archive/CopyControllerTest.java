package com.github.mu.tools.archive;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CopyControllerTest {

    @Test
    void testGetCurrentMountedDevicesPvt() {

        FileOpsController abstractFileOpsControlller = new FileOpsController(null, null,
                                                                             null, "1",
                                                                             "1:{2}/{-4},2:{2}/{-4}/ext",
                                                                             "", ".csv",
                                                                             "zero-result,ext",
                                                                             "lost+found,System Volume Information");
        String lbskResult = "sdd1     8:49   1   249M  0 part /media/user/3E7096385DFE2576\n"
                            + "sdd2     8:50   1  1000M  0 part /media/user/35b88cce-f4fc-41f0-9c08-db40a8436645\n"
                            + "sdc1     8:49   1   249M  0 part /media/user/3E74096385DFE2576\n"
                            + "sdc2     8:50   1  1000M  0 part /media/user/35b88cce-f4fc-41f0-9c08-db40a8436645\n"
                            + "sdb1     8:50   1  1000M  0 part /media/user/35b88cce-f4fc-41f0-9c08-db40a8436645\n"
                            + "sda1     8:49   1   249M  0 part /media/user/3E74096385DFE2576\n"
                            + "sda5     8:50   1  1000M  0 part /media/user/35b88cce-f4fc-41f0-9c08-db40a8436645\n";

        Map<String, Map<String, Path>> res = abstractFileOpsControlller.getCurrentMountedDevicesPvt(lbskResult);
        assertThat(res.size()).isEqualTo(2);
        Map.Entry<String, Map<String, Path>> firstValue = res.entrySet().iterator().next();
        assertThat(firstValue.getKey()).isEqualTo("sdc");
        assertThat(firstValue.getValue().size()).isSameAs(2);
        Map.Entry<String, Path> ent = firstValue.getValue().entrySet().iterator().next();
        assertThat(ent.getValue().toString()).contains("3E74096385DFE2576");
        assertThat(ent.getKey()).isEqualTo("1");
    }

    @Test
    void testFindBaseFolderForPartition() {
        FileOpsController
                abstractFileOpsControlller = new FileOpsController(new InteractiveModeStatus(), null,
                                                                   null, "1",
                                                                   "1:{2}/{-4},2:{2}/{-4}/ext",
                                                                   "", ".csv",
                                                                   "zero-result,ext",
                                                                   "lost+found,System Volume Information");
        String s1 = abstractFileOpsControlller.findDestinationFolderForPartition("014000028.csv", "1");
        assertThat(s1).endsWith("01/014000028");
        String s2 = abstractFileOpsControlller.findDestinationFolderForPartition("014000028.csv", "2");
        assertThat(s2).endsWith("01/014000028/ext");
    }

}