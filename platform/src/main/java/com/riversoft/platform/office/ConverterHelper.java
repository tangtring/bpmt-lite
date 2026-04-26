package com.riversoft.platform.office;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Office conversion is disabled in bpmt-lite default distribution.
 */
public class ConverterHelper {
    private static final Logger logger = LoggerFactory.getLogger(ConverterHelper.class);

    private ConverterHelper() {
    }

    public static boolean touch() {
        return false;
    }

    public static void reset() {
        logger.info("Office conversion is disabled in bpmt-lite default distribution.");
    }

    public static boolean convert(InputStream in, String inputPixel, OutputStream out, String outPixel) {
        logger.warn("Office conversion is disabled: {} -> {}", inputPixel, outPixel);
        return false;
    }

    public static boolean convert(File in, File out) {
        logger.warn("Office conversion is disabled: {} -> {}", in, out);
        return false;
    }
}
