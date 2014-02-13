package com.maroonstd.kaintayo;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class MainActivity extends Activity implements
LocationListener,
GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener{

	//Location services variables
	// A request to connect to Location Services
	private LocationRequest mLocationRequest;

	// Stores the current instantiation of the location client in this object
	private LocationClient mLocationClient;

	// Handle to SharedPreferences for this app
	SharedPreferences mPrefs;

	// Handle to a SharedPreferences editor
	SharedPreferences.Editor mEditor;

	/*
	 * Note if updates have been turned on. Starts out as "false"; is set to "true" in the
	 * method handleRequestSuccess of LocationUpdateReceiver.
	 *
	 */
	boolean mUpdatesRequested = false;
	// end of location services variables
	
    // Constants
    static String TWITTER_CONSUMER_KEY = "LLo9tdXrD4YZqY9Wty4nxA";
    static String TWITTER_CONSUMER_SECRET = "c6b74Kc1cQA9IXgtbEbGbH63HJuAnQUCpU0bJEwQ93w";
 
    // Preference Constants
    static String PREFERENCE_NAME = "twitter_oauth";
    static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
    static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
    static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLogedIn";
 
    static final String TWITTER_CALLBACK_URL = "oauth://kainv1";
 
    // Twitter oauth urls
    static final String URL_TWITTER_AUTH = "auth_url";
    static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";
    static final String URL_TWITTER_OAUTH_TOKEN = "oauth_token";
    
	//progress dialog for connection
	ProgressDialog aDialog;
	 
    // Twitter
    private static Twitter twitter;
    private static RequestToken requestToken;
    private static AccessToken accessToken;

    // Twitter user vars
    private long userID;
    private User user;
    private String username;
     
    // Shared Preferences
    private static SharedPreferences mSharedPreferences;
     
    // Internet Connection detector
    private ConnectionDetector cd;
     
    // Alert Dialog Manager
    AlertDialogManager alert = new AlertDialogManager();
    
    ImageButton loginTwitterButton;
	boolean twitterLogin=false;
	/**
	 * end twitter variables
	 */
    
	/**
	 * For Facebook session
	 */
	private boolean isResumed = false;
	private UiLifecycleHelper uiHelper;
	private Session.StatusCallback callback = 
			new Session.StatusCallback() {
				@Override
				public void call(Session session, SessionState state, Exception exception) {
					onSessionStateChange(session, state, exception);
				}
			};
			
	/**
	 * end Facebook session
	 */
	
	
	//Constants
	private String TAG = "KainTayo Debug";
	
	//UI elements
	private Spinner distanceSpinner;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		distanceSpinner = (Spinner) findViewById(R.id.distanceSpinner);
		distanceSpinner.getBackground().setAlpha(255);
		// Create an ArrayAdapter using the string array and a default spinner layout
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
		        R.array.distance, R.layout.spinner_item);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		distanceSpinner.setAdapter(adapter);

		final EditText restoName = (EditText)findViewById(R.id.restoName);
		
		//for nearby any
		Button searchButton1 = (Button)findViewById(R.id.nearbyButton);
//		searchButton1.getBackground().setAlpha(64);
		searchButton1.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				int distanceRad = 0;
				String distanceStr=distanceSpinner.getSelectedItem().toString();
				
				if(distanceStr.equals("1 kilometro")){
					distanceRad = 1000;
				}else if(distanceStr.equals("5 kilometro")){
					distanceRad = 5000;
				}else if(distanceStr.equals("10 kilometro")){
					distanceRad = 10000;
				}else if(distanceStr.equals("20 kilometro")){
					distanceRad = 20000;
				}else{
					distanceRad = 0;
				}
				searchNearby(distanceRad,"");
				Log.d(TAG,"distance="+distanceRad);
			}
		});
		//for nearby specific
		Button searchButton2 = (Button)findViewById(R.id.nearbyButton2);
//		searchButton2.getBackground().setAlpha(64);
		searchButton2.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				int distanceRad = 0;
				String distanceStr=distanceSpinner.getSelectedItem().toString();
				String restoStr = restoName.getText().toString();
				
				if(distanceStr.equals("1 kilometro")){
					distanceRad = 1000;
				}else if(distanceStr.equals("5 kilometro")){
					distanceRad = 5000;
				}else if(distanceStr.equals("10 kilometro")){
					distanceRad = 10000;
				}else if(distanceStr.equals("20 kilometro")){
					distanceRad = 20000;
				}else{
					distanceRad = 0;
				}

				if(restoStr.isEmpty()){
		        	Toast.makeText(getApplicationContext(),
		                    "Kelangan ng pangalan ng kainan!", Toast.LENGTH_SHORT)
		                    .show();
				}else{
					searchNearby(distanceRad,restoStr);
				}
				Log.d(TAG,"distance="+distanceRad +"resto="+restoStr);
			}
		});
		
		uiHelper = new UiLifecycleHelper(this, callback);
		uiHelper.onCreate(savedInstanceState);

		checkLocationSettings();
        mLocationRequest = LocationRequest.create();

        /*
         * Set the update interval
         */
        mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);

        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Set the interval ceiling to one minute
        mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);

        // Note that location updates are on at create until the user selects an action
        mUpdatesRequested = true;

        // Open Shared Preferences
        mPrefs = getSharedPreferences(LocationUtils.SHARED_PREFERENCES, Context.MODE_PRIVATE);

        // Get an editor
        mEditor = mPrefs.edit();

        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this, this, this);
        
        /**
         * Twitter stuff
         */
        
        cd = new ConnectionDetector(getApplicationContext());
        
        // Check if Internet present
        if (!cd.isConnectingToInternet()) {
            // Internet Connection is not present
            alert.showAlertDialog(MainActivity.this, "Internet Connection Error",
                    "Please connect to working Internet connection", false);
            // stop executing code by return
            return;
        }
         
        // Check if twitter keys are set
        if(TWITTER_CONSUMER_KEY.trim().length() == 0 || TWITTER_CONSUMER_SECRET.trim().length() == 0){
            // Internet Connection is not present
            alert.showAlertDialog(MainActivity.this, "Twitter oAuth tokens", "Please set your twitter oauth tokens first!", false);
            // stop executing code by return
            return;
        }
 
        // Shared Preferences
        mSharedPreferences = getApplicationContext().getSharedPreferences(
                "MyPref", 0);
 
        Editor ed = mSharedPreferences.edit();
