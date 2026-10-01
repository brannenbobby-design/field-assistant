package com.brannenservices.phonecontrol;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.util.DisplayMetrics;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.*;

public class PhoneControlService extends AccessibilityService {
 private static PhoneControlService self;
 private static final Map<String,String> APPS=new HashMap<>();
 static {
  APPS.put("gallery","com.sec.android.gallery3d"); APPS.put("photos","com.google.android.apps.photos");
  APPS.put("square","com.squareup"); APPS.put("facebook","com.facebook.katana");
  APPS.put("messenger","com.facebook.orca"); APPS.put("youtube","com.google.android.youtube");
  APPS.put("maps","com.google.android.apps.maps"); APPS.put("chrome","com.android.chrome");
  APPS.put("settings","com.android.settings"); APPS.put("camera","com.sec.android.app.camera");
 }
 public static PhoneControlService instance(){return self;}
 @Override protected void onServiceConnected(){self=this;}
 @Override public void onDestroy(){if(self==this)self=null;super.onDestroy();}
 @Override public void onAccessibilityEvent(AccessibilityEvent e){}
 @Override public void onInterrupt(){}

 public static boolean runCommand(String command,String arg){
  PhoneControlService s=self;if(s==null||!ControlPrefs.enabled(s))return false;
  switch(command){
   case "HOME":return s.performGlobalAction(GLOBAL_ACTION_HOME); case "BACK":return s.performGlobalAction(GLOBAL_ACTION_BACK);
   case "RECENTS":return s.performGlobalAction(GLOBAL_ACTION_RECENTS); case "NOTIFICATIONS":return s.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
   case "OPEN_APP":return s.openApp(arg); case "OPEN_NAMED_APP":return s.openNamedApp(arg); case "TAP_TEXT":return s.tapText(arg);
   case "SWIPE_UP":return s.swipe(.50f,.78f,.50f,.28f,420); case "SWIPE_DOWN":return s.swipe(.50f,.28f,.50f,.78f,420);
   case "SWIPE_LEFT":return s.swipe(.82f,.50f,.18f,.50f,420); case "SWIPE_RIGHT":return s.swipe(.18f,.50f,.82f,.50f,420); default:return false;
  }
 }
 private boolean openNamedApp(String name){
  if(name==null)return false;String n=name.toLowerCase(Locale.US).trim();String pkg=APPS.get(n);
  if(pkg!=null&&openApp(pkg))return true;
  PackageManager pm=getPackageManager();
  Intent launcher=new Intent(Intent.ACTION_MAIN);launcher.addCategory(Intent.CATEGORY_LAUNCHER);for(android.content.pm.ResolveInfo ri:pm.queryIntentActivities(launcher,0)){CharSequence l=ri.loadLabel(pm);if(l!=null&&l.toString().equalsIgnoreCase(name.trim())){Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);i.setClassName(ri.activityInfo.packageName,ri.activityInfo.name);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);return true;}}
  return false;
 }
 private boolean openApp(String pkg){if(pkg==null||pkg.isEmpty())return false;Intent i=getPackageManager().getLaunchIntentForPackage(pkg);if(i==null)return false;i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);return true;}
 private boolean tapText(String text){if(text==null||text.trim().isEmpty())return false;AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return false;List<AccessibilityNodeInfo> nodes=root.findAccessibilityNodeInfosByText(text.trim());if(nodes==null)return false;for(AccessibilityNodeInfo n:nodes){AccessibilityNodeInfo c=n;while(c!=null){if(c.isClickable())return c.performAction(AccessibilityNodeInfo.ACTION_CLICK);c=c.getParent();}}return false;}
 public static String readScreen(){PhoneControlService s=self;if(s==null||!ControlPrefs.enabled(s))return "";AccessibilityNodeInfo root=s.getRootInActiveWindow();if(root==null)return "";LinkedHashSet<String> out=new LinkedHashSet<>();s.collect(root,out,0);StringBuilder b=new StringBuilder();for(String x:out){if(b.length()+x.length()>2500)break;if(b.length()>0)b.append(" | ");b.append(x);}return b.toString();}
 private void collect(AccessibilityNodeInfo n,LinkedHashSet<String> out,int depth){if(n==null||depth>18)return;CharSequence t=n.getText(),d=n.getContentDescription();if(t!=null&&!t.toString().trim().isEmpty())out.add(t.toString().trim());if(d!=null&&!d.toString().trim().isEmpty())out.add(d.toString().trim());for(int i=0;i<n.getChildCount();i++)collect(n.getChild(i),out,depth+1);}
 private boolean swipe(float sx,float sy,float ex,float ey,long duration){DisplayMetrics d=getResources().getDisplayMetrics();Path p=new Path();p.moveTo(d.widthPixels*sx,d.heightPixels*sy);p.lineTo(d.widthPixels*ex,d.heightPixels*ey);GestureDescription g=new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(p,0,duration)).build();return dispatchGesture(g,null,null);}
}
