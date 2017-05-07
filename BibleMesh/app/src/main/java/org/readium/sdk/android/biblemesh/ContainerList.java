//  Copyright (c) 2014 Readium Foundation and/or its licensees. All rights reserved.
//  Redistribution and use in source and binary forms, with or without modification, 
//  are permitted provided that the following conditions are met:
//  1. Redistributions of source code must retain the above copyright notice, this 
//  list of conditions and the following disclaimer.
//  2. Redistributions in binary form must reproduce the above copyright notice, 
//  this list of conditions and the following disclaimer in the documentation and/or 
//  other materials provided with the distribution.
//  3. Neither the name of the organization nor the names of its contributors may be 
//  used to endorse or promote products derived from this software without specific 
//  prior written permission.
//
//  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
//  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
//  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
//  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
//  INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
//  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
//  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
//  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
//  OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
//  OF THE POSSIBILITY OF SUCH DAMAGE

package org.readium.sdk.android.biblemesh;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.json.JSONException;
import org.readium.sdk.android.EPub3;
import org.readium.sdk.android.Container;
import org.readium.sdk.android.biblemesh.model.BookmarkDatabase;
import org.readium.sdk.android.SdkErrorHandler;
import org.readium.sdk.android.biblemesh.model.OpenPageRequest;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Layout;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import android.app.AlertDialog;
import android.content.DialogInterface;

import com.google.analytics.tracking.android.EasyTracker;

//import com.google.android.gms.analytics.GoogleAnalytics;

/**
 * @author chtian
 */
public class ContainerList extends Activity implements SdkErrorHandler {
	public BookAdapter bookListAdapter;
	//List<EPubTitle> booksArray;
	final private String PATH = Environment.getExternalStorageDirectory() + "/Android/data/org.readium.sdk.android.biblemesh/";
	static Container container;

	protected abstract class SdkErrorHandlerMessagesCompleted {
		Intent m_intent = null;

		public SdkErrorHandlerMessagesCompleted(Intent intent) {
			m_intent = intent;
		}

		public void done() {
			if (m_intent != null) {
				once();
				m_intent = null;
			}
		}

		public abstract void once();
	}

