package com.brannenservices.phonecontrol;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.util.DisplayMetrics;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class PhoneControlService extends AccessibilityService {
 private static PhoneControlService self;
 public static PhoneControlService instance(){return self;}
 @Override protected void onServiceConnected(){self=this;}
 @Override public void onDestroy(){if(self==this)self=null;super.onDestroy();}
 @Override public void onAccessibilityEvent(AccessibilityEvent e){}
 @Override public void onInterrupt(){}

 public static boolean runCommand(String command,String arg){
   PhoneControlService s=self;
   if(s==null || !ControlPrefs.enabled(s)) return false;
   switch(command){
    case "HOME": return s.performGlobalAction(GLOBAL_ACTION_HOME);
    case "BACK": return s.performGlobalAction(GLOBAL_ACTION_BACK);
    case "RECENTS": return s.performGlobalAction(GLOBAL_ACTION_RECENTS);
    case "NOTIFICATIONS": return s.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
    case "OPEN_APP": return s.openApp(arg);
    case "TAP_TEXT": return s.tapText(arg);
    case "SWIPE_UP": return s.swipe(0.50f,0.78f,0.50f,0.28f,420);
    case "SWIPE_DOWN": return s.swipe(0.50f,0.28f,0.50f,0.78f,420);
    case "SWIPE_LEFT": return s.swipe(0.82f,0.50f,0.18f,0.50f,420);
    case "SWIPE_RIGHT": return s.swipe(0.18f,0.50f,0.82f,0.50f,420);
    default: return false;
   }
 }
 private boolean openApp(String pkg){
   if(pkg==null||pkg.isEmpty()) return false;
   PackageManager pm=getPackageManager(); Intent i=pm.getLaunchIntentForPackage(pkg);
   if(i==null) return false; i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); return true;
 }
 private boolean tapText(String text){
   if(text==null||text.trim().isEmpty()) return false;
   AccessibilityNodeInfo root=getRootInActiveWindow(); if(root==null)return false;
   List<AccessibilityNodeInfo> nodes=root.findAccessibilityNodeInfosByText(text.trim());
   if(nodes==null)return false;
   for(AccessibilityNodeInfo n:nodes){
     AccessibilityNodeInfo cur=n;
     while(cur!=null){
       if(cur.isClickable()) return cur.performAction(AccessibilityNodeInfo.ACTION_CLICK);
       cur=cur.getParent();
     }
   }
   return false;
 }
 private boolean swipe(float sx,float sy,float ex,float ey,long duration){
   DisplayMetrics d=getResources().getDisplayMetrics();
   Path p=new Path(); p.moveTo(d.widthPixels*sx,d.heightPixels*sy); p.lineTo(d.widthPixels*ex,d.heightPixels*ey);
   GestureDescription g=new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(p,0,duration)).build();
   return dispatchGesture(g,null,null);
 }
}