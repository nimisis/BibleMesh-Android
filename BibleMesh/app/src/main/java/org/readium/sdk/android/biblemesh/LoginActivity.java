package org.readium.sdk.android.biblemesh;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//import java.net.CookieManager;
import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;


public class LoginActivity extends Activity {

	static public Integer userID;
	static public Integer bookID;
	static public Long serverTimeOffset;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		//Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		//setSupportActionBar(toolbar);

		userID = 1;
		bookID = 0;
		serverTimeOffset = 0L;
//		//llocation = new LLocation();

		//CookieHandler.setDefault(new CookieManager()); // Apparently for some folks this line works already, for me on Android 17 it does not.
		//CookieSyncManager.createInstance(yourContext);
		CookieManager cookieManager = CookieManager.getInstance();
		/*if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			CookieSyncManager.createInstance(this);
		}*/
		cookieManager.setAcceptCookie(true);


		PackageInfo pInfo = null;
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			String version = pInfo.versionName;
			TextView vtv = (TextView) findViewById(R.id.versionTextView);
			vtv.setText("v"+version);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void login(View view) {
		Intent intent = new Intent();
		intent.setClass(LoginActivity.this, LoginWebViewActivity.class);
		startActivityForResult(intent, 99);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			Bundle res = data.getExtras();
			if (res == null) {
				Log.v("result", "no token");
			} else {
				String result = res.getString("token");
				try {
					JSONObject jsonObject = new JSONObject(result);
					Long serverTime = jsonObject.getLong("currentServerTime");
					Long unixtime = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
					Log.v("login", "diff2: " + (serverTime - unixtime));
					serverTimeOffset = serverTime - unixtime;
					JSONObject jsonObject2 = jsonObject.getJSONObject("userInfo");
					userID = jsonObject2.getInt("id");
					Log.v("login", "id is "+userID);
				} catch (JSONException e) {
					Log.v("json", e.getMessage());
				}
			}

			Integer dummy = 1;
			new LibraryTask(LoginActivity.this).execute(dummy);
		}
	}
}
