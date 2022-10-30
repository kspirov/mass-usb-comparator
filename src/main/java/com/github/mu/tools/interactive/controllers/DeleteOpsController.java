package com.github.mu.tools.interactive.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.github.mu.tools.helpers.CommonShellCommandsHelper;
import com.github.mu.tools.helpers.ConfigHelpers;
import com.github.mu.tools.interactive.OperationMode;
import com.github.mu.tools.interactive.model.InteractiveModeStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DeleteOpsController extends AbstractFileOpsController {


    public DeleteOpsController(InteractiveModeStatus model,
                               FoldersOpHelper copyFoldersHelper,
                               ConfigHelpers configHelpers,
                               CommonShellCommandsHelper shellCommandsHelper,
                               @Value("${master.partition}") String masterPartition,
                               @Value("${partition.bases}") String partitionBases,
                               @Value("${copy.filter}") String copyFilter) {

        super(OperationMode.DELETE, model, copyFoldersHelper, shellCommandsHelper, configHelpers, masterPartition,
              partitionBases, copyFilter);
    }
}
