package com.vivalnk.sdk.engineer.test;

import com.vivalnk.sdk.demo.base.utils.SPUtils;
import com.vivalnk.sdk.demo.repository.device.DeviceManager;
import com.vivalnk.sdk.common.eventbus.Subscribe;
import com.vivalnk.sdk.common.utils.FileUtils;
import com.vivalnk.sdk.model.Device;
import com.vivalnk.sdk.model.SampleData;
import com.vivalnk.sdk.model.SampleData.DataKey;
import java.util.Map;

public class FBLogger extends AbsLogger {
  @Subscribe
  public void onDataEvent(final DeviceManager.VitalSampleData data) {
    if (data == null) {
      return;
    }
    if (!(Boolean)SPUtils.get(data.device.getSn() + "fbTest", false)){
      return;
    }
    String logFileName = SPUtils.get(data.device.getSn() + "fbTestFile", "");

    postIO(new Runnable() {
      @Override
      public void run() {
        Device device = data.device;

        Map<String, Object> map = data.data;
        if (null != map.get("data") && map.get("data") instanceof SampleData) {
          SampleData sampleData = (SampleData) map.get("data");

          int[] rri = sampleData.getData(DataKey.rri);
          long timeStamp = sampleData.getData(DataKey.time);
          if (rri != null) {
            for (int i = 0;i<rri.length; i++) {
              if(rri[i] > 0) {
                FileUtils.writeFile(FileManager.getFBFilePath(device.getSn(), logFileName, timeStamp), String.valueOf(rri[i]) + "\n");
              }
            }
          }


        }
      }
    });


  }
}
