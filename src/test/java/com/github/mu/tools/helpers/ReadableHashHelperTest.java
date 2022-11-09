package com.github.mu.tools.helpers;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class ReadableHashHelperTest {

    @Test
    void getReadableHash() {
        ReadableHashHelper helper = new ReadableHashHelper();
        byte[] bytes = "HelloWorld".getBytes(StandardCharsets.UTF_8);
        String humanReadableHash = helper.getReadableHash(bytes);
        assertEquals("Q4XE4U-GOTGIN-RMCBGM-GEPSO5-2EN6Y2-2QHLUT-Q2UZ3K-CYJ2N3-CLCA", humanReadableHash);
    }
}