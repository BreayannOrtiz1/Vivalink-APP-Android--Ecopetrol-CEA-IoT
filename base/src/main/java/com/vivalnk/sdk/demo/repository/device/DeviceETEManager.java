package com.vivalnk.sdk.demo.repository.device;

import android.os.Handler;
import android.os.Looper;
import com.vivalnk.sdk.common.eventbus.EventBus;
import com.vivalnk.sdk.common.utils.log.VitalLog;
import com.vivalnk.sdk.model.Device;
import com.vivalnk.sdk.vital.ete.ETECode;
import com.vivalnk.sdk.vital.ete.ETEDataReceiveListener;
import com.vivalnk.sdk.vital.ete.ETEManager;
import com.vivalnk.sdk.vital.ete.ETEParameter;
import com.vivalnk.sdk.vital.ete.ETEResult;

public class DeviceETEManager {

  private static final String TAG = "DeviceETEManager";
  public Device device;
  public ETEManager eteManager;
  public boolean flash;

  private ETEDataReceiveListener eteDataReceiveListener = new ETEDataReceiveListener() {
    @Override
    public void onETEResultUpdated(ETEResult result) {
      StringBuilder eteResult = new StringBuilder("ETE result :");
      eteResult.append(" TimeStamp=").append(result.dataTimeStamp);
      eteResult.append(", HR=").append(result.ETEcorrectedHr);
      eteResult.append(", artifactPercent=").append(result.ETEartifactPercent);
      eteResult.append(", minHR=").append(result.ETEminimalHr);
      eteResult.append(", maxHR=").append(result.ETEmaximalHr);
      eteResult.append(", EPOC=").append(result.ETEepoc);
      eteResult.append(", TLPeak=").append(result.ETEtrainingLoadPeak);
      eteResult.append(", TE=").append(result.ETEtrainingEffect);
      eteResult.append(", kcal=").append(result.ETEenergyExpenditure);
      eteResult.append(", kcalC=").append(result.ETEenergyExpenditureCumulative);
      eteResult.append(", maxMET=").append(result.ETEmaximalMET);
      eteResult.append(", METmins=").append(result.ETEmaximalMETminutes);
      eteResult.append(", METminPercentage=").append(result.ETEmaximalMETpercentage);
      eteResult.append(", RelaxStressIntensity=").append(result.ETErelaxStressIntensity);
      eteResult.append(", meanMAD=").append(result.ETEmeanMAD);
      eteResult.append(", state=").append(result.ETEcurrentState);
      eteResult.append(", StressBalance=").append(result.ETEstressBalance);
      eteResult.append(", resp=").append(result.ETErespirationRate);
      eteResult.append(", activityScore=").append(result.ETEactivityScore);
      eteResult.append(", sleepQuality=").append(result.ETEsleepQualityIndex);
      eteResult.append(", maxSleepQuality=").append(result.ETEmaxSleepQualityIndex);
      eteResult.append(", sleepStart=").append(result.ETEsleepStart);
      eteResult.append(", sleepEnd=").append(result.ETEsleepEnd);
      eteResult.append(", sleepEndCandidate=").append(result.ETEsleepEndCandidate);

      DeviceETEResult deviceETEResult = new DeviceETEResult();
      deviceETEResult.device = device;
      deviceETEResult.flash = flash;
      deviceETEResult.result = result;
      runOnUiThread(() -> EventBus.getDefault().post(deviceETEResult));
    }
  };

  public DeviceETEManager(Device device, boolean flash) {
    this.device = device;
    this.eteManager = createETEManager();
    this.flash = flash;
  }

  private ETEManager createETEManager() {
    ETEManager eteManager = new ETEManager();
    ETEParameter parameter = new ETEParameter();
    parameter.age = 30;
    parameter.gender = ETEParameter.GENDER_MEN;
    parameter.height = 180;
    parameter.weight = 74;
    int code = eteManager.setETEParameters(parameter);
    if (code != ETECode.SUCCESS) {
      VitalLog.d(TAG, "ETE set parameter fail");
      return null;
    }
    eteManager.registerETEDataReceiveListener(eteDataReceiveListener);
    return eteManager;
  }

  private void runOnUiThread(Runnable runnable) {
    new Handler(Looper.getMainLooper()).post(new Runnable() {
      @Override
      public void run() {
        runnable.run();
      }
    });
  }

  public void unregisterETEDataReceiveListener() {
    if (eteManager != null) {
      eteManager.unregisterETEDataReceiveListener();
    }
  }

  public static class DeviceETEResult {
    public Device device;
    public ETEResult result;
    public boolean flash;
  }

  public static String getKey(Device device, boolean flash) {
    return device.getId() + "" + flash;
  }

}
