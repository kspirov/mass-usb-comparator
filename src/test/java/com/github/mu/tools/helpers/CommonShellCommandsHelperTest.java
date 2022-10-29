package com.github.mu.tools.helpers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.github.mu.tools.interactive.controllers.ArchiveOpsController;
import com.github.mu.tools.interactive.model.InteractiveModeStatus;

class CommonShellCommandsHelperTest {

    @Test
    void testGetCurrentMountedDevicesPvt() {

        CommonShellCommandsHelper helper = new CommonShellCommandsHelper("1");
        String lbskResult = "sdd1     8:49   1   249M  0 part /media/user/3E7096385DFE2576\n"
                            + "sdd2     8:50   1  1000M  0 part /media/user/35b88cce-f4fc-41f0-9c08-db40a8436645\n"
                            + "sdc1     8:49   1   249M  0 part /media/user/3E74096385DFE2576\n"
                            + "sdc2     8:50   1  1000M  0 part /media/user/35b88cce-f4fc-41f0-9c08-db40a8436645\n"
                            + "sdb1     8:50   1  1000M  0 part /media/user/35b88cce-f4fc-41f0-9c08-db40a8436645\n"
                            + "sda1     8:49   1   249M  0 part /media/user/3E74096385DFE2576\n"
                            + "sda5     8:50   1  1000M  0 part /media/user/35b88cce-f4fc-41f0-9c08-db40a8436645\n";

        Map<String, Map<String, Path>> res = helper.getCurrentMountedDevices(lbskResult,
                                                                             new TreeSet<>(Arrays.asList("1", "2")));
        assertThat(res.size()).isEqualTo(2);
        Map.Entry<String, Map<String, Path>> firstValue = res.entrySet().iterator().next();
        assertThat(firstValue.getKey()).isEqualTo("sdc");
        assertThat(firstValue.getValue().size()).isSameAs(2);
        Map.Entry<String, Path> ent = firstValue.getValue().entrySet().iterator().next();
        assertThat(ent.getValue().toString()).contains("3E74096385DFE2576");
        assertThat(ent.getKey()).isEqualTo("1");
    }
}