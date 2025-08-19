package com.vivalnk.sdk.engineer.test;

import android.util.Log;
import com.vivalnk.sdk.common.eventbus.EventBus;
import com.vivalnk.sdk.model.Device;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public abstract class AbsLogger {

  protected Device mDevice;

  private final Subject<Runnable> logEventPS = PublishSubject.<Runnable>create().toSerialized();

  protected AbsLogger() {
    initEventSource();
  }

  protected AbsLogger(Device device) {
    this.mDevice = device;
    initEventSource();
  }

  private void initEventSource() {
    logEventPS
        .observeOn(Schedulers.io())
        .doOnError(new Consumer<Throwable>() {
          @Override
          public void accept(Throwable throwable) throws Exception {
            Log.e("error", throwable.getLocalizedMessage());
          }
        })
        .subscribe(new Consumer<Runnable>() {
          @Override
          public void accept(Runnable runnable) throws Exception {
            runnable.run();
          }
        });
  }

  public void start() {
    register(this);
  }
  public void stop() {
    unregister(this);
  }
  public boolean isStarted() {
    return EventBus.getDefault().isRegistered(this);
  }

  protected void register(Object object) {
    if (EventBus.getDefault().isRegistered(this) == false) {
      EventBus.getDefault().register(this);
    }
  }

  protected void unregister(Object object) {
    if (EventBus.getDefault().isRegistered(this) == true) {
      EventBus.getDefault().unregister(this);
    }
  }

  protected void postIO(Runnable runnable) {
    logEventPS.onNext(runnable);
  }

}