//        ed.putBoolean(PREF_KEY_TWITTER_LOGIN, false);
        ed.commit();
        
        loginTwitterButton = (ImageButton) findViewById(R.id.loginTwitter);
        if(isTwitterLoggedInAlready()){
        	loginTwitterButton.setImageResource(R.drawable.twitter_logout_lowres);
        }
        
        loginTwitterButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(isTwitterLoggedInAlready()){
					logoutFromTwitter();
		        }else{
		        	new loginTwitter().execute();
		        }
			}
		});
        /**
         * end twitter stuff
         */
        
	    AdView adView = (AdView)this.findViewById(R.id.adView);
	    AdRequest adRequest = new AdRequest.Builder().build();
	    adView.loadAd(adRequest);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	 /*
     * Called when the Activity is no longer visible at all.
     * Stop updates and disconnect.
     */
    @Override
    public void onStop() {

        // If the client is connected
        if (mLocationClient.isConnected()) {
            stopPeriodicUpdates();
        }

        // After disconnect() is called, the client is considered "dead".
        mLocationClient.disconnect();

        super.onStop();
    }
    /*
     * Called when the Activity is going into the background.
     * Parts of the UI may be visible, but the Activity is inactive.
     */
    @Override
    public void onPause() {

        // Save the current setting for updates
        mEditor.putBoolean(LocationUtils.KEY_UPDATES_REQUESTED, mUpdatesRequested);
        mEditor.commit();

        super.onPause();
		uiHelper.onPause();
		isResumed = false;
    }

    /*
     * Called when the Activity is restarted, even before it becomes visible.
     */
    @Override
    public void onStart() {

        super.onStart();

        /*
         * Connect the client. Don't re-start any requests here;
         * instead, wait for onResume()
         */
        mLocationClient.connect();

    }
    /*
     * Called when the system detects that this Activity is now visible.
     */
    @Override
    public void onResume() {
        super.onResume();

        // If the app already has a setting for getting location updates, get it
        if (mPrefs.contains(LocationUtils.KEY_UPDATES_REQUESTED)) {
            mUpdatesRequested = mPrefs.getBoolean(LocationUtils.KEY_UPDATES_REQUESTED, false);

        // Otherwise, turn off location updates until requested
        } else {
            mEditor.putBoolean(LocationUtils.KEY_UPDATES_REQUESTED, true);
            mEditor.commit();
        }

        uiHelper.onResume();
		isResumed = true;
    }
	
    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d(LocationUtils.APPTAG, "Google Play Service unavailable");

            // Continue
            return true;
        // Google Play services was not available for some reason
        } else {
        	/*
        	 * TODO error dialog
        	 * 
            // Display an error dialog
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            if (dialog != null) {
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(dialog);
                errorFragment.show(getSupportFragmentManager(), LocationUtils.APPTAG);
            }*/
            return false;
        }
    }
    
    public void getLocation() {

        // If Google Play Services is available
        if (servicesConnected()) {

            // Get the current location
        	// use this as updated location
            Location currentLocation = mLocationClient.getLastLocation();
            System.out.println("Current location:"+LocationUtils.getLatLng(this, currentLocation));
            // Display the current location in the UI
            startPeriodicUpdates();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
    	System.out.println("Location services:Connected");
        getLocation();
    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
    	System.out.println("Location services:Disconnected");
//        mConnectionStatus.setText(R.string.disconnected);
    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {

                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */

            } catch (IntentSender.SendIntentException e) {

                // Log the error
                e.printStackTrace();
            }
        } else {
        	// TODO: error dialog
            // If no resolution is available, display a dialog to the user with the error.
            // showErrorDialog(connectionResult.getErrorCode());
        }
    }
    
    @Override
    public void onLocationChanged(Location location) {
        // In the UI, set the latitude and longitude to the value received
    	
    }

    private void startPeriodicUpdates() {
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    /**
     * In response to a request to stop updates, send a request to
     * Location Services
     */
    private void stopPeriodicUpdates() {
        mLocationClient.removeLocationUpdates(this);
    }
    
    private void searchNearby(int distance, String searchName){
		if(distance>0){
			Intent i = new Intent(this, GenerateMap.class);
			i.putExtra("radius",distance);
			i.putExtra("searchName",searchName);
			i.putExtra("latitude", mLocationClient.getLastLocation().getLatitude());
			i.putExtra("longitude", mLocationClient.getLastLocation().getLongitude());
			startActivity(i);
		}else{
			Log.d(TAG,">distance not > 0");
		}
	}
	
	private void onSessionStateChange(Session session, SessionState state, Exception exception){
		if(state.isOpened()){
		// If the session state is open:
		// Show the authenticated fragment
			System.out.println("session opened");
		}else if (state.isClosed()){
		// If the session state is closed:
		// show the login fragment
			System.out.println("session closed");
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	  super.onActivityResult(requestCode, resultCode, data);
	  uiHelper.onActivityResult(requestCode, resultCode, data);
	  Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		uiHelper.onDestroy();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);
	    uiHelper.onSaveInstanceState(outState);
	}
	
	/**
	 * Twitter Functions
	 */
    private boolean isTwitterLoggedInAlready() {
        // return twitter login status from Shared Preferences
        return mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
    }
    
    /**
     * Function to login twitter
     * 
     * if return type is void (3rd parameter), onpostexecute not called
     * */
    class loginTwitter extends AsyncTask<Void,Void,Integer>{
        /**
         * getting Places JSON
         * */
    	protected void onPreExecute(){
    		Log.d(TAG,"login twitter pre-execute");
    		twitterLogin = true;
    	}
    	
        protected Integer doInBackground(Void... args) {
        	// Check if already logged in
        	System.out.println("twitter login start");
            if (!isTwitterLoggedInAlready()) {
                ConfigurationBuilder builder = new ConfigurationBuilder();
                builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
                builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
                Configuration configuration = builder.build();
                 
                TwitterFactory factory = new TwitterFactory(configuration);
                twitter = factory.getInstance();
     
                try {
                	System.out.println("twitter before getOAuth");
                    requestToken = twitter
                            .getOAuthRequestToken(TWITTER_CALLBACK_URL);
                    System.out.println("twitter authentication url:"+Uri
                            .parse(requestToken.getAuthenticationURL()));
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri
                            .parse(requestToken.getAuthenticationURL())));
                    System.out.println("twitter after getOAuth");
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
            } else {
                // user already logged into twitter
//                Toast.makeText(getApplicationContext(),
//                        "Already Logged into twitter", Toast.LENGTH_LONG).show();
            }
            System.out.println("twitter end request token");
            return 1;
        }

        protected void onPostExecute(Integer result) {
            System.out.println("twitter get request token result:"+result);
        }
    }
    
    class getAccessToken extends AsyncTask<Void,Void,Integer>{
    	/**
         * Before starting background thread Show Progress Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            aDialog = new ProgressDialog(MainActivity.this);
            aDialog.setMessage("Getting access token...");
            aDialog.setIndeterminate(false);
            aDialog.setCancelable(true);
            aDialog.show();
            twitterLogin = false;
        }
        
        @Override
        protected Integer doInBackground(Void... args) {
        	if (!isTwitterLoggedInAlready()) {
                Uri uri = getIntent().getData();
                if (uri != null && uri.toString().startsWith(TWITTER_CALLBACK_URL)) {
                   // oAuth verifier
                   String verifier = uri
                           .getQueryParameter(URL_TWITTER_OAUTH_VERIFIER);
                   try {
                       // Get the access token
                       accessToken = twitter.getOAuthAccessToken(
                               requestToken, verifier);
                      	
                       // Shared Preferences
                       Editor e = mSharedPreferences.edit();
                      	
                       // After getting access token, access token secret
                       // store them in application preferences
                       e.putString(PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
                       e.putString(PREF_KEY_OAUTH_SECRET,
                               accessToken.getTokenSecret());
                       // Store login status - true
                       e.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
                       e.commit(); // save changes

                       Log.e("Twitter OAuth Token", "> " + accessToken.getToken());

	       	        	// Getting user details from twitter
	       	            // For now i am getting his name only
	       	            userID = accessToken.getUserId();
	       				user = twitter.showUser(userID);
	       	            username = user.getName();
	       	            System.out.println("username:"+username);

                   } catch (Exception e) {
                       // Check log for login errors
                       Log.e("Twitter Login Error", "> " + e.getMessage());
                       e.printStackTrace();
                   }
               }else{
                   System.out.println("twitter uri:"+uri);
               }
           }
            return 1;
        }
 
        /**
         * After completing background task Dismiss the progress dialog and show
         * the data in UI Always use runOnUiThread(new Runnable()) to update UI
         * from background thread, otherwise you will get error
         * **/
        protected void onPostExecute(Integer result) {
        	aDialog.dismiss();
        	Toast.makeText(getApplicationContext(),
                    "Logged in successfully", Toast.LENGTH_SHORT)
                    .show();
        	
        	 // updating UI from Background Thread
	        runOnUiThread(new Runnable() {
	            @Override
	            public void run() {
	                // Displaying in xml ui
	                System.out.println("<b>Welcome " + username + "</b>");
	                loginTwitterButton.setImageResource(R.drawable.twitter_logout_lowres);
	            }
	        });
        }
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        System.out.println("twitter onNewIntent");
        setIntent(intent);
        if(twitterLogin){
        	Log.d(TAG,"get access token");
        	new getAccessToken().execute();
        }
    }
    
    /**
     * Function to logout from twitter
     * It will just clear the application shared preferences
     * */
    private void logoutFromTwitter() {
        // Clear the shared preferences
        Editor e = mSharedPreferences.edit();
        e.remove(PREF_KEY_OAUTH_TOKEN);
        e.remove(PREF_KEY_OAUTH_SECRET);
        e.remove(PREF_KEY_TWITTER_LOGIN);
        e.commit();
        // After this take the appropriate action
        // I am showing the hiding/showing buttons again
        // You might not needed this code
    	loginTwitterButton.setImageResource(R.drawable.twitter_login_lowres);
    }
    
    private void checkLocationSettings(){
		LocationManager locMan = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		//check if network or gps location is disabled and ask user to enable them if they want
		if(!isNetworkEnabled(locMan) || !isGPSEnabled(locMan)){
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
			// set title
			alertDialogBuilder.setTitle("Location Settings");
			// set dialog message
			alertDialogBuilder.setMessage("For better performance, enable location services through GPS " +
					"or network provider in user settings. Do you want to set them now?")
			.setCancelable(false)
			.setPositiveButton("Yes",new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,int id) {
				// if this button is clicked, close
				// current activity
					startActivityForResult(new Intent(android.provider.Settings.
							ACTION_LOCATION_SOURCE_SETTINGS), 0);	
				}
			})
			.setNegativeButton("Maybe Later",new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,int id) {
					// if this button is clicked, just close
					// the dialog box and do nothing
					dialog.cancel();
				}
			});
			// create alert dialog
			AlertDialog alertDialog = alertDialogBuilder.create();
			// show it
			alertDialog.show();
		}
    }
    
	boolean isNetworkEnabled(LocationManager locMan){
		return locMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
	}
	boolean isGPSEnabled(LocationManager locMan){
		return locMan.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}
}
