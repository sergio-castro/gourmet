package com.gourmet;

import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.gourmet.R;
import com.gourmet.R.id;
import com.gourmet.R.layout;
import com.gourmet.R.menu;
import com.gourmet.R.string;
import com.gourmet.activities.ClientRequestActivity;
import com.gourmet.database.context.DBContextManager;
import com.gourmet.database.context.UserLocationManager;
import com.gourmet.database.dao.GourmetAddressDAO;
import com.gourmet.database.dao.GourmetClientDAO;
import com.gourmet.database.dao.GourmetRestoDAO;
import com.gourmet.model.Address;
import com.gourmet.model.Client;
import com.gourmet.model.interfaces.IUser;
import com.gourmet.session.UserSessionManager;

/**
 * 
 *  !!!!! Code of this Activity was generated by Eclipse and then the necessary parts have been customized !!!!
 *  
 * Activity which displays a login screen to the user, offering 
 * well.
 * 
 * 
 */
public class LoginActivity extends Activity { 
	
	
	// =================== Added  Attributes  to customize behavior according to needs
	
	public static GourmetClientDAO datasourceClient;
	private IUser loggedUser;
	private UserSessionManager session;
	private DBContextManager dbContext;
	/////////////
	
	/**
	 * The default email to populate the email field with.
	 */
	public static final String EXTRA_USER_NAME = "Username";
 
	/**
	 * Keep track of the login task to ensure we can cancel it if requested.
	 */
	private UserLoginTask mAuthTask = null;

	// Values for email and password at the time of the login attempt.
	private String mEmail;

	// UI references.
	private EditText mUserNameView;
	private EditText mPasswordView;
	private View mLoginFormView;
	private View mLoginStatusView;
	private TextView mLoginStatusMessageView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_login);

		// Set up the login form.
		mEmail = getIntent().getStringExtra(EXTRA_USER_NAME);
		mUserNameView = (EditText) findViewById(R.id.username);
		mUserNameView.setText(mEmail);

		mPasswordView = (EditText) findViewById(R.id.password);
		mPasswordView
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView textView, int id,
							KeyEvent keyEvent) {
						if (id == R.id.login || id == EditorInfo.IME_NULL) {
							attemptLogin();
							return true;
						}
						return false;
					}
				});

		mLoginFormView = findViewById(R.id.login_form);
		mLoginStatusView = findViewById(R.id.login_status);
		mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

		findViewById(R.id.sign_in_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						attemptLogin();
					}
				});
		
		//Initialize and Open database access
		datasourceClient = GourmetClientDAO.getInstance(getApplicationContext());
		dbContext = DBContextManager.getInstance(getApplicationContext());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.login, menu);
		return true;
	}

	/**
	 * Attempts to sign in the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		if (mAuthTask != null) {
			return;
		}

		// Reset errors.
		mUserNameView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		mEmail = mUserNameView.getText().toString();
//		mPassword = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		

		// Check for a valid email address.
		if (TextUtils.isEmpty(mEmail)) {
			mUserNameView.setError(getString(R.string.error_field_required));
			focusView = mUserNameView;
			cancel = true;
		} 

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
			showProgress(true);
			mAuthTask = new UserLoginTask();
			mAuthTask.execute((Void) null);
		}
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			mLoginStatusView.setVisibility(View.VISIBLE);
			mLoginStatusView.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginStatusView.setVisibility(show ? View.VISIBLE
									: View.GONE);
						}
					});

			mLoginFormView.setVisibility(View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginFormView.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}

	/**
	 * Represents an asynchronous login/registration task used to authenticate
	 * the user.
	 */
	public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Void... params) {
		
			loggedUser = null;
			dbContext.resetNeutralProfile();
			List<Client> allowedClients = datasourceClient.getAllClients();
			for (Client client : allowedClients) {
				if(client.getClName().equals(mEmail)){
					loggedUser = client;
					return true;
				}
			}			
	
			//TODO Handle Restaurant user too		
			return false;
		}

		@Override
		protected void onPostExecute(final Boolean success) {
			mAuthTask = null;
			showProgress(false);

			if (success) {
				startUserSession();
			} else {
				mUserNameView.setError(getString(R.string.error_invalid_username));
				mUserNameView.requestFocus();
			}
		}

	

		@Override
		protected void onCancelled() {
			mAuthTask = null;
			showProgress(false);
		}
	}
	
	/**
	 * Start the user session 
	 */
	private void startUserSession() {
		
		session = UserSessionManager.getInstance(getApplicationContext());
		
		session.registerUser(this.loggedUser.getUserName());
		session.registerNumeric(UserSessionManager.CLIENT_ID_KEY, this.loggedUser.getUserID());
		session.registerNumeric(UserSessionManager.LANG_ID_KEY,this.loggedUser.getLanguageID());
		session.registerNumeric(UserSessionManager.AGE_KEY, this.loggedUser.getAge());
		session.registerRawData(UserSessionManager.PROFILE_TYPE, UserSessionManager.CLIENT);
		
		//Change context
		dbContext.setUserProfile(session);
		dbContext.setProfileActive(true);
		//Set up location manager 
		UserLocationManager.assignSession(session);
		
		//TODO handle restaurant too
		// Restaurant have only  1 default Language when they act as user of App (limitation)

		 Intent clReqIntent = new Intent(getApplicationContext(), ClientRequestActivity.class);
		 startActivity(clReqIntent);
		 finish();
		
	}
	
	

	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		//datasourceClient.closeDataSource();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();

		//datasourceClient.openDataSource();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();

		datasourceClient.openDataSource();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
		//datasourceClient.closeDataSource();
	}
}
