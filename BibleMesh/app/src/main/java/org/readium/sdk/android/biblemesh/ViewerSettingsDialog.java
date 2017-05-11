/*
 * ViewerSettingsDialog.java
 * SDKLauncher-Android
 *
 * Created by Yonathan Teitelbaum (Mantano) on 2013-07-30.
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

import org.readium.sdk.android.biblemesh.model.ViewerSettings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import java.io.Serializable;

import static java.lang.Integer.parseInt;

/**
 * This dialog displays the viewer settings to the user.
 * The model is represented by the class {@link ViewerSettings}
 *
 */
public class ViewerSettingsDialog extends DialogFragment {
    //public ViewerSettingsDialog() {}

	/**
	 * Interface to notify the listener when a viewer settings have been changed.
	 */
	public interface OnViewerSettingsChange extends Serializable {
		public void onViewerSettingsChange(ViewerSettings settings);
	}

	protected static final String TAG = "ViewerSettingsDialog";
	
	private OnViewerSettingsChange mListener;

	private ViewerSettings mOriginalSettings;

	public ViewerSettingsDialog() {};

	public static ViewerSettingsDialog newInstance(OnViewerSettingsChange listener, ViewerSettings originalSettings) {
		ViewerSettingsDialog fragment = new ViewerSettingsDialog();
		Bundle args = new Bundle();
		args.putSerializable("listener", listener);
		args.putSerializable("settings", originalSettings);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public Dialog onCreateDialog(
			Bundle savedInstanceState) {

        mListener = (OnViewerSettingsChange) getArguments().getSerializable("listener");
        mOriginalSettings = (ViewerSettings) getArguments().getSerializable("settings");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        final View dialogView = inflater.inflate(R.layout.viewer_settings, null);

        final RadioGroup spreadGroup = (RadioGroup) dialogView.findViewById(R.id.spreadSettings);
        switch (mOriginalSettings.getSyntheticSpreadMode()) {
            case AUTO:
                spreadGroup.check(R.id.spreadAuto);
                break;
            case DOUBLE:
                spreadGroup.check(R.id.spreadDouble);
                break;
            case SINGLE:
                spreadGroup.check(R.id.spreadSingle);
                break;
        }

        final RadioGroup scrollGroup = (RadioGroup) dialogView.findViewById(R.id.scrollSettings);
        switch (mOriginalSettings.getScrollMode()) {
            case AUTO:
                scrollGroup.check(R.id.scrollAuto);
                break;
            case DOCUMENT:
                scrollGroup.check(R.id.scrollDocument);
                break;
            case CONTINUOUS:
                scrollGroup.check(R.id.scrollContinuous);
                break;
        }

        final EditText fontSizeText = (EditText) dialogView.findViewById(R.id.fontSize);
        fontSizeText.setText("" + mOriginalSettings.getFontSize());

        final EditText columnGapText = (EditText) dialogView.findViewById(R.id.columnGap);
        columnGapText.setText("" + mOriginalSettings.getColumnGap());

		final Button minusFont = (Button) dialogView.findViewById(R.id.minusbutton);
		minusFont.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Integer f = Integer.parseInt(fontSizeText.getText().toString());
				f -= 10;
				if (f > 0) {
					fontSizeText.setText(f.toString());
				}
			}
		});

		final Button plusFont = (Button) dialogView.findViewById(R.id.plusbutton);
		plusFont.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Integer f = Integer.parseInt(fontSizeText.getText().toString());
				f += 10;
				fontSizeText.setText(f.toString());
			}
		});

        builder.setView(dialogView)
                .setTitle(R.string.settings)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (mListener != null) {
                            int fontSize = parseString(fontSizeText.getText().toString(), 100);
                            int columnGap = parseString(columnGapText.getText().toString(), 20);

                            ViewerSettings.SyntheticSpreadMode syntheticSpreadMode = null;
                            switch (spreadGroup.getCheckedRadioButtonId()) {
                                case R.id.spreadAuto:
                                    syntheticSpreadMode = ViewerSettings.SyntheticSpreadMode.AUTO;
                                    break;
                                case R.id.spreadSingle:
                                    syntheticSpreadMode = ViewerSettings.SyntheticSpreadMode.SINGLE;
                                    break;
                                case R.id.spreadDouble:
                                    syntheticSpreadMode = ViewerSettings.SyntheticSpreadMode.DOUBLE;
                                    break;
                            }

                            ViewerSettings.ScrollMode scrollMode = null;
                            switch (scrollGroup.getCheckedRadioButtonId()) {
                                case R.id.scrollAuto:
                                    scrollMode = ViewerSettings.ScrollMode.AUTO;
                                    break;
                                case R.id.scrollDocument:
                                    scrollMode = ViewerSettings.ScrollMode.DOCUMENT;
                                    break;
                                case R.id.scrollContinuous:
                                    scrollMode = ViewerSettings.ScrollMode.CONTINUOUS;
                                    break;
                            }

                            ViewerSettings settings = new ViewerSettings(syntheticSpreadMode, scrollMode, fontSize, columnGap);
                            mListener.onViewerSettingsChange(settings);
                        }
                        dismiss();
                    }

                    private int parseString(String s, int defaultValue) {
                        try {
                            return parseInt(s);
                        } catch (Exception e) {
                            Log.e(TAG, "" + e.getMessage(), e);
                        }
                        return defaultValue;
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                       dismiss();
                    }
                });

        return builder.create();
	}

}
