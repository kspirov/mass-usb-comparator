package com.github.mu.tools.interactive.controllers;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import com.github.mu.tools.interactive.model.InteractiveModeStatus;

@Component
public class FoldersOpHelper {

    public void copyFolders(InteractiveModeStatus.CopyWorkerStatus model, String deviceName,
                            String sourcePath, String destinationPath, String copyFilter) throws IOException {
        File source = new File(sourcePath);
        File destination = new File(destinationPath);
        FileFilter apiFilter = (pathname) -> {
            String name = pathname.getName();
            String[] filters = copyFilter.split(",");
            for (String filter : filters) {
                if (name.contains(filter)) {
                    return false;
                }
            }
            model.setOperationArguments(pathname.getPath());
            model.setSourceDevice(deviceName);
            model.setOperation("Copying file");
            return true;
        };

        FileUtils.copyDirectory(source, destination, apiFilter, false);

    }

    public void zapFolders(InteractiveModeStatus.CopyWorkerStatus model,
                            String sourcePath) throws IOException {
        File source = new File(sourcePath);
        FileFilter apiFilter = (pathname) -> {
            String name = pathname.getName();
            model.setOperationArguments(pathname.getPath());
            model.setOperation("Deleting file");
            return true;
        };
        FileUtils.deleteDirectory(source);
    }


}
