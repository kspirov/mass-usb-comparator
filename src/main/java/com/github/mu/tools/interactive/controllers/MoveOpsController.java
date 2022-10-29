package com.github.mu.tools.interactive.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.github.mu.tools.helpers.CommonShellCommandsHelper;
import com.github.mu.tools.interactive.OperationMode;
import com.github.mu.tools.interactive.model.InteractiveModeStatus;
import com.github.mu.tools.helpers.ConfigHelpers;

import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public abstract class MoveOpsController extends AbstractFileOpsController {


    public MoveOpsController(InteractiveModeStatus model,
                             FoldersOpHelper copyFoldersHelper,
                             ConfigHelpers configHelpers,
                             CommonShellCommandsHelper shellCommandsHelper,
                             @Value("${master.partition}") String masterPartition,
                             @Value("${partition.bases}") String partitionBases,
                             @Value("${copy.filter}") String copyFilter) {

        super(OperationMode.MOVE, model, copyFoldersHelper, shellCommandsHelper, configHelpers, masterPartition,
              partitionBases, copyFilter);
    }

}
