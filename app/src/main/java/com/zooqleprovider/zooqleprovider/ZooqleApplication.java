package com.zooqleprovider.zooqleprovider;

import android.app.Application;
import com.plunder.provider.PlunderProvider;

public class ZooqleApplication extends Application {
  @Override public void onCreate() {
    super.onCreate();

    PlunderProvider provider = PlunderProvider.get();
    provider.setName("Zooqle");
    provider.setMovieSearchProvider(new ZooqleSearchProvider("Movies"));
    provider.setTvSearchProvider(new ZooqleSearchProvider("TV"));
  }
}
