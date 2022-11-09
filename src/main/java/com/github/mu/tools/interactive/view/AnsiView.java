package com.github.mu.tools.interactive.view;

import static com.github.mu.tools.interactive.view.AnsiConstants.ANSI_RESET;
import static com.github.mu.tools.interactive.view.AnsiConstants.BACKGROUND_BLUE;
import static com.github.mu.tools.interactive.view.AnsiConstants.BACKGROUND_WHITE;
import static com.github.mu.tools.interactive.view.AnsiConstants.BLACK;
import static com.github.mu.tools.interactive.view.AnsiConstants.BLUE;
import static com.github.mu.tools.interactive.view.AnsiConstants.CLS;
import static com.github.mu.tools.interactive.view.AnsiConstants.CYAN;
import static com.github.mu.tools.interactive.view.AnsiConstants.GREEN;
import static com.github.mu.tools.interactive.view.AnsiConstants.HIDE_CURSOR;
import static com.github.mu.tools.interactive.view.AnsiConstants.RED;
import static com.github.mu.tools.interactive.view.AnsiConstants.SCREEN_START;
import static com.github.mu.tools.interactive.view.AnsiConstants.SHOW_CURSOR;
import static com.github.mu.tools.interactive.view.AnsiConstants.WHITE;
import static com.github.mu.tools.interactive.view.AnsiConstants.YELLOW;

import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.github.mu.tools.interactive.model.InteractiveModeStatus;

@Component
public class AnsiView implements Runnable {

    private final InteractiveModeStatus model;

    public AnsiView(InteractiveModeStatus model) {
        this.model = model;
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
        System.out.println("Interactive mode - just plug USB media and the "+BLUE+model.getOperationTypeDisplayName()+BLACK+" will be performed for you.");
        System.out.println("Once the work is done, the media will be unmounted automatically so you can change it.  ");
        System.out.println(ANSI_RESET);

        System.out.println(YELLOW + "Destination: " + WHITE + StringUtils.collectionToCommaDelimitedString(model.getBaseFolders()) + ANSI_RESET);

        long millis = System.currentTimeMillis() - model.getStartTimeMillis();
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) -
                       TimeUnit.MINUTES.toSeconds(minutes);
        String time = String.format("%d min, %d sec", minutes, seconds);
        System.out.println(YELLOW + "Time since the start: " + WHITE + time + ANSI_RESET);

        System.out.println(
                YELLOW + "Number of successful partitions: " + WHITE + model.getSuccessfulPartitionCommand() +
                YELLOW);


        if (!model.getSuccessfulId().isEmpty()) {
            System.out.println();
            System.out.print("Last successful partitions: ");
            boolean first = true;
            for (String id : model.getSuccessfulId()) {
                if (first) {
                    first = false;
                } else {
                    System.out.print(YELLOW + ", ");
                }
                System.out.print(WHITE + id);

            }
        }
        if (!model.getErrorId().isEmpty()) {
            System.out.println();
            System.out.print("Last problematic partitions: ");
            boolean first = true;
            for (String id : model.getErrorId()) {
                if (first) {
                    first = false;
                } else {
                    System.out.print(YELLOW + ", ");
                }
                System.out.print(RED + id);

            }

        }
        System.out.println();
        System.out.println(
                YELLOW + "Ready USBs: " + WHITE + model.getSuccessfulDiskCommand() + ANSI_RESET);
        System.out.println();

        System.out.print(BACKGROUND_BLUE);

        TreeSet<String> keys = new TreeSet<>();
        keys.addAll(model.getCurrentWorkers().keySet());
        for (String key : keys) {
            InteractiveModeStatus.CopyWorkerStatus value = model.getCurrentWorkers().get(key);
            if (value != null) {
                System.out.println(
                        BACKGROUND_BLUE + YELLOW + "Device: " + WHITE + value.getSourceDevice() + " " + YELLOW
                        + "Operation " + WHITE
                        + value.getOperation() + " " + GREEN + value.getOperationArguments());
            }
        }
        if (!CollectionUtils.isEmpty(model.getErrors())) {
            System.out.println(BACKGROUND_WHITE + RED);
            for (String r : model.getErrors()) {
                System.out.println(r + "\n");
            }
        }
        boolean activeTasksPresent = !model.getCurrentWorkers().isEmpty();
        System.out.println();
        if (activeTasksPresent) {
            System.out.println(BACKGROUND_WHITE + BLACK + "Active tasks presents -" + RED
                               + " do not remove any media at this moment!                         " + ANSI_RESET);
        } else {
            System.out.println(BACKGROUND_WHITE + BLACK
                               + "No active tasks.                                                                       "
                               + ANSI_RESET);
        }

        System.out.print(BACKGROUND_WHITE + BLACK);
        System.out.println();
        System.out.println("Press ENTER once you are done with all USB!                                             ");
        System.out.println(ANSI_RESET);
        System.out.println(ANSI_RESET);
    }
}
