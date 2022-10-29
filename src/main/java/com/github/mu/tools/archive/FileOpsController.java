package com.github.mu.tools.archive;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.github.mu.tools.helpers.ConfigHelpers;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FileOpsController extends AbstractFileOpsControlller {

    public FileOpsController(InteractiveModeStatus model,
                             FoldersOpHelper copyFoldersHelper,
                             ConfigHelpers configHelpers,
                             @Value("${master.partition}") String masterPartition,
                             @Value("${partition.bases}") String partitionBases,
                             @Value("${master.file.start}") String masterFileStart,
                             @Value("${master.file.end}") String masterFileEnd,
                             @Value("${master.file.filter}") String masterFileFilter,
                             @Value("${copy.filter}") String copyFilter) {

        super(model, copyFoldersHelper, configHelpers, masterPartition, partitionBases, masterFileStart, masterFileEnd,
              masterFileFilter, copyFilter);
    }

    public void run() {
        ConcurrentHashMap<String, Runnable> usedDevices = new ConcurrentHashMap<>();
        Map<String, Map<String, Path>> mounted;
        Set<String> partitions = getPartitionBases().keySet();
        while (!(mounted = getCurrentMountedDevices()).isEmpty() || model.isInteractiveModeEnabled()) {
            try {
                for (Map.Entry<String, Map<String, Path>> entry : mounted.entrySet()) {
                    String driveName = entry.getKey();
                    Path masterPartitionPath = entry.getValue().get(masterPartition);
                    File masterFile = configHelpers.findMasterFile(masterPartitionPath.toFile());
                    for (String partitionIndex : partitions) {
                        Path sourceFolder = entry.getValue().get(partitionIndex);
                        String destinationFolder = findDestinationFolderForPartition(masterFile.getPath(), partitionIndex);
                        final String sourceDevice = driveName + partitionIndex;

                        if (!usedDevices.contains(sourceDevice)) {
                            InteractiveModeStatus.CopyWorkerStatus statusMsg =
                                    InteractiveModeStatus.CopyWorkerStatus.builder()
                                            .sourceDevice(sourceDevice)
                                            .operation("Copying")
                                            .operationArguments(destinationFolder)
                                            .build();
                            model.getCurrentWorkers().put(sourceDevice, statusMsg);
                            Runnable worker = () -> {
                                try {
                                    helper.copyFolders(statusMsg, statusMsg.getSourceDevice(),
                                                       sourceFolder.toFile().getAbsolutePath(),
                                                       destinationFolder, copyFilter);
                                    model.getSuccessfulPartitionCommand().incrementAndGet();
                                    model.addSuccessfulId(masterFile.getName()
                                                                  .replaceAll("\\.\\w+", ""));
                                } catch (RuntimeException | IOException e) {
                                    String error = "Cannot copy " + sourceFolder + " to " + destinationFolder + " exception: "
                                                   + e.getMessage();
                                    model.addError(error);
                                    log.error(e.getMessage(), e);
                                }
                                try {
                                    unmountDevice(statusMsg, sourceDevice, sourceDevice);
                                    usedDevices.remove(sourceDevice);
                                } catch (RuntimeException | IOException e) {
                                    String error = "Cannot unmount " + sourceDevice + " exception: " + e.getMessage();
                                    model.addError(error);
                                    log.error(e.getMessage(), e);

                                } finally {
                                    model.getCurrentWorkers().remove(sourceDevice);
                                }
                            };
                            usedDevices.put(sourceDevice, worker);
                            workerTaskPool.execute(worker);
                        }
                    }

                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
