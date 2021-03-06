package com.liudong.douban;

import android.app.Application;
import android.content.IntentFilter;

import com.liudong.douban.data.remote.network.NetworkConnectChangedReceiver;
import com.liudong.douban.di.components.ApplicationComponent;
import com.liudong.douban.di.components.DaggerApplicationComponent;
import com.liudong.douban.di.modules.ApplicationModule;

/**
 * Created by liudong on 2016/11/22.
 * Base Application for Application
 */

public class MyApplication extends Application {

    ApplicationComponent mAppComponent;

    private static MyApplication application;
    private boolean isConnected;  //是否有网络连接
    private boolean isLogin;  //是否登录

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        initReceiver();
    }

    private void initReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        NetworkConnectChangedReceiver changedReceiver = new NetworkConnectChangedReceiver(this);
        registerReceiver(changedReceiver, filter);
    }

    public ApplicationComponent getAppComponent() {
        if (mAppComponent == null) {
            mAppComponent = DaggerApplicationComponent.builder().
                    applicationModule(new ApplicationModule(this)).build();
        }
        return mAppComponent;
    }

    public static MyApplication getInstance() {
        return application;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public boolean isLogin() {
        return isLogin;
    }

    public void setLogin(boolean login) {
        isLogin = login;
    }
}
