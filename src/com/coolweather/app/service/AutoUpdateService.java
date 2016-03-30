package com.coolweather.app.service;

import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
/**
 * ��̨�Զ����������Ĺ��ܣ���ʱ������
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
		//��һ���µ��߳�
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				//���´���
				updateWeather();
				
			}
		}).start();
		//ʱ�����
		AlarmManager manager = (AlarmManager)getSystemService(ALARM_SERVICE);
		int anHour = 8 * 60 * 60 * 1000 ;   //����8��Сʱ�ĺ�����
		long triggerAtTime = SystemClock.elapsedRealtime()+anHour;
		//������ͼ
		Intent i = new Intent(this , AutoUpdateService.class);
		//����PendingIntent
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
		manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
		return super.onStartCommand(intent, flags, startId);
	}
	
	/**
	 * ����������Ϣ
	 */
	private void updateWeather() {
		//����PreferenceManager��getDefaultSharedPreferences()��������ȡSharedPreferences���󣬲��������this
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		//��ȡweather_code
		String weatherCode = prefs.getString("weather_code", "");
		//����address
		String address = "http://www.weather.com.cn/data/cityinfo/" + weatherCode + ".html";
		
		//���÷���http����
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












