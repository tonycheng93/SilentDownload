package com.sky.silentdownload.silentupgrade.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.sky.silentdownload.App;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Created by tonycheng on 2017/3/8.
 */

public class FirstStart {

    /**
     * FIRST_START:应用第一次启动,不考虑版本和日期. FIRST_TODAY:应用今天第一次启动,不考虑版本. FIRST_VERSION:当前版本第一次启动.
     */
    public enum ACTION {
        FIRST_START, FIRST_TODAY, FIRST_VERSION
    }

    /**
     * 本应用启动时进行标志,用来判断是否第一次启动,
     *
     * @param app_key 应用标志:SERVICE, APPSTORE, LAUNCHER, GAMECENTER.
     */
    public synchronized static void markFirstStartFlag(Context c, String app_key) {
        //在SharedPreferences中记录应用启动.
        Context mContext = c;
        SharedPreferences sp = mContext.getSharedPreferences("isFirstStart", Activity.MODE_APPEND);
        SharedPreferences.Editor editor = sp.edit();
        Long timeStamp = System.currentTimeMillis();

        //记录在editor的<key,value>中,key是app_key,value为当前时间戳@versionCode.
//        editor.putString(app_key, timeStamp.toString() + "@" + CoocaaApplication.getVersionCode());
        editor.putString(app_key, timeStamp.toString() + "@" + App.getVersionCode());
        editor.apply();
    }

    /**
     * 判断本应用是否是某条件下的第一次启动.
     *
     * @param app_key 应用标志,自己设.
     * @param action  条件标志:FIRST_START, FIRST_TODAY, FIRST_VERSION.
     * @return 是第一次启动返回true, 否则返回false.
     */
    public static boolean isFirstStart(Context c, String app_key, ACTION action) {
        Context mContext = c;
        //从SharedPreferences中读取应用启动记录.
        SharedPreferences sp = mContext.getSharedPreferences("isFirstStart", Activity.MODE_APPEND);
        if (sp == null) {
            Log.i("xzx", "sp == null");
            return false;
        }
        Map<String, ?> allRecords;

        //读取isFirstStart.xml中所有记录.
        try {
            allRecords = sp.getAll();
        } catch (NullPointerException e) {
            Log.e("xzx exception", e.toString());
            return false;
        }

        switch (action) {
            case FIRST_START:
                //应用是否第一次启动,只需判断<Key,value>中的key是否含有app_key.
                //如果已经含有app_key,则本次不是第一次启动.返回false.
                for (Map.Entry<String, ?> entry : allRecords.entrySet()) {
                    if (entry.getKey().contains(app_key)) return false;
                }
                break;

            case FIRST_TODAY:
                //应用是否今天第一次启动,需判断<key,value>中的key是否含有app_key,
                //并且value是否为今天日期
                //如果找到一条app_key的启动时间为今天,则本次启动不是今天第一次,返回false.
                for (Map.Entry<String, ?> entry : allRecords.entrySet()) {
                    if (entry.getKey().contains(app_key)) {
                        //取出时间戳
                        String timeStamp = entry.getValue().toString().split("@")[0];
//                        String version = entry.getValue().toString().split("@")[1];
                        //今天日期格式化
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        String today = sdf.format(new Date());

                        //value中日期格式化
                        //value中记录的时间戳从String转为Long转为date,然后用sdf格式化
                        Long previousMs = Long.parseLong(timeStamp);
                        Date previousDate = new Date(previousMs);
                        String previous = sdf.format(previousDate);

                        //对比日期
                        if (previous.equals(today)) {
                            return false;
                        }
                    }
                }
                break;

            case FIRST_VERSION:
                //应用当前版本是否第一次启动,需判断<key,value>中的key是否含有app_key,
                //并且对比value中的版本号与当前版本号,如果找到含有app_key且版本号是当前版本号,
                //则当前版本不是第一次启动,返回false.
                for (Map.Entry<String, ?> entry : allRecords.entrySet()) {
                    if (entry.getKey().contains(app_key)) {
                        String currentVersion = String.valueOf(App.getVersionCode());
                        if (entry.getValue().toString().contains(currentVersion)) {
                            return false;
                        }
                    }
                }
                break;
        }
        //所有条件判断完后,仍没有找到启动记录,则本次为第一次启动,返回true.
        return true;
    }
}
