package com.github.mu.tools.interactive.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;

import com.github.mu.tools.helpers.CommonShellCommandsHelper;
import com.github.mu.tools.interactive.OperationMode;
import com.github.mu.tools.interactive.model.InteractiveModeStatus;
import com.github.mu.tools.helpers.ConfigHelpers;
import com.google.common.util.concurrent.Uninterruptibles;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public abstract class AbstractFileOpsController implements Runnable {

    private final ThreadPoolExecutor workerTaskPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(20);

    private final InteractiveModeStatus model;

    private final FoldersOpHelper helper;

    private final ConfigHelpers configHelpers;
    private final String masterPartition;
    private final String partitionBases;
    private final String copyFilter;

    private final OperationMode opsMode;

    private final CommonShellCommandsHelper shellCommandsHelper;

    public AbstractFileOpsController(OperationMode opsMode,
                                     InteractiveModeStatus model,
                                     FoldersOpHelper copyFoldersHelper,
                                     CommonShellCommandsHelper shellCommandsHelper,
                                     ConfigHelpers configHelpers,
                                     @Value("${master.partition}") String masterPartition,
                                     @Value("${partition.bases}") String partitionBases,
                                     @Value("${copy.filter}") String copyFilter) {

        this.opsMode = opsMode;
        this.model = model;
        this.helper = copyFoldersHelper;
        this.configHelpers = configHelpers;
        this.shellCommandsHelper = shellCommandsHelper;
        this.masterPartition = masterPartition;
        this.partitionBases = partitionBases;
        this.copyFilter = copyFilter;
    }


    String findDestinationFolderForPartition(String masterPath, String partitionIndex) {
        log.info("Find destination folder for masterName{} partitionIndex {} ", masterPath, partitionIndex);
        String masterName = new File(masterPath).getName();
        Map<String, String> bases = getPartitionBases();
        String pattern = bases.get(partitionIndex);
        String[] tokens = pattern.split("/");
        StringBuilder result = new StringBuilder();
        result.append(model.getBaseFolder());
        for (String token : tokens) {
            result.append("/");
            token = token.trim();
            if (token.startsWith("{") && token.endsWith("}")) {
                int ndx = Integer.parseInt(token.substring(1, token.length() - 1));
                if (ndx < 0) {
                    result.append(masterName, 0, masterName.length() + ndx);
                } else {
                    result.append(masterName, 0, ndx);
                }
            } else {
                result.append(token);
            }
        }
        return result.toString();
    }


    /**
     * Returns a map by device name and value - map by device index and mount point
     *
     * @return Result like {sda: {1: media/..., 2: media/...}}
     */
    public Map<String, Map<String, Path>> getCurrentMountedDevices() {
        String result = shellCommandsHelper.getCurrentMountedPartitions();
        return shellCommandsHelper.getCurrentMountedDevices(result, getPartitionBases().keySet());
    }



    Map<String, String> getPartitionBases() {
        log.info("Splitting partition bases of "+partitionBases);
        TreeMap<String, String> res = new TreeMap<>();
        String[] split = partitionBases.trim().split(",");
        for (String s : split) {
            String[] atoms = s.split(":");
            res.put(atoms[0], atoms[1]);
        }
        return res;
    }


    public void run() {
        ConcurrentHashMap<String, Runnable> usedDevices = new ConcurrentHashMap<>();
        Map<String, Map<String, Path>> mounted;
        Set<String> partitions = getPartitionBases().keySet();
        while (!(mounted = getCurrentMountedDevices()).isEmpty() || model.isInteractiveModeEnabled()) {
            for (Map.Entry<String, Map<String, Path>> entry : mounted.entrySet()) {
                Path masterPartitionPath = entry.getValue().get(masterPartition);
                File masterFile = configHelpers.findMasterFile(masterPartitionPath.toFile());
                if (!usedDevices.contains(masterFile.getAbsoluteFile())) {
                    Runnable worker = new FileOpsWorker(entry, partitions, masterFile);
                    usedDevices.put(masterFile.getAbsolutePath(), worker);
                    workerTaskPool.execute(worker);
                }
            }
            Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        }
    }

    class FileOpsWorker implements Runnable {

        private String driveName;
        private Map.Entry<String, Map<String, Path>> entry;
        private Collection<String> partitions;
        private File masterFile;

        FileOpsWorker(Map.Entry<String, Map<String, Path>> entry, Collection<String> partitions, File masterFile) {
            this.driveName = entry.getKey();
            this.entry = entry;
            this.partitions = partitions;
            this.masterFile = masterFile;
        }

        @Override
        public void run() {
            for (String partitionIndex : partitions) {
                Path sourceFolder = entry.getValue().get(partitionIndex);
                String destinationFolder = findDestinationFolderForPartition(masterFile.getPath(), partitionIndex);
                final String sourceDevice = driveName + partitionIndex;
                String opDisplayName =
                        opsMode==OperationMode.MOVE? "Moving":
                        opsMode==OperationMode.DELETE? "Deleting":
                        "Archiving";
                InteractiveModeStatus.CopyWorkerStatus statusMsg =
                        InteractiveModeStatus.CopyWorkerStatus.builder()
                                .sourceDevice(sourceDevice)
                                .operation(opDisplayName)
                                .operationArguments(destinationFolder)
                                .build();

                model.getCurrentWorkers().put(sourceDevice, statusMsg);
                String masterFileId = masterFile.getName()
                        .replaceAll("\\.\\w+", "");
                String masterFileIdWithPartition = masterFileId+"/P"+partitionIndex;
                if (opsMode == OperationMode.MOVE || opsMode==OperationMode.ARCHIVE) {
                    try {
                        helper.copyFolders(statusMsg, statusMsg.getSourceDevice(),
                                           sourceFolder.toFile().getAbsolutePath(),
                                           destinationFolder, copyFilter);
                        model.getSuccessfulPartitionCommand().incrementAndGet();
                        if (partitionIndex.equals(masterPartition)) {
                            model.getSuccessfulDiskCommand().incrementAndGet();
                        }
                    } catch (RuntimeException | IOException e) {
                        String error =
                                "Cannot copy " +sourceFolder + "to "+ destinationFolder + " exception: "
                                + e.getMessage();
                        model.addError(error);
                        model.addErrorId(masterFileIdWithPartition);
                        log.error(e.getMessage(), e);
                    }
                }
                if (opsMode==OperationMode.MOVE || opsMode==OperationMode.DELETE) {
                    try {
                        helper.zapFolders(statusMsg, sourceFolder.toFile().getAbsolutePath());
                    } catch (RuntimeException | IOException e) {
                        String error =
                                "Cannot process " + destinationFolder + " exception: "
                                + e.getMessage();
                        model.addError(error);
                        model.addErrorId(masterFileIdWithPartition);
                        log.error(e.getMessage(), e);
                    }
                }
                try {
                    shellCommandsHelper.unmountDevice(statusMsg, sourceDevice, sourceDevice);
                    if (!model.getErrorId().contains(masterFileIdWithPartition)) {
                        model.addSuccessfulId(masterFileIdWithPartition);
                    }

                } catch (RuntimeException | IOException e) {
                    String error = "Cannot unmount " + sourceDevice + " exception: " + e.getMessage();
                    model.addError(error);
                    model.addErrorId(masterFileIdWithPartition);
                    log.error(e.getMessage(), e);
                } finally {
                    model.getCurrentWorkers().remove(sourceDevice);
                }
            }
        }
    }
}
