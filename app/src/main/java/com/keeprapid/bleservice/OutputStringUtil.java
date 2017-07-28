package com.keeprapid.bleservice;

import android.text.TextUtils;

import java.util.regex.Pattern;

/**
 * Created by zhou0 on 2017/7/11.
 */

public class OutputStringUtil {

    /**
     * 转换成 16进制字符串
     *
     * @param b 字节
     * @return 字符串
     */
    private static String toHexStr(byte b) {
        String str = Integer.toHexString(0xFF & b);
        if (str.length() == 1)
            str = "0" + str;
        return str.toUpperCase();
    }

    /**
     * 转换成 16进制字符串
     *
     * @param bytes 字节
     * @return 字符串
     */
    public static String toHexString(byte... bytes) {
        if (bytes == null)
            return null;
        StringBuilder sb = new StringBuilder();
        if (bytes.length < 20) {
            sb.append("[");
            for (int i = 0; i < bytes.length; i++) {
                sb.append(toHexStr(bytes[i])).append(",");
            }
            sb.append("]");
        } else {
            sb.append("[");
            for (int i = 0; i < 4; i++) {
                sb.append(toHexStr(bytes[i])).append(",");
            }
            sb.append("...");
            for (int i = bytes.length - 5; i < bytes.length; i++) {
                sb.append(toHexStr(bytes[i])).append(",");
            }
            sb.setLength(sb.length() - 1);
            sb.append("]");
        }
        return sb.toString();
    }

    /**
     * 转换成 16进制字符串数组
     *
     * @param bytes 字节
     * @return 字符串
     */
    public static String[] toHexArray(byte... bytes) {
        if (bytes == null)
            return null;
        String[] arrayHex = new String[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            arrayHex[i] = toHexStr(bytes[i]);
        }
        return arrayHex;
    }

    public static byte[] toCmdArray(String text){
        if(text.length() % 2 == 1)
            text = text + "0";
        byte[] params = new byte[text.length() / 2];
        for (int i = 0; i < text.length(); i++){
            if(i % 2 == 1){
                String cmd = text.substring(i - 1, i) + text.substring(i, i + 1).toUpperCase();
                String reg = "^[0-9A-Z]+$";
                if(Pattern.compile(reg).matcher(cmd).matches())
                    params[i / 2] = (byte) Integer.parseInt(cmd, 16);
            }
        }
        return params;
    }
}
