/**
 * This code is a subject of copyright protection under GNU AFFERO GENERAL PUBLIC LICENSE. For more details, check
 * licence.txt at the root folder of this project.
 */
package com.github.mu.tools;

import java.util.Map;

public abstract class OptionRunner {

    /**
     * Implements chain of responsibility.
     */
    public abstract boolean accept(String option);

    public abstract void run(Map<String, String> optionArguments);

}
