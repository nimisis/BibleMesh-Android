package org.readium.sdk.android.biblemesh;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Stack;

import org.json.JSONException;
import org.readium.sdk.android.EPub3;
import org.readium.sdk.android.Container;
import org.readium.sdk.android.biblemesh.model.BookmarkDatabase;
import org.readium.sdk.android.SdkErrorHandler;
import org.readium.sdk.android.biblemesh.model.OpenPageRequest;

public class GetBookDataTask extends AsyncTask<EPubTitle, Integer, Long> {


	private ContainerList activity;
	private DBHelper dbHelper;
	private String fstr;
	private String idref;
	private String cfi;

	public GetBookDataTask(ContainerList activity, DBHelper dbHelper, String fstr) {
		this.activity = activity;
		this.dbHelper = dbHelper;
		this.fstr = fstr;
		this.idref = null;
		this.cfi = null;
	}

	@Override
	protected void onCancelled() {
		Log.v("GetBookDataTask", "onCancelled");
		//dbHelper
		//// FIXME: 26/01/2017 reset downloadStatus
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

	protected Long doInBackground(EPubTitle... vid) {
        long totalSize = 0;
			HttpURLConnection httpConn = null;
			DBCursor cursor = dbHelper.getLocation(LoginActivity.userID, vid[0].bookID);
			idref = cursor.getColIDRef();
			cfi = cursor.getColElementCFI();

			//DBCursor c = dbHelper.getHighlights(vid[0].bookID, LoginActivity.userID);
			//Log.v("background" , "num highlights:"+ c.getCount());
			try {
		        URL url = new URL("https://read.biblemesh.com/users/"+LoginActivity.userID+"/books/"+vid[0].bookID.toString()+".json");
		        httpConn = (HttpURLConnection) url.openConnection();
		        httpConn.setRequestMethod("GET");
		        //httpConn.setRequestProperty("Content-length", "0");
		        //httpConn.setUseCaches(false);
		        //httpConn.setAllowUserInteraction(false);
		        //httpConn.setConnectTimeout(timeout);
		        //httpConn.setReadTimeout(timeout);
		        httpConn.connect();
		        int responseCode = httpConn.getResponseCode();

		        // always check HTTP response code first
		        if (responseCode == HttpURLConnection.HTTP_OK) {
			        //fix String contentType = httpConn.getContentType();
			        //int contentLength = httpConn.getContentLength();

			        BufferedReader br = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
			        StringBuilder sb = new StringBuilder();
			        String line;
			        while ((line = br.readLine()) != null) {
				        sb.append(line+"\n");
			        }
			        br.close();

			        String str = sb.toString();

			        Log.v("getbooktask", str);

			        JSONObject jsonobject = new JSONObject(sb.toString());
			        String latest_location = jsonobject.getString("latest_location");
			        JSONObject jsonlobject = new JSONObject(latest_location);
			        String sidref = jsonlobject.getString("idref");
			        String selementCfi = jsonlobject.getString("elementCfi");
			        Long updated_at = jsonobject.getLong("updated_at");

			        //fixme not sure about this logic
					if (updated_at > cursor.getColLastUpdated()) {
						Log.v("getbook", "server is newer");
						//remove highlights
						dbHelper.removeHighlights(vid[0].bookID);

						//insert highlights
						JSONArray jArray = jsonobject.getJSONArray("highlights");
						for (int i = 0; i < jArray.length(); i++) {
							JSONObject jsonhobject = jArray.getJSONObject(i);
							String cfi = jsonhobject.getString("cfi");
							Integer color = jsonhobject.getInt("color");
							String note = jsonhobject.getString("note");
							Long hupdated_at = jsonhobject.getLong("updated_at");
							dbHelper.insertHighlight(vid[0].bookID, cfi, color, note, hupdated_at);
						}

						idref = sidref;
						cfi = selementCfi;
						dbHelper.setLocation(vid[0].bookID, idref, cfi, updated_at);
					} else {
						Log.v("getbook", "use local data");
					}

		        } else {
			        System.out.println("No file to download. Server replied HTTP code: " + responseCode);
		        }

                Log.v("doInBackground", "done");
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
    	Log.v("GetBookDataTask", "onPostExecute");

		Log.v("task", "idref:"+idref+" cfi:"+cfi);
	    activity.openBook(fstr, idref, cfi);

    	//DownloadsAdapter va = (DownloadsAdapter) activity.getListView().getAdapter();
    	//va.notifyDataSetChanged();

    }
}
