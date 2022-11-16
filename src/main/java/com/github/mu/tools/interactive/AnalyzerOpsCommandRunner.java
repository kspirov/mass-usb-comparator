package com.github.mu.tools.interactive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.github.mu.tools.AbstractCommandRunner;
import com.github.mu.tools.helpers.CommonShellCommandsHelper;
import com.github.mu.tools.interactive.controllers.AbstractFileOpsController;
import com.github.mu.tools.interactive.controllers.AnalyzerController;
import com.github.mu.tools.interactive.controllers.ArchiveOpsController;
import com.github.mu.tools.interactive.controllers.DeleteOpsController;
import com.github.mu.tools.interactive.controllers.MoveOpsController;
import com.github.mu.tools.interactive.model.InteractiveModeStatus;
import com.github.mu.tools.interactive.view.AnalyzerAnsiView;
import com.github.mu.tools.interactive.view.AnsiView;
import com.google.common.util.concurrent.Uninterruptibles;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AnalyzerOpsCommandRunner extends AbstractCommandRunner {

    private static final String ANALYZER_OPTION_NAME = "analyzer";

    private final CommonShellCommandsHelper cmdHelper;
    private final AnalyzerAnsiView view;

    private final InteractiveModeStatus model;
    private AnalyzerController controller;
    private final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

    public AnalyzerOpsCommandRunner(InteractiveModeStatus model,
                                    CommonShellCommandsHelper cmdHelper,
                                    AnalyzerAnsiView view,
                                    AnalyzerController controller) {
        this.cmdHelper = cmdHelper;
        this.view = view;
        this.model = model;
        this.controller = controller;
    }

    @Override
    public boolean accept(String option) {
        return option.equals(ANALYZER_OPTION_NAME);
    }


    @Override
    public void run(String command, Map<String, String> optionArguments) {

        Future controllerDone = executor.submit(controller);
        Future viewDone = executor.submit(view);

        model.setInteractiveModeEnabled(true);
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
