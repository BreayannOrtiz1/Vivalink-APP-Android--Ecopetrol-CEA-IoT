package com.vivalnk.sdk.demo.repository.device;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import com.vivalnk.sdk.Callback;
import com.vivalnk.sdk.CommandRequest;
import com.vivalnk.sdk.DataReceiveListener;
import com.vivalnk.sdk.SampleDataReceiveListener;
import com.vivalnk.sdk.VitalClient;
import com.vivalnk.sdk.ble.BluetoothConnectListener;
import com.vivalnk.sdk.ble.BluetoothScanListener;
import com.vivalnk.sdk.command.abpm.ParametersKey;
import com.vivalnk.sdk.command.base.CommandAllType;
import com.vivalnk.sdk.common.ble.connect.BleConnectOptions;
import com.vivalnk.sdk.common.ble.exception.BleCode;
import com.vivalnk.sdk.common.ble.scan.ScanOptions;
import com.vivalnk.sdk.common.ble.utils.BluetoothUtils;
import com.vivalnk.sdk.common.eventbus.EventBus;
import com.vivalnk.sdk.common.utils.log.VitalLog;
import com.vivalnk.sdk.demo.repository.database.DatabaseManager;
import com.vivalnk.sdk.demo.repository.database.VitalData;
import com.vivalnk.sdk.demo.repository.database.VitalDevice;
import com.vivalnk.sdk.engineer.test.LogerManager;
import com.vivalnk.sdk.model.BatteryInfo;
import com.vivalnk.sdk.model.Device;
import com.vivalnk.sdk.model.DeviceModel;
import com.vivalnk.sdk.model.SampleData;
import com.vivalnk.sdk.open.config.ClockSyncConfig;
import com.vivalnk.sdk.open.config.LocationGrantConfig;
import com.vivalnk.sdk.open.config.NetworkGrantConfig;
import com.vivalnk.sdk.repository.device.UploaderStrategy;
import com.vivalnk.sdk.utils.GSON;
import com.vivalnk.sdk.utils.RxTimer;
import com.vivalnk.sdk.vital.ete.ETEManager;
import com.vivalnk.sdk.vital.ete.ETEResult;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by JakeMo on 18-4-26.
 */
public class DeviceManager {

  public static final String TAG = "DeviceManager";

  private Context mContext;

  private Subject<VitalSampleData> subject = PublishSubject.<VitalSampleData>create().toSerialized();

  private Map<String, DeviceETEManager> eteManagerMap;

  public void closeRTSData(Device device) {
    CommandRequest requestRTS = new CommandRequest.Builder()
        .setTimeout(3000)
        .setType(CommandAllType.rtsData_Control)
        .addParam(ParametersKey.KEY_control_open_rt, false)
        .build();
    execute(device, requestRTS, null);

  }

  public void openRTSData(Device device) {
    CommandRequest requestRTS = new CommandRequest.Builder()
        .setTimeout(3000)
        .setType(CommandAllType.rtsData_Control)
        .addParam(ParametersKey.KEY_control_open_rt, true)
        .build();
    execute(device, requestRTS, null);
  }

  public void openHistoryData(Device device) {
    CommandRequest requestHistory = new CommandRequest.Builder()
        .setTimeout(3000)
        .setType(CommandAllType.historyData_Control)
        .addParam(ParametersKey.KEY_control_open_history, true)
        .build();
    execute(device, requestHistory, null);
  }


  public void closeHistoryData(Device device) {
    CommandRequest requestHistory = new CommandRequest.Builder()
        .setTimeout(3000)
        .setType(CommandAllType.historyData_Control)
        .addParam(ParametersKey.KEY_control_open_history, false)
        .build();
    execute(device, requestHistory, null);
  }

  private static class SingletonHolder {

    private static final DeviceManager
        INSTANCE = new DeviceManager();
  }

  private DeviceManager() {
  }

  public static DeviceManager getInstance() {
    return SingletonHolder.INSTANCE;
  }

