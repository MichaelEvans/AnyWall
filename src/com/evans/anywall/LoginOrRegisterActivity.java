package com.evans.anywall;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class LoginOrRegisterActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login_or_register);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.login_or_register, menu);
		return true;
	}

}
