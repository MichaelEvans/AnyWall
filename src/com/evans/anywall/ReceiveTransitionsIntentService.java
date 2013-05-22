package com.evans.anywall;

import java.util.List;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;

public class ReceiveTransitionsIntentService extends IntentService{

	public ReceiveTransitionsIntentService() {
		super("ReceiveTransitionsIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// First check for errors
		if (LocationClient.hasError(intent)) {
			// Get the error code with a static method
			int errorCode = LocationClient.getErrorCode(intent);
			// Log the error
			Log.e("ReceiveTransitionsIntentService",
					"Location Services error: " +
							Integer.toString(errorCode));
			/*
			 * You can also send the error code to an Activity or
			 * Fragment with a broadcast Intent
			 */
			/*
			 * If there's no error, get the transition type and the IDs
			 * of the geofence or geofences that triggered the transition
			 */
		} else {
			// Get the type of transition (entry or exit)
			int transitionType =
					LocationClient.getGeofenceTransition(intent);
			// Test that a valid transition was reported
			if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER){
				List <Geofence> triggerList =
						LocationClient.getTriggeringGeofences(intent);

				for(Geofence gf : triggerList){
					Intent i = new Intent("com.evans.geofence").putExtra("requestId", gf.getRequestId());
					i.putExtra("enter", true);
					this.sendBroadcast(i);
					Log.e("Enter Transition", "Geofences" + triggerList.size());
				}
			}else if(transitionType == Geofence.GEOFENCE_TRANSITION_EXIT){
				List <Geofence> triggerList =
						LocationClient.getTriggeringGeofences(intent);

				for(Geofence gf : triggerList){
					Intent i = new Intent("com.evans.geofence").putExtra("requestId", gf.getRequestId());
					i.putExtra("enter", false);
					this.sendBroadcast(i);
					Log.e("Exit Transition", "Geofences" + triggerList.size());
				}
			}


			
			//toggle geofence

			//				String[] triggerIds = new String[geofenceList.size()];
			//
			//				for (int i = 0; i < triggerIds.length; i++) {
			//					// Store the Id of each geofence
			//					triggerIds[i] = triggerList.get(i).getRequestId();
			//				}
			/*
			 * At this point, you can store the IDs for further use
			 * display them, or display the details associated with
			 * them.
			 */

			// An invalid transition was reported
			else {
				Log.e("ReceiveTransitionsIntentService",
						"Geofence transition error: " +
								Integer.toString(transitionType));
			}
		}
	}
}