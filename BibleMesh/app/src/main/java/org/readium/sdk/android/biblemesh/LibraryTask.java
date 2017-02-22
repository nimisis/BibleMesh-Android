package org.readium.sdk.android.biblemesh;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.CookieManager;

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

public class LibraryTask extends AsyncTask<Integer, Integer, Long> {

	private LoginActivity activity;

	public LibraryTask(LoginActivity activity) {
		this.activity = activity;
	}

	@Override
	protected void onCancelled() {
		Log.v("LibraryTask", "onCancelled");
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
			String url = "https://read.biblemesh.com/epub_content/epub_library.json";
			URL resourceUrl = new URL(url);//usersetup.json");
			httpConn = (HttpURLConnection) resourceUrl.openConnection();
			//httpConn.setDoOutput(true);
			httpConn.setRequestMethod("GET");
			//httpConn.setUseCaches(false);
			//httpConn.setAllowUserInteraction(false);
			//httpConn.setConnectTimeout(timeout);
			//httpConn.setReadTimeout(timeout);

			String cookies = CookieManager.getInstance().getCookie(url);
			if (cookies != null) {
				Log.v("librarytask", "have cookies");
				httpConn.setRequestProperty("Cookie", cookies);
			}

			httpConn.connect();

			int responseCode = httpConn.getResponseCode();

			Log.v("librarytask", "Response: "+responseCode);

			// always check HTTP response code first
			if (responseCode == HttpURLConnection.HTTP_OK) {
				BufferedReader br = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = br.readLine()) != null) {
					sb.append(line + "\n");
				}
				br.close();
				Log.v("login", "librarytask:"+sb.toString());
				/*JSONObject jsonObject = new JSONObject(sb.toString());
				Long serverTime = jsonObject.getLong("currentServerTime");
				Long unixtime = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
				Log.v("login", "diff2: " + (serverTime - unixtime));
				activity.serverTimeOffset = serverTime - unixtime;*/


				DBHelper dbHelper = new DBHelper(activity);
				DBCursor cursor = dbHelper.getLocations(LoginActivity.userID);

				/*for(int i = 0; i < cursor.getCount(); i++) {
					Log.v("locs", "bookID:"+cursor.getColBookID()+" booktitle:"+cursor.getColTitle());
					cursor.moveToNext();
				}
				cursor.moveToFirst();*/

				Log.v("direct", String.format("Got %d locations", cursor.getCount()));
				if (cursor.getCount() == 0) {
					cursor = null;
				}

				try {
					JSONArray jArray = new JSONArray(sb.toString());
					for (int i = 0; i < jArray.length(); i++) {
						JSONObject jsonobject = jArray.getJSONObject(i);
						Integer bookID = jsonobject.getInt("id");
						while ((cursor != null) && (cursor.getColBookID() < bookID)) {
							//delete
							dbHelper.deleteLocation(cursor.getColBookID());
							//fix delete all associated highlights too.
							if (!cursor.moveToNext()) {
								cursor = null;
							}
						}
						Boolean insertNew = false;
						if (cursor != null) {
							if (cursor.getColBookID() == bookID) {
								if (!cursor.moveToNext()) {
									cursor = null;
								}
							} else {
								insertNew = true;
							}
						} else {
							insertNew = true;
						}
						if (insertNew) {
							Log.v("db", "inserting new location");
							dbHelper.insertLocation(bookID, LoginActivity.userID);
							//search books for this bookid
							DBCursor cursor2 = dbHelper.getBook(bookID);
							//if does not exist, insert
							Integer cnt = cursor2.getCount();
							if (cnt == 0) {
								Log.v("db", "inserting new book");
								String title = jsonobject.getString("title");
								String author = jsonobject.getString("author");
								String coverHref = jsonobject.getString("coverHref");
								String rootURL = jsonobject.getString("rootUrl");
								String updatedAtStr = jsonobject.getString("updated_at");
								dbHelper.insertBook(bookID, title, author, coverHref, rootURL, updatedAtStr);
							} else if (cnt > 1) {
								//fix assert
							}
						}
					}
					//delete leftovers
					while (cursor != null) {
						//delete
						dbHelper.deleteLocation(cursor.getColBookID());
						//fix delete all associated highlights too.
						if (!cursor.moveToNext()) {
							cursor = null;
						}
					}
				} catch (JSONException e) {
					Log.v("json", e.getMessage());
				}

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
    	Log.v("ServerTimeTask", "onPostExecute");

	    Intent intent = new Intent(activity.getApplicationContext(),
			    ContainerList.class);
	    activity.startActivity(intent);
    }
}
