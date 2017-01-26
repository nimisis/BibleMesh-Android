package org.readium.sdk.android.biblemesh;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

/*import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;*/

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadTask extends AsyncTask<EPubTitle, Integer, Long> {
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
		//dbHelper
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
				// TODO Auto-generated method stub
				Log.v("progress", "cancel");
			}
		});*/
	}

	protected Long doInBackground(EPubTitle... vid) {
        long totalSize = 0;

			vid[0].downloadStatus = 1;
			dbHelper.SetDownloadStatus(vid[0].bookID, vid[0].downloadStatus);
	        try {
		        URL url = new URL("https://read.biblemesh.com/epub_content/book_"+vid[0].bookID.toString()+"/book.epub");
		        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		        int responseCode = httpConn.getResponseCode();

		        // always check HTTP response code first
		        if (responseCode == HttpURLConnection.HTTP_OK) {
			        //fix String contentType = httpConn.getContentType();
			        int contentLength = httpConn.getContentLength();

			        dialog.setMax(contentLength);

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
			        //String saveFilePath = saveDir + File.separator + fileName;

			        // opens an output stream to save into file
			        //FileOutputStream outputStream = new FileOutputStream(saveFilePath);

			        int bytesRead = -1;
			        byte[] buffer = new byte[BUFFER_SIZE];
			        while ((bytesRead = inputStream.read(buffer)) != -1) {
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
		        httpConn.disconnect();

                Log.v("doInBackground", "done");
	        } catch (IOException e) {
            	Log.d("err", "Error: " + e);
		        vid[0].downloadStatus = 0;
	        } finally {
		        //urlConnection.disconnect();
	        }
		dbHelper.SetDownloadStatus(vid[0].bookID, vid[0].downloadStatus);
        return totalSize;
    }

    protected void onProgressUpdate(Integer... progress) {
        //Log.v("onProgressUpdate", progress[0]+"%");
        dialog.setProgress(progress[0]);
    }

    protected void onPostExecute(Long result) {
    	Log.v("DownloadTask", "onPostExecute");

    	dialog.dismiss();
    	//DownloadsAdapter va = (DownloadsAdapter) activity.getListView().getAdapter();
    	//va.notifyDataSetChanged();
    }
}
