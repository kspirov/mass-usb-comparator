/**
 * This specific file is under MIT license (not under the project license). Permission is hereby granted, free of
 * charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished
 * to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.github.mu.tools.helpers;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

/**
 * Calculate a hascode and display it in very human-readable way.
 */
@Component
public class ReadableHashHelper {

    /**
     * Calculates sha256 on given byte array and returns the result in a human-readable form.
     */
    public String getReadableHash(File file) throws IOException {
        byte[] content = FileUtils.readFileToByteArray(file);
        return getReadableHash(content);
    }

    /**
     * Calculates sha256 on given byte array and returns the result in a human-readable form.
     */
    public String getReadableHash(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] encodedhash = digest.digest(content);
            Base32 base32 = new Base32();
            String base = base32.encodeAsString(encodedhash);
            StringBuilder result = new StringBuilder(base);
            while (base.charAt(result.length() - 1) == '=') {
                result.deleteCharAt(result.length() - 1);
            }
            String[] split = result.toString().split("(?<=\\G.{6})"); // lookbehind regexp, split the string on every 6 symbols
            result = new StringBuilder();
            for (int i = 0; i < split.length; i++) {
                if (i > 0) {
                    result.append("-");
                }
                result.append(split[i]);
            }

            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
