package org.readium.sdk.android.biblemesh;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by admin on 10/03/2017.
 */

public class FontManager {
	public static final String ROOT = "fonts/",
			FONTAWESOME = ROOT + "FontAwesome.ttf";

	public static Typeface getTypeface(Context context, String font) {
		return Typeface.createFromAsset(context.getAssets(), font);
	}
	/*public static void markAsIconContainer(View v, Typeface typeface) {
		if (v instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup) v;
			for (int i = 0; i < vg.getChildCount(); i++) {
				View child = vg.getChildAt(i);
				markAsIconContainer(child);
			}
		} else if (v instanceof TextView) {
			((TextView) v).setTypeface(typeface);
		}
	}*/
}