	private Context context;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.container, menu);

		Typeface face = Typeface.createFromAsset(getAssets(),"fonts/fontawesome-webfont.ttf");
		TextDrawable faIcon = new TextDrawable(this);
		faIcon.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		faIcon.setTextAlign(Layout.Alignment.ALIGN_CENTER);
		faIcon.setTextColor(Color.parseColor("#ffffff"));
		faIcon.setTypeface(face);
		faIcon.setText(getResources().getText(R.string.fa_refresh));
		MenuItem menuItem = menu.findItem(R.id.refresh);
		menuItem.setIcon(faIcon);

		TextDrawable faIcon2 = new TextDrawable(this);
		faIcon2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		faIcon2.setTextAlign(Layout.Alignment.ALIGN_CENTER);
		faIcon2.setTextColor(Color.parseColor("#ffffff"));
		faIcon2.setTypeface(face);
		faIcon2.setText(getResources().getText(R.string.fa_logout));
		MenuItem menuItem2 = menu.findItem(R.id.logout);
		menuItem2.setIcon(faIcon2);

		return true;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.container_list);

		//uri scheme
		Intent intent = getIntent();
		Uri data = intent.getData();
		if (data != null) {
			//handle the request (consider need to login)
			List<String> params = data.getPathSegments();
			String first = params.get(0);
			String second = params.get(1);
			Log.v("uri scheme params", "first:" + first + " second:" + second);
		}

		context = this;
		BookmarkDatabase.initInstance(getApplicationContext());

		final ListView view = (ListView) findViewById(R.id.containerList);

		final DBHelper dbHelper = DBHelper.getInstance(getApplicationContext());//new DBHelper(getApplicationContext());
		DBCursor cursor = dbHelper.getLocations();

		final List<EPubTitle> booksArray = new ArrayList<EPubTitle>();
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

		//BookAdapter
		bookListAdapter = new BookAdapter(this, booksArray);//, R.layout.container_list);
		view.setAdapter(bookListAdapter);

		view.setOnItemClickListener(new ListView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
			                        long arg3) {
				EPubTitle ep = booksArray.get(arg2);
				Integer downloadStatus = ep.downloadStatus;//cursor.getColDownloadStatus();
				Log.v("library", "downloadStatus is " + downloadStatus);
				Boolean downloadIt = false;
				switch(downloadStatus) {
					case 0:
					case 2:
						//if file exists, open it
						String fstr = PATH + "book_"+Integer.toString(ep.bookID)+".epub";
						File f = new File(fstr);
						if (f.exists()) {
							Log.v("library", "file exists:"+f.length());

							//fix do comparison between expectedfsize and received size
							if (ep.fsize != f.length()) {
								Log.v("library", "unexpected file size, delete and re-download");
								f.delete();
								downloadIt = true;
							} else {
								view.setEnabled(false);
								LoginActivity.bookID = ep.bookID;
								new GetBookDataTask(ContainerList.this, dbHelper, fstr).execute(ep);
							}
						} else {//otherwise download it
							Log.v("library", "download it");
							downloadIt = true;
						}
						break;
					case 1:
						//download in progress
						Log.v("library", "download in progress");
						break;
					default:
						//assert
						break;
				}

				if (downloadIt) {
					//check internet connection status
					//if not connected, alert
					//else start download
					//if (NetworkUtil.getConnectivityStatus(getApplicationContext()) == NetworkUtil.TYPE_NOT_CONNECTED) {

					switch (NetworkUtil.getConnectivityStatus(context)) {
						case 0://TYPE_NOT_CONNECTED
						{
							//// FIXME: 25/01/2017
							Log.v("library", "Not connected");
						}
						break;
						case 1: //TYPE_WIFI
						case 2: //TYPE_MOBILE
						{
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
								if (PackageManager.PERMISSION_GRANTED == checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
									//storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "myPhoto");
									new DownloadTask(ContainerList.this, dbHelper).execute(ep);
								} else {
									requestPermission(context);
								}
							} else {
								new DownloadTask(ContainerList.this, dbHelper).execute(ep);
							}
							//// FIXME: 25/01/2017 do check about wifi/mobile
						}

					}
				}
			}
		});

		// Loads the native lib and sets the path to use for cache
		Log.v("library", "cache path:"+getCacheDir().getAbsolutePath());
		EPub3.setCachePath(getCacheDir().getAbsolutePath());
	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
			case R.id.refresh:
				Log.v("container", "refresh");
				Integer dummy = 1;
				new LibraryTask(this, false).execute(dummy);
				return true;
			case R.id.logout:
				Log.v("container", "logout");

				AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);

				alertBuilder.setTitle("Log out");
				alertBuilder.setMessage("Are you sure you want to log out?");

				alertBuilder.setCancelable(false);

				alertBuilder.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								new LogoutTask(ContainerList.this).execute(1);
								dialog.dismiss();
							}
						}
				);
				alertBuilder.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
							}
						}
				);

				AlertDialog alert = alertBuilder.create();
				alert.setCanceledOnTouchOutside(false);

				alert.show(); //async!

				return true;
		}
		return false;
	}

	private final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;

	private void requestPermission(final Context context){
		if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			// Provide an additional rationale to the user if the permission was not granted
			// and the user would benefit from additional context for the use of the permission.
			// For example if the user has previously denied the permission.

			new AlertDialog.Builder(context)
					.setMessage("Please grant storage permission")
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							requestPermissions(
									new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
									REQUEST_WRITE_EXTERNAL_STORAGE);
						}
					}).show();

		} else {
			// permission has not been granted yet. Request it directly.
			requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
		switch (requestCode) {
			case REQUEST_WRITE_EXTERNAL_STORAGE: {
				if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					Toast.makeText(context,
							"Permission granted",
							Toast.LENGTH_SHORT).show();

				} else {
					Toast.makeText(context,
							"Permission denied",
							Toast.LENGTH_SHORT).show();
					super.onRequestPermissionsResult(requestCode, permissions, grantResults);
				}
				//return;
			}
		}
	}

	private Stack<String> m_SdkErrorHandler_Messages = null;

	// async!
	private void popSdkErrorHandlerMessage(final Context ctx, final SdkErrorHandlerMessagesCompleted callback) {
		if (m_SdkErrorHandler_Messages != null) {

			if (m_SdkErrorHandler_Messages.size() == 0) {
				m_SdkErrorHandler_Messages = null;
				callback.done();
				return;
			}

			String message = m_SdkErrorHandler_Messages.pop();

			AlertDialog.Builder alertBuilder = new AlertDialog.Builder(ctx);

			alertBuilder.setTitle("EPUB warning");
			alertBuilder.setMessage(message);

			alertBuilder.setCancelable(false);

			alertBuilder.setOnCancelListener(
					new DialogInterface.OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							m_SdkErrorHandler_Messages = null;
							callback.done();
						}
					}
			);

			alertBuilder.setOnDismissListener(
					new DialogInterface.OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dialog) {
							popSdkErrorHandlerMessage(ctx, callback);
						}
					}
			);

			alertBuilder.setPositiveButton("Ignore",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					}
			);
			alertBuilder.setNegativeButton("Ignore all",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					}
			);

			AlertDialog alert = alertBuilder.create();
			alert.setCanceledOnTouchOutside(false);

			alert.show(); //async!
		} else {
			callback.done();
		}
	}

	@Override
	public boolean handleSdkError(String message, boolean isSevereEpubError) {

		System.out.println("SdkErrorHandler: " + message + " (" + (isSevereEpubError ? "warning" : "info") + ")");

		if (m_SdkErrorHandler_Messages != null && isSevereEpubError) {
			m_SdkErrorHandler_Messages.push(message);
		}

		// never throws an exception
		return true;
	}

	@Override
	protected void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
		//GoogleAnalytics.getInstance(this).reportActivityStart(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
		//GoogleAnalytics.getInstance(this).reportActivityStop(this);
	}

	void openBook(String fstr, String idref, String cfi) {
		m_SdkErrorHandler_Messages = new Stack<String>();

		EPub3.setSdkErrorHandler(ContainerList.this);
		//Container
				container = EPub3.openBook(fstr);
		EPub3.setSdkErrorHandler(null);

		ContainerHolder.getInstance().put(container.getNativePtr(), container);

		//Intent intent = new Intent(getApplicationContext(), BookDataActivity.class);
		//intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		//intent.putExtra(Constants.BOOK_NAME, bookName);
		//intent.putExtra(Constants.CONTAINER_ID, container.getNativePtr());

		Intent intent = new Intent(context, WebViewActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(Constants.CONTAINER_ID, container.getNativePtr());
		Log.v("task2", "idref:"+idref+" cfi:"+cfi);
		OpenPageRequest openPageRequest = OpenPageRequest.fromIdrefAndCfi(idref, cfi);
		try {
			intent.putExtra(Constants.OPEN_PAGE_REQUEST_DATA, openPageRequest.toJSON().toString());
			//startActivity(intent);
		} catch (JSONException e) {
			Log.e("Biblemesh", "" + e.getMessage(), e);
		}

		SdkErrorHandlerMessagesCompleted callback = new SdkErrorHandlerMessagesCompleted(intent) {
			@Override
			public void once() {
				startActivity(m_intent);
			}
		};

		// async!
		popSdkErrorHandlerMessage(context, callback);

		//re-enable listview after preventing double-click
		final ListView view = (ListView) findViewById(R.id.containerList);
		view.setEnabled(true);
	}

}
