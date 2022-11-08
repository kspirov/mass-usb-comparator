package com.github.mu.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
public class ToolsApplication implements ApplicationRunner {

    @Autowired
    private List<AbstractCommandRunner> runners;

    public static void main(String[] args) {
        SpringApplication.run(ToolsApplication.class, args);
    }

    private AbstractCommandRunner getRunner(String option) {
        if (runners != null) {
            for (AbstractCommandRunner runner : runners) {
                if (runner.accept(option)) {
                    return runner;
                }
            }
        }
        return null;
    }


    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("MASS USB TOOLS, version 1.09");
        System.out.println();
        if (args.getNonOptionArgs().size() != 1) {
            System.err.println("Wrong number of command line arguments - please give a single command");
            System.out.println();
            System.exit(3);
        }
        String argument = args.getNonOptionArgs().get(0);
        AbstractCommandRunner runner = getRunner(argument);
        if (runner == null) {
            System.err.println("Wrong command argument " + argument);
            System.out.println();
            System.exit(3);
        }
        log.info("# NonOptionArgs: {} ", args.getNonOptionArgs().size());

        for (String a: args.getNonOptionArgs()) {
            log.info("Non optional {} ", a);
        }

        log.info("# OptionArgs: {} ", args.getOptionNames().size());

        Map<String, String> optionArguments = new LinkedHashMap<>();
        args.getOptionNames().forEach(optionName -> {
            String value = args.getOptionValues(optionName).get(0);
            optionArguments.put(optionName, value);
            log.info(optionName + "=" + value);
        });
        runner.run(argument, optionArguments);
    }
}
