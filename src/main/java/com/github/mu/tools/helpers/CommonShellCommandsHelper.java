package com.github.mu.tools.helpers;

import static com.github.mu.tools.interactive.ArchivingConstants.DEV_FOLDER_START;
import static com.github.mu.tools.interactive.ArchivingConstants.UMOUNT_CMD;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.github.mu.tools.interactive.ArchivingConstants;
import com.github.mu.tools.interactive.model.InteractiveModeStatus;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CommonShellCommandsHelper {

    private final String masterPartition;
    public CommonShellCommandsHelper(@Value("${master.partition}") String masterPartition) {
        this.masterPartition = masterPartition;
    }

    public void unmountDevice(InteractiveModeStatus.CopyWorkerStatus model, String displayName, String masterName)
            throws IOException {
        model.setOperation("Unmounting device");
        model.setSourceDevice(displayName);
        model.setOperationArguments(masterName);
        String osDeviceName = DEV_FOLDER_START + masterName;
        CommandLine cmdLine = CommandLine.parse(String.format(UMOUNT_CMD, osDeviceName));
        DefaultExecutor executor = new DefaultExecutor();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        executor.setStreamHandler(new PumpStreamHandler(baos, null));
        ExecuteWatchdog watchdog = new ExecuteWatchdog(60000);
        executor.setWatchdog(watchdog);
        executor.setExitValue(0);
        executor.execute(cmdLine);
    }


    /**
     * Returns a map by device name and value - map by device index and mount point
     *
     * @return Result like {sda: {1: media/..., 2: media/...}}
     */
    public String getCurrentMountedPartitions() {
        String line = ArchivingConstants.LIST_PARTITIONS_CMD;
        CommandLine cmdLine = CommandLine.parse(line);
        DefaultExecutor executor = new DefaultExecutor();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        executor.setStreamHandler(new PumpStreamHandler(baos, null));
        ExecuteWatchdog watchdog = new ExecuteWatchdog(60000);
        executor.setWatchdog(watchdog);
        executor.setExitValue(0);
        try {
            executor.execute(cmdLine);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String result = baos.toString();
        log.info("List partitions {} ",result);
        return result;
    }


     public Map<String, Map<String, Path>> getCurrentMountedDevices(String lsblkResult,
                                                                    Set<String> partitionBases) {
        String[] entries = lsblkResult.split("\\r?\\n|\\r");
        TreeMap<String, String> allPartitionsMap = new TreeMap<>();
        Set<String> partitionsToCopy = partitionBases;
        for (String entry : entries) {
            entry = entry.trim();
            if (StringUtils.hasText(entry)) {
                String[] e = entry.split("\\s+");
                String first = e[0].trim();
                String last = e[e.length - 1].trim();
                if (last.startsWith(ArchivingConstants.MEDIA_FOLDER_START)) {
                    allPartitionsMap.put(first, last);
                }
            }
        }
        Map<String, Map<String, Path>> fullPartitionsResult = new TreeMap<>();
        nextPartition:
        for (Map.Entry<String, String> partition : allPartitionsMap.entrySet()) {
            if (partition.getKey().endsWith(masterPartition)) {
                Map<String, Path> partitionGroup = new TreeMap<>();
                String diskKey = partition.getKey().substring(
                        0, partition.getKey().length() - masterPartition.length());
                for (String currentPartitionIndex : partitionsToCopy) {
                    String partitionName = diskKey + currentPartitionIndex;
                    String mountPoint = allPartitionsMap.get(partitionName);
                    if (!StringUtils.hasText(mountPoint)) {
                        continue nextPartition;
                    }
                    partitionGroup.put(currentPartitionIndex, new File(mountPoint).toPath());
                }
                fullPartitionsResult.put(diskKey, partitionGroup);
            }
        }
        return fullPartitionsResult;
    }


}