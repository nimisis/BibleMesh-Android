/*
 * WebViewActivity.java
 * SDKLauncher-Android
 *
 * Created by Yonathan Teitelbaum (Mantano) on 2013-07-10.
 */
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.readium.sdk.android.Container;
import org.readium.sdk.android.ManifestItem;
import org.readium.sdk.android.Package;
import org.readium.sdk.android.SpineItem;
import org.readium.sdk.android.biblemesh.model.BookmarkDatabase;
import org.readium.sdk.android.biblemesh.model.OpenPageRequest;
import org.readium.sdk.android.biblemesh.model.Page;
import org.readium.sdk.android.biblemesh.model.PaginationInfo;
import org.readium.sdk.android.biblemesh.model.ReadiumJSApi;
import org.readium.sdk.android.biblemesh.model.ViewerSettings;
import org.readium.sdk.android.biblemesh.util.EpubServer;
import org.readium.sdk.android.biblemesh.util.EpubServer.DataPreProcessor;
import org.readium.sdk.android.biblemesh.util.HTMLUtil;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.VideoView;

import static org.readium.sdk.android.biblemesh.R.menu.container;

public class WebViewActivity extends FragmentActivity implements
		ViewerSettingsDialog.OnViewerSettingsChange {

	private final boolean quiet = false;

	private static final String TAG = "WebViewActivity";
	private static final String ASSET_PREFIX = "file:///android_asset/readium-shared-js/";
	private static final String READER_SKELETON = "file:///android_asset/readium-shared-js/reader.html";

	private Handler hide2;
	private Runnable r;
	// Installs "hook" function so that top-level window (application) can later
	// inject the window.navigator.epubReadingSystem into this HTML document's
	// iframe
	private static final String INJECT_EPUB_RSO_SCRIPT_1 = ""
			+ "window.readium_set_epubReadingSystem = function (obj) {"
			+ "\nwindow.navigator.epubReadingSystem = obj;"
			+ "\nwindow.readium_set_epubReadingSystem = undefined;"
			+ "\nvar el1 = document.getElementById(\"readium_epubReadingSystem_inject1\");"
			+ "\nif (el1 && el1.parentNode) { el1.parentNode.removeChild(el1); }"
			+ "\nvar el2 = document.getElementById(\"readium_epubReadingSystem_inject2\");"
			+ "\nif (el2 && el2.parentNode) { el2.parentNode.removeChild(el2); }"
			+ "\n};";

	// Iterate top-level iframes, inject global
	// window.navigator.epubReadingSystem if the expected hook function exists (
	// readium_set_epubReadingSystem() ).
	private static final String INJECT_EPUB_RSO_SCRIPT_2 = ""
			+ "var epubRSInject =\nfunction(win) {"
			+ "\nvar ret = '';"
			+ "\nret += win.location.href;"
			+ "\nret += ' ---- ';"
			+
			// "\nret += JSON.stringify(win.navigator.epubReadingSystem);" +
			// "\nret += ' ---- ';" +
			"\nif (win.frames)"
			+ "\n{"
			+ "\nfor (var i = 0; i < win.frames.length; i++)"
			+ "\n{"
			+ "\nvar iframe = win.frames[i];"
			+ "\nret += ' IFRAME ';"
			+ "\nif (iframe.readium_set_epubReadingSystem)"
			+ "\n{"
			+ "\nret += ' EPBRS ';"
			+ "\niframe.readium_set_epubReadingSystem(window.navigator.epubReadingSystem);"
			+ "\n}" + "\nret += epubRSInject(iframe);" + "\n}" + "\n}"
			+ "\nreturn ret;" + "\n};" + "\nepubRSInject(window);";

	// Script tag to inject the "hook" function installer script, added to the
	// head of every epub iframe document
	private static final String INJECT_HEAD_EPUB_RSO_1 = ""
			+ "<script id=\"readium_epubReadingSystem_inject1\" type=\"text/javascript\">\n"
			+ "//<![CDATA[\n" + INJECT_EPUB_RSO_SCRIPT_1 + "\n" + "//]]>\n"
			+ "</script>";
	// Script tag that generates an HTTP request to a fake script => triggers
	// push of window.navigator.epubReadingSystem into this HTML document's
	// iframe
	private static final String INJECT_HEAD_EPUB_RSO_2 = ""
			+ "<script id=\"readium_epubReadingSystem_inject2\" type=\"text/javascript\" "
			+ "src=\"/%d/readium_epubReadingSystem_inject.js\"> </script>";
	// Script tag to load the mathjax script payload, added to the head of epub
	// iframe documents, only if <math> tags are detected
	private static final String INJECT_HEAD_MATHJAX = "<script type=\"text/javascript\" src=\"/readium_MathJax.js\"> </script>";

	// Location of payloads in the asset folder
	private static final String PAYLOAD_MATHJAX_ASSET = "reader-payloads/MathJax.js";
	private static final String PAYLOAD_ANNOTATIONS_CSS_ASSET = "reader-payloads/annotations.css";

	private final DataPreProcessor dataPreProcessor = new DataPreProcessor() {

		@Override
		public byte[] handle(byte[] data, String mime, String uriPath,
				ManifestItem item) {
			if (mime == null
					|| (mime != "text/html" && mime != "application/xhtml+xml")) {
				return null;
			}

			if (!quiet)
				Log.d(TAG, "PRE-PROCESSED HTML: " + uriPath);

			String htmlText = new String(data, Charset.forName("UTF-8"));

			// String uuid = mPackage.getUrlSafeUniqueID();
			String newHtml = htmlText; // HTMLUtil.htmlByReplacingMediaURLsInHTML(htmlText,
										// cleanedUrl, uuid);
										// //"PackageUUID"

			// Set up the script tags to add to the head
			String tagsToInjectToHead = INJECT_HEAD_EPUB_RSO_1
			// Slightly change fake script src url with an
			// increasing count to prevent caching of the
			// request
					+ String.format(INJECT_HEAD_EPUB_RSO_2,
							++mEpubRsoInjectCounter);
			// Checks for the existance of MathML => request
			// MathJax payload
			if (newHtml.contains("<math") || newHtml.contains("<m:math")) {
				tagsToInjectToHead += INJECT_HEAD_MATHJAX;
			}

			newHtml = HTMLUtil.htmlByInjectingIntoHead(newHtml,
					tagsToInjectToHead);

			// Log.d(TAG, "HTML head inject: " + newHtml);

			return newHtml.getBytes();
		}
	};

	private WebView mWebview;
	private Container mContainer;
	private Package mPackage;
	private OpenPageRequest mOpenPageRequestData;
	private ProgressBar mProgress;
	private ViewerSettings mViewerSettings;
	private ReadiumJSApi mReadiumJSApi;
	private EpubServer mServer;

	private boolean mIsMoAvailable;
	private boolean mIsMoPlaying;
	private int mEpubRsoInjectCounter = 0;

	class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
		private static final String DEBUG_TAG = "Gestures";

		/*@Override
		public boolean onDown(MotionEvent event) {
			Log.d(DEBUG_TAG,"onDown: " + event.toString());
			return false;
		}*/

		@Override
		public boolean onFling(MotionEvent event1, MotionEvent event2,
		                       float velocityX, float velocityY) {
			//Log.d(DEBUG_TAG, "onFling: " + event1.toString()+event2.toString());
			if (velocityX > 0) {
				mReadiumJSApi.openPageLeft();
			} else {
				mReadiumJSApi.openPageRight();
			}
			return true;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		setContentView(R.layout.activity_web_view);

		final int abTitleId = getResources().getIdentifier("action_bar_title", "id", "android");
		findViewById(abTitleId).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});

		mWebview = (WebView) findViewById(R.id.webview);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
				&& 0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE)) {
			WebView.setWebContentsDebuggingEnabled(true);
		}

		mProgress = (ProgressBar) findViewById(R.id.progressBar);
		initWebView();

		final GestureDetector gestureDetector = new GestureDetector(this, new MyGestureListener());

		mWebview.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		});

		Intent intent = getIntent();
		if (intent.getFlags() == Intent.FLAG_ACTIVITY_NEW_TASK) {
			Bundle extras = intent.getExtras();
			if (extras != null) {
				mContainer = ContainerHolder.getInstance().get(
						extras.getLong(Constants.CONTAINER_ID));
				if (mContainer == null) {
					finish();
					return;
				}
				mPackage = mContainer.getDefaultPackage();

				String rootUrl = "http://" + EpubServer.HTTP_HOST + ":"
						+ EpubServer.HTTP_PORT + "/";
				mPackage.setRootUrls(rootUrl, null);

				try {
					mOpenPageRequestData = OpenPageRequest.fromJSON(extras
							.getString(Constants.OPEN_PAGE_REQUEST_DATA));
				} catch (JSONException e) {
					Log.e(TAG,
							"Constants.OPEN_PAGE_REQUEST_DATA must be a valid JSON object: "
									+ e.getMessage(), e);
				}
			}
		}

		// No need, EpubServer already launchers its own thread
		// new AsyncTask<Void, Void, Void>() {
		// @Override
		// protected Void doInBackground(Void... params) {
		// //xxx
		// return null;
		// }
		// }.execute();

		mServer = new EpubServer(EpubServer.HTTP_HOST, EpubServer.HTTP_PORT,
				mPackage, quiet, dataPreProcessor);
		mServer.startServer();

		// Load the page skeleton
		mWebview.loadUrl(READER_SKELETON);
		mViewerSettings = new ViewerSettings(
				ViewerSettings.SyntheticSpreadMode.SINGLE,
				ViewerSettings.ScrollMode.AUTO, 100, 20);

		mReadiumJSApi = new ReadiumJSApi(new ReadiumJSApi.JSLoader() {
			@Override
			public void loadJS(String javascript) {
				mWebview.loadUrl(javascript);
			}
		});

		/*Button back = (Button)findViewById(R.id.btnBack);
		back.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
				if(getActionBar.isShowing())
				{
					hideActionBar();
				}
				else
				{
					getActionBar.show();
					hideActionBar();
				}
			}
		});*/
		r = new Runnable() {
			@Override
			public void run() {
				getActionBar().hide();
				//getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
			}
		};
		hide2 = null;
		hideActionBar();
		//ActionBar actionBar = getActionBar();
		//actionBar.hide();
		//getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
	}

	public void showActionBar(View v) {
		getActionBar().show();
		hideActionBar();
	}

	public void hideActionBar()
	{
		if (hide2 != null) {
			hide2.removeCallbacks(r);
		}

		hide2 = new Handler();

		hide2.postDelayed(r, 3000); // e.g. 3000 milliseconds
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mServer.stop();
		mWebview.loadUrl(READER_SKELETON);
		((ViewGroup) mWebview.getParent()).removeView(mWebview);
		mWebview.removeAllViews();
		mWebview.clearCache(true);
		mWebview.clearHistory();
		mWebview.destroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			mWebview.onPause();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			mWebview.onResume();
		}
	}

	@SuppressLint({ "SetJavaScriptEnabled", "NewApi" })
	private void initWebView() {
		mWebview.getSettings().setJavaScriptEnabled(true);

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
			mWebview.getSettings().setMediaPlaybackRequiresUserGesture(false);
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			mWebview.getSettings().setAllowUniversalAccessFromFileURLs(true);
		}
		mWebview.setWebViewClient(new EpubWebViewClient());
		mWebview.setWebChromeClient(new EpubWebChromeClient());

		mWebview.addJavascriptInterface(new EpubInterface(), "LauncherUI");
	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
			case R.id.add_bookmark:
				if (!quiet)
					Log.d(TAG, "Add a bookmark");
				mReadiumJSApi.bookmarkCurrentPage();
				//mReadiumJSApi.highlightSelection();
				return true;
			/*case R.id.add_highlight:
				if (!quiet)
					Log.d(TAG, "Add highlight");
				mReadiumJSApi.highlightSelection();
				return true;*/
			case R.id.settings:
				if (!quiet)
					Log.d(TAG, "Show settings");
				showSettings();
				return true;
			case R.id.toc:
				if (!quiet)
					Log.d(TAG, "Show toc");
				showToC();
				return true;
			case R.id.mo_previous:
				mReadiumJSApi.previousMediaOverlay();
				return true;
			case R.id.mo_play:
				mReadiumJSApi.toggleMediaOverlay();
				return true;
			case R.id.mo_pause:
				mReadiumJSApi.toggleMediaOverlay();
				return true;
			case R.id.mo_next:
				mReadiumJSApi.nextMediaOverlay();
				return true;
		}
		return false;
	}

	/*public void onClick(View v) {
		if (v.getId() == R.id.left) {
			mReadiumJSApi.openPageLeft();
		} else if (v.getId() == R.id.right) {
			mReadiumJSApi.openPageRight();
		}
	}*/

	private void showSettings() {
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fm.beginTransaction();
		DialogFragment dialog = ViewerSettingsDialog.newInstance(this, mViewerSettings);
		/*Bundle args = new Bundle();
		args.putParcelable("settings", (Parcelable) mViewerSettings);
		args.putParcelable("listener", (Parcelable) this);
		dialog.setArguments(args);*/
		dialog.show(fm, "dialog");
		fragmentTransaction.commit();
	}

	private void showToC() {
		Log.v("web", "show ToC");

		Intent intent = new Intent(WebViewActivity.this, TableOfContentsActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(Constants.BOOK_NAME, ContainerList.container.getName());
		intent.putExtra(Constants.CONTAINER_ID, ContainerList.container.getNativePtr());

		startActivity(intent);
		finish();
	}

	@Override
	public void onViewerSettingsChange(ViewerSettings viewerSettings) {
		updateSettings(viewerSettings);
	}

	private void updateSettings(ViewerSettings viewerSettings) {
		mViewerSettings = viewerSettings;
		mReadiumJSApi.updateSettings(viewerSettings);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.web_view, menu);

		MenuItem mo_previous = menu.findItem(R.id.mo_previous);
		MenuItem mo_next = menu.findItem(R.id.mo_next);
		MenuItem mo_play = menu.findItem(R.id.mo_play);
		MenuItem mo_pause = menu.findItem(R.id.mo_pause);

		// show menu only when its reasonable

		mo_previous.setVisible(mIsMoAvailable);
		mo_next.setVisible(mIsMoAvailable);

		if (mIsMoAvailable) {
			mo_play.setVisible(!mIsMoPlaying);
			mo_pause.setVisible(mIsMoPlaying);
		}
		Typeface face = Typeface.createFromAsset(getAssets(),"fonts/fontawesome-webfont.ttf");
		TextDrawable faIcon = new TextDrawable(this);
		faIcon.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		faIcon.setTextAlign(Layout.Alignment.ALIGN_CENTER);
		faIcon.setTextColor(Color.parseColor("#ffffff"));
		faIcon.setTypeface(face);
		faIcon.setText(getResources().getText(R.string.fa_cog));
		TextDrawable faIcon2 = new TextDrawable(this);
		faIcon2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		faIcon2.setTextAlign(Layout.Alignment.ALIGN_CENTER);
		faIcon2.setTextColor(Color.parseColor("#ffffff"));
		faIcon2.setTypeface(face);
		faIcon2.setText(getResources().getText(R.string.fa_toc));
		MenuItem menuItem = menu.findItem(R.id.settings);
		menuItem.setIcon(faIcon);
		MenuItem menuItem2 = menu.findItem(R.id.toc);
		menuItem2.setIcon(faIcon2);

		/*MenuItem menuItem3 = menu.findItem(R.id.toc);
		item.setOnMenuItemClickListener
				(
						new MenuItem.OnMenuItemClickListener ()
						{
							public boolean onMenuItemClick(MenuItem item)
							{ return (showDirectory(item)); }
						}
				);*/

		return true;
	}

	public final class EpubWebViewClient extends WebViewClient {

		private static final String HTTP = "http";
		private static final String UTF_8 = "utf-8";

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			if (!quiet)
				Log.d(TAG, "onPageStarted: " + url);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			if (!quiet)
				Log.d(TAG, "onPageFinished: " + url);
		}

		@Override
		public void onLoadResource(WebView view, String url) {
			if (!quiet)
				Log.d(TAG, "onLoadResource: " + url);
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if (!quiet)
				Log.d(TAG, "shouldOverrideUrlLoading: " + url);
			return false;
		}

		private void evaluateJavascript(final String script) {

			runOnUiThread(new Runnable() {
				@Override
				public void run() {

					if (!quiet)
						Log.d(TAG, "WebView evaluateJavascript: " + script + "");

					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

						if (!quiet)
							Log.d(TAG, "WebView evaluateJavascript KitKat+ API");

						mWebview.evaluateJavascript(script,
								new ValueCallback<String>() {
									@Override
									public void onReceiveValue(String str) {
										if (!quiet)
											Log.d(TAG,
													"WebView evaluateJavascript RETURN: "
															+ str);
									}
								});
					} else {

						if (!quiet)
							Log.d(TAG, "WebView loadUrl() API");

						mWebview.loadUrl("javascript:var exec = function(){\n"
								+ script + "\n}; exec();");
					}
				}
			});
		}

		@Override
		public WebResourceResponse shouldInterceptRequest(WebView view,
				String url) {
			if (!quiet)
				Log.d(TAG, "-------- shouldInterceptRequest: " + url);

			if (url != null && url != "undefined") {

				String localHttpUrlPrefix = "http://" + EpubServer.HTTP_HOST
						+ ":" + EpubServer.HTTP_PORT;
				boolean isLocalHttp = url.startsWith(localHttpUrlPrefix);

				// Uri uri = Uri.parse(url);
				// uri.getScheme()

				if (url.startsWith("http") && !isLocalHttp) {
					if (!quiet)
						Log.d(TAG, "HTTP (NOT LOCAL): " + url);
					return super.shouldInterceptRequest(view, url);
				}

				String cleanedUrl = cleanResourceUrl(url, false);
				if (!quiet)
					Log.d(TAG, url + " => " + cleanedUrl);

				if (cleanedUrl
						.matches("\\/?\\d*\\/readium_epubReadingSystem_inject.js")) {
					if (!quiet)
						Log.d(TAG, "navigator.epubReadingSystem inject ...");

					// Fake script requested, this is immediately invoked after
					// epubReadingSystem hook is in place,
					// => execute js on the reader.html context to push the
					// global window.navigator.epubReadingSystem into the
					// iframe(s)

					evaluateJavascript(INJECT_EPUB_RSO_SCRIPT_2);
					
					return new WebResourceResponse("text/javascript", UTF_8,
							new ByteArrayInputStream(
									"(function(){})()".getBytes()));
				}
				
				if (cleanedUrl.matches("\\/?readium_MathJax.js")) {
					if (!quiet)
						Log.d(TAG, "MathJax.js inject ...");

					InputStream is = null;
					try {
						is = getAssets().open(PAYLOAD_MATHJAX_ASSET);
					} catch (IOException e) {

						Log.e(TAG, "MathJax.js asset fail!");

						return new WebResourceResponse(null, UTF_8,
								new ByteArrayInputStream("".getBytes()));
					}

					return new WebResourceResponse("text/javascript", UTF_8, is);
				}

				if (cleanedUrl.matches("\\/?readium_Annotations.css")) {
					if (!quiet)
						Log.d(TAG, "annotations.css inject ...");

					InputStream is = null;
					try {
						is = getAssets().open(PAYLOAD_ANNOTATIONS_CSS_ASSET);
					} catch (IOException e) {

						Log.e(TAG, "annotations.css asset fail!");

						return new WebResourceResponse(null, UTF_8,
								new ByteArrayInputStream("".getBytes()));
					}

					return new WebResourceResponse("text/css", UTF_8, is);
				}

				String mime = null;
				int dot = cleanedUrl.lastIndexOf('.');
				if (dot >= 0) {
					mime = EpubServer.MIME_TYPES.get(cleanedUrl.substring(
							dot + 1).toLowerCase());
				}
				if (mime == null) {
					mime = "application/octet-stream";
				}

				ManifestItem item = mPackage.getManifestItem(cleanedUrl);
				String contentType = item != null ? item.getMediaType() : null;
				if (mime != "application/xhtml+xml"
						&& mime != "application/xml" // FORCE
						&& contentType != null && contentType.length() > 0) {
					mime = contentType;
				}

				if (url.startsWith("file:")) {
					if (item == null) {
						Log.e(TAG, "NO MANIFEST ITEM ... " + url);
						return super.shouldInterceptRequest(view, url);
					}

					String cleanedUrlWithQueryFragment = cleanResourceUrl(url,
							true);
					String httpUrl = "http://" + EpubServer.HTTP_HOST + ":"
							+ EpubServer.HTTP_PORT + "/"
							+ cleanedUrlWithQueryFragment;
					Log.e(TAG, "FILE to HTTP REDIRECT: " + httpUrl);

					try {
						URLConnection c = new URL(httpUrl).openConnection();
						((HttpURLConnection) c).setUseCaches(false);
						if (mime == "application/xhtml+xml"
								|| mime == "text/html") {
							((HttpURLConnection) c).setRequestProperty(
									"Accept-Ranges", "none");
						}
						InputStream is = c.getInputStream();
						return new WebResourceResponse(mime, null, is);
					} catch (Exception ex) {
						Log.e(TAG,
								"FAIL: " + httpUrl + " -- " + ex.getMessage(),
								ex);
					}
				}
				if (!quiet)
					Log.d(TAG, "RESOURCE FETCH ... " + url);
				return super.shouldInterceptRequest(view, url);
			}

			Log.e(TAG, "NULL URL RESPONSE: " + url);
			return new WebResourceResponse(null, UTF_8,
					new ByteArrayInputStream("".getBytes()));
		}
	}

	private String cleanResourceUrl(String url, boolean preserveQueryFragment) {
		String cleanUrl = null;

		String httpUrl = "http://" + EpubServer.HTTP_HOST + ":"
				+ EpubServer.HTTP_PORT;
		if (url.startsWith(httpUrl)) {
			cleanUrl = url.replaceFirst(httpUrl, "");
		} else {
			cleanUrl = (url.startsWith(ASSET_PREFIX)) ? url.replaceFirst(
					ASSET_PREFIX, "") : url.replaceFirst("file://", "");
		}

		String basePath = mPackage.getBasePath();
		if (basePath.charAt(0) != '/') {
			basePath = '/' + basePath;
		}
		if (cleanUrl.charAt(0) != '/') {
			cleanUrl = '/' + cleanUrl;
		}
		cleanUrl = (cleanUrl.startsWith(basePath)) ? cleanUrl.replaceFirst(
				basePath, "") : cleanUrl;

		if (cleanUrl.charAt(0) == '/') {
			cleanUrl = cleanUrl.substring(1);
		}

		if (!preserveQueryFragment) {
			int indexOfQ = cleanUrl.indexOf('?');
			if (indexOfQ >= 0) {
				cleanUrl = cleanUrl.substring(0, indexOfQ);
			}

			int indexOfSharp = cleanUrl.indexOf('#');
			if (indexOfSharp >= 0) {
				cleanUrl = cleanUrl.substring(0, indexOfSharp);
			}
		}
		return cleanUrl;
	}

	public class EpubWebChromeClient extends WebChromeClient implements
			MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
		@Override
		public void onShowCustomView(View view, CustomViewCallback callback) {
			if (!quiet)
				Log.d(TAG, "here in on ShowCustomView: " + view);
			super.onShowCustomView(view, callback);
			if (view instanceof FrameLayout) {
				FrameLayout frame = (FrameLayout) view;
				if (!quiet)
					Log.d(TAG,
							"frame.getFocusedChild(): "
									+ frame.getFocusedChild());
				if (frame.getFocusedChild() instanceof VideoView) {
					VideoView video = (VideoView) frame.getFocusedChild();
					// frame.removeView(video);
					// a.setContentView(video);
					video.setOnCompletionListener(this);
					video.setOnErrorListener(this);
					video.start();
				}
			}
		}

		public void onCompletion(MediaPlayer mp) {
			if (!quiet)
				Log.d(TAG, "Video completed");

			// a.setContentView(R.layout.main);
			// WebView wb = (WebView) a.findViewById(R.id.webview);
			// a.initWebView();
		}

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {

			Log.e(TAG, "MediaPlayer onError: " + what + ", " + extra);
			return false;
		}
	}

	public class EpubInterface {

		@JavascriptInterface
		public void onPaginationChanged(String currentPagesInfo) {
			if (!quiet)
				Log.d(TAG, "onPaginationChanged1: " + currentPagesInfo);
			try {
				PaginationInfo paginationInfo = PaginationInfo
						.fromJson(currentPagesInfo);
				List<Page> openPages = paginationInfo.getOpenPages();
				if (!openPages.isEmpty()) {
					final Page page = openPages.get(0);
					final int tpages = mPackage.getSpineItems().size() * page.getSpineItemPageCount();
					final int pindex = page.getSpineItemPageIndex()+1 + page.getSpineItemIndex() * page.getSpineItemPageCount();
					runOnUiThread(new Runnable() {
						public void run() {
							mProgress.setMax(tpages);
							mProgress.setProgress(pindex);
							SpineItem spineItem = mPackage.getSpineItem(page
									.getIdref());
							boolean isFixedLayout = spineItem
									.isFixedLayout(mPackage);
							mWebview.getSettings().setBuiltInZoomControls(
									isFixedLayout);
							mWebview.getSettings()
									.setDisplayZoomControls(false);

							Long unixtime = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() + LoginActivity.serverTimeOffset;
							Integer annotationID = -1;
							Boolean del = false;
							mReadiumJSApi.updateLocation(unixtime, annotationID, del);
							updateHighlights(page.getIdref());
						}
					});
				}
			} catch (JSONException e) {
				Log.e(TAG, "" + e.getMessage(), e);
			}
		}

		@JavascriptInterface
		public void onSettingsApplied() {
			if (!quiet)
				Log.d(TAG, "onSettingsApplied");
		}

		@JavascriptInterface
		public void onReaderInitialized() {
			if (!quiet)
				Log.d(TAG, "onReaderInitialized");

            if (!quiet)
                Log.d(TAG, "openPageRequestData: " + mOpenPageRequestData);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mReadiumJSApi.openBook(mPackage, mViewerSettings,
                            mOpenPageRequestData);
                }
            });
		}

		@JavascriptInterface
		public void onContentLoaded() {
			if (!quiet)
				Log.d(TAG, "onContentLoaded");

		}

		@JavascriptInterface
		public void onPageLoaded() {
			if (!quiet)
				Log.d(TAG, "onPageLoaded");
		}

		@JavascriptInterface
		public void onIsMediaOverlayAvailable(String available) {
			if (!quiet)
				Log.d(TAG, "onIsMediaOverlayAvailablee:" + available);
			mIsMoAvailable = available.equals("true");

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					invalidateOptionsMenu();
				}
			});
		}

		@JavascriptInterface
		public void onMediaOverlayStatusChanged(String status) {
			if (!quiet)
				Log.d(TAG, "onMediaOverlayStatusChanged:" + status);
			// this should be real json parsing if there will be more data that
			// needs to be extracted

			if (status.indexOf("isPlaying") > -1) {
				mIsMoPlaying = status.indexOf("\"isPlaying\":true") > -1;
			}

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					invalidateOptionsMenu();
				}
			});
		}

		//
		// @JavascriptInterface
		// public void onMediaOverlayTTSSpeak() {
		// Log.d(TAG, "onMediaOverlayTTSSpeak");
		// }
		//
		// @JavascriptInterface
		// public void onMediaOverlayTTSStop() {
		// Log.d(TAG, "onMediaOverlayTTSStop");
		// }

		@JavascriptInterface
		public void updateLocation(final String bookmarkData, final String unixtimestr, final String annotationIDstr, final String delstr) {
			if (!quiet)
				Log.d(TAG, "updateHighlights:" + bookmarkData);

			try {
				JSONObject bookmarkJson = new JSONObject(
						bookmarkData);

				Long unixtime = Long.parseLong(unixtimestr);
				Integer annotationID = Integer.parseInt(annotationIDstr);
				Boolean del = false;
				if (delstr.equals("1")) {
					del = true;
				}

				//update local values
				DBHelper dbHelper = new DBHelper(WebViewActivity.this);
				if (annotationID == -1) {
					dbHelper.setLocation(LoginActivity.bookID,
							bookmarkJson.getString("idref"),
							bookmarkJson.getString("contentCFI"),
							unixtime);
				} else {
				}
				//Log.v("webview", "unix:"+unixtime+ "ts:"+ts1+" ts2:"+ts2);

				HttpURLConnection httpConn = null;
				try {
					JSONObject postDict = new JSONObject();
					JSONObject latest_location = new JSONObject();
					latest_location.put("idref", bookmarkJson.getString("idref"));
					latest_location.put("elementCfi", bookmarkJson.getString("contentCFI"));
					String locStr = latest_location.toString();
					String locStr2 = locStr.replace("\\/", "/");
					postDict.put("latest_location", locStr2);
					postDict.put("updated_at", unixtime);
					JSONArray highlights = new JSONArray();
					if (annotationID == -1) {
					} else {
						DBCursor hc = dbHelper.getHighlight(LoginActivity.bookID, annotationID);
						JSONObject hld = new JSONObject();
						hld.put("spineIdRef", hc.getColIDRef());
						hld.put("cfi", hc.getColCFI());
						hld.put("color", hc.getColColor());
						hld.put("note", hc.getColNote());
						hld.put("updated_at", hc.getColLastUpdated());
						if (del) {
							hld.put("_delete", true);
						}
						highlights.put(hld);
					}
					postDict.put("highlights", highlights);
					String patch = postDict.toString();
					String patch2 = patch.replace("\\/", "/");
					String url = "https://read.biblemesh.com/users/"+LoginActivity.userID+"/books/"+LoginActivity.bookID+".json";
					URL resourceUrl = new URL(url);
					httpConn = (HttpURLConnection) resourceUrl.openConnection();
					httpConn.setDoOutput(true);

					if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
						/*header override method
						httpConn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
						httpConn.setRequestMethod("POST");*/
						httpConn.setRequestMethod("PUT");
					} else {
						httpConn.setRequestMethod("PATCH");
					}

					httpConn.setRequestProperty("Accept", "application/json");
					httpConn.setRequestProperty("Content-Type", "application/json");
					Integer len = patch2.length();
					httpConn.setRequestProperty("Content-Length", len.toString());
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

					OutputStream os = httpConn.getOutputStream();
					os.write(patch2.getBytes());
					os.flush();

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
						Log.v("webview", "update response:"+sb.toString());
					} else if (responseCode == 412) {
						Log.v("webview", "got 412");
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
			} catch (JSONException e) {
				Log.e(TAG, "" + e.getMessage(), e);
			}
		}

		@JavascriptInterface
		public void updateHighlights(final String idref) {//fix could have more than one idref
			if (!quiet)
				Log.d(TAG, "updateLocation:" + idref);

			//// FIXME: 29/01/2017 runOnUiThread?
			mReadiumJSApi.removeHighlights(idref);
		}

		@JavascriptInterface
		public void removeHighlights(final String response, final String idref) {
			if (!quiet)
				Log.d(TAG, "removeHighlights:" + response);

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					DBHelper dbHelper = new DBHelper(WebViewActivity.this);
					DBCursor cursor = dbHelper.getHighlights(LoginActivity.bookID);
					for (int rowNum = 0; rowNum < cursor.getCount(); rowNum++) {
						cursor.moveToPosition(rowNum);
						if (idref.equals(cursor.getColIDRef())) {

							Integer random = (int) (Math.random() * 1000000 + 1);
							dbHelper.updateHighlight(
									cursor.getColID(), LoginActivity.bookID, cursor.getColColor(), cursor.getColNote(), cursor.getColLastUpdated(), random);
							mReadiumJSApi.addHighlight(cursor.getColIDRef(), cursor.getColCFI(), random);
						} else {
							Log.v("webview", "skip as idref not matched");
						}
					}
				}
			});
		}

		@JavascriptInterface
		public void highlightSelection(final String response) {
			if (!quiet)
				Log.d(TAG, "highlightSelection:" + response);

			try {
				final JSONObject selJson = new JSONObject(response);
				final String selidref = selJson.getString("idref");
				final String selcfi = selJson.getString("cfi");

				final Integer random = (int) (Math.random() * 1000000 + 1);

				DBHelper dbHelper = new DBHelper(WebViewActivity.this);
				final Long unixtime = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() + LoginActivity.serverTimeOffset;
				dbHelper.insertHighlight(LoginActivity.bookID, selidref, selcfi, 1, "", unixtime, random);

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mReadiumJSApi.addHighlight(selidref, selcfi, random);
						mReadiumJSApi.updateLocation(unixtime, random, false);
					}
				});

			} catch (JSONException e) {
				Log.v("webview", e.getMessage());
			}
		}

/*
		@JavascriptInterface
		public void addHighlight(final String response) {
			if (!quiet)
				Log.d(TAG, "addHighlight:" + response);

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					//if (idref.equals(cursor.getColIDRef())) {
					mReadiumJSApi.addHighlight(selidref, selcfi, random);
					//mReadiumJSApi.updateLocation(unixtime, random, false);
					//} else {
					//	Log.v("webview", "skip as idref not matched");
					//}
					//}
				}
			});
		}*/

		@JavascriptInterface
		public void getBookmarkData(final String bookmarkData) {
			AlertDialog.Builder builder = new AlertDialog.Builder(
					WebViewActivity.this).setTitle(R.string.add_bookmark);

			final EditText editText = new EditText(WebViewActivity.this);
			editText.setId(android.R.id.edit);
			editText.setHint(R.string.title);
			builder.setView(editText);
			builder.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (which == DialogInterface.BUTTON_POSITIVE) {
								String title = editText.getText().toString();
								try {
									JSONObject bookmarkJson = new JSONObject(
											bookmarkData);
									BookmarkDatabase.getInstance().addBookmark(
											mContainer.getName(),
											title,
											bookmarkJson.getString("idref"),
											bookmarkJson
													.getString("contentCFI"));
								} catch (JSONException e) {
									Log.e(TAG, "" + e.getMessage(), e);
								}
							}
						}
					});
			builder.setNegativeButton(android.R.string.cancel, null);
			builder.show();
		}

		@JavascriptInterface
		public void onAnnotationClicked(final String id) {
			if (!quiet)
				Log.d(TAG, "onAnnotationClicked:" + id);
			// this should be real json parsing if there will be more data that
			// needs to be extracted

			final DBHelper dbHelper = new DBHelper(WebViewActivity.this);
			final DBCursor cursor = dbHelper.getHighlight(LoginActivity.bookID, Integer.parseInt(id));
			final DBCursor cursor2 = dbHelper.getLocation(LoginActivity.bookID);

			final EditText editText = new EditText(WebViewActivity.this);
			editText.setLines(3);
			editText.setId(android.R.id.edit);
			editText.setHint("Notes");
			editText.setText(cursor.getColNote());

			AlertDialog.Builder builder = new AlertDialog.Builder(
					WebViewActivity.this)
					//.setTitle("")
					//.setMessage(id)
					.setView(editText)
					.setCancelable(true)
					.setNegativeButton("Delete", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									mReadiumJSApi.updateLocation(cursor2.getColLastUpdated(), Integer.parseInt(id), true);
								}});
							dialog.dismiss();
						}
					})
					.setNeutralButton("Share", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							//// FIXME: 07/02/2017 todo

							try {
								String shareurl = URLEncoder.encode("{\"idref\":\""+cursor.getColIDRef()+"\",\"elementCfi\":\""+cursor.getColCFI()+"\"}", "UTF-8");
								Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
								sharingIntent.setType("text/plain");
								sharingIntent.putExtra(Intent.EXTRA_TEXT, "https://read.biblemesh.com/book/"+LoginActivity.bookID+"?goto="+shareurl+"&highlight=%20");//todo
								startActivity(Intent.createChooser(sharingIntent, "Share via"));

								dialog.dismiss();
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
						}
					})
					.setPositiveButton("Save",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dbHelper.updateHighlight(cursor.getColID(), LoginActivity.bookID, cursor.getColColor(), editText.getText().toString(), cursor.getColLastUpdated(), Integer.parseInt(id));


							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									mReadiumJSApi.updateLocation(cursor2.getColLastUpdated(), Integer.parseInt(id), false);
								}
							});
							dialog.dismiss();

						}
					});
			//builder.setNegativeButton(android.R.string.cancel, null);
			builder.show();

		}
	}

	private ActionMode mActionMode = null;

	@Override
	public void onActionModeStarted(ActionMode mode) {
		if (mActionMode == null) {
			mActionMode = mode;
			Menu menu = mode.getMenu();
			// Remove the default menu items (select all, copy, paste, search)
			menu.clear();

			// If you want to keep any of the defaults,
			// remove the items you don't want individually:
			// menu.removeItem(android.R.id.[id_of_item_to_remove])

			// Inflate your own menu items
			mode.getMenuInflater().inflate(R.menu.context_menu, menu);

			MenuItem item = menu.findItem(R.id.highlight_item);
			item.setOnMenuItemClickListener
					(
							new MenuItem.OnMenuItemClickListener ()
							{
								public boolean onMenuItemClick(MenuItem item)
								{
									mReadiumJSApi.highlightSelection();
									return true;
								}
							}
					);
		}

		super.onActionModeStarted(mode);
	}

	@Override
	public void onActionModeFinished(ActionMode mode) {
		mActionMode = null;
		super.onActionModeFinished(mode);
	}

/*
	//http://stackoverflow.com/questions/14390908/how-to-control-the-android-webview-history-back-stack
	@Override
	public void onBackPressed() {
		if (mWebview.canGoBack()) {
			mWebview.goBack();
		} else {
			super.onBackPressed();
		}
	}*/
}
