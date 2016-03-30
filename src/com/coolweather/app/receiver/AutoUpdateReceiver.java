package com.coolweather.app.receiver;

import com.coolweather.app.service.AutoUpdateService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AutoUpdateReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		//用Intent来传递服务
		Intent i = new Intent(context , AutoUpdateService.class);
		//启动服务
		context.startService(i);

	}

}
