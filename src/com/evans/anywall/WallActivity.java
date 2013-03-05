package com.evans.anywall;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;

import com.evans.anywall.adapter.PostListAdapter;
import com.evans.anywall.model.Post;
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
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseQuery.CachePolicy;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class WallActivity extends Activity implements LocationListener{

	private ListView postList;
	private GoogleMap mMap;
	private MapFragment mMapFragment;
	private Circle mCircle;
	private HashMap<Post, Marker> mMarkers;
	private LocationManager mLocationManager;
	private LocationListener mLocationListener;
	private PostListAdapter arrayAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wall);

		postList = (ListView) findViewById(R.id.postList);
		arrayAdapter = new PostListAdapter(
				WallActivity.this, new ArrayList<Post>());
		postList.setAdapter(arrayAdapter);
		mMapFragment = ((MapFragment) getFragmentManager().findFragmentById(
				R.id.map));
		mMap = mMapFragment.getMap();
		mMap.setMyLocationEnabled(true);
		mMarkers = new HashMap<Post, Marker>();

		String appId = getResources().getString(R.string.app_id);
		String clientKey = getResources().getString(R.string.client_key);
		Parse.initialize(this, appId, clientKey);

		ParseUser currentUser = ParseUser.getCurrentUser();
		if (currentUser == null) {
			Intent i = new Intent(this, LoginActivity.class);
			startActivity(i);
			finish();
		}

		// Acquire a reference to the system Location Manager
		mLocationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);

		// Register the listener with the Location Manager to receive location
		// updates
		mLocationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, 50, 0, this);
		//populateData();
	}

	protected void onStop() {
		super.onStop();
		mLocationManager.removeUpdates(this);
	}

	private void updateCircle(Location location) {
		LatLng latlng = new LatLng(location.getLatitude(),
				location.getLongitude());
		
		CircleOptions circleOptions = new CircleOptions().center(latlng)
				.fillColor(0x330000FF)
				.strokeColor(0x770000FF)
				.radius(75); // In meters
		// 300
		// Get back the mutable Circle
		if (mCircle == null) {
			mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 17));
			mCircle = mMap.addCircle(circleOptions);
		} else {
			mCircle.setCenter(latlng);
		}
	}

	private void populateData() {
		Log.w("WallActivity", "Populating data");
		ParseQuery query = new ParseQuery("Post");
		// query.whereNear("location", userLocation);
		query.setLimit(20);
		query.setCachePolicy(CachePolicy.CACHE_THEN_NETWORK);
		Location location = getLastLocation();
		query.whereWithinKilometers(
				"location",
				new ParseGeoPoint(location.getLatitude(), location
						.getLongitude()), 100000);
		//final ArrayList<Post> posts = new ArrayList<Post>();
		//		for (Marker m : mMarkers) {
		//			m.remove();
		//		}
		
		query.include("user");
		query.findInBackground(new FindCallback() {
			public void done(List<ParseObject> pointList, ParseException e) {
				final List<Post> allPosts = new ArrayList<Post>();
				final ArrayList<Post> postsInRange = new ArrayList<Post>();
				if (e == null) {
					//1. new posts
					for (ParseObject result : pointList) {
						Post newPost = new Post(result);

						allPosts.add(newPost);
						boolean found = false;
						for(Post curentPost : allPosts){
							if(newPost.equals(curentPost)){
								found = true;
							}
						}
						if (!found){// If we did not already have this wall post
							allPosts.add(newPost);
						}
					}
					//2. allposts that are no longer needed
					ArrayList<Post> postsToRemove = new ArrayList<Post>();
					for(Post currentPost : allPosts){
						boolean found = false;
						for(ParseObject result : pointList){
							Post allNewPost = new Post(result);
							if(currentPost.equals(allNewPost)){
								found = true;
							}
						}
						if (!found){// If we did not already have this wall post
							postsToRemove.add(currentPost);
						}
					}
					//3. update for map
					Log.d("WallActivity", "Debug: " + allPosts.size());
					for(Post newPost : allPosts){
						Marker marker;
						if(!isWithin(75, newPost.getLatitude(), newPost.getLongitude())){
							marker = mMap.addMarker(new MarkerOptions()
							.position(
									new LatLng(newPost.getLatitude(), newPost
											.getLongitude()))
											.title("Can't view post! Get closer.")
											.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
									);
						}else{
							Log.w("WallActivity", "Adding post in range " + newPost.toString());
							postsInRange.add(newPost);
							marker = mMap.addMarker(new MarkerOptions()
							.position(
									new LatLng(newPost.getLatitude(), newPost
											.getLongitude()))
											.title(newPost.getUser().getUsername())
											.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
											.snippet(newPost.getBody())
									);
						}
						mMarkers.put(newPost, marker);
					}
					//4. update map, update lists
					allPosts.removeAll(postsToRemove);
					for(Post p : postsToRemove){
						mMarkers.remove(p);
					}

					//					if(isWithin(75, newPost.getLatitude(), newPost.getLongitude())){ //only show items within 75 meters
					//						posts.add(newPost);
					//					}

					//					mMarkers.add(marker);

					Log.d("score", "Retrieved " + pointList.size() + " points");
					Log.e("WallActivity", "Adding post in range " + postsInRange.size());
					arrayAdapter.clear();
					arrayAdapter.addAll(postsInRange);
					arrayAdapter.notifyDataSetChanged();
				} else {
					Log.d("score", "Error: " + e.getMessage());
				}
				// updateMarkers(getLastLocation());
			}
		});
		//		arrayAdapter = new PostListAdapter(
		//				WallActivity.this, postsInRange);
		//Log.w("WallActivity", "Posts in range " + postsInRange);

		//postList.setAdapter(arrayAdapter);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_wall, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_refresh:
			//refreshData();
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

					Location location = getLastLocation();
					post.setLocation(location);
					post.setUser(ParseUser.getCurrentUser());
					post.saveInBackground(new SaveCallback() {
						@Override
						public void done(ParseException arg0) {
							populateData();
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
		Location location = getLastLocation();
		float[] curDist = new float[1];
		Location.distanceBetween(location.getLatitude(),
				location.getLongitude(), lat, lon,
				curDist);
		float distance = curDist[0];
		return distance <= meters;
	}

	private Location getLastLocation() {
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		String provider = mLocationManager
				.getBestProvider(new Criteria(), true);
		Location location = mLocationManager.getLastKnownLocation(provider);
		return location;
	}

	private void logout() {
		ParseUser.logOut();
		Intent i = new Intent(this, LoginActivity.class);
		startActivity(i);
	}

	@Override
	public void onLocationChanged(Location location) {
		Log.v("Location", "Updated location: " + location);
		updateCircle(location);
		populateData();
	}

	@Override
	public void onProviderDisabled(String provider) {}

	@Override
	public void onProviderEnabled(String provider) {}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {}
}
