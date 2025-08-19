package com.vivalnk.sdk.engineer.test;

import com.vivalnk.sdk.demo.repository.device.DeviceManager;
import com.vivalnk.sdk.common.eventbus.Subscribe;
import com.vivalnk.sdk.common.utils.FileUtils;
import com.vivalnk.sdk.common.utils.log.LogLevel;
import com.vivalnk.sdk.model.Device;
import com.vivalnk.sdk.model.SampleData;
import com.vivalnk.sdk.utils.DateFormat;
import java.util.Map;
import org.apache.commons.lang3.time.DateFormatUtils;

public class DataLogger extends AbsLogger {

  @Subscribe
  public void onDataEvent(final DeviceManager.VitalSampleData data) {
    if (data == null) {
      return;
    }

    postIO(new Runnable() {
      @Override
      public void run() {
        Device device = data.device;

        Map<String, Object> map = data.data;
        if (null != map.get("data") && map.get("data") instanceof SampleData) {
          SampleData sampleData = (SampleData) map.get("data");

          StringBuilder sb = new StringBuilder();
          long timeStamp = System.currentTimeMillis();
          String tag = "DATA";
          LogLevel priority = LogLevel.INFO;
          String threadInfo = Thread.currentThread().toString();
          String message = sampleData.toFileString();
          sb.append("\n")
              .append(DateFormatUtils.format((timeStamp), DateFormat.sPattern))
              .append(" -- ").append("Log[").append(tag).append(",").append(priority.toString()).append("]")
              .append(" -- ").append(threadInfo)
              .append(" :")
              .append(message);

          FileUtils.writeFile(FileManager.getDataFilePath(device.getSn()), sb.toString());

        }
      }
    });


  }

}
