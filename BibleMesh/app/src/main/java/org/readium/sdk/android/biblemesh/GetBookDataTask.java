package org.readium.sdk.android.biblemesh;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.webkit.CookieManager;

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
		//// FIXME: 26/01/2017 reset downloadStatus
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

	protected Long doInBackground(EPubTitle... vid) {
        long totalSize = 0;
			HttpURLConnection httpConn = null;
			DBCursor cursor = dbHelper.getLocation(vid[0].bookID);
			idref = cursor.getColIDRef();
			cfi = cursor.getColElementCFI();

			//DBCursor c = dbHelper.getHighlights(vid[0].bookID, LoginActivity.userID);
			//Log.v("background" , "num highlights:"+ c.getCount());
			try {
		        String url = "https://read.biblemesh.com/users/"+LoginActivity.userID+"/books/"+vid[0].bookID.toString()+".json";

				URL resourceUrl = new URL(url);
				httpConn = (HttpURLConnection) resourceUrl.openConnection();
		        httpConn.setRequestMethod("GET");
		        //httpConn.setRequestProperty("Content-length", "0");
		        //httpConn.setUseCaches(false);
		        //httpConn.setAllowUserInteraction(false);
		        //httpConn.setConnectTimeout(timeout);
		        //httpConn.setReadTimeout(timeout);

				String cookies = CookieManager.getInstance().getCookie(url);
				if (cookies != null) {
					Log.v("getbookdatatask", "have cookies");
					httpConn.setRequestProperty("Cookie", cookies);
				}

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

			        /* pseudo code
                 for each server highlight, look for matching in local,
                 if match found
                 compare lastupdated value
                 if server value is newer (i.e. bigger)
                 update local
                 if server value is older (i.e. smaller)
                 ignore
                 if highlight is in queue to update the server (unlikely)
                 ignore
                 else
                 update server (should never happen as !)
                 if same
                 ignore (no change)
                 else
                 add highlight to local

                 for each local highlight not matched above
                 if highlight is in queue to update the server
                 ignore
                 else
                 delete (must have been deleted somewhere else)
                */
			        DBCursor hcursor = dbHelper.getHighlights(vid[0].bookID);
			        Log.v("getbookdata", "numhighlights:"+hcursor.getCount());
					Boolean validHighlights[] = new Boolean[hcursor.getCount()];
			        for (int i = 0; i < hcursor.getCount(); i++) {
				        validHighlights[i] = false;
			        }
			        JSONArray jArray = jsonobject.getJSONArray("highlights");
			        for (int i = 0; i < jArray.length(); i++) {
				        JSONObject jsonhobject = jArray.getJSONObject(i);
				        String hcfi = jsonhobject.getString("cfi");
				        String hidref = jsonhobject.getString("spineIdRef");

				        Boolean foundMatch = false;
				        hcursor.moveToFirst();
				        for (int j = 0; j < hcursor.getCount(); j++) {
							if (hcursor.getColCFI().equals(hcfi) &&
									hcursor.getColIDRef().equals(hidref)) {
								foundMatch = true;
								validHighlights[j] = true;
								if (jsonhobject.getLong("updated_at") > hcursor.getColLastUpdated()) {
									dbHelper.updateHighlight(hcursor.getColID(),
											vid[0].bookID,
											jsonhobject.getInt("color"),
											jsonhobject.getString("note"),
											jsonhobject.getLong("updated_at"), 0);
								} else if (jsonhobject.getLong("updated_at") < hcursor.getColLastUpdated()) {
									//ignore
								} else {
									//ignore
								}
							}
					        hcursor.moveToNext();
				        }
				        if (!foundMatch) {
					        Log.v("getbookdata", "insert highlight");
					        dbHelper.insertHighlight(vid[0].bookID,
							        hidref,
							        hcfi,
							        jsonhobject.getInt("color"),
							        jsonhobject.getString("note"),
							        jsonhobject.getLong("updated_at"), 0);
				        }
			        }
			        //remove unmatched
			        hcursor.moveToFirst();
			        Log.v("getbookdata", "num:"+hcursor.getCount());
			        for (int i = 0; i < hcursor.getCount(); i++) {
				        if (validHighlights[i]) {
					        //skip
					        Log.v("getbookdata", "skip removing this highlight");
				        } else {
					        Log.v("getbookdata", "remove highlight:"+hcursor.getColIDRef());
					        dbHelper.removeHighlight(hcursor.getColID());
				        }
				        hcursor.moveToNext();
			        }

			        /*for (int i = 0; i < hcursor.getCount(); i++) {

				        hcursor.moveToNext();
			        }*/
			        //remove highlights
			        /*dbHelper.removeHighlights(vid[0].bookID);

			        //insert highlights
			        JSONArray jArray = jsonobject.getJSONArray("highlights");
			        for (int i = 0; i < jArray.length(); i++) {
				        JSONObject jsonhobject = jArray.getJSONObject(i);
				        dbHelper.insertHighlight(vid[0].bookID, jsonhobject.getString("spineIdRef"),
						        jsonhobject.getString("cfi"),
						        jsonhobject.getInt("color"),
						        jsonhobject.getString("note"),
						        jsonhobject.getLong("updated_at"));
			        }*/


					if (updated_at > cursor.getColLastUpdated()) {
						Log.v("getbook", "server is newer");

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
