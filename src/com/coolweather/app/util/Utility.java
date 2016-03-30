package com.coolweather.app.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import com.coolweather.app.db.CoolWeatherDB;
import com.coolweather.app.model.City;
import com.coolweather.app.model.County;
import com.coolweather.app.model.Province;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class Utility {

	/**
	 * 解析和处理服务器返回的省级数据
	 */
	public synchronized static boolean handleProvincesResponse(
			CoolWeatherDB coolWeatherDB, String response) {
		if (!TextUtils.isEmpty(response)) {
			//根据传回来的数据，分割
			String[] allProvinces = response.split(",");
			//遍历
			if (allProvinces != null && allProvinces.length > 0) {
				for (String p : allProvinces) {
					String[] array = p.split("\\|");
					Province province = new Province();
					province.setProvinceCode(array[0]);
					province.setProvinceName(array[1]);
					// 将解析出来的数据存储到Province表
					coolWeatherDB.saveProvince(province);
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * 解析和处理服务器返回的市级数据
	 */
	public static boolean handleCitiesResponse(CoolWeatherDB coolWeatherDB,
			String response, int provinceId) {
		//判空
		if (!TextUtils.isEmpty(response)) {
			//分割
			String[] allCities = response.split(",");
			if (allCities != null && allCities.length > 0) {
				for (String c : allCities) {
					//分割，传回来的数据格式为“代号|城市，代号|城市"
					String[] array = c.split("\\|");
					City city = new City();
					city.setCityCode(array[0]);
					city.setCityName(array[1]);
					city.setProvinceId(provinceId);
					// 将解析出来的数据存储到City表
					coolWeatherDB.saveCity(city);
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * 解析和处理服务器返回的县级数据
	 */
	public static boolean handleCountiesResponse(CoolWeatherDB coolWeatherDB,
			String response, int cityId) {
		if (!TextUtils.isEmpty(response)) {
			//分割
			String[] allCounties = response.split(",");
			if (allCounties != null && allCounties.length > 0) {
				//遍历
				for (String c : allCounties) {
					//分割，传回来的数据格式为“代号|城市，代号|城市"
					String[] array = c.split("\\|");
					County county = new County();
					county.setCountyCode(array[0]);
					county.setCountyName(array[1]);
					county.setCityId(cityId);
					// 将解析出来的数据存储到County表
					coolWeatherDB.saveCounty(county);
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * 解析服务器返回的JSON数据，并将解析出来的数据储存到本地
	 */
	public static void handleWeatherResponse(Context context , String response){
		try{
			//new一个JSONObject对象，并把数据传入
			JSONObject jsonObject = new JSONObject(response);
			//用JSONObject的getJSONObject()方法获取相应对象
			JSONObject weatherInfo = jsonObject.getJSONObject("weatherinfo");
			
			//用获得的JSONObject对象的getString依次取出数据
			String cityName = weatherInfo.getString("city");
			String weatherCode = weatherInfo.getString("cityid");
			String temp1 = weatherInfo.getString("temp1");
			String temp2 = weatherInfo.getString("temp2");
			String weatherDesp = weatherInfo.getString("weather");
			String publishTime = weatherInfo.getString("ptime");
			//调用自定义方法saveWeatherInfo把数据储存到SharedPreferences文件中
			saveWeatherInfo(context , cityName , weatherCode ,temp1 ,temp2 ,weatherDesp ,publishTime);
			
		}
		catch(JSONException e){
			e.printStackTrace();
		}
	}

	
	

	/**
	 * 将服务器返回的所有天气信息储存到SharedPreferences文件中
	 */
	private static void saveWeatherInfo(Context context, String cityName,
			String weatherCode, String temp1, String temp2, String weatherDesp,
			String publishTime) {
		//用SimpleDateFormat来设置中国的日期格式
		SimpleDateFormat sdf = new SimpleDateFormat("yyy年M月d日",Locale.CHINA);
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		//开发放数据
		editor.putBoolean("city_selected", true);
		editor.putString("city_name",cityName);
		editor.putString("weather_code", weatherCode);
		editor.putString("temp1", temp1);
		editor.putString("temp1",temp2);
		editor.putString("weather_desp", weatherDesp);
		editor.putString("publish_time", publishTime);
		editor.putString("current_date", sdf.format(new Date()));
		//提交
		editor.commit();
		
	}
	
		

}











