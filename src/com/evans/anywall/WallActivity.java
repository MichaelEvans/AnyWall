package com.evans.anywall;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.evans.anywall.adapter.PostListAdapter;
import com.evans.anywall.geo.SimpleGeofence;
import com.evans.anywall.model.Post;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationClient.OnAddGeofencesResultListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseQuery.CachePolicy;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class WallActivity extends Activity implements
GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener,
LocationListener,
OnAddGeofencesResultListener{

	private static final long SECONDS_PER_HOUR = 60;
	private static final long MILLISECONDS_PER_SECOND = 1000;
	private static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;
	private static final long GEOFENCE_EXPIRATION_TIME =
			GEOFENCE_EXPIRATION_IN_HOURS *
			SECONDS_PER_HOUR *
			MILLISECONDS_PER_SECOND;

	private ListView postList;
	private GoogleMap mMap;
	private MapFragment mMapFragment;
	private Circle mCircle;
	private HashMap<String, Marker> mMarkers;
	private HashMap<String, Post> geofenceRequest;
	// private LocationManager mLocationManager;
	private LocationClient mLocationClient;
	private PostListAdapter arrayAdapter;
	private MenuItem refreshItem;

	List<Geofence> mCurrentGeofences;

	// Stores the PendingIntent used to request geofence monitoring
	private PendingIntent mGeofenceRequestIntent;
	// Defines the allowable request types.
	//public enum REQUEST_TYPE = {ADD};
	//private REQUEST_TYPE mRequestType;
	// Flag that indicates if a request is underway.
	private boolean mInProgress;
	private BroadcastReceiver mReceiver;
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wall);

		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		// check availability.

		postList = (ListView) findViewById(R.id.postList);
		arrayAdapter = new PostListAdapter(WallActivity.this,
				new ArrayList<Post>());
		postList.setAdapter(arrayAdapter);
		mMapFragment = ((MapFragment) getFragmentManager().findFragmentById(
				R.id.map));
		mMap = mMapFragment.getMap();
		mMap.setMyLocationEnabled(true);
		mLocationClient = new LocationClient(this, this, this);
		mMarkers = new HashMap<String, Marker>();
		mCurrentGeofences = new ArrayList<Geofence>();
		geofenceRequest = new HashMap<String, Post>();
		mInProgress = false;

		IntentFilter intentFilter = new IntentFilter(
				"com.evans.geofence");
		mReceiver = new BroadcastReceiver(){
			public void onReceive(Context context, Intent intent) {
				String requestId = intent.getStringExtra("requestId");
				boolean enter = intent.getBooleanExtra("enter", false);
				if(enter)
					enterGeofence(requestId);
				else
					exitGeofence(requestId);
			}
		};
		this.registerReceiver(mReceiver, intentFilter);

		// Acquire a reference to the system Location Manager
		// mLocationManager = (LocationManager) this
		// .getSystemService(Context.LOCATION_SERVICE);
		//
		// // Register the listener with the Location Manager to receive
		// location
		// // updates
		// mLocationManager.requestLocationUpdates(
		// LocationManager.NETWORK_PROVIDER, 50, 0, this);

	}

	protected void onStart() {
		super.onStart();
		// Connect the client.
		mLocationClient.connect();
	}

	protected void onStop() {
		mLocationClient.disconnect();
		super.onStop();
		// mLocationManager.removeUpdates(this);
	}

	private void updateCircle(Location location) {
		LatLng latlng = new LatLng(location.getLatitude(),
				location.getLongitude());

		CircleOptions circleOptions = new CircleOptions().center(latlng)
				.fillColor(0x330000FF).strokeColor(0x770000FF).radius(75); // In
		// meters
		// 300
		// Get back the mutable Circle
		if (mCircle == null) {
			mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 16));
			mCircle = mMap.addCircle(circleOptions);
		} else {
			mCircle.setCenter(latlng);
		}
	}

	private void populateData(){
		ParseQuery query = new ParseQuery("Post");
		query.setLimit(20);
		query.setCachePolicy(CachePolicy.CACHE_THEN_NETWORK);
		Location location = mLocationClient.getLastLocation();
		query.whereWithinKilometers("location", new ParseGeoPoint(location.getLatitude(), location
				.getLongitude()), 100000);
		query.include("user");
		query.findInBackground(new FindCallback() {

			@Override
			public void done(List<ParseObject> list, ParseException e) {
				if(e == null)
					updatePosts(list);
				//else log error!
				//
				//refreshItem.setActionView(null);
			}
		});

	}

	private Geofence createGeofence(Post p){
		SimpleGeofence geo = new SimpleGeofence(p.getId(), p.getLatitude(), 
				p.getLongitude(), 25, GEOFENCE_EXPIRATION_TIME, Geofence.GEOFENCE_TRANSITION_ENTER |
				Geofence.GEOFENCE_TRANSITION_EXIT);
		//add marker to map



		return geo.toGeofence();

	}
	private void updatePosts(List<ParseObject> list) {
		ArrayList<Post> postsInRange = new ArrayList<Post>();
		List<Geofence> geofences = new ArrayList<Geofence>();
		for(ParseObject obj : list){
			Post newPost = new Post(obj);
			if(isWithin(75, newPost.getLatitude(), newPost.getLongitude()))
				postsInRange.add(newPost);
			Geofence fence = createGeofence(newPost);
			geofences.add(fence);
			geofenceRequest.put(fence.getRequestId(), newPost);
		}
		arrayAdapter.clear();
		arrayAdapter.addAll(postsInRange);
		arrayAdapter.notifyDataSetChanged();
		PendingIntent mTransitionPendingIntent =
				getTransitionPendingIntent();
		mLocationClient.addGeofences(geofences, mTransitionPendingIntent, this);
	}

	private PendingIntent getTransitionPendingIntent() {
		// Create an explicit Intent
		Intent intent = new Intent(this,
				ReceiveTransitionsIntentService.class);
		/*
		 * Return the PendingIntent
		 */
		return PendingIntent.getService(
				this,
				0,
				intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
	}
	//		private void populateData() {
	//			Log.w("WallActivity", "Populating data");
	//			ParseQuery query = new ParseQuery("Post");
	//			// query.whereNear("location", userLocation);
	//			query.setLimit(20);
	//			query.setCachePolicy(CachePolicy.CACHE_THEN_NETWORK);
	//			Location location = mLocationClient.getLastLocation();
	//			query.whereWithinKilometers(
	//					"location",
	//					new ParseGeoPoint(location.getLatitude(), location
	//							.getLongitude()), 100000);
	//			// final ArrayList<Post> posts = new ArrayList<Post>();
	//			// for (Post p : mMarkers.keySet()) {
	//			// Marker m = mMarkers.get(p);
	//			// m.remove();
	//			// }
	//	
	//			query.include("user");
	//			query.findInBackground(new FindCallback() {
	//				public void done(List<ParseObject> pointList, ParseException e) {
	//					final List<Post> allPosts = new ArrayList<Post>();
	//					final ArrayList<Post> postsInRange = new ArrayList<Post>();
	//					if (e == null) {
	//						// 1. new posts
	//						for (ParseObject result : pointList) {
	//							Post newPost = new Post(result);
	//	
	//							allPosts.add(newPost);
	//							boolean found = false;
	//							for (Post curentPost : allPosts) {
	//								if (newPost.equals(curentPost)) {
	//									found = true;
	//								}
	//							}
	//							if (!found) {// If we did not already have this wall
	//								// post
	//								allPosts.add(newPost);
	//							}
	//						}
	//						// 2. allposts that are no longer needed
	//						ArrayList<Post> postsToRemove = new ArrayList<Post>();
	//						for (Post currentPost : allPosts) {
	//							boolean found = false;
	//							for (ParseObject result : pointList) {
	//								Post allNewPost = new Post(result);
	//								if (currentPost.equals(allNewPost)) {
	//									found = true;
	//								}
	//							}
	//							if (!found) {// If we did not already have this wall
	//								// post
	//								postsToRemove.add(currentPost);
	//							}
	//						}
	//						// 3. update for map
	//						Log.d("WallActivity", "Debug: " + allPosts.size());
	//						for (Post newPost : allPosts) {
	//							Marker marker;
	//							if (!isWithin(75, newPost.getLatitude(),
	//									newPost.getLongitude())) {
	//								marker = mMap.addMarker(new MarkerOptions()
	//								.position(
	//										new LatLng(newPost.getLatitude(),
	//												newPost.getLongitude()))
	//												.title("Can't view post! Get closer.")
	//												.icon(BitmapDescriptorFactory
	//														.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
	//							} else {
	//								Log.w("WallActivity", "Adding post in range "
	//										+ newPost.toString());
	//								postsInRange.add(newPost);
	//								marker = mMap.addMarker(new MarkerOptions()
	//								.position(
	//										new LatLng(newPost.getLatitude(),
	//												newPost.getLongitude()))
	//												.title(newPost.getUser().getUsername())
	//												.icon(BitmapDescriptorFactory
	//														.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
	//														.snippet(newPost.getBody()));
	//							}
	//							mMarkers.put(newPost, marker);
	//						}
	//						// 4. update map, update lists
	//						allPosts.removeAll(postsToRemove);
	//						for (Post p : postsToRemove) {
	//							mMarkers.remove(p);
	//						}
	//	
	//						Log.d("score", "Retrieved " + pointList.size() + " points");
	//						Log.e("WallActivity", "Adding post in range "
	//								+ postsInRange.size());
	//						arrayAdapter.clear();
	//						arrayAdapter.addAll(postsInRange);
	//						arrayAdapter.notifyDataSetChanged();
	//					} else {
	//						Log.d("score", "Error: " + e.getMessage());
	//					}
	//					// updateMarkers(getLastLocation());
	//				}
	//			});
	//			// refreshItem.setActionView(null);
	//		}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_wall, menu);
		refreshItem = menu.findItem(R.id.menu_refresh);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_refresh:
			//populateData();
			item.setActionView(R.layout.actionbar_indeterminate_progress);
			return true;
		case R.id.menu_post:
			showPostDialog();
			return true;
		case R.id.menu_logout:
			logout();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void showPostDialog() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("New Post");

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
		input.setSingleLine(false);
		input.setMinLines(2);
		alert.setView(input);

		alert.setPositiveButton("Post", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				if (value.length() > 0) {
					Post post = new Post();
					post.setBody(value);
					// mLocationManager.get

					Location location = mLocationClient.getLastLocation();
					post.setLocation(location);
					post.setUser(ParseUser.getCurrentUser());
					post.saveInBackground(new SaveCallback() {
						@Override
						public void done(ParseException arg0) {
							//populateData();
						}
					});
				}
			}
		});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
			}
		});

		alert.show();
	}

	private boolean isWithin(float meters, double lat, double lon){
		Location location = mLocationClient.getLastLocation();
		float[] curDist = new float[1];
		Location.distanceBetween(location.getLatitude(),
				location.getLongitude(), lat, lon,
				curDist);
		float distance = curDist[0];
		return distance <= meters;
	}

	//	private Location getLastLocation() {
	//		Criteria criteria = new Criteria();
	//		criteria.setAccuracy(Criteria.ACCURACY_FINE);
	//		String provider = mLocationManager
	//				.getBestProvider(new Criteria(), true);
	//		Location location = mLocationManager.getLastKnownLocation(provider);
	//		return location;
	//	}

	private void logout() {
		ParseUser.logOut();
		Intent i = new Intent(this, LoginOrRegisterActivity.class);
		mLocationClient.disconnect();
		startActivity(i);
		finish();
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnected(Bundle dataBundle) {
		Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
		Location location = mLocationClient.getLastLocation();
		if(location != null)
			updateCircle(location);
		populateData();
	}

	@Override
	public void onDisconnected() {

	}

	@Override
	public void onLocationChanged(Location location) {
		updateCircle(location);

	}

	@Override
	public void onAddGeofencesResult(
			int statusCode, String[] geofenceRequestIds) {
		// If adding the geofences was successful
		if (LocationStatusCodes.SUCCESS == statusCode) {
			/*
			 * Handle successful addition of geofences here.
			 * You can send out a broadcast intent or update the UI.
			 * geofences into the Intent's extended data.
			 */
			//        	Marker marker = mMap.addMarker(new MarkerOptions()
			//    		.position(
			//    				new LatLng(p.getLatitude(),
			//    						p.getLongitude()))
			//    						.title("Can't view post! Get closer.")
			//    						.icon(BitmapDescriptorFactory
			//    								.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
			//    		mMarkers.put(geo, marker);
			//			Marker marker = mMap.addMarker(new MarkerOptions()
			//			.position(new LatLng(p.getLatitude(), p.getLongitude()))
			//			.title(p.getUser().getUsername())
			//			.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
			//			.snippet(p.getBody()));

			for(String requestId : geofenceRequestIds){
				Post post = geofenceRequest.get(requestId);
				Marker marker = mMap.addMarker(new MarkerOptions()
				.position(new LatLng(post.getLatitude(), post.getLongitude()))
				.title("Can't view post! Get closer.")
				.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
				mMarkers.put(requestId, marker);
			}

		} else {
			// If adding the geofences failed
			/*
			 * Report errors here.
			 * You can log the error using Log.e() or update
			 * the UI.
			 */
		}
		// Turn off the in progress flag and disconnect the client
		mInProgress = false;
		mLocationClient.disconnect();
	}

	public void enterGeofence(String requestId) {
		Marker m = mMarkers.get(requestId);
		Post p = geofenceRequest.get(requestId);
		if(m.getSnippet() == null){
			m.setTitle(p.getUser().getUsername());
			m.setSnippet(p.getBody());
			m.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
		}
	}

	public void exitGeofence(String requestId) {
		Marker m = mMarkers.get(requestId);
		Post p = geofenceRequest.get(requestId);
		if(m != null && m.getSnippet() != null){
			m.setTitle("Can't view post! Get closer.");
			m.setSnippet(null);
			m.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

		}
	}
	// @Override
	// public void onLocationChanged(Location location) {
	// Log.v("Location", "Updated location: " + location);
	// updateCircle(location);
	// populateData();
	// }
	//
	// @Override
	// public void onProviderDisabled(String provider) {}
	//
	// @Override
	// public void onProviderEnabled(String provider) {}
	//
	// @Override
	// public void onStatusChanged(String provider, int status, Bundle extras)
	// {}


}
