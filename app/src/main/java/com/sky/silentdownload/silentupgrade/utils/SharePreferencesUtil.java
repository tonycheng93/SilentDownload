package com.sky.silentdownload.silentupgrade.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by BaoCheng on 2017/3/7.
 */

public class SharePreferencesUtil {


    public static void put(Context context, String prefName, String key, long value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(key, value).apply();
    }

    public static long get(Context context, String prefName, String key, long defaultValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        long value = sharedPreferences.getLong(key, defaultValue);
        return value;
    }
}
