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

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class HasherRunner extends AbstractCommandRunner {

    private final ConfigHelpers helpers;

    public HasherRunner(ConfigHelpers helpers) {
        this.helpers = helpers;
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
        TreeMap<String, byte[]> hashes = new TreeMap<>();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            int fileCount = 0;
            for (File file : current) {
                fileCount++;
                byte[] content = FileUtils.readFileToByteArray(file);
                byte[] encodedhash = digest.digest(content);
                hashes.put(file.getName(), encodedhash);
            }
            File o = new File(output);
            System.out.println("Hashes calculated, writing to "+o);
            writer = new PrintWriter(new FileWriter(o));
            Base32 base32 = new Base32();
            for (Map.Entry<String, byte[]>  e: hashes.entrySet()) {
                String base = base32.encodeAsString(e.getValue());
                writer.println(e.getKey()+","+base.substring(0, base.length()-4));
            }
            writer.flush();
            System.out.println("\n Done, bye \n");

        } catch (NoSuchAlgorithmException | IOException | SecurityException e) {
            log.error(e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

}
