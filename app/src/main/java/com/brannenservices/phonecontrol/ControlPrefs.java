package com.brannenservices.phonecontrol;
import android.content.Context;
final class ControlPrefs {
 private static final String P="control", K="enabled";
 static boolean enabled(Context c){return c.getSharedPreferences(P,0).getBoolean(K,false);}
 static void setEnabled(Context c,boolean v){c.getSharedPreferences(P,0).edit().putBoolean(K,v).apply();}
}