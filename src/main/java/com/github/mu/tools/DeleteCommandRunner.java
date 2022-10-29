package com.github.mu.tools;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.stereotype.Component;

import com.github.mu.tools.interactive.controllers.DeleteOpsController;
import com.github.mu.tools.interactive.model.InteractiveModeStatus;
import com.github.mu.tools.interactive.view.AnsiView;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DeleteCommandRunner extends AbstractCommandRunner {

    private final AnsiView view;

    private final DeleteOpsController controller;

    private final InteractiveModeStatus model;
    private final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);


    public DeleteCommandRunner(InteractiveModeStatus model, AnsiView view, DeleteOpsController controller) {
        this.view = view;
        this.controller = controller;
        this.model = model;
    }

    @Override
    public boolean accept(String option) {
        return option.equals("delete");
    }


    @Override
    public void run(Map<String, String> optionArguments) {

        model.setInteractiveModeEnabled(true);
        model.setStartTimeMillis(System.currentTimeMillis());
        model.setOperationTypeDisplayName("delete operation");
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
