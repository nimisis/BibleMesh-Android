package org.readium.sdk.android.biblemesh;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class ConnectivityReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		//Log.v(ConnectivityReceiver.class.getSimpleName(), "action: "
          //      + intent.getAction());
		//String action = intent.getAction();
		int state = 0;

        boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
        if (!noConnectivity) {
	        //String reason = intent.getStringExtra(ConnectivityManager.EXTRA_REASON);
	        //boolean isFailover = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);
	        NetworkInfo currentNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
	        //NetworkInfo otherNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);
	        if (currentNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
	        	Log.v("Connectivity", "Connected and wifi");
	        	state = 2;
	        } else {
	        	Log.v("Connectivity", "Connected but not wifi");
	        	state = 1;
	        }

			//new CatchupTask(context).execute(1);

	        //Log.v(ConnectivityReceiver.class.getSimpleName(), "Status : " + noConnectivity + ", Reason :" + reason + ", FailOver :" + isFailover + ", Current Network Info : " + currentNetworkInfo + ", OtherNetwork Info :" + otherNetworkInfo);
        } else {
        	Log.v("Connectivity", "Not connected!");
        }

		/*Log.v("direct", "here"+state);
        Intent broadcastIntent = new Intent();
		broadcastIntent.setAction("CONNECTIVITY_RECEIVED_ACTION");
		broadcastIntent.putExtra("state", state);
		context.sendBroadcast(broadcastIntent);*/
	}
	
}
