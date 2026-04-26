package com.riversoft.util;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class OfficeUtilsTest {

    @Test
    public void testOfficeConversionDisabled() {
        try {
            OfficeUtils.ppt2jpgs(new File("sample.ppt"));
            Assert.fail("Office conversion should be disabled.");
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage().contains("不支持 Office/PDF 转换"));
        }
    }

    @Test
    public void testPdfConversionDisabled() {
        try {
            OfficeUtils.pdf2jpgs(new File("sample.pdf"));
            Assert.fail("PDF conversion should be disabled.");
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage().contains("不支持 Office/PDF 转换"));
        }
    }
}
