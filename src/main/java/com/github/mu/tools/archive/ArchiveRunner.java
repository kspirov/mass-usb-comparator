package com.github.mu.tools.archive;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.github.mu.tools.OptionRunner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ArchiveRunner extends OptionRunner {

    private final AnsiView view;

    private final AbstractFileOpsControlller controller;

    private final InteractiveModeStatus model;
    private final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);


    public ArchiveRunner(InteractiveModeStatus model, AnsiView view, AbstractFileOpsControlller controller) {
        this.view = view;
        this.controller = controller;
        this.model = model;
    }

    @Override
    public boolean accept(String option) {
        return option.equals("archive");
    }


    @Override
    public void run(Map<String, String> optionArguments) {
        String output = optionArguments.get("folder");
        if (!StringUtils.hasText(output)) {
            output = "./archive";
        }
        log.info("Output folder base {} ", output);

        model.setInteractiveModeEnabled(true);
        model.setStartTimeMillis(System.currentTimeMillis());
        model.setBaseFolder(output);

        Future controllerDone = executor.submit(controller);
        Future viewDone = executor.submit(view);

        try {
            System.in.read();
            model.setInteractiveModeEnabled(false); // this should interrupt the process
            controllerDone.get();
            viewDone.get();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.exit(0);
    }

}
