package com.github.mu.tools.interactive.controllers;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import com.github.mu.tools.interactive.model.InteractiveModeStatus;
import com.google.common.util.concurrent.Uninterruptibles;

@Component
public class FoldersOpHelper {

    public void copyFolders(InteractiveModeStatus.CopyWorkerStatus model, String deviceName,
                            File source, File destination, String copyFilter) throws IOException {
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
        File[] files = source.listFiles();
        for (File f: files) {
            String name = f.getName();
            model.setOperationArguments(name);
            model.setOperation("Deleting file");
            FileUtils.forceDelete(f);
        }
    }


}
