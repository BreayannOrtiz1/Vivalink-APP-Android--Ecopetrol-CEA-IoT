package com.vivalnk.sdk.demo.base.custom;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * Created by JakeMo on 18-8-9.
 */
public class BackProgressDialog extends ProgressDialog {

  public BackProgressDialog(Context context) {
    super(context);
  }

  public BackProgressDialog(Context context, int theme) {
    super(context, theme);
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
    if (isShowing()) {
      dismiss();
    }
  }
}
