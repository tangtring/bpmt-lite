package com.riversoft.api.auth;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.riversoft.api.http.ApiException;

public final class HmacSignature {

    private static final String UTF_8 = "UTF-8";

    private HmacSignature() {
    }

    public static String canonical(String method, String path, String query, String timestamp, String nonce, String bodyHash) {
        return safe(method) + "\n"
                + safe(path) + "\n"
                + safe(query) + "\n"
                + safe(timestamp) + "\n"
                + safe(nonce) + "\n"
                + safe(bodyHash);
    }

    public static String sign(String secret, String canonical) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(safe(secret).getBytes(UTF_8), "HmacSHA256"));
            return toHex(mac.doFinal(safe(canonical).getBytes(UTF_8)));
        } catch (Exception e) {
            throw new ApiException(500, "SIGNATURE_FAILED", "签名计算失败。");
        }
    }

    public static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(bytes == null ? new byte[0] : bytes));
        } catch (Exception e) {
            throw new ApiException(500, "BODY_HASH_FAILED", "请求 body 哈希计算失败。");
        }
    }

    public static String normalizeQuery(Map<String, String[]> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        List<String[]> pairs = new ArrayList<String[]>();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();
            if (values == null || values.length == 0) {
                pairs.add(new String[] { key, "" });
            } else {
                for (String value : values) {
                    pairs.add(new String[] { key, value == null ? "" : value });
                }
            }
        }

        Collections.sort(pairs, new Comparator<String[]>() {
            public int compare(String[] left, String[] right) {
                int key = safe(left[0]).compareTo(safe(right[0]));
                if (key != 0) {
                    return key;
                }
                return safe(left[1]).compareTo(safe(right[1]));
            }
        });

        StringBuilder builder = new StringBuilder();
        for (String[] pair : pairs) {
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(encode(pair[0])).append('=').append(encode(pair[1]));
        }
        return builder.toString();
    }

    public static boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = getBytes(left);
        byte[] rightBytes = getBytes(right);
        int result = leftBytes.length ^ rightBytes.length;
        int max = Math.max(leftBytes.length, rightBytes.length);
        for (int i = 0; i < max; i++) {
            byte leftByte = i < leftBytes.length ? leftBytes[i] : 0;
            byte rightByte = i < rightBytes.length ? rightBytes[i] : 0;
            result |= leftByte ^ rightByte;
        }
        return result == 0;
    }

    private static byte[] getBytes(String value) {
        try {
            return safe(value).getBytes(UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new ApiException(500, "SIGNATURE_FAILED", "签名计算失败。");
        }
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(safe(value), UTF_8).replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new ApiException(500, "SIGNATURE_FAILED", "签名计算失败。");
        }
    }

    private static String toHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        char[] hex = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xff;
            chars[i * 2] = hex[value >>> 4];
            chars[i * 2 + 1] = hex[value & 0x0f];
        }
        return new String(chars);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
