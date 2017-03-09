package org.readium.sdk.android.biblemesh;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.CookieManager;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
	private ImageView bmImage;
	private EPubTitle book;
		  
		  public DownloadImageTask(EPubTitle book, ImageView bmImage) {
		      this.bmImage = bmImage;
		      if (book != null) {
		    	  this.book = book;
		      }
		  }

		  protected Bitmap doInBackground(String... urls) {
			  Log.v("downloadimage", "doinbackground");
		      String urldisplay = urls[0];
		      Bitmap mIcon11 = null;
		      /*try {
		        InputStream in = new java.net.URL(urldisplay).openStream();
		        mIcon11 = BitmapFactory.decodeStream(in);
		      } catch (Exception e) {
		          Log.e("Error", e.getMessage());
		          e.printStackTrace();
		      }
		      return mIcon11;*/

			  HttpURLConnection httpConn = null;
			  try {
				  URL resourceUrl, base, next;
				  String loca;
				  String url = urldisplay;//"https://read.biblemesh.com/epub_content/book_" + vid[0].bookID.toString() + "/book.epub";
				  int responseCode;

				  while (true) {
					  resourceUrl = new URL(url);
					  httpConn = (HttpURLConnection) resourceUrl.openConnection();
					  httpConn.setRequestMethod("GET");

					  String cookies = CookieManager.getInstance().getCookie(url);
					  if (cookies != null) {
						  Log.v("downloadimagetask", "have cookies");
						  httpConn.setRequestProperty("Cookie", cookies);
					  }

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
					  InputStream inputStream = httpConn.getInputStream();

					  mIcon11 = BitmapFactory.decodeStream(inputStream);
					  inputStream.close();
				  } else {
					  Log.v("download image", "response:"+responseCode+" for "+url);
					  //System.out.println("No file to download. Server replied HTTP code: " + responseCode);
				  }

				  Log.v("doInBackground", "done");
			  } catch (IOException e) {
				  Log.d("err", "Error: " + e);
			  } finally {
				  if (httpConn != null) {
					  httpConn.disconnect();
				  }
			  }
			  return mIcon11;
		  }

		  protected void onPostExecute(Bitmap result) {
			  Log.v("downloadimage", "postexecute");
		      bmImage.setImageBitmap(result);
		      if (book != null) {
		    	  book.cover = result;
		      }
		  }
}

