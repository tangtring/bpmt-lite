package com.riversoft.platform.script.function;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.riversoft.core.script.annotation.ScriptSupport;

/**
 * Created by exizhai on 1/30/2016.
 */
@ScriptSupport("qrcode")
public class QRCodeHelper {

	private static final int DEFAULT_WIDTH = 125;
	private static final int DEFAULT_HEIGHT = 125;
	private static final int DEFAULT_ON_COLOR = MatrixToImageConfig.BLACK;
	private static final int DEFAULT_OFF_COLOR = MatrixToImageConfig.WHITE;

	/**
	 * 默认的二维码
	 * 
	 * @param text
	 * @return
	 */
	public static File file(String text) {
		return from(text).file();
	}

	public static ByteArrayOutputStream stream(String text) {
		return from(text).stream();
	}

	public static String img(String text) {
		return "data:image/png;base64," + Base64.encodeBase64String(stream(text).toByteArray());
	}

	/**
	 * 可以设置长宽
	 * 
	 * @param text
	 * @param width
	 * @param height
	 * @return
	 */
	public static File file(String text, int width, int height) {
		return from(text).withSize(width, height).file();
	}

	public static ByteArrayOutputStream stream(String text, int width, int height) {
		return from(text).withSize(width, height).stream();
	}

	public static String img(String text, int width, int height) {
		return "data:image/png;base64," + Base64.encodeBase64String(stream(text, width, height).toByteArray());
	}

	/**
	 * 可以设置长宽和颜色
	 * 
	 * @param text
	 * @param width
	 * @param height
	 * @param onColor
	 * @param offColor
	 * @return
	 */
	public static File file(String text, int width, int height, int onColor, int offColor) {
		return from(text).withSize(width, height).withColor(onColor, offColor).file();
	}

	public static ByteArrayOutputStream stream(String text, int width, int height, int onColor, int offColor) {
		return from(text).withSize(width, height).withColor(onColor, offColor).stream();
	}

	public static String img(String text, int width, int height, int onColor, int offColor) {
		return "data:image/png;base64," + Base64.encodeBase64String(stream(text, width, height, onColor, offColor).toByteArray());
	}

	/**
	 * 也可以使用链式API
	 *
	 * @param text
	 * @return
	 */
	public static Builder from(String text) {
		return new Builder(text);
	}

	public static class Builder {
		private final String text;
		private int width = DEFAULT_WIDTH;
		private int height = DEFAULT_HEIGHT;
		private int onColor = DEFAULT_ON_COLOR;
		private int offColor = DEFAULT_OFF_COLOR;

		private Builder(String text) {
			this.text = text;
		}

		public Builder withSize(int width, int height) {
			this.width = width;
			this.height = height;
			return this;
		}

		public Builder withColor(int onColor, int offColor) {
			this.onColor = onColor;
			this.offColor = offColor;
			return this;
		}

		public File file() {
			File file;
			try {
				file = File.createTempFile("QRCode", ".png");
				FileOutputStream outputStream = new FileOutputStream(file);
				try {
					stream().writeTo(outputStream);
				} finally {
					outputStream.close();
				}
				return file;
			} catch (IOException e) {
				throw new IllegalStateException("生成二维码文件失败", e);
			}
		}

		public ByteArrayOutputStream stream() {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			try {
				MatrixToImageWriter.writeToStream(createMatrix(), "PNG", outputStream, new MatrixToImageConfig(onColor, offColor));
				return outputStream;
			} catch (IOException | WriterException e) {
				throw new IllegalStateException("生成二维码失败", e);
			}
		}

		private BitMatrix createMatrix() throws WriterException {
			Map<EncodeHintType, Object> hints = new HashMap<>();
			hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
			return new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hints);
		}
	}
}
