package org.readium.sdk.android.biblemesh;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.ValueCallback;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class LogoutTask extends AsyncTask<Integer, Integer, Long> {

	private ContainerList activity;

	public LogoutTask(ContainerList activity) {
		this.activity = activity;
	}

	@Override
	protected void onCancelled() {
		Log.v("LogoutTask", "onCancelled");
	}
		
	@Override
	protected void onPreExecute() {
		/*dialog.show(activity, "title", "message", false, true, new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				Log.v("progress", "cancel");
			}
		});*/
	}

	protected Long doInBackground(Integer... vid) {
        long totalSize = 0;
		HttpURLConnection httpConn = null;
		try {
			String url = "https://read.biblemesh.com/logout";
			URL resourceUrl = new URL(url);
			httpConn = (HttpURLConnection) resourceUrl.openConnection();
			//httpConn.setDoOutput(true);
			httpConn.setRequestMethod("GET");
			//httpConn.setUseCaches(false);
			//httpConn.setAllowUserInteraction(false);
			//httpConn.setConnectTimeout(timeout);
			//httpConn.setReadTimeout(timeout);

			String cookies = CookieManager.getInstance().getCookie(url);
			if (cookies != null) {
				Log.v("logouttask", "have cookies");
				httpConn.setRequestProperty("Cookie", cookies);
			}

			httpConn.connect();

			int responseCode = httpConn.getResponseCode();

			Log.v("logout", "Response: "+responseCode);

			// always check HTTP response code first
			if (responseCode == HttpURLConnection.HTTP_OK) {


				BufferedReader br = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = br.readLine()) != null) {
					sb.append(line + "\n");
				}
				br.close();
				Log.v("login", "logout:"+sb.toString());

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					CookieManager.getInstance().removeAllCookies(null);
				} else {
					CookieManager.getInstance().removeAllCookie();
				}

				/*JSONObject jsonObject = new JSONObject(sb.toString());
				Long serverTime = jsonObject.getLong("currentServerTime");
				Long unixtime = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
				Log.v("login", "diff2: " + (serverTime - unixtime));
				activity.serverTimeOffset = serverTime - unixtime;*/
			} else {
				System.out.println("Server replied HTTP code: " + responseCode);
			}
		} catch (IOException e) {
			Log.d("err", "Error: " + e);
			//vid[0].downloadStatus = 0;
		} finally {
			if (httpConn != null) {
				httpConn.disconnect();
			}
		}
        return totalSize;
    }

    protected void onProgressUpdate(Integer... progress) {
    }

    protected void onPostExecute(Long result) {
    	Log.v("LogoutTask", "onPostExecute");

	    LoginActivity.firstload = true;
	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
		    LoginActivity.cookieManager.removeAllCookies(new ValueCallback<Boolean>() {
			    @Override
			    public void onReceiveValue(Boolean value) {
				    Log.d("removecookie", "onReceiveValue " + value);
			    }
		    });
	    } else {
		    LoginActivity.cookieManager.removeAllCookie();
	    }
	    /*Intent intent = new Intent(activity.getApplicationContext(),
			    ContainerList.class);
	    activity.startActivity(intent);*/
	    activity.finish();
    }
}