  public void init(Context context) {
    mContext = context.getApplicationContext();

    DatabaseManager.getInstance().init(mContext);

    subject
        .subscribeOn(Schedulers.io()) //Observable事件源代码所在线程
        .observeOn(Schedulers.io())   //Observer观察者回调方法所在线程
        .subscribe(new Observer<VitalSampleData>() {
          @Override
          public void onSubscribe(Disposable d) {

          }

          @Override
          public void onNext(VitalSampleData vitalSampleData) {
            handleSampleData(vitalSampleData.device, vitalSampleData.data);
          }

          @Override
          public void onError(Throwable e) {
            VitalLog.e(e);
          }

          @Override
          public void onComplete() {

          }
        });

    //if (BuildConfig.DEBUG) {
    VitalClient.getInstance().openLog();
    VitalClient.getInstance().allowWriteToFile(true);
    //}

    VitalClient.Builder builder = new VitalClient.Builder();
    builder.setConnectResumeListener(myConnectListener);

    //allow sdk upload data to vCloud
    //default is not allow
    UploaderStrategy uploadStrategy = new UploaderStrategy();
    uploadStrategy.upload = true;
    uploadStrategy.save = true;
    builder.setUploadStrategy(uploadStrategy);

    //allow sdk obtain network information, example, network operator name, network local ip address, network type.
    //default is not allow
    NetworkGrantConfig networkGrantConfig = new NetworkGrantConfig();
    networkGrantConfig.allow = true;
    builder.setNetworkGrantConfig(networkGrantConfig);

    //allow sdk obtain GPS location
    //default is not allow
    LocationGrantConfig locationGrantConfig = new LocationGrantConfig();
    locationGrantConfig.allow = true;
    builder.setLocationGrantConfig(locationGrantConfig);

    //set clock sync gap time once connected
    //default gapTime = 0, means
    //Do not force the synchronize the clock if the gapTime has been exceeded since the last synchronization
    ClockSyncConfig clockSyncConfig = new ClockSyncConfig();
    builder.setLocationGrantConfig(locationGrantConfig);

    VitalClient.getInstance().init(mContext, builder);

    eteManagerMap = new HashMap<>();

    LogerManager.getInstance().init();

  }

  public ETEManager getETEManager(Device device, boolean flash) {
    String key = DeviceETEManager.getKey(device, flash);
    DeviceETEManager deviceETEMannager = eteManagerMap.get(key);
    if (deviceETEMannager == null || deviceETEMannager.eteManager == null) {
      deviceETEMannager = new DeviceETEManager(device, flash);
      eteManagerMap.put(key, deviceETEMannager);
    }
    return deviceETEMannager.eteManager;
  }

  private DataReceiveListener dataReceiveListener = new DataReceiveListener() {
    @Override
    public void onReceiveData(Device device, Map<String, Object> data) {

      VitalSampleData liveData = new VitalSampleData();
      liveData.device = device;
      liveData.data = data;

      if (device.getModel() != DeviceModel.Checkme_O2) {
        subject.onNext(liveData);
      }

      runOnUiThread(() -> EventBus.getDefault().post(liveData));
    }

    @Override
    public void onBatteryChange(Device device, Map<String, Object> data) {
      //for VV330
      if (data.get("data") != null && data.get("data") instanceof BatteryInfo) {
        BatteryInfo batteryInfo = (BatteryInfo) data.get("data");

        BatteryData batteryData = new BatteryData();
        batteryData.device = device;
        batteryData.batteryInfo = batteryInfo;

        runOnUiThread(() -> EventBus.getDefault().post(batteryData));
      }
    }

    @Override
    public void onDeviceInfoUpdate(Device device, Map<String, Object> data) {
      //for SpO2, data has field like bellow
      /**
       * {
       *     "Region": "CE",
       *     "Model": "1641",
       *     "HardwareVer": "AA",
       *     "SoftwareVer": "4.7.0",
       *     "BootloaderVer": "0.1.0.0",
       *     "FileVer": "3",
       *     "SPCPVer": "1.4",
       *     "SN": "1912217640",
       *     "CurTIME": "2020-07-27,11:08:24",
       *     "CurBAT": "30%",
       *     "CurBatState": "0",
       *     "CurOxiThr": "90",
       *     "CurMotor": "50",
       *     "CurPedtar": "99999",
       *     "CurMode": "1",
       *     "BranchCode": "21010000",
       *     "FileList": "20200723114655,20200723170412,20200724083753,"
       * }
       */

      //TODO for VV330

    }

    @Override
    public void onLeadStatusChange(Device device, boolean isLeadOn) {
      LeadStatusData leadStatusData = new LeadStatusData();
      leadStatusData.device = device;
      leadStatusData.leadOn = isLeadOn;

      runOnUiThread(() -> EventBus.getDefault().post(leadStatusData));
    }

    @Override
    public void onFlashStatusChange(Device device, int remainderFlashBlock) {
      Log.d(TAG, "remainder Flash Block num = " + remainderFlashBlock);
    }

    @Override
    public void onFlashUploadFinish(Device device) {
      Log.d(TAG, device.getName() + " flash upload finish");
    }
  };

  private SampleDataReceiveListener sampleDataReceiveListener = new SampleDataReceiveListener() {
    @Override
    public void onReceiveSampleData(Device device, boolean flash, SampleData data) {
      //simplify the usage of onReceiveData, just output sample data
      Log.d(TAG, "onReceiveSampleData: flash = " + flash + ", " + "data = " + data);
    }
  };

