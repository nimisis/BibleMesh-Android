package org.readium.sdk.android.biblemesh;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class LoginWebViewActivity extends Activity {

	WebView mWebview;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login_web_view);
		//Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		//setSupportActionBar(toolbar);

		mWebview = (WebView) findViewById(R.id.my_web_view);

		mWebview.getSettings().setJavaScriptEnabled(true); // enable javascript

		final Activity activity = this;

		mWebview.setWebChromeClient(new WebChromeClient() {
			public boolean onConsoleMessage(ConsoleMessage cmsg)
			{
				String token = cmsg.message();
				Log.v("authtest", token);
				token = token.replace("<pre style=\"word-wrap: break-word; white-space: pre-wrap;\">", "");
				token = token.replace("</pre>", "");
				if (token.startsWith("[{")) {
					Log.v("authtest", "got token, finishing...");

					Bundle conData = new Bundle();
					conData.putString("token", token);
					Intent intent = new Intent();
					intent.putExtras(conData);
					setResult(RESULT_OK, intent);

					//MainActivity.this.finish();
					finish();
				}
				return true;
			}
		});

		mWebview.setWebViewClient(new WebViewClient() {

			public void onReceivedError(WebView view, int errorCode,
			                            String description, String failingUrl) {
				Log.e("Load Signup page", description);
				Toast.makeText(
						activity,
						"Problem loading. Make sure internet connection is available.",
						Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				view.loadUrl("javascript:console.log(document.body.innerHTML);");
			}
		});
		int c = NetworkUtil.getConnectivityStatus(getApplicationContext());
		if (NetworkUtil.getConnectivityStatus(getApplicationContext()) == NetworkUtil.TYPE_NOT_CONNECTED) {
			Intent intent = new Intent();
			setResult(RESULT_OK, intent);

			//MainActivity.this.finish();
			finish();
		} else {
			Map<String, String> extraHeaders = new HashMap<String, String>();
			extraHeaders.put("App-Request", "true");
			mWebview.loadUrl("https://read.biblemesh.com/epub_content/epub_library.json", extraHeaders);
		}
		//getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}
}
