package org.readium.sdk.android.biblemesh;

import android.graphics.Bitmap;

public class EPubTitle {

	public Integer bookID;
	public Integer downloadStatus;
	public Integer fsize;
	public String author;
	public String title;
	public String coverHref;
	public String rootURL;
	public Bitmap cover;

	public EPubTitle() {
		downloadStatus = 0;
		cover = null;
		coverHref = "";
	}
}
