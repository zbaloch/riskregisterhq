package com.riskregister.riskregisterapp.utils;

import org.springframework.stereotype.Component;

@Component
public class NameUtils {

    /**
     * Formats a full name to abbreviated form: "First L."
     * e.g. "Zaheer Baloch" → "Zaheer B."
     * Returns the original value if it has no space or is blank.
     */
    public String shortName(String fullName) {
        if (fullName == null || fullName.isBlank()) return fullName;
        int space = fullName.indexOf(' ');
        if (space < 0 || space == fullName.length() - 1) return fullName;
        return fullName.substring(0, space) + " " + fullName.charAt(space + 1) + ".";
    }
}
