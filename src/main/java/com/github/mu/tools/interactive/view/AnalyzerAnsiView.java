package com.github.mu.tools.interactive.view;

import static com.github.mu.tools.interactive.view.AnsiConstants.ANSI_RESET;
import static com.github.mu.tools.interactive.view.AnsiConstants.BACKGROUND_BLUE;
import static com.github.mu.tools.interactive.view.AnsiConstants.BACKGROUND_WHITE;
import static com.github.mu.tools.interactive.view.AnsiConstants.BLACK;
import static com.github.mu.tools.interactive.view.AnsiConstants.BLUE;
import static com.github.mu.tools.interactive.view.AnsiConstants.CLS;
import static com.github.mu.tools.interactive.view.AnsiConstants.GREEN;
import static com.github.mu.tools.interactive.view.AnsiConstants.HIDE_CURSOR;
import static com.github.mu.tools.interactive.view.AnsiConstants.RED;
import static com.github.mu.tools.interactive.view.AnsiConstants.SCREEN_START;
import static com.github.mu.tools.interactive.view.AnsiConstants.SHOW_CURSOR;
import static com.github.mu.tools.interactive.view.AnsiConstants.WHITE;
import static com.github.mu.tools.interactive.view.AnsiConstants.YELLOW;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.github.mu.tools.interactive.model.AnalyzerStatus;
import com.github.mu.tools.interactive.model.InteractiveModeStatus;

@Component
public class AnalyzerAnsiView implements Runnable {

    private final InteractiveModeStatus model;
    private final AnalyzerStatus analyzerModel;

    public AnalyzerAnsiView(InteractiveModeStatus model, AnalyzerStatus analyzerModel) {
        this.model = model;
        this.analyzerModel = analyzerModel;
    }

    @Override
    public void run() {

        System.out.println(CLS + HIDE_CURSOR);
        while (model.isInteractiveModeEnabled()) {
            printModel();
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println(SHOW_CURSOR + "Bye!");
        System.out.println();
    }


    public void printModel() {
        System.out.println(CLS);
        System.out.println(SCREEN_START);

        System.out.print(BACKGROUND_WHITE + BLACK);
        System.out.print("Media name:         Master name:                  Partitions OK:           Is empty:");
        System.out.print(BACKGROUND_WHITE + BLACK);
        System.out.println(ANSI_RESET);

        synchronized (AnalyzerStatus.ANALYZER_MODEL_MUTEX) {
            for (AnalyzerStatus.AnalysedDisk disk : analyzerModel.getAnalysedDiskMap().values()) {
                System.out.print(ANSI_RESET);
                System.out.print(org.apache.commons.lang3.StringUtils.rightPad(disk.getDiskName(), 20));
                System.out.print(org.apache.commons.lang3.StringUtils.rightPad(disk.getMasterName(), 30));
                System.out.print(org.apache.commons.lang3.StringUtils.rightPad("" + disk.isRightPartitionNumber(), 25));
                System.out.print(org.apache.commons.lang3.StringUtils.rightPad("" + disk.isEmpty(), 20));
                System.out.println();
            }
        }

        System.out.println();
        System.out.println();
        System.out.println("Press ENTER once you are done with all USB!                                             ");
        System.out.println(ANSI_RESET);
        System.out.println(ANSI_RESET);
    }
}
