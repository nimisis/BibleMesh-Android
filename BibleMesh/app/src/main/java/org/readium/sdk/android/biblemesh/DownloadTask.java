package org.readium.sdk.android.biblemesh;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.webkit.CookieManager;
import android.widget.ListView;

/*import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;*/

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadTask extends AsyncTask<EPubTitle, Integer, Integer> {
	private static final int BUFFER_SIZE = 4096;
	private Activity activity;
	private DBHelper dbHelper;
	private ProgressDialog dialog;

	public DownloadTask(Activity activity, DBHelper dbHelper) {
		this.activity = activity;
		this.dbHelper = dbHelper;
	}

	@Override
	protected void onCancelled() {
		Log.v("DownloadTask", "onCancelled");
		//// FIXME: 26/01/2017 reset downloadStatus
	}
		
	@Override
	protected void onPreExecute() {
        dialog = new ProgressDialog(activity);
		//dialog.setMessage(activity.getParent().getString(R.string.downloading));
		dialog.setMessage("Downloading...");//getString(R.string.please_wait_while_loading));
		//dialog.setIndeterminate(false);
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setMax(100);
		dialog.setCancelable(true);//fix
		//dialog.setProgressNumberFormat(null);
		
		dialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // actually could set running = false; right here, but I'll
                // stick to contract.
                cancel(true);
            }
        });
		dialog.show();
		/*dialog.show(activity, "title", "message", false, true, new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				Log.v("progress", "cancel");
			}
		});*/
	}

	protected Integer doInBackground(EPubTitle... vid) {
        Integer totalSize = 0;

			vid[0].downloadStatus = 1;
			dbHelper.setDownloadStatus(vid[0].bookID, vid[0].downloadStatus);
			HttpURLConnection httpConn = null;
			try {
				URL resourceUrl, base, next;
				String loca;
				String url = "https://read.biblemesh.com/epub_content/book_" + vid[0].bookID.toString() + "/book.epub";
				int responseCode;

				while (true) {
					resourceUrl = new URL(url);
					httpConn = (HttpURLConnection) resourceUrl.openConnection();
					httpConn.setRequestMethod("GET");

					String cookies = CookieManager.getInstance().getCookie(url);
					if (cookies != null) {
						Log.v("downloadtask", "have cookies");
						httpConn.setRequestProperty("Cookie", cookies);
					}
					// Starts the query
					//conn.connect();

					//httpConn.setRequestProperty("Content-length", "0");
					//httpConn.setUseCaches(false);
					//httpConn.setAllowUserInteraction(false);
					//httpConn.setConnectTimeout(timeout);
					//httpConn.setReadTimeout(timeout);
					httpConn.setInstanceFollowRedirects(false);
					httpConn.connect();
					responseCode = httpConn.getResponseCode();
					switch(responseCode) {
						case HttpURLConnection.HTTP_MOVED_TEMP:
						case HttpURLConnection.HTTP_MOVED_PERM:
						case 307:
							Log.v("download", "follow redirect");
							loca = httpConn.getHeaderField("Location");
							base     = new URL(url);
							next     = new URL(base, loca);  // Deal with relative URLs
							url      = next.toExternalForm();
							continue;
					}
					break;
				}
		        // always check HTTP response code first
		        if (responseCode == HttpURLConnection.HTTP_OK) {
			        //fix String contentType = httpConn.getContentType();
			        totalSize = httpConn.getContentLength();

			        vid[0].fsize = totalSize;
			        dbHelper.setDownloadFSize(vid[0].bookID, totalSize);

			        dialog.setMax(totalSize);

			        String PATH = Environment.getExternalStorageDirectory() + "/Android/data/org.readium.sdk.android.biblemesh/";
			        Log.v("doInBackground", "PATH:"+PATH);
			        File file = new File(PATH);
			        if (file.mkdirs()) {
				        Log.v("doInBackground", "makedirs success");
			        } else {
				        Log.v("doInBackground", "makedirs failed");
			        }
			        String fileName = "book_"+vid[0].bookID.toString()+".epub";

			        File outputFile = new File(file, fileName);
			        FileOutputStream fos = new FileOutputStream(outputFile);
			        // opens input stream from the HTTP connection
			        InputStream inputStream = httpConn.getInputStream();

			        int bytesRead = -1;
			        totalSize = 0;
			        byte[] buffer = new byte[BUFFER_SIZE];
			        while ((bytesRead = inputStream.read(buffer)) != -1) {
				        Log.v("download", "bytes:"+bytesRead);
				        fos.write(buffer, 0, bytesRead);
				        totalSize += bytesRead;
				        publishProgress((int) totalSize);
			        }

			        fos.close();
			        inputStream.close();

			        System.out.println("File downloaded");

			        vid[0].downloadStatus = 2;
		        } else {
			        vid[0].downloadStatus = 0;
			        System.out.println("No file to download. Server replied HTTP code: " + responseCode);
		        }

                Log.v("doInBackground", "done");
	        } catch (IOException e) {
            	Log.d("err", "Error: " + e);
		        vid[0].downloadStatus = 0;
	        } finally {
				if (httpConn != null) {
					httpConn.disconnect();
				}
	        }
		dbHelper.setDownloadStatus(vid[0].bookID, vid[0].downloadStatus);
        return totalSize;
    }

    protected void onProgressUpdate(Integer... progress) {
        //Log.v("onProgressUpdate", progress[0]+"%");
        dialog.setProgress(progress[0]);
    }

    protected void onPostExecute(Integer result) {
    	Log.v("DownloadTask", "onPostExecute");

	    ListView l = (ListView) activity.findViewById(R.id.containerList);
    	BookAdapter va = (BookAdapter) l.getAdapter();
    	va.notifyDataSetChanged();

	    dialog.dismiss();
    }
}
