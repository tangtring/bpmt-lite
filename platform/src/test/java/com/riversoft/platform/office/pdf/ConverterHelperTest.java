package com.riversoft.platform.office.pdf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import com.riversoft.platform.office.ConverterHelper;

public class ConverterHelperTest {

    @Test
    public void testTouchReturnsFalseWhenOfficeConversionDisabled() {
        Assert.assertFalse(ConverterHelper.touch());
    }

    @Test
    public void testStreamConvertReturnsFalseWhenOfficeConversionDisabled() {
        Assert.assertFalse(ConverterHelper.convert(new ByteArrayInputStream(new byte[0]), "doc", new ByteArrayOutputStream(), "pdf"));
    }

    @Test
    public void testFileConvertReturnsFalseWhenOfficeConversionDisabled() {
        Assert.assertFalse(ConverterHelper.convert(new File("sample.doc"), new File("sample.pdf")));
    }
}
