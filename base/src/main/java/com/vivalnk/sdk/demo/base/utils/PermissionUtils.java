package com.vivalnk.sdk.demo.base.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

/**
 * Created by JakeMo on 18-8-1.
 */
public class PermissionUtils {

  public static void gotoAppDetailSettings(Context context) {
    Intent localIntent = new Intent();
    localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    if (Build.VERSION.SDK_INT >= 9) {
      localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
      localIntent.setData(Uri.fromParts("package", context.getPackageName(), null));
    } else if (Build.VERSION.SDK_INT <= 8) {
      localIntent.setAction(Intent.ACTION_VIEW);
      localIntent.setClassName("com.android.settings","com.android.settings.InstalledAppDetails");
      localIntent.putExtra("com.android.settings.ApplicationPkgName", context.getPackageName());
    }
    context.startActivity(localIntent);
  }

}
