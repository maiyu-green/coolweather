package com.coolweather.app.service;

import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
/**
 * 后台自动更新天气的功能，定时任务处理
 */
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

public class AutoUpdateService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//开一个新的线程
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				//更新处理
				updateWeather();
				
			}
		}).start();
		//时间管理
		AlarmManager manager = (AlarmManager)getSystemService(ALARM_SERVICE);
		int anHour = 8 * 60 * 60 * 1000 ;   //这是8个小时的毫秒数
		long triggerAtTime = SystemClock.elapsedRealtime()+anHour;
		//启用意图
		Intent i = new Intent(this , AutoUpdateService.class);
		//调用PendingIntent
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
		manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
		return super.onStartCommand(intent, flags, startId);
	}
	
	/**
	 * 更新天气信息
	 */
	private void updateWeather() {
		//调用PreferenceManager的getDefaultSharedPreferences()方法来获取SharedPreferences对象，并传入参数this
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		//获取weather_code
		String weatherCode = prefs.getString("weather_code", "");
		//设置address
		String address = "http://www.weather.com.cn/data/cityinfo/" + weatherCode + ".html";
		
		//设置访问http请求
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			
			@Override
			public void onFinish(String response) {
				Utility.handleWeatherResponse(AutoUpdateService.this, response);
				
			}
			
			@Override
			public void onError(Exception e) {
				e.printStackTrace();
			}
		});
		
	}

}