  private void handleSampleData(final Device device, final Map<String, Object> data) {
    SampleData sampleData = (SampleData) data.get("data");

    if (sampleData == null) {
      return;
    }

    //save to database
    DatabaseManager.getInstance().getDataDAO().insert(new VitalData(sampleData));

    //当存在RRI/ACC的时候，输入FB，获取结果
    analyzerData(device, sampleData, sampleData.isFlash());

    runOnUiThread(() -> EventBus.getDefault().post(sampleData));

  }

  public ETEResult getETEResultSync(Device device, boolean flash) {
    ETEManager eteManager = getETEManager(device, flash);
    if (eteManager == null) {
      return null;
    }
    return eteManager.getResultSync(System.currentTimeMillis());
  }

  private BluetoothConnectListener myConnectListener = new MyConnectListener();

  private void analyzerData(Device device, SampleData data, boolean flash) {

    ETEManager eteManager = getETEManager(device, flash);
    if (null != eteManager) {
      eteManager.analyzerData(data);
    }

  }

  Handler mMainHandler = new Handler(Looper.getMainLooper());
  private BluetoothScanListener scanListener = new BluetoothScanListener() {
    @Override
    public void onStart() {
      runOnUiThread(() -> EventBus.getDefault().post(ScanEvent.onStart()));
    }

    @Override
    public void onDeviceFound(Device device) {
      runOnUiThread(() -> EventBus.getDefault().post(ScanEvent.onDeviceFound(device)));
    }

    @Override
    public void onStop() {
      runOnUiThread(() -> EventBus.getDefault().post(ScanEvent.onStop()));
    }

    @Override
    public void onError(int code, String msg) {
      runOnUiThread(() -> EventBus.getDefault().post(ScanEvent.onError(code, msg)));
    }
  };

  public int checkBle() {
    return VitalClient.getInstance().checkBle();
  }

  public void enableBle() {
    VitalClient.getInstance().enableBle();
  }

  public void disableBle() {
    VitalClient.getInstance().disableBle();
  }

  /**
   * start scan device.
   */
  public void startScan() {
    ScanOptions options = new ScanOptions.Builder()
        .setTimeout(30 * 1000)
        .setEnableLog(true)
        .build();
    VitalClient.getInstance().startScan(options, scanListener);
  }

  public void stopScan() {
    VitalClient.getInstance().stopScan(scanListener);
  }

  public void connect(final Device device) {
    connect(device, 6);
  }

  /**
   * connect device.
   *
   * @param device your vital device
   */
  public void connect(final Device device, int retry) {
    ConnectEvent connectEvent = new ConnectEvent();
    connectEvent.device = device;
    connectEvent.event = ConnectEvent.ON_ERROR;
    if (null == device || TextUtils.isEmpty(device.getId())) {
      connectEvent.code = BleCode.BLUETOOTH_CONNECT_ERROR;
      connectEvent.msg = "device can not be null";
      runOnUiThread(() -> EventBus.getDefault().post(connectEvent));
      return;
    }

    if (BluetoothUtils.isDeviceConnected(mContext, device.getId())) {
      connectEvent.code = BleCode.BLUETOOTH_CONNECT_ERROR;
      connectEvent.msg = "device is connected";
      runOnUiThread(() -> EventBus.getDefault().post(connectEvent));
      return;
    }

    BleConnectOptions options = new BleConnectOptions.Builder()
        .setConnectRetry(retry)
        .setConnectTimeout(10 * 1000)
        .setAutoConnect(true)
        .build();
    VitalLog.d(TAG, "user connect to " + GSON.toJson(device));
    VitalClient.getInstance().connect(device, options, myConnectListener);
  }

  public void disconnect(Device device) {
    VitalClient.getInstance().disconnect(device);
  }

  public void disconnectAll() {
    VitalClient.getInstance().disconnectAll();
  }

  public boolean isConnected(Device device) {
    if (null == device || TextUtils.isEmpty(device.getId())) {
      throw new NullPointerException("device is null, or has a empty  mac id");
    }
    return BluetoothUtils.isDeviceConnected(mContext, device.getId());
  }

  private void registDataReceiver(Device device) {
    VitalClient.getInstance().registDataReceiver(device, dataReceiveListener);
  }

  public void execute(Device device, CommandRequest command, Callback callback) {
    if (null == device || TextUtils.isEmpty(device.getId())) {
      throw new NullPointerException("device is null, or has a empty  mac id");
    }
    VitalClient.getInstance().execute(device, command, callback);
  }

  public void runOnUiThread(Runnable runnable) {
    mMainHandler.post(runnable);
  }

  public void destroy() {
    VitalClient.getInstance().destroy();
    for (Map.Entry<String, DeviceETEManager> deviceETEManagerEntry : eteManagerMap.entrySet()) {
      DeviceETEManager deviceETEMannager = deviceETEManagerEntry.getValue();
      deviceETEMannager.eteManager.unregisterETEDataReceiveListener();
      eteManagerMap.remove(deviceETEManagerEntry.getKey());
    }
  }

  private class MyConnectListener implements BluetoothConnectListener {

    @Override
    public boolean onResume(Device device) {
      //false: not interrupt the sdk reconnecting progress
      //true : you have a custom connecting operation, you don't need sdk to reconnect the device
      Log.d(TAG, "Connect onResume " + device.toString());
      runOnUiThread(() -> EventBus.getDefault().post(ConnectEvent.onResume(device)));
      return false;
    }

    @Override
    public void onStartScan(Device device) {
      Log.d(TAG, "Connect onStartScan " + device.toString());
      runOnUiThread(() -> EventBus.getDefault().post(ConnectEvent.onStartScan(device)));
    }

    @Override
    public void onStopScan(Device device) {
      Log.d(TAG, "Connect onStopScan " + device.toString());
      runOnUiThread(() -> EventBus.getDefault().post(ConnectEvent.onStopScan(device)));
    }

    @Override
    public void onStart(Device device) {
      Log.d(TAG, "Connect onStart " + device.toString());
      runOnUiThread(() -> EventBus.getDefault().post(ConnectEvent.onStart(device)));
    }

    @Override
    public void onConnecting(Device device) {
      Log.d(TAG, "Connect onConnecting " + device.toString());
      runOnUiThread(() -> EventBus.getDefault().post(ConnectEvent.onConnecting(device)));
    }

    @Override
    public void onConnected(Device device) {
      Log.d(TAG, "Connect onConnected " + device.toString());
      runOnUiThread(() -> EventBus.getDefault().post(ConnectEvent.onConnected(device)));
    }

    @Override
    public void onServiceReady(Device device) {
      Log.d(TAG, "Connect onServiceReady " + device.toString());
      registDataReceiver(device);
      runOnUiThread(() -> EventBus.getDefault().post(ConnectEvent.onServiceReady(device)));
    }

    @Override
    public void onEnableNotify(Device device) {
      Log.d(TAG, "Connect onEnableNotify " + device.toString());
      runOnUiThread(() -> EventBus.getDefault().post(ConnectEvent.onEnableNotify(device)));
    }

    @Override
    public void onDeviceReady(Device device) {
      Log.d(TAG, "Connect onDeviceReady " + device.toString());
      DatabaseManager.getInstance().getDeviceDAO().insert(new VitalDevice(device));
      if (device.getModel() != DeviceModel.Checkme_O2) {
        openHistoryData(device);
      }
      runOnUiThread(() -> EventBus.getDefault().post(ConnectEvent.onDeviceReady(device)));
    }

    @Override
    public void onTryRescanning(Device device) {
      Log.d(TAG, "Connect onTryRescanning " + device.toString());
      runOnUiThread(() -> EventBus.getDefault().post(ConnectEvent.onTryRescanning(device)));
    }

    @Override
    public void onTryReconnect(Device device) {
      Log.d(TAG, "Connect onTryReconnect " + device.toString());
      runOnUiThread(() -> EventBus.getDefault().post(ConnectEvent.onTryReconnect(device)));
    }

    @Override
    public void onRetryConnect(Device device, int totalRetryCount, int currentCount, long timeout) {
      Log.d(TAG, "Connect onRetryConnect: mac = " + device.toString() + ", totalCount =" + totalRetryCount + ", current = " + currentCount);
      runOnUiThread(() -> EventBus.getDefault().post(ConnectEvent.onRetryConnect(device)));
    }

    @Override
    public void onDisConnecting(Device device, boolean isForce) {
      Log.d(TAG, "Connect onDisConnecting " + device.toString() + " isForce=" + isForce);
      runOnUiThread(() -> EventBus.getDefault().post(ConnectEvent.onDisConnecting(device, isForce)));
    }

    @Override
    public void onDisconnected(Device device, boolean isForce) {
      DatabaseManager.getInstance().getDeviceDAO().delete(device.getId());
      Log.d(TAG, "Connect onDisconnected " + device.toString() + " isForce=" + isForce);
      runOnUiThread(() -> EventBus.getDefault().post(ConnectEvent.onDisconnected(device, isForce)));
    }

    @Override
    public void onError(Device device, int code, String msg) {
      Log.d(TAG, "Connect onError " + device.toString() + " code=" + code + " msg=" + msg);
      runOnUiThread(() -> EventBus.getDefault().post(ConnectEvent.onError(device, code, msg)));
    }

  }

  public static class VitalSampleData {
    public Device device;
    public Map<String, Object> data;
  }

  public static class BatteryData {
    public Device device;
    public BatteryInfo batteryInfo;
  }

  public static class RssiData {
    public Device device;
    public Integer rssi;
  }

  public static class LeadStatusData {
    public Device device;
    public boolean leadOn;
  }

}
