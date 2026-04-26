package com.riversoft.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Office/PDF conversion helpers are disabled in bpmt-lite default distribution.
 */
public class OfficeUtils {

    private static final String UNSUPPORTED_MESSAGE = "bpmt-lite 默认发行版本不支持 Office/PDF 转换。";

    private OfficeUtils() {
    }

    public static List<File> ppt2jpgs(File ppt) throws IOException {
        throw unsupported();
    }

    public static List<File> ppt2jpgs(File ppt, float scale) throws IOException {
        throw unsupported();
    }

    public static List<File> ppt2jpgs(InputStream ppt, String pptName) throws IOException {
        throw unsupported();
    }

    public static List<File> ppt2jpgs(InputStream ppt, String pptName, float scale) throws IOException {
        throw unsupported();
    }

    public static File ppt2pdf(File ppt) throws IOException {
        throw unsupported();
    }

    public static File ppt2pdf(InputStream ppt, String pptName) throws IOException {
        throw unsupported();
    }

    public static File ppt2html(File ppt) throws IOException {
        throw unsupported();
    }

    public static File ppt2html(InputStream ppt, String pptName) throws IOException {
        throw unsupported();
    }

    public static File word2pdf(File word) throws Exception {
        throw unsupported();
    }

    public static File word2pdf(InputStream word, String wordName) throws Exception {
        throw unsupported();
    }

    public static File word2html(File word) throws Exception {
        throw unsupported();
    }

    public static File word2html(File word, boolean imageBase64) throws Exception {
        throw unsupported();
    }

    public static File word2html(InputStream word, String wordName) throws Exception {
        throw unsupported();
    }

    public static File word2html(InputStream word, String wordName, boolean imageBase64) throws Exception {
        throw unsupported();
    }

    public static File excel2pdf(File excel) throws Exception {
        throw unsupported();
    }

    public static File excel2pdf(InputStream excel, String excelName) throws Exception {
        throw unsupported();
    }

    public static List<File> excel2jpgs(File excel) throws Exception {
        throw unsupported();
    }

    public static List<File> excel2jpgs(InputStream excel, String excelName) throws Exception {
        throw unsupported();
    }

    public static List<File> excel2svgs(File excel) throws Exception {
        throw unsupported();
    }

    public static List<File> excel2svgs(InputStream excel, String excelName) throws Exception {
        throw unsupported();
    }

    public static File excel2html(File excel) throws Exception {
        throw unsupported();
    }

    public static File excel2html(InputStream excel, String excelName) throws Exception {
        throw unsupported();
    }

    public static List<File> pdf2jpgs(File pdf) throws IOException {
        throw unsupported();
    }

    public static List<File> pdf2jpgs(File pdf, float scaling) throws IOException {
        throw unsupported();
    }

    public static List<File> pdf2jpgs(InputStream pdf, String pdfName) throws IOException {
        throw unsupported();
    }

    public static List<File> pdf2jpgs(InputStream pdf, String pdfName, float scaling) throws IOException {
        throw unsupported();
    }

    private static IOException unsupported() {
        return new IOException(UNSUPPORTED_MESSAGE);
    }
}
