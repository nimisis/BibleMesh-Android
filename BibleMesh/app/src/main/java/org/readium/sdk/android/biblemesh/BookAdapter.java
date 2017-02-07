package org.readium.sdk.android.biblemesh;

//import java.io.File;
//import java.io.IOException;

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
		//R.layout.
		//super(ac)
        this.activity = activity;
        this.frontBooks = objects;
        Log.v("shared", "num"+objects.size());
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View rowView = convertView;
        BookView sqView = null;

        //Inflate the view
        if(convertView == null) {

         // Get a new instance of the row layout view
            LayoutInflater inflater = activity.getLayoutInflater();
            rowView = inflater.inflate(R.layout.listbooks, null);
	        //rowView = inflater.inf

            // Hold the view objects in an object,
            // so they don't need to be re-fetched
            sqView = new BookView();
            sqView.author = (TextView) rowView.findViewById(R.id.author);
            sqView.title = (TextView) rowView.findViewById(R.id.title);
            sqView.cover = (ImageView) rowView.findViewById(R.id.cover);


	        //http://stackoverflow.com/questions/5776851/load-image-from-url
	        //Log.v("shared", "new"+position);
            // Cache the view objects in the tag,

            // so they can be re-accessed later

            /*rowView.setOnClickListener(new View.OnClickListener() {
            @Override
                public void onClick(View v) {
	            Log.v("shared", "click");
            }
            });*/

	        sqView.author.setVisibility(View.VISIBLE);
	        sqView.cover.setVisibility(View.VISIBLE);

            rowView.setTag(sqView);
        }
        else
        {
	        //Log.v("shared", "old"+position);
            //alertView = (LinearLayout) convertView;
            sqView = (BookView) rowView.getTag();
        }

        // Transfer the stock data from the data object
        // to the view objects

        /*if ((page < pageCnt) && (position == (frontBooks.size() - 1))) {
	        sqView.title.setText("[Load more]");
	        sqView.author.setVisibility(View.INVISIBLE);
	        sqView.cover.setVisibility(View.INVISIBLE);
        } else */
        {

        //sqView.cover.setImageBitmap(null);
        sqView.title.setText(frontBooks.get(position).title);
        sqView.author.setText(frontBooks.get(position).author);
        if (frontBooks.get(position).cover == null) {
	        sqView.cover.setImageBitmap(null);
	        Log.e("shared", "pos"+position+" is null");
	        if (frontBooks.get(position).coverHref.equals("")) {
	        } else {
		        //fix
		        new DownloadImageTask(frontBooks.get(position), sqView.cover)
        .execute("https://read.biblemesh.com/"+frontBooks.get(position).coverHref);
		        //https://read.biblemesh.com/%@", [[ep locationToEpub] coverHref]
	        }
        } else {
	        //Log.e("shared", "pos"+position+" is not null");
	        sqView.cover.setImageBitmap(frontBooks.get(position).cover);
        }
        }

        return rowView;
	}

    protected static class BookView {
        protected TextView title;
        protected TextView author;
        protected ImageView cover;
    }
}


