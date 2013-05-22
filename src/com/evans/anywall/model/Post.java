package com.evans.anywall.model;

import android.location.Location;

import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseUser;

public class Post extends ParseObject{
	private static String className = "Post";
	private String body;
	private ParseUser user;
	private ParseGeoPoint location;
	private String id;
	
	public Post() {
		super(className);
	}

	public Post(ParseObject obj){
		super(className);
		this.body = obj.getString("body");
		this.user = obj.getParseUser("user");
		this.location = obj.getParseGeoPoint("location");
		this.id = obj.getObjectId();
	}
	public String getBody() {
		return body;
	}
	
	public void setBody(String body){
		this.put("body", body);
		this.body = body;
	}

	public ParseUser getUser() {
		return user;
	}
	
	public void setUser(ParseUser currentUser) {
		this.put("user", currentUser);
		this.user = currentUser;
	}

	public void setLocation(Location location) {
		this.location = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
		this.put("location", this.location);
	}
	
	public ParseGeoPoint getLocation(){
		return location;
	}
	
	public double getLatitude(){
		return location.getLatitude();
	}

	public double getLongitude(){
		return location.getLongitude();
	}
	
	@Override
	public boolean equals(Object o){
		if(o == null || !o.getClass().equals(getClass()))
			return false;
		Post other = (Post)o;
		if(getBody().equals(other.getBody()) && getUser().equals(other.getUser()) && getLocation().equals(other.getLocation()))
			return true;
		else
			return false;
	}

	@Override
	public String toString() {
		return "Post [body=" + body + ", user=" + user + ", location="
				+ location + "]";
	}

	public String getId() {
		return id;
	}
	
}
