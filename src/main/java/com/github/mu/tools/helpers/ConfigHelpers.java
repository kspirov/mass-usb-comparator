package com.github.mu.tools.helpers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;

@Component
public class ConfigHelpers {

    private final String masterPartition;
    private final String partitionBases;
    private final String masterFileStart;
    private final String masterFileEnd;
    private final String masterFileFilter;
    private final String copyFilter;

    public ConfigHelpers(@Value("${master.partition}") String masterPartition,
                         @Value("${partition.bases}") String partitionBases,
                         @Value("${master.file.start}") String masterFileStart,
                         @Value("${master.file.end}") String masterFileEnd,
                         @Value("${master.file.filter}") String masterFileFilter,
                         @Value("${copy.filter}") String copyFilter) {
        this.masterPartition = masterPartition;
        this.partitionBases = partitionBases;
        this.masterFileStart = masterFileStart;
        this.masterFileEnd = masterFileEnd;
        this.masterFileFilter = masterFileFilter;
        this.copyFilter = copyFilter;
    }

    public File findMasterFile(File folder) {
        List<File> res = findMasterFiles(folder, true);
        if (res.isEmpty()) {
            return null;
        }
        return res.get(0);
    }

    public List<File> findMasterFiles(File folder, boolean onlyFirst) {
        List<File> result = new ArrayList<>();
        LinkedList<File> foldersToScan = new LinkedList();
        foldersToScan.add(folder);
        while (!foldersToScan.isEmpty()) {
            File f = foldersToScan.removeFirst();
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) {
                    if (c.isDirectory()) {
                        foldersToScan.addFirst(c);
                    } else {
                        String name = c.getName();
                        if (isMasterFileName(name)) {
                            result.add(c.getAbsoluteFile());
                            if (onlyFirst) {
                                return result;
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    @VisibleForTesting
    boolean isMasterFileName(String name) {
        if (name.startsWith(masterFileStart) && name.endsWith(masterFileEnd)) {
            String[] filters = masterFileFilter.split(",");
            for (String filter : filters) {
                if (name.contains(filter.trim())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }


}
