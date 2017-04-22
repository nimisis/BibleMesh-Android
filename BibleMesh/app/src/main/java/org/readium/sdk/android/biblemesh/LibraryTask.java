package org.readium.sdk.android.biblemesh;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.CookieManager;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.readium.sdk.android.Container;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class LibraryTask extends AsyncTask<Integer, Integer, Long> {

	private Activity activity;
	private Boolean launch;

	public LibraryTask(Activity activity, Boolean launchlibrary) {
		this.activity = activity;
		this.launch = launchlibrary;
	}

	@Override
	protected void onCancelled() {
		Log.v("LibraryTask", "onCancelled");
	}
		
	@Override
	protected void onPreExecute() {
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

				DBHelper dbHelper = new DBHelper(activity);
				DBCursor cursor = dbHelper.getLocations();

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
		if (launch) {
			Intent intent = new Intent(activity.getApplicationContext(),
					ContainerList.class);
			((LoginActivity) activity).startActivity(intent);
		} else {
			final DBHelper dbHelper = new DBHelper(activity);
			DBCursor cursor = dbHelper.getLocations();
			List<EPubTitle> booksArray = new ArrayList<EPubTitle>();
			for (int rowNum = 0; rowNum < cursor.getCount(); rowNum++) {
				cursor.moveToPosition(rowNum);
				EPubTitle ep = new EPubTitle();
				ep.downloadStatus = cursor.getColDownloadStatus();
				ep.bookID = cursor.getColBookID();
				//reset downloadStatus so it is never stuck in "downloading" phase
				if (ep.downloadStatus == 1) {
					ep.downloadStatus = 0;
					dbHelper.setDownloadStatus(ep.bookID, 0);
				}
				ep.fsize = cursor.getColFSize();
				ep.author = cursor.getColAuthor();
				ep.title = cursor.getColTitle();
				ep.coverHref = cursor.getColCoverHref();
				booksArray.add(ep);
			}
			ListView view = (ListView) activity.findViewById(R.id.containerList);
			((ContainerList) activity).bookListAdapter = new BookAdapter(activity, booksArray);
			view.setAdapter(((ContainerList) activity).bookListAdapter);
		}
    }
}
