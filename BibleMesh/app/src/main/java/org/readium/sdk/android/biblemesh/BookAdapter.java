package org.readium.sdk.android.biblemesh;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

//import org.apache.http.client.methods.HttpPost;

import java.util.List;

public class BookAdapter extends ArrayAdapter<EPubTitle> {

	private final Activity activity;
	private final List<EPubTitle> frontBooks;

	public BookAdapter(Activity activity, List<EPubTitle> objects, int layout) {
		super(activity, layout, objects);
		this.activity = activity;
        this.frontBooks = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View rowView = convertView;
        BookView sqView = null;

        if(convertView == null) {
			LayoutInflater inflater = activity.getLayoutInflater();
            rowView = inflater.inflate(R.layout.listbooks, null);

            // Hold the view objects in an object,
            // so they don't need to be re-fetched
            sqView = new BookView();
            sqView.author = (TextView) rowView.findViewById(R.id.author);
	        sqView.title = (TextView) rowView.findViewById(R.id.title);
	        sqView.status = (TextView) rowView.findViewById(R.id.status);
	        sqView.cover = (ImageView) rowView.findViewById(R.id.cover);

	        //http://stackoverflow.com/questions/5776851/load-image-from-url

	        //sqView.author.setVisibility(View.VISIBLE);
	        //sqView.cover.setVisibility(View.VISIBLE);

            rowView.setTag(sqView);
        } else {
	        sqView = (BookView) rowView.getTag();
        }

        sqView.title.setText(frontBooks.get(position).title);
        sqView.author.setText(frontBooks.get(position).author);
	        switch (frontBooks.get(position).downloadStatus) {
		        case 0:
			        sqView.status.setText("Download");
			        break;
		        case 1:
			        sqView.status.setText("Downloading...");
			        break;
		        default:
			        sqView.status.setText("");
			        break;
	        }
        if (frontBooks.get(position).cover == null) {
	        sqView.cover.setImageBitmap(null);
	        Log.e("shared", "pos"+position+" is null");
	        if (frontBooks.get(position).coverHref.equals("")) {
	        } else {
		        new DownloadImageTask(frontBooks.get(position), sqView.cover)
        .execute("https://read.biblemesh.com/"+frontBooks.get(position).coverHref);
	        }
        } else {
	        sqView.cover.setImageBitmap(frontBooks.get(position).cover);
        }

        return rowView;
	}

    protected static class BookView {
        protected TextView title;
	    protected TextView author;
	    protected TextView status;
        protected ImageView cover;
    }
}


