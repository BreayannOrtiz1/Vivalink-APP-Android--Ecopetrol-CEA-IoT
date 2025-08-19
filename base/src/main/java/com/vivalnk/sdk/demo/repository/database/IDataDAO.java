package com.vivalnk.sdk.demo.repository.database;

import com.vivalnk.sdk.common.utils.ListUtils;
import java.util.List;

public interface IDataDAO {
  void insert(VitalData... data);

  void update(VitalData... data);

  void delete(VitalData... data);

  void deleteAll();

  default long getCount() {
    return 0;
  }

  default long getCount(String deviceId) {
    return 0;
  }

  List<VitalData> queryAll();
  default List<VitalData> queryAllOrderByTimeASC(String deviceId) {
    return ListUtils.getEmptyList();
  }
  default List<VitalData> queryOldestAll(long count) {
    return ListUtils.getEmptyList();
  }
  default List<VitalData> queryLatestAll(long count) {
    return ListUtils.getEmptyList();
  }

  default List<VitalData> queryAll(String deviceID) {
    return ListUtils.getEmptyList();
  }
  default List<VitalData> queryOldestAll(String deviceID, long count) {
    return ListUtils.getEmptyList();
  }
  default List<VitalData> queryLatestAll(String deviceID, long count) {
    return ListUtils.getEmptyList();
  }

  VitalData query(String deviceID, long time);

  List<VitalData> query(String deviceID, long startTime, long endTime);
  
}
