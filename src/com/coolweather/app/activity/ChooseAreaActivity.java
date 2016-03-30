package com.coolweather.app.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.app.R;
import com.coolweather.app.db.CoolWeatherDB;
import com.coolweather.app.model.City;
import com.coolweather.app.model.County;
import com.coolweather.app.model.Province;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;

public class ChooseAreaActivity extends Activity {

	public static final int LEVEL_PROVINCE = 0;
	public static final int LEVEL_CITY = 1;
	public static final int LEVEL_COUNTY = 2;
	
	private ProgressDialog progressDialog;
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private CoolWeatherDB coolWeatherDB;
	private List<String> dataList = new ArrayList<String>();
	/**
	 * 省列表
	 */
	private List<Province> provinceList;
	/**
	 * 市列表
	 */
	private List<City> cityList;
	/**
	 * 县列表
	 */
	private List<County> countyList;
	/**
	 * 选中的省份
	 */
	private Province selectedProvince;
	/**
	 * 选中的城市
	 */
	private City selectedCity;
	/**
	 * 当前选中的级别
	 */
	private int currentLevel;
	/**
	 * 是否从WeatherActivity中跳转过来。
	 */
	private boolean isFromWeatherActivity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//设置跳转
		isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity", false);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs.getBoolean("city_selected", false) && !isFromWeatherActivity) {
			//用意图intent来启动第二个activity
			Intent intent = new Intent(this, WeatherActivity.class);
			startActivity(intent);
			finish();
			return;
		}
		//隐藏标题栏
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		//设置当前布局
		setContentView(R.layout.choose_area);
		//UI界面设计
		listView = (ListView) findViewById(R.id.list_view);
		titleText = (TextView) findViewById(R.id.title_text);
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList);
		listView.setAdapter(adapter);
		coolWeatherDB = CoolWeatherDB.getInstance(this);
		//为listView设置监听器事件
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int index,
					long arg3) {
				if (currentLevel == LEVEL_PROVINCE) {
					//当前选择是省时，要查询城市
					selectedProvince = provinceList.get(index);
					queryCities();
				} else if (currentLevel == LEVEL_CITY) {
					//当现在的选择是城市时，要查询县级
					selectedCity = cityList.get(index);
					queryCounties();
				} else if (currentLevel == LEVEL_COUNTY) {
					//当现在的选择是县级时，就需查询天气数据了
					String countyCode = countyList.get(index).getCountyCode();
					//用Intent来开启WeatherActivity
					Intent intent = new Intent(ChooseAreaActivity.this, WeatherActivity.class);
					intent.putExtra("county_code", countyCode);
					startActivity(intent);
					finish();
				}
			}
		});
		queryProvinces();  // 加载省级数据
	}

	/**
	 * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询。
	 */
	private void queryProvinces() {
		//加载省级数据表
		provinceList = coolWeatherDB.loadProvinces();
		if (provinceList.size() > 0) {
			//先清零
			dataList.clear();
			//遍历
			for (Province province : provinceList) {
				dataList.add(province.getProvinceName());
			}
			adapter.notifyDataSetChanged();
			//显示
			listView.setSelection(0);
			titleText.setText("中国");
			//让当前选择置为省级
			currentLevel = LEVEL_PROVINCE;
		} else {
			//从服务器上查询省级
			queryFromServer(null, "province");
		}
	}

	/**
	 * 查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器上查询。
	 */
	private void queryCities() {
		//根据选中的省加载市级表
		cityList = coolWeatherDB.loadCities(selectedProvince.getId());
		if (cityList.size() > 0) {
			//先清泠
			dataList.clear();
			//遍历
			for (City city : cityList) {
				dataList.add(city.getCityName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedProvince.getProvinceName());
			//让当前选择为市级
			currentLevel = LEVEL_CITY;
		} else {
			//根据选中的省从服务器上查询市级表
			queryFromServer(selectedProvince.getProvinceCode(), "city");
		}
	}
	
	/**
	 * 查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询。
	 */
	private void queryCounties() {
		//根据选中的城市加载县级数据表
		countyList = coolWeatherDB.loadCounties(selectedCity.getId());
		if (countyList.size() > 0) {
			//先清零
			dataList.clear();
			//遍历
			for (County county : countyList) {
				dataList.add(county.getCountyName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			//让当前置为县级
			currentLevel = LEVEL_COUNTY;
		} else {
			//根据选中的市级从服务器上查询县级数据
			queryFromServer(selectedCity.getCityCode(), "county");
		}
	}
	
	/**
	 * 根据传入的代号和类型从服务器上查询省市县数据。
	 */
	private void queryFromServer(final String code, final String type) {
		String address;
		if (!TextUtils.isEmpty(code)) {
			address = "http://www.weather.com.cn/data/list3/city" + code + ".xml";
		} else {
			address = "http://www.weather.com.cn/data/list3/city.xml";
		}
		showProgressDialog();
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			@Override
			public void onFinish(String response) {
				boolean result = false;
				if ("province".equals(type)) {
					//省级处理
					result = Utility.handleProvincesResponse(coolWeatherDB,
							response);
				} else if ("city".equals(type)) {
					//市级处理
					result = Utility.handleCitiesResponse(coolWeatherDB,
							response, selectedProvince.getId());
				} else if ("county".equals(type)) {
					//县级处理
					result = Utility.handleCountiesResponse(coolWeatherDB,
							response, selectedCity.getId());
				}
				if (result) {
					// 通过runOnUiThread()方法回到主线程处理逻辑
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							//关闭进度显示窗口
							closeProgressDialog();
							if ("province".equals(type)) {
								//查询省
								queryProvinces();
							} else if ("city".equals(type)) {
								//查询市
								queryCities();
							} else if ("county".equals(type)) {
								//查询县
								queryCounties();
							}
						}
					});
				}
			}

			@Override
			public void onError(Exception e) {
				// 通过runOnUiThread()方法回到主线程处理逻辑
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						//关闭进度窗口
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this,
										"加载失败", Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
	}
	
	/**
	 * 显示进度对话框
	 */
	private void showProgressDialog() {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("正在加载...");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}
	
	/**
	 * 关闭进度对话框
	 */
	private void closeProgressDialog() {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
	}
	
	/**
	 * 捕获Back按键，根据当前的级别来判断，此时应该返回市列表、省列表、还是直接退出。
	 */
	@Override
	public void onBackPressed() {
		if (currentLevel == LEVEL_COUNTY) {
			//当处在县级时，按返回键则回到市级，即查询市级
			queryCities();
		} else if (currentLevel == LEVEL_CITY) {
			//当处在市级时，按返回键则回到省级，即查询省级
			queryProvinces();
		} else {
			//跳转activity,防止，一打开就处于县级
			if (isFromWeatherActivity) {
				Intent intent = new Intent(this, WeatherActivity.class);
				startActivity(intent);
			}
			finish();
		}
	}

}