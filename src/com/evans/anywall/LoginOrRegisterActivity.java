package com.evans.anywall;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.parse.Parse;
import com.parse.ParseUser;

public class LoginOrRegisterActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login_or_register);
		
		String appId = getResources().getString(R.string.app_id);
		String clientKey = getResources().getString(R.string.client_key);
		Parse.initialize(this, appId, clientKey);

		ParseUser currentUser = ParseUser.getCurrentUser();
		if (currentUser != null) {
			Intent i = new Intent(this, WallActivity.class);
			startActivity(i);
			finish();
		}
	}

	public void registerClick(View v){
		Intent i = new Intent(this, RegisterActivity.class);
		startActivity(i);
		//finish();
	}
	
	public void loginClick(View v){
		Intent i = new Intent(this, LoginActivity.class);
		startActivity(i);
		//finish();
	}
	
}
