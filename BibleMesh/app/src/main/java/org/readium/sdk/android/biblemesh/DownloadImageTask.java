package org.readium.sdk.android.biblemesh;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.InputStream;

public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
		  ImageView bmImage;
			EPubTitle book;
		  
		  public DownloadImageTask(EPubTitle book, ImageView bmImage) {
		      this.bmImage = bmImage;
		      if (book != null) {
		    	  this.book = book;
		      }
		  }

		  protected Bitmap doInBackground(String... urls) {
			  Log.v("download", "doinbackground");
		      String urldisplay = urls[0];
		      Bitmap mIcon11 = null;
		      try {
		        InputStream in = new java.net.URL(urldisplay).openStream();
		        mIcon11 = BitmapFactory.decodeStream(in);
		      } catch (Exception e) {
		          Log.e("Error", e.getMessage());
		          e.printStackTrace();
		      }
		      return mIcon11;
		  }

		  protected void onPostExecute(Bitmap result) {
			  Log.v("download", "postexecute");
		      bmImage.setImageBitmap(result);
		      if (book != null) {
		    	  book.cover = result;
		      }
		  }
}

