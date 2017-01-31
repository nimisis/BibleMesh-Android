package org.readium.sdk.android.biblemesh;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.Toolbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.TimeZone;

public class LoginActivity extends Activity {

	static public Integer userID;
	static public Long serverTimeOffset;
	//public LLocation llocation;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		//Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		//setSupportActionBar(toolbar);

		userID = 1;
		serverTimeOffset = 0L;
//		//llocation = new LLocation();
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

				DBHelper dbHelper = new DBHelper(getBaseContext());
				DBCursor cursor = dbHelper.getLocations(userID);
				Log.v("direct", String.format("Got %d locations", cursor.getCount()));
				if (cursor.getCount() == 0) {
					cursor = null;
				}

				try {
					JSONArray jArray = new JSONArray(result);
					for (int i = 0; i < jArray.length(); i++) {
						JSONObject jsonobject = jArray.getJSONObject(i);
						Integer bookID = jsonobject.getInt("id");
						while ((cursor != null) && (cursor.getColBookID() < bookID)) {
							//delete
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
							dbHelper.insertLocation(bookID, userID);
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
				} catch (JSONException e) {
					Log.v("json", e.getMessage());
				}
			}

			Integer dummy = 1;
			new ServerTimeTask(LoginActivity.this).execute(dummy);
		}
	}
}
