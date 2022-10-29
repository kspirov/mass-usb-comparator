package com.github.mu.tools.archive;

import static com.github.mu.tools.archive.AnsiConstants.ANSI_RESET;
import static com.github.mu.tools.archive.AnsiConstants.BACKGROUND_BLUE;
import static com.github.mu.tools.archive.AnsiConstants.BACKGROUND_WHITE;
import static com.github.mu.tools.archive.AnsiConstants.BLACK;
import static com.github.mu.tools.archive.AnsiConstants.BLUE;
import static com.github.mu.tools.archive.AnsiConstants.CLS;
import static com.github.mu.tools.archive.AnsiConstants.GREEN;
import static com.github.mu.tools.archive.AnsiConstants.HIDE_CURSOR;
import static com.github.mu.tools.archive.AnsiConstants.RED;
import static com.github.mu.tools.archive.AnsiConstants.SCREEN_START;
import static com.github.mu.tools.archive.AnsiConstants.SHOW_CURSOR;
import static com.github.mu.tools.archive.AnsiConstants.WHITE;
import static com.github.mu.tools.archive.AnsiConstants.YELLOW;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class AnsiView implements Runnable {

    private final InteractiveModeStatus model;

    private int tasksHash;

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
    }


    public void printModel() {
        int h = calculateTasksHash();
        if (tasksHash != h) {
            // task is changed, clear the screen instead of just moving the cursor
            System.out.println(CLS);
            tasksHash = h;
        }
        System.out.println(SCREEN_START);
        System.out.print(BACKGROUND_WHITE + BLACK);
        System.out.println("Interactive mode - just plug USB media and the "+BLUE+model.getOperationTypeDisplayName()+BLACK+" will be performed for you.");
        System.out.println("Once the work is done, the media will be unmounted automatically so you can change it.  ");
        System.out.println(ANSI_RESET);

        System.out.println(YELLOW + "Base folder: " + WHITE + model.getBaseFolder() + ANSI_RESET);
        System.out.println(
                YELLOW + "Number of successful partitions : " + WHITE + model.getSuccessfulPartitionCommand()
                + ANSI_RESET);

        long millis = System.currentTimeMillis() - model.getStartTimeMillis();
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) -
                       TimeUnit.MINUTES.toSeconds(minutes);
        String time = String.format("%d min, %d sec", minutes, seconds);
        System.out.println(YELLOW + "Time since the start: " + WHITE + time + ANSI_RESET);
        System.out.println();


        if (!model.getSuccessfulId().isEmpty()) {
            System.out.print("Last successful partitions: ");
            boolean first = true;
            for (String id : model.getSuccessfulId()) {
                if (first) {
                    first = false;
                } else {
                    System.out.println(YELLOW + ", ");
                }
                System.out.println(WHITE + id + " ");

            }
            System.out.println();
            System.out.println();
        }
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
                               + "No active tasks, all media correctly unmounted! You can remove/change the USB.          "
                               + ANSI_RESET);
        }

        System.out.print(BACKGROUND_WHITE + BLACK);
        System.out.println("");
        System.out.println("Press ENTER once you are done with all USB!                                             ");
        System.out.println(ANSI_RESET);
        System.out.println(ANSI_RESET);
    }

    public int calculateTasksHash() {
        ConcurrentHashMap.KeySetView<String, InteractiveModeStatus.CopyWorkerStatus> keySet =
                model.getCurrentWorkers().keySet();

        if (keySet == null || keySet.isEmpty()) {
            return 0;
        }
        Set set = new TreeSet();
        set.addAll(keySet);
        return set.hashCode();
    }
}
