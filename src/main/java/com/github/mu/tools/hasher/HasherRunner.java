package com.github.mu.tools.hasher;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.github.mu.tools.AbstractCommandRunner;
import com.github.mu.tools.helpers.ConfigHelpers;
import com.github.mu.tools.helpers.ReadableHashHelper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class HasherRunner extends AbstractCommandRunner {

    private final ConfigHelpers helpers;
    private final ReadableHashHelper hashHelper;

    public HasherRunner(ConfigHelpers helpers, ReadableHashHelper hashHelper) {
        this.helpers = helpers;
        this.hashHelper = hashHelper;
    }
    @Override
    public boolean accept(String option) {
        return "hasher".equals(option);
    }

    @Override
    public void run(String command, Map<String, String> optionArguments) {
        String input = optionArguments.get("folder");
        if (!StringUtils.hasText(input)) {
            input = "./archive";
        }
        log.info("Input folder base {} ", input);
        String output = optionArguments.get("file");
        if (!StringUtils.hasText(output)) {
            output = "hasher.csv";
        }
        log.info("Output file {} ", output);
        List<File> current = helpers.findMasterFiles(new File(input), false);
        log.info("Master files found: "+current.size());
        System.out.println("Calculating hash for "+current.size()+" files...");
        PrintWriter writer = null;
        TreeMap<String, String> hashes = new TreeMap<>();
        try {
            for (File file : current) {
                String hash = hashHelper.getReadableHash(file);
                String masterName = file.getName();
                String masterKey = masterName.substring(0, masterName.lastIndexOf("."));
                if (hashes.containsKey(masterKey)) {
                    if (hashes.get(masterKey).equals(hash)) {
                        log.warn("The following entry was available more than once, but the hash is the same:  {}", file);
                        System.out.println("WARN: The following entry was available more than once, but the hash is the same: "+file);
                    } else {
                        log.error("ERROR: the following entry was available twice, and the hash is different: {}",file);
                        System.out.println("ERROR: the following entry was available twice, and the hash is different: "+file);
                    }
                } else {
                    hashes.put(masterKey, hash);
                }
            }
            File o = new File(output);
            System.out.println("Hashes calculated, writing to "+o);
            writer = new PrintWriter(new FileWriter(o));
            for (Map.Entry<String, String>  e: hashes.entrySet()) {
                writer.println(e.getKey()+","+e.getValue());
            }
            writer.flush();
            System.out.println("\n Done, bye \n");

        } catch (IOException | SecurityException e) {
            log.error(e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

}
