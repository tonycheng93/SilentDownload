package com.sky.silentdownload.silentupgrade.utils;

import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * Created by BaoCheng on 2017/2/20.
 */

public class MD5Util {

    private static final String TAG = "MD5Util";

    /**
     * 将字符串进行MD5编码
     */
    public static String md5Encode(String str) {
        String tempStr;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(str.getBytes());
            tempStr = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            tempStr = String.valueOf(str.hashCode());
        }
        return tempStr;
    }

    /**
     * bytes to hex string
     */
    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public static boolean checkMD5(String md5, File updateFile) {
        if (TextUtils.isEmpty(md5) || updateFile == null) {
            Log.e(TAG, "MD5 string empty or updateFile is null");
            return false;
        }

        String calculatedDigest = calculateMD5(updateFile);
        if (calculatedDigest == null) {
            Log.e(TAG, "calculatedDigest is null");
            return false;
        }

        return calculatedDigest.equalsIgnoreCase(md5);
    }

    /**
     * 计算文件MD5值
     *
     * @param updateFile 需要计算MD5值的文件
     * @return 返回生成的MD5
     */
    public static String calculateMD5(File updateFile) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Exception while getting digest", e);
            return null;
        }

        InputStream in;
        try {
            in = new FileInputStream(updateFile);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Exception while getting FileInputStream", e);
            return null;
        }

        byte[] buffer = new byte[8192];
        int len;
        try {
            while ((len = in.read(buffer)) > 0) {
                digest.update(buffer, 0, len);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            //file to 32 chars
            output = String.format("%32s", output).replace(' ', '0');
            return output;
        } catch (IOException e) {
            throw new RuntimeException("Unable to process file for MD5", e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                Log.e(TAG, "Exception on closing MD5 input stream", e);
            }
        }
    }
}
