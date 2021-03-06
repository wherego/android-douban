package com.liudong.douban.data.remote;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.liudong.douban.MyApplication;
import com.liudong.douban.utils.MyAdapterFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitConfig {

    public DouBanService douBanService(final MyApplication application) {

        Interceptor REWRITE_RESPONSE_INTERCEPTOR = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                Response response = chain.proceed(request);

                String cacheControl = request.cacheControl().toString();
                if (TextUtils.isEmpty(cacheControl)) {
                    cacheControl = "public, max-age=60";
                }
                return response.newBuilder()
                        .header("Cache-Control", cacheControl)
                        .removeHeader("Pragma") //清除头信息，因为服务器如果不支持，会返回一些干扰信息
                        .build();
            }
        };

        Interceptor OFFLINE_INTERCEPTOR = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                if (!application.isConnected()) {
                    Log.i("OFFLINE_INTERCEPTOR", "请求缓存");
                    int maxStale = 60 * 60 * 24 * 14; //设置缓存超时时间为2周
                    request = request.newBuilder()
                            .header("Cache-Control", "public, only-if-cached, max-stale=" + maxStale) //需要服务器支持
                            .removeHeader("Pragma")
                            .build();
                }
                return chain.proceed(request);
            }
        };

        //设置缓存路径
        File httpCacheDirectory = new File(application.getCacheDir(), "responses");
        //设置缓存 10M
        Cache cache = new Cache(httpCacheDirectory, 10 * 1024 * 1024);

        //创建OkHttpClient，并添加拦截器和缓存
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .addNetworkInterceptor(REWRITE_RESPONSE_INTERCEPTOR)
                .addInterceptor(OFFLINE_INTERCEPTOR)
                .cache(cache).build();

        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(MyAdapterFactory.create())
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(DouBanService.BaseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
        return retrofit.create(DouBanService.class);
    }
}