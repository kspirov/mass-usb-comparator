package com.github.mu.tools.interactive.view;

public interface AnsiConstants {

    String CLS = "\033[H\033[2J";
    String SCREEN_START = "\033[H\033[1;1f";
    String ANSI_RESET = "\u001B[0m";

    String HIDE_CURSOR = "\u001B[?25l";
    String SHOW_CURSOR = "\u001B[?25h";

    String BLACK = "\u001B[30m";
    String RED = "\u001B[31m";
    String GREEN = "\u001B[32m";
    String YELLOW = "\u001B[33m";
    String BLUE = "\u001B[34m";
    String MAGENTA = "\u001B[35m";
    String CYAN = "\u001B[36m";
    String WHITE = "\u001B[37m";

    String BACKGROUND_BLACK = "\u001B[40m";
    String BACKGROUND_RED = "\u001B[41m";
    String BACKGROUND_GREEN = "\u001B[42m";
    String BACKGROUND_YELLOW = "\u001B[43m";
    String BACKGROUND_BLUE = "\u001B[44m";
    String BACKGROUND_MAGENTA = "\u001B[45m";
    String BACKGROUND_CYAN = "\u001B[46m";
    String BACKGROUND_WHITE = "\u001B[47m";
}

