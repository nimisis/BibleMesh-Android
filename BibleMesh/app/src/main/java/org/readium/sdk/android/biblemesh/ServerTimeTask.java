package org.readium.sdk.android.biblemesh;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.TimeZone;

public class ServerTimeTask extends AsyncTask<Integer, Integer, Long> {

	private LoginActivity activity;

	public ServerTimeTask(LoginActivity activity) {
		this.activity = activity;
	}

	@Override
	protected void onCancelled() {
		Log.v("ServerTimeTask", "onCancelled");
	}
		
	@Override
	protected void onPreExecute() {
		/*dialog.show(activity, "title", "message", false, true, new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				// TODO Auto-generated method stub
				Log.v("progress", "cancel");
			}
		});*/
	}

	protected Long doInBackground(Integer... vid) {
        long totalSize = 0;
		HttpURLConnection httpConn = null;
		try {
			URL url = new URL("https://read.biblemesh.com/currenttime.json");
			httpConn = (HttpURLConnection) url.openConnection();
			//httpConn.setDoOutput(true);
			httpConn.setRequestMethod("GET");
			//httpConn.setUseCaches(false);
			//httpConn.setAllowUserInteraction(false);
			//httpConn.setConnectTimeout(timeout);
			//httpConn.setReadTimeout(timeout);
			httpConn.connect();

			int responseCode = httpConn.getResponseCode();

			Log.v("webview", "Response: "+responseCode);

			// always check HTTP response code first
			if (responseCode == HttpURLConnection.HTTP_OK) {
				BufferedReader br = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = br.readLine()) != null) {
					sb.append(line + "\n");
				}
				br.close();
				Log.v("login", "servertime:"+sb.toString());
				JSONObject jsonObject = new JSONObject(sb.toString());
				Long serverTime = jsonObject.getLong("currentServerTime");
				Long unixtime = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
				Log.v("login", "diff2: " + (serverTime - unixtime));
				activity.serverTimeOffset = serverTime - unixtime;
			} else {
				System.out.println("Server replied HTTP code: " + responseCode);
			}
		} catch (IOException e) {
			Log.d("err", "Error: " + e);
			//vid[0].downloadStatus = 0;
		} catch (JSONException e) {
			Log.v("json", e.getMessage());
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
    	Log.v("ServerTimeTask", "onPostExecute");

	    //// FIXME: 03/01/2017 still need to request titles
	    Intent intent = new Intent(activity.getApplicationContext(),
			    ContainerList.class);
	    activity.startActivity(intent);
    }
}
