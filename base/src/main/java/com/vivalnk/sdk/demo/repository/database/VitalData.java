package com.vivalnk.sdk.demo.repository.database;

import com.vivalnk.sdk.data.DataJsonConverter;
import com.vivalnk.sdk.model.SampleData;
import com.vivalnk.sdk.model.common.IVitalData;
import java.io.Serializable;

/**
 * Created by JakeMo on 18-4-25.
 */
public class VitalData extends SampleData implements Serializable {

  public VitalData() {
  }

  public VitalData(DataJsonConverter.DataFormated data) {
    super(data);
  }

  public VitalData(IVitalData sampleData) {
    super(sampleData);
  }

}
