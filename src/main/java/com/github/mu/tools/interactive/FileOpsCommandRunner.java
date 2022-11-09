package com.github.mu.tools.interactive;

import java.io.IOException;
import java.sql.Time;
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
import com.github.mu.tools.interactive.controllers.ArchiveOpsController;
import com.github.mu.tools.interactive.controllers.DeleteOpsController;
import com.github.mu.tools.interactive.controllers.MoveOpsController;
import com.github.mu.tools.interactive.model.InteractiveModeStatus;
import com.github.mu.tools.interactive.view.AnsiView;
import com.google.common.util.concurrent.Uninterruptibles;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FileOpsCommandRunner extends AbstractCommandRunner {

    private static final String ARCHIVE_OPTION_NAME = "archive";
    private static final String MOVE_OPTION_NAME = "move";
    private static final String DELETE_OPTION_NAME = "delete";
    private final CommonShellCommandsHelper cmdHelper;
    private final AnsiView view;

    private final ArchiveOpsController archiveOpsController;

    private final DeleteOpsController deleteOpsController;

    private final MoveOpsController moveOpsController;

    private final InteractiveModeStatus model;
    private final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

    public FileOpsCommandRunner(InteractiveModeStatus model,
                                CommonShellCommandsHelper cmdHelper,
                                AnsiView view,
                                ArchiveOpsController archiveOpsController,
                                DeleteOpsController deleteOpsController,
                                MoveOpsController moveOpsController) {
        this.cmdHelper = cmdHelper;
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
        String errors = optionArguments.get("errors");
        int maxErrorSize = 0;
        if (StringUtils.hasText(errors)) {
            maxErrorSize = Integer.parseInt(errors);
        }
        model.setMaxErrorSize(maxErrorSize);
        model.setInteractiveModeEnabled(true);
        model.setStartTimeMillis(System.currentTimeMillis());
        String[] baseFolders = output.split(",");
        ArrayList<String> base = new ArrayList<>();
        for (String b : baseFolders) {
            if (StringUtils.hasText(b)) {
                base.add(b.trim());
            }
        }
        model.setBaseFolders(base);

        AbstractFileOpsController opsController =
                command.equals(DELETE_OPTION_NAME) ? deleteOpsController :
                command.equals(MOVE_OPTION_NAME) ? moveOpsController :
                archiveOpsController;

        try {
            cmdHelper.stopUdisk2Service();
        } catch (IOException e) {
            log.warn(e.getMessage(), e);
        }
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        try {
            cmdHelper.startUdisk2Service();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            System.out.print("Cannot start udisks2 service, system reboot might be needed");
        }
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);

        System.out.println("Ready to start the interactive mode. Please unplug all devices and press [Enter]");
        try {
            System.in.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
