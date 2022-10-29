package com.github.mu.tools.interactive;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.github.mu.tools.AbstractCommandRunner;
import com.github.mu.tools.interactive.controllers.AbstractFileOpsController;
import com.github.mu.tools.interactive.controllers.ArchiveOpsController;
import com.github.mu.tools.interactive.controllers.DeleteOpsController;
import com.github.mu.tools.interactive.controllers.MoveOpsController;
import com.github.mu.tools.interactive.model.InteractiveModeStatus;
import com.github.mu.tools.interactive.view.AnsiView;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FileOpsCommandRunner extends AbstractCommandRunner {

    private static final String ARCHIVE_OPTION_NAME = "archive";
    private static final String MOVE_OPTION_NAME = "move";
    private static final String DELETE_OPTION_NAME = "delete";
    private final AnsiView view;

    private final ArchiveOpsController archiveOpsController;

    private final DeleteOpsController deleteOpsController;

    private final MoveOpsController moveOpsController;

    private final InteractiveModeStatus model;
    private final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

    public FileOpsCommandRunner(InteractiveModeStatus model, AnsiView view,
                                ArchiveOpsController archiveOpsController,
                                DeleteOpsController deleteOpsController,
                                MoveOpsController moveOpsController) {
        this.view = view;
        this.archiveOpsController = archiveOpsController;
        this.deleteOpsController = deleteOpsController;
        this.moveOpsController = moveOpsController;
        this.model = model;
    }

    @Override
    public boolean accept(String option) {
        return option.equals(ARCHIVE_OPTION_NAME) ||
               option.equals(DELETE_OPTION_NAME) ||
               option.equals(MOVE_OPTION_NAME);
    }


    @Override
    public void run(String command, Map<String, String> optionArguments) {
        String output = optionArguments.get("folder");
        if (!StringUtils.hasText(output)) {
            output = "./archive";
        }
        log.info("Output folder base {} ", output);

        model.setInteractiveModeEnabled(true);
        model.setStartTimeMillis(System.currentTimeMillis());
        model.setBaseFolder(output);

        AbstractFileOpsController opsController =
                command.equals(DELETE_OPTION_NAME)? deleteOpsController:
                command.equals(MOVE_OPTION_NAME)? moveOpsController:
                archiveOpsController;

        Future controllerDone = executor.submit(opsController);
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
