package com.github.mu.tools.interactive.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;

import com.github.mu.tools.helpers.CommonShellCommandsHelper;
import com.github.mu.tools.interactive.OperationMode;
import com.github.mu.tools.interactive.model.InteractiveModeStatus;
import com.github.mu.tools.helpers.ConfigHelpers;
import com.google.common.util.concurrent.Uninterruptibles;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;


@Slf4j
public abstract class AbstractFileOpsController implements Runnable {


    private static boolean UNMOUNT_EMPTY_FLASHES = false;

    private static final int WORKER_POOL_SIZE = 4;
    private static final int SECONDARY_WORKER_POOL_SIZE = 3;
    private final ThreadPoolExecutor workerTaskPool =
            (ThreadPoolExecutor) Executors.newFixedThreadPool(WORKER_POOL_SIZE);
    private final ThreadPoolExecutor secondaryWorkerPool =
            (ThreadPoolExecutor) Executors.newFixedThreadPool(SECONDARY_WORKER_POOL_SIZE);

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


    String findDestinationFolderForPartition(String baseFolder, String masterPath, String partitionIndex) {
        log.info("Find destination folder for masterName{} partitionIndex {} ", masterPath, partitionIndex);
        String masterName = new File(masterPath).getName();
        Map<String, String> bases = getSplitPartitionBases(partitionBases);
        String pattern = bases.get(partitionIndex);
        String[] tokens = pattern.split("/");
        StringBuilder result = new StringBuilder();
        result.append(baseFolder);
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
        return shellCommandsHelper.getCurrentMountedDevices(result, getSplitPartitionBases(partitionBases).keySet());
    }


    static Map<String, String> getSplitPartitionBases(String partitionBases) {
        log.trace("Splitting partition bases of " + partitionBases);
        TreeMap<String, String> res = new TreeMap<>();
        String[] split = partitionBases.trim().split(",");
        for (String s : split) {
            String[] atoms = s.split(":");
            res.put(atoms[0], atoms[1]);
        }
        return res;
    }


    public void run() {

        Map<String, String> usedDevices = Collections.synchronizedMap(new HashMap<String, String>());
        Map<String, String> problematicDevices = Collections.synchronizedMap(new HashMap<String, String>());
        Map<String, Map<String, Path>> mounted;
        Set<String> partitions = getSplitPartitionBases(partitionBases).keySet();
        while (!usedDevices.isEmpty() || model.isInteractiveModeEnabled()) {
            mounted = getCurrentMountedDevices();
            diskLoop:
            for (Map.Entry<String, Map<String, Path>> entry : mounted.entrySet()) {
                if (partitions.size() != entry.getValue().size()) {
                    continue diskLoop;
                }
                Path masterPartitionPath = entry.getValue().get(masterPartition);
                File masterFile = configHelpers.findMasterFile(masterPartitionPath.toFile());
                if (masterFile != null) {
                    String masterName = masterFile.getName();
                    String masterKey = entry.getKey() + ":" + masterFile.getName();

                    if (!usedDevices.containsKey(masterKey) && !problematicDevices.containsKey(masterKey)) {
                        Runnable worker = new FileOpsWorker(entry, partitions, masterFile,
                                                            () -> {
                                                                usedDevices.remove(masterKey);
                                                            },
                                                            () -> {
                                                                problematicDevices.put(masterKey, masterName);
                                                                usedDevices.remove(masterKey);
                                                            });
                        usedDevices.put(masterKey, masterName);
                        workerTaskPool.execute(worker);
                    }
                } else {
                    if (UNMOUNT_EMPTY_FLASHES) {
                        String masterKey = entry.getKey() + ":" + "UNKNOWN";
                        Map<String, Path> map = entry.getValue();
                        Collection<Path> folders = map.values();
                        boolean emptyFolders = existsAndHasOnlyEmptyFolders(folders, masterKey);
                        if (emptyFolders) {
                            if (!usedDevices.containsKey(masterKey)) {
                                log.info("Will unmout the disk {} ", masterKey);
                                Runnable worker = new UnmountWorker(entry, partitions, () -> {
                                    usedDevices.remove(masterKey);
                                },
                                                                    () -> {
                                                                        usedDevices.remove(masterKey);
                                                                    }, folders, masterKey);
                                usedDevices.put(masterKey, masterKey);
                                secondaryWorkerPool.execute(worker);
                            }
                        }
                    }

                }
            }
            Uninterruptibles.sleepUninterruptibly(3000, TimeUnit.MILLISECONDS);
        }
        log.info("bye.");
    }
    private boolean existsAndHasOnlyEmptyFolders(Collection<Path> folders, String masterKey) {
        boolean emptyFolders = true;
        loop:
        for (Path p : folders) {
            File f = p.toFile();
            if (!f.exists()) {
                log.info("Won't unmount the disk, as it is already unmounted {}", masterKey);
                emptyFolders = false;
                break loop;
            }
            File[] allFiles = p.toFile().listFiles();
            if (allFiles != null && allFiles.length > 0) {
                log.info("Won't unmount the disk {}", masterKey);
                emptyFolders = false;
                break loop;
            }
        }
        return emptyFolders;
    }

