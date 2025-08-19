package com.vivalnk.sdk.demo.base.app;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;
import com.vivalnk.sdk.Callback;
import com.vivalnk.sdk.CommandRequest;
import com.vivalnk.sdk.VitalClient;
import com.vivalnk.sdk.demo.base.i18n.ErrorMessageHandler;
import com.vivalnk.sdk.demo.base.mvp.IPresenter;
import com.vivalnk.sdk.demo.repository.device.ConnectEvent;
import com.vivalnk.sdk.demo.repository.device.DeviceManager;
import com.vivalnk.sdk.common.eventbus.Subscribe;
import com.vivalnk.sdk.model.Device;
import java.util.Map;

public abstract class ConnectedActivity<P extends IPresenter> extends BaseToolbarActivity<P> {

  protected Device mDevice;

  protected boolean isOTAing;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    toolbar.setNavigationOnClickListener((v) -> finish());
    initDevice();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!VitalClient.getInstance().isConnected(mDevice)) {
      finish();
    }
  }

  @Subscribe
  public void onConnectEvent(ConnectEvent connectEvent) {
    if (connectEvent == null) {
      return;
    }

    if (!connectEvent.device.equals(mDevice)) {
      return;
    }

    if (ConnectEvent.ON_DISCONNECTED.equalsIgnoreCase(connectEvent.event)
        || ConnectEvent.ON_ERROR.equalsIgnoreCase(connectEvent.event)) {
      if (!isOTAing) {
        finish();
      }
    } else if (connectEvent.event.equals(ConnectEvent.ON_DEVICE_READY)) {
      Log.v(TAG, "device connect success");
    }
  }

  protected CommandRequest getCommandRequest(int type, int timeout) {
    CommandRequest request = new CommandRequest.Builder()
        .setTimeout(timeout)
        .setType(type)
        .build();
    return request;
  }

  protected CommandRequest getCommandRequest(int type, int timeout, String key,
      Object param) {
    CommandRequest request = new CommandRequest.Builder()
        .setTimeout(timeout)
        .setType(type)
        .addParam(key, param)
        .build();
    return request;
  }

  public void execute(final int type, final Callback callback) {
    CommandRequest request = getCommandRequest(type, 3000);
    execute(request, callback);
  }

  public void execute(final int type) {
    CommandRequest request = getCommandRequest(type, 3000);
    execute(request);
  }

  public void execute(final CommandRequest request) {
    execute(request, null);
  }

  public void execute(CommandRequest request, Callback callback, boolean showDialog, boolean showToast) {
    DeviceManager.getInstance().execute(mDevice, request, new Callback() {
      @Override
      public void onStart() {
        if (showDialog) {
          showProgressDialog(ErrorMessageHandler.getInstance().getOnStartMessage(mDevice.getModel(), request.getType()));
        }
        if (null != callback) {
          callback.onStart();
        }
      }

      @Override
      public void onComplete(Map<String, Object> data) {
        dismissProgressDialog();
        if (showToast) {
          showToast(ErrorMessageHandler.getInstance().getOnCompleteMessage(mDevice.getModel(), request.getType(), data));
        }
        if (null != callback) {
          callback.onComplete(data);
        }
      }

      @Override
      public void onError(int code, String msg) {
        dismissProgressDialog();
        if (showToast) {
          showToast(ErrorMessageHandler.getInstance().getOnErrorMesage(mDevice.getModel(), request.getType(), code, msg));
        }
        if (null != callback) {
          callback.onError(code, msg);
        }
      }
    });
  }

  public void execute(final CommandRequest request, final Callback callback) {
    execute(request, callback, true, true);
  }

  protected void initDevice() {
    try {
      mDevice = (Device) getIntent().getExtras().getSerializable("device");
    } catch (Exception e) {
      e.printStackTrace();
    }

    assert (mDevice != null);
    setTitle(mDevice.getName());
  }

  protected void navToConnectedActivity(Device device, Class clazz) {
    Bundle extras = new Bundle();
    extras.putSerializable("device", device);
    navTo(this, extras, clazz);
  }
}