    class FileOpsWorker implements Runnable {


        private final String driveName;
        private final Map.Entry<String, Map<String, Path>> entry;
        private final Collection<String> partitions;
        private final File masterFile;

        private final Runnable onDone;
        private final Runnable onError;

        FileOpsWorker(Map.Entry<String, Map<String, Path>> entry, Collection<String> partitions, File masterFile,
                      Runnable onDone, Runnable onError) {
            this.driveName = entry.getKey();
            this.entry = entry;
            this.partitions = partitions;
            this.masterFile = masterFile;
            this.onDone = onDone;
            this.onError = onError;
        }

        @Override
        public void run() {

            log.info("Starting executor for disk " + driveName);
            try {
                boolean success = doItForDisk();
                if (success) {
                    onDone.run();
                } else {
                    onError.run();
                }
            } catch (Throwable t) {
                onError.run();
            }
        }

        private boolean doItForDisk() {
            int currentPartition = 0;
            boolean allPartitionsOK = false;
            Collection<String> sourceDevices = partitions.stream().map(x -> driveName + x).collect(
                    Collectors.toList());
            for (String partitionIndex : partitions) {
                currentPartition++;
                Path sourceFolder = entry.getValue().get(partitionIndex);

                final String sourceDevice = driveName + partitionIndex;

                InteractiveModeStatus.CopyWorkerStatus statusMsg =
                        InteractiveModeStatus.CopyWorkerStatus.builder()
                                .sourceDevice(sourceDevice)
                                .operation("Initial preparation")
                                .operationArguments(sourceFolder.toFile().getPath())
                                .build();
                log.info("Adding a worker {}", sourceDevice);
                model.getCurrentWorkers().put(sourceDevice, statusMsg);
                Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
                String opDisplayName =
                        opsMode == OperationMode.MOVE ? "Moving" :
                        opsMode == OperationMode.DELETE ? "Deleting" :
                        "Archiving";

                statusMsg = InteractiveModeStatus.CopyWorkerStatus.builder()
                        .sourceDevice(sourceDevice)
                        .operation(opDisplayName)
                        .operationArguments(sourceFolder.toFile().getPath())
                        .build();
                model.getCurrentWorkers().put(sourceDevice, statusMsg);
                boolean lastPartition = currentPartition == partitions.size();
                allPartitionsOK =
                        doItForPartition(partitionIndex, lastPartition, sourceDevice, sourceDevices, sourceFolder,
                                         statusMsg);
                if (!allPartitionsOK) {
                    break;
                }
            }
            currentPartition = 0;
            for (String partitionIndex : partitions) {
                currentPartition++;
                Path sourceFolder = entry.getValue().get(partitionIndex);

                final String sourceDevice = driveName + partitionIndex;

                InteractiveModeStatus.CopyWorkerStatus statusMsg =
                        InteractiveModeStatus.CopyWorkerStatus.builder()
                                .sourceDevice(sourceDevice)
                                .operation("Finalization")
                                .operationArguments(sourceFolder.toFile().getPath())
                                .build();
                model.getCurrentWorkers().put(sourceDevice, statusMsg);
                Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
                model.getCurrentWorkers().put(sourceDevice, statusMsg);
                boolean lastPartition = currentPartition == partitions.size();
                try {
                    allPartitionsOK =
                            finalizeItForPartition(partitionIndex, lastPartition, sourceDevice, sourceDevices, sourceFolder,
                                             statusMsg);
                    if (!allPartitionsOK) {
                        break;
                    }
                } finally {
                    log.info("Removing a worker {}", sourceDevice);
                    model.getCurrentWorkers().remove(sourceDevice);
                }
            }
            if (allPartitionsOK) {
                model.getSuccessfulDiskCommand().incrementAndGet();
            }
            return allPartitionsOK;
        }

        private boolean doItForPartition(String partitionIndex, boolean lastPartition, String currentSourceDevice,
                                         Collection<String> allSourceDevices,
                                         Path sourceFolder,
                                         InteractiveModeStatus.CopyWorkerStatus statusMsg) {
            log.info("Copying for partition {} {} "+partitionIndex, lastPartition);
            String masterFileId = masterFile.getName()
                    .replaceAll("\\.\\w+", "");
            String masterFileIdWithPartition = masterFileId + "/P" + partitionIndex;
            if (opsMode == OperationMode.MOVE || opsMode == OperationMode.ARCHIVE) {
                for (String baseFolder : model.getBaseFolders()) {
                    String destinationFolder =
                            findDestinationFolderForPartition(baseFolder, masterFile.getPath(), partitionIndex);
                    try {
                        File destination = new File(destinationFolder);
                        if (destination.exists()) {
                            destinationFolder = destinationFolder + "/copy-" + UUID.randomUUID();
                            log.warn("Destination copy exists already, creating copy {} ", destinationFolder);
                            destination = new File(destinationFolder);
                        }
                        File finalDestination = destination;
                        RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
                                .handle(IOException.class)
                                .withDelay(Duration.ofSeconds(10))
                                .withJitter(0.50)
                                .withMaxRetries(4);
                        Failsafe.with(retryPolicy).run(() -> helper.copyFolders(statusMsg, statusMsg.getSourceDevice(),
                                                                                sourceFolder.toFile(),
                                                                                finalDestination, copyFilter));
                        int n = finalDestination.listFiles().length;
                        if (!model.getErrorId().contains(masterFileIdWithPartition)) {
                            model.addSuccessfulId(masterFileIdWithPartition+"/"+n+"");
                        }

                    } catch (RuntimeException e) {
                        String message = e.getMessage();
                        if (e.getCause() != null) {
                            message = e.getCause().getMessage();
                        }
                        String error =
                                "Cannot copy " + sourceFolder + "to " + destinationFolder + " exception: "
                                + message;
                        model.addError(error);
                        model.addErrorId(masterFileIdWithPartition);
                        log.error(message, e);
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean finalizeItForPartition(String partitionIndex, boolean lastPartition, String currentSourceDevice,
                                         Collection<String> allSourceDevices,
                                         Path sourceFolder,
                                         InteractiveModeStatus.CopyWorkerStatus statusMsg) {
            log.info("Finalizing for partition {} {} "+partitionIndex, lastPartition);
            String masterFileId = masterFile.getName()
                    .replaceAll("\\.\\w+", "");
            String masterFileIdWithPartition = masterFileId + "/P" + partitionIndex;

            if (opsMode == OperationMode.MOVE || opsMode == OperationMode.DELETE) {
                if (opsMode == OperationMode.MOVE) {
                    Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
                }
                try {
                    RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
                            .handle(IOException.class)
                            .withDelay(Duration.ofSeconds(10))
                            .withJitter(0.50)
                            .withMaxRetries(4);
                    Failsafe.with(retryPolicy)
                            .run(() -> helper.zapFolders(statusMsg, sourceFolder.toFile().getAbsolutePath()));

                } catch (RuntimeException e) {
                    String message = e.getMessage();
                    if (e.getCause() != null) {
                        message = e.getCause().getMessage();
                    }
                    String error =
                            "Cannot process " + sourceFolder.toFile().getPath() + " exception: "
                            + message;
                    model.addError(error);
                    model.addErrorId(masterFileIdWithPartition);
                    log.error(message, e);
                    return false;
                }
            }
            model.getSuccessfulPartitionCommand().incrementAndGet();

            if (lastPartition) {
                try {
                    Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
                    shellCommandsHelper.unmountPartition(statusMsg,
                                                         allSourceDevices.toArray(new String[allSourceDevices.size()]));

                } catch (RuntimeException | IOException e) {
                    String error = "Cannot unmount " + currentSourceDevice + " exception: " + e.getMessage();
                    model.addError(error);
                    model.addErrorId(masterFileIdWithPartition);
                    log.error(e.getMessage(), e);
                    return false;
                }
            }

            return true;
        }
    }

    class UnmountWorker implements Runnable {


        private final String driveName;
        private final Collection<String> partitions;

        private final Runnable onDone;
        private final Runnable onError;
        private Collection<Path> folders;
        private String masterKey;

        UnmountWorker(Map.Entry<String, Map<String, Path>> entry, Collection<String> partitions,
                      Runnable onDone, Runnable onError, Collection<Path> folders, String masterKey) {
            this.driveName = entry.getKey();
            this.partitions = partitions;
            this.onDone = onDone;
            this.onError = onError;
            this.folders = folders;
            this.masterKey = masterKey;
        }

        @Override
        public void run() {

            log.info("Starting executor for disk " + driveName);
            Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);
            boolean emptyFolders = existsAndHasOnlyEmptyFolders(folders, masterKey);
            if (!emptyFolders) {
                log.info("Executor for disk won't be started, circumstances changed " + driveName);
            }
            try {
                boolean success = doItForDisk();
                if (success) {
                    onDone.run();
                } else {
                    onError.run();
                }
            } catch (Throwable t) {
                onError.run();
            }
        }

        private boolean doItForDisk() {

            Collection<String> allSourceDevices = partitions.stream().map(x -> driveName + x).collect(
                    Collectors.toList());

            try {
                Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
                shellCommandsHelper.unmountPartition(null,
                                                     allSourceDevices.toArray(new String[allSourceDevices.size()]));
            } catch (RuntimeException | IOException e) {
                log.error(e.getMessage(), e);
                return false;
            }
            return true;
        }
    }
}
