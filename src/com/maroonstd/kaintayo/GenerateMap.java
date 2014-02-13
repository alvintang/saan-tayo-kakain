package com.maroonstd.kaintayo;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.RequestAsyncTask;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class GenerateMap extends Activity {

	private String DEBUG="KAINv1";
	private int userIcon, foodIcon;
	private GoogleMap theMap;
	private LocationManager locMan;
	private Marker userMarker;
	private Marker[] placeMarkers;
	private MarkerOptions[] places;
	private final int MAX_PLACES = 60;
	private double lat=0;
	private double lng=0;
	private float zoom=0;
	private ListView placesList;
	private PlaceListAdapter arrayAdapter;
	private ArrayList<Marker> placesListView;
	
	ProgressDialog aDialog;
	
//	private TextView radiusText,shopList;
	private int nearbySearchRadius;
	private	int selected=0;
	String searchName;
	String nextPageToken;
	String url;
	int page=0, nextPageFlag=0;
	int i=0,placeMarkerIndex=0,markersAdded=0; //for markers added

	// button for next resto
	Button nextResto;
	
	/**
	 * Twitter Stuff
	 */
	private static SharedPreferences mSharedPreferences; //for Twitter log on

    static String TWITTER_CONSUMER_KEY = "LLo9tdXrD4YZqY9Wty4nxA";
    static String TWITTER_CONSUMER_SECRET = "c6b74Kc1cQA9IXgtbEbGbH63HJuAnQUCpU0bJEwQ93w";
 
    // Preference Constants
    static String PREFERENCE_NAME = "twitter_oauth";
    static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
    static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
    static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLogedIn";
 
    static final String TWITTER_CALLBACK_URL = "oauth://t4jsample";
 
    // Twitter oauth urls
    static final String URL_TWITTER_AUTH = "auth_url";
    static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";
    static final String URL_TWITTER_OAUTH_TOKEN = "oauth_token";
    
    private ProgressDialog pDialog;
	private ImageButton shareButtonTw;
    /**
     * end Twitter stuff
     */
     
	/**
	 * fb stuff
	 */
	private static final String TAG = "SelectionFragment";
	private static final int REAUTH_ACTIVITY_CODE = 100;
	private ImageButton shareButtonFB;
	
	private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");
	private static final String PENDING_PUBLISH_KEY = "pendingPublishReauthorization";
	private boolean pendingPublishReauthorization = false;
	
	private UiLifecycleHelper uiHelper;
	private Session.StatusCallback callback = new Session.StatusCallback() {
		
		@Override
		public void call(Session session, SessionState state, Exception exception) {
			onSessionStateChange(session,state,exception);
		}
	};
	/**
	 * fb stuff end
	 */
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_generate);
		
		/*
		 * fb stuff start
		 */
		uiHelper = new UiLifecycleHelper(this, callback);
	    uiHelper.onCreate(savedInstanceState);
		
	    shareButtonFB = (ImageButton) findViewById(R.id.shareButtonFB);
	    shareButtonFB.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	            publishStory();        
	        }
	    });
	    
	    shareButtonTw = (ImageButton) findViewById(R.id.shareButtonTw);
	    shareButtonTw.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	            System.out.println("Tweet!");
	            new updateTwitterStatus().execute("Kakain ako sa "+placeMarkers[selected].getTitle()+
	            		"! via Saan Tayo Kakain? http://play.google.com/store/apps/details?id=com.maroonstd.kaintayo");
	        }
	    });
	    
	    nextResto = (Button) findViewById(R.id.nextStore);
	    nextResto.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	            System.out.println("Next");
	            selectNextResto();
	        }
	    });
	    //check for an open session
	    Session session = Session.getActiveSession();
	    if(session != null && session.isOpened()){
	    	shareButtonFB.setVisibility(View.VISIBLE);
	    	//get user's data
	    	makeMeRequest(session);
	    }
	    if (savedInstanceState != null) {
	        pendingPublishReauthorization = 
	            savedInstanceState.getBoolean(PENDING_PUBLISH_KEY, false);
	    }
	    
	    /*
	     * fb stuff end
	     */
	    
	    /**
	     * If twitter is logged on, show tweet button
	     */
	    // Shared Preferences
        mSharedPreferences = getApplicationContext().getSharedPreferences(
                "MyPref", 0);
	    if(!isTwitterLoggedInAlready()){
	    	shareButtonTw.setVisibility(View.INVISIBLE);
	    }else{
	    	shareButtonTw.setVisibility(View.VISIBLE);
	    }
	    
		userIcon = R.drawable.green_point;
		foodIcon = R.drawable.red_point;
		searchName = "";
		nextPageToken = "";
		
		lat=0;
		lng=0;
		//initialize text part
//		radiusText = (TextView) findViewById(R.id.radius);
//		shopList = (TextView) findViewById(R.id.shoplist);
		Bundle extras = getIntent().getExtras();
		if(extras!=null){
			nearbySearchRadius=extras.getInt("radius");
			searchName=extras.getString("searchName");
			lat=extras.getDouble("latitude");
			lng=extras.getDouble("longitude");
		}
		switch(nearbySearchRadius){
			case(1000):{
				zoom=15;
				break;
			}
			case(5000):{
				zoom=13;
				break;
			}
			case(10000):{
				zoom=12;
				break;
			}
			case(20000):{
				zoom=11;
				break;
			}
		}
//		radiusText.setText("Mga kainan sa loob ng "+nearbySearchRadius/1000+" kilometro");
		//initialize location manager here so that isNetworkEnabled() and isGPSEnabled() will not return null

		//initialize the map
		if(theMap==null){
			theMap = ((MapFragment)getFragmentManager().findFragmentById(R.id.the_map)).getMap();
			if(theMap!=null){
				theMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
				placeMarkers = new Marker[MAX_PLACES];
				updatePlaces(lat,lng);
			}
		}
		
	    ImageButton openMaps = (ImageButton) findViewById(R.id.openMaps);
	    openMaps.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	            openInMaps(placeMarkers[selected]);
	        }
	    });
	    ImageButton locateUser = (ImageButton) findViewById(R.id.locateUser);
	    locateUser.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	            goToUserLocation();
	        }
	    });
	    
	 // Look up the AdView as a resource and load a request.
	    AdView adView = (AdView)this.findViewById(R.id.adView);
	    AdRequest adRequest = new AdRequest.Builder().build();
	    adView.loadAd(adRequest);
	    
	    // Add ListView
	    placesList = (ListView)findViewById(R.id.placeList);
	    ImageButton showList = (ImageButton) findViewById(R.id.listplaces);
	    showList.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	            listPlaces();
	        }
	    });
	    placesListView = new ArrayList<Marker>();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.my_map, menu);
		return true;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	    if (requestCode == REAUTH_ACTIVITY_CODE) {
	        uiHelper.onActivityResult(requestCode, resultCode, data);
	    }
	}
	
	@Override
	public void onResume() {
	    super.onResume();
	    uiHelper.onResume();
	}

	@Override
	public void onSaveInstanceState(Bundle bundle) {
	    super.onSaveInstanceState(bundle);
	    bundle.putBoolean(PENDING_PUBLISH_KEY, pendingPublishReauthorization);
	    uiHelper.onSaveInstanceState(bundle);
	}

	@Override
	public void onPause() {
	    super.onPause();
	    uiHelper.onPause();
	}

	@Override
	public void onDestroy() {
	    super.onDestroy();
	    uiHelper.onDestroy();
	}
	
	private void onSessionStateChange(final Session session, SessionState state, Exception exception){
		if (state.isOpened()) {
	        shareButtonFB.setVisibility(View.VISIBLE);
	        if (pendingPublishReauthorization && 
	                state.equals(SessionState.OPENED_TOKEN_UPDATED)) {
	            pendingPublishReauthorization = false;
//	            publishStory();
	        }
	    } else if (state.isClosed()) {
	        shareButtonFB.setVisibility(View.INVISIBLE);
	    }
		if(session != null && session.isOpened()){
			//get user's data
			makeMeRequest(session);
		}
	}
	
	// don't really need this
	private void makeMeRequest(final Session session) {
		// make an API call to get user data and define a new callback to handle the response
				Request request = Request.newMeRequest(session, new Request.GraphUserCallback() {
					
					@Override
					public void onCompleted(GraphUser user, Response response) {
						// if response is successful
						if (session == Session.getActiveSession()){
							if(user != null){
								//set the id for the ProfilePictureView
								//view that in turn displays the profile picture
//								profilePictureView.setProfileId(user.getId());
								//set the TextView's text to display user's name
//								userNameView.setText(user.getName());
								
							}
						}
						if(response.getError() != null){
							// handle errors
						}
					}
				});
				request.executeAsync();
	}

	private void publishStory() {

		Session session = Session.getActiveSession();
		
		if(session != null){
			//check for publish permissions
			List<String> permissions = session.getPermissions();
			if (!isSubsetOf(PERMISSIONS,permissions)){
				pendingPublishReauthorization = true;
				Session.NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(this, PERMISSIONS);
				session.requestNewPublishPermissions(newPermissionsRequest);
				//return; 
				/*
				 * (from http://stackoverflow.com/questions/14945276/issues-posting-picture-to-facebook-session-requestnewpublishpermissions)
				 * the facebook tutorial used that return statement, because the code you are using is in a
				 * fragment class. the technique is when execution will request additional permission it 
				 * should return from the publishstory() after passing the request to FB SDK for handle. 
				 * cause existing code should not be executed until the request is completed. 
				 * now when the request is completed, the session state will be changed 
				 * and the onSessionStateChanged() method of MainActivity will be called
				 * 
				 * (added by me)
				 * since an activity class is used in this case, when you return it ends the whole function 
				 * (as opposed to using a fragment class, which is my guess) 
				 */
			}

			Bundle postParams = new Bundle();
			postParams.putString("name", "Saan tayo kakain? Sa "+placeMarkers[selected].getTitle()+ "!");
			postParams.putString("caption", placeMarkers[selected].getSnippet());
	        postParams.putString("description", "Saan tayo kakain? Kahit saan! Ako na hahanap para sa'yo!");
	        postParams.putString("link", "http://play.google.com/store/apps/details?id=com.maroonstd.kaintayo"); // TODO: put google play link
	        postParams.putString("picture", "http://i.imgur.com/vRqnOi7.png");
	        
	        Request.Callback callback = new Request.Callback() {
				
				@Override
				public void onCompleted(Response response) {
					JSONObject graphResponse = response.getGraphObject().getInnerJSONObject();
					String postId = null;
					
					try{
						postId = graphResponse.getString("id");
					}catch(JSONException e){
						Log.i(TAG,"JSON error "+e.getMessage());
					}
					
					FacebookRequestError error = response.getError();
					if(error != null){
						Toast.makeText(getApplicationContext(), error.getErrorMessage(), Toast.LENGTH_SHORT).show();
					}else{
						Toast.makeText(getApplicationContext(), "Post successful!", Toast.LENGTH_LONG).show();
					}
				}
			};
			Request request = new Request(session,"me/feed",postParams,HttpMethod.POST,callback);
			RequestAsyncTask task = new RequestAsyncTask(request);
			task.execute();
		}else{
			System.out.println("session null");
		}
	}

	private boolean isSubsetOf(Collection<String> subset, Collection<String> superset) {
	    for (String string : subset) {
	    	System.out.println("permissions:"+subset.toString());
	        if (!superset.contains(string)) {
	            return false;
	        }
	    }
	    return true;
	}
	
	private void updatePlaces(double lat, double lng){
		//update location
//		Location lastLoc;
//		if(isNetworkEnabled()){
//			lastLoc = locMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//		}else if(isGPSEnabled()){
//			lastLoc = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//			Log.e("Location", "Network provider disabled");
//		}else{
//			lastLoc = locMan.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
//			Log.e("Location", "Network provider disabled");
//			Log.e("Location", "GPS provider disabled");
//		}
		LatLng lastLatLng=new LatLng(lat,lng);

		if(userMarker!=null) userMarker.remove();
		
		userMarker = theMap.addMarker(new MarkerOptions()
			.position(lastLatLng)
			.title("You are here")
			.icon(BitmapDescriptorFactory.fromResource(userIcon))
			.snippet("Your last recorded location"));

		CameraPosition initialCam = new CameraPosition.Builder()
		.target(lastLatLng)
		.zoom(zoom)
		.bearing(0)
		.tilt(0)
		.build();

		theMap.animateCamera(CameraUpdateFactory.newCameraPosition(initialCam),3000,null);
		
		String latVal=String.valueOf(lat);
		String lngVal=String.valueOf(lng);

		try {
	        url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="
	        +URLEncoder.encode(latVal, "UTF-8")
	        +","
	        +URLEncoder.encode(lngVal, "UTF-8")
	        +"&radius="
	        +nearbySearchRadius
	        +"&sensor="
	        +URLEncoder.encode("true", "UTF-8")
	        +"&types="
	        +URLEncoder.encode("food|bar|restaurant|bakery", "UTF-8")
//	        +"&rankby=distance"
	        +"&key="
	        +URLEncoder.encode("AIzaSyC7MBems8mlFZ-kvmbphQwcJkzzRAY4hDE", "UTF-8");
	        if(searchName.length()>0){
	        	url = url+"&name="+URLEncoder.encode(searchName, "UTF-8");
	        	System.out.println("kain url:"+url);
	        }
            aDialog = new ProgressDialog(GenerateMap.this);
            aDialog.setMessage("Naghahanap...");
            aDialog.setIndeterminate(false);
            aDialog.setCancelable(false);
            aDialog.show();
	        new GetPlaces().execute(url);
	    } catch (UnsupportedEncodingException e) {
	        e.printStackTrace();
	    }
		
	}
	
	private class GetPlaces extends AsyncTask<String,Void,String>{
		@Override
		protected void onPreExecute(){
            super.onPreExecute();

		}
		
		@Override
		protected String doInBackground(String... placesURL){
			StringBuilder placesBuilder = new StringBuilder();
			for(String placeSearchURL: placesURL){
				HttpClient placesClient = new DefaultHttpClient();
				try{
					Log.i(DEBUG, "places url:"+placeSearchURL);					
					HttpGet placesGet = new HttpGet(placeSearchURL);
					HttpResponse placesResponse = placesClient.execute(placesGet);
					StatusLine placeSearchStatus = placesResponse.getStatusLine();
					
					if(placeSearchStatus.getStatusCode()==200){
						HttpEntity placesEntity = placesResponse.getEntity();
						InputStream placesContent = placesEntity.getContent();
						InputStreamReader placesInput = new InputStreamReader(placesContent);
						BufferedReader placesReader = new BufferedReader(placesInput);
						String lineIn;
						while((lineIn = placesReader.readLine())!=null){
							placesBuilder.append(lineIn);
						}
					}
					
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			Log.i(DEBUG, "placesBuilder:"+placesBuilder.toString());
			return placesBuilder.toString();
		}
		
		protected void onPostExecute(String result){
			if(placeMarkers!=null && nextPageFlag==0){
				for(int pm=0; pm<placeMarkers.length; pm++){
					if(placeMarkers[pm]!=null){
						placeMarkers[pm].remove();
					}
				}
			}
			
			try{
				JSONObject resultObject = new JSONObject(result);
				try{
					nextPageToken=resultObject.getString("next_page_token");
					page++; //increment page for determining limits of adding marker
					nextPageFlag=1; //flag is true if there is next page
				}catch(Exception e){
					Log.i(DEBUG,"next page token error");
					e.printStackTrace();
					nextPageFlag=0;
				}
				JSONArray placesArray = resultObject.getJSONArray("results");
				places = new MarkerOptions[placesArray.length()];
				for(int p=0; p<placesArray.length(); p++){
					boolean missingValue=false;
					LatLng placeLL = null;
					String placeName="";
					String vicinity="";
					int currIcon = foodIcon;
					try{
						missingValue=false;
						JSONObject placeObject = placesArray.getJSONObject(p);
						JSONObject loc = placeObject.getJSONObject("geometry").getJSONObject("location");
						placeLL = new LatLng(Double.valueOf(loc.getString("lat")),Double.valueOf(loc.getDouble("lng")));
						JSONArray types = placeObject.getJSONArray("types");
//						for(int t=0; t<types.length(); t++){
//							String thisType=types.getString(t).toString();
//							if(thisType.contains("food")){
//								currIcon = foodIcon;
//								break;
//							}
//							if(thisType.contains("bar")){
//								currIcon = drinkIcon;
//								break;
//							}
//							if(thisType.contains("store")){
//								currIcon = shopIcon;
//								break;
//							}
//						}
						vicinity = placeObject.getString("vicinity");
						placeName = placeObject.getString("name");
					}catch(JSONException jse){
						missingValue = true;
						jse.printStackTrace();
					}
					if(missingValue){
						places[p]=null;
					}else{
						places[p]=new MarkerOptions()
								.position(placeLL)
								.title(placeName)
								.icon(BitmapDescriptorFactory.fromResource(currIcon))
								.snippet(vicinity);
					}
						
				}
			}catch(Exception e){
				e.printStackTrace();
			}

			
			if(places!=null && placeMarkers!=null){
				for(int p=0; p<places.length && p<placeMarkers.length;p++){
					if(places[p]!=null){
						placeMarkers[placeMarkerIndex]=theMap.addMarker(places[p]);
						placesListView.add(placeMarkers[placeMarkerIndex]);
						Log.e(TAG,">"+placeMarkerIndex);
						placeMarkerIndex++;
						markersAdded++;
					}
				}
			}
			//select Random result
			//problem yung last 20 lang pagpipilian niya.
			if(nextPageFlag==1){
				Log.i(DEBUG, "page token:"+page++);
				String nextUrl = url + "&pagetoken="+nextPageToken;
				nextPageToken="";
				Log.i(DEBUG, "url:"+nextUrl);
				try {
					Thread.sleep(1500); //should sleep to get next page
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		        new GetPlaces().execute(nextUrl);	
			}else{
				System.out.println("page token done");
				aDialog.dismiss();
				Random r = new Random();
				if(markersAdded>0){
					selected=r.nextInt(markersAdded);
					System.out.println("kainv1 random number:"+selected);
//					shopList.setText(placeMarkers[selected].getTitle()+"\n"+placeMarkers[selected].getSnippet());
					placeMarkers[selected].showInfoWindow();
					LatLng lastLatLng = placeMarkers[selected].getPosition();
					CameraPosition initialCam = new CameraPosition.Builder()
					.target(lastLatLng)
					.zoom(zoom)
					.bearing(0)
					.tilt(0)
					.build();

					theMap.animateCamera(CameraUpdateFactory.newCameraPosition(initialCam),1000,null);
				}
				else{
					selected=0;
//					shopList.setText("No nearby places found");
				}
			}
		}
	}
	
	/**
	 * Twitter Functions
	 */
	private boolean isTwitterLoggedInAlready() {
        // return twitter login status from Shared Preferences
        return mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
    }
	
	/**
     * Function to update status
     * */
    class updateTwitterStatus extends AsyncTask<String, String, String> {
 
    	AccessToken accessToken;
        /**
         * Before starting background thread Show Progress Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(GenerateMap.this);
            pDialog.setMessage("Updating to twitter...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }
 
        /**
         * getting Places JSON
         * */
        protected String doInBackground(String... args) {
            Log.d("Tweet Text", "> " + args[0]);
            String status = args[0];
            try {
                ConfigurationBuilder builder = new ConfigurationBuilder();
                builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
                builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
                 
                // Access Token 
                String access_token = mSharedPreferences.getString(PREF_KEY_OAUTH_TOKEN, "");
                // Access Token Secret
                String access_token_secret = mSharedPreferences.getString(PREF_KEY_OAUTH_SECRET, "");
                 
                accessToken = new AccessToken(access_token, access_token_secret);
                Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);
                 
                // Update status
                twitter4j.Status response = twitter.updateStatus(status);
                 
                Log.d("Status", "> " + response.getText());
            } catch (TwitterException e) {
                // Error in updating status
                Log.d("Twitter Update Error", e.getMessage());
            }
            return null;
        }
 
        /**
         * After completing background task Dismiss the progress dialog and show
         * the data in UI Always use runOnUiThread(new Runnable()) to update UI
         * from background thread, otherwise you will get error
         * **/
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after getting all products
            pDialog.dismiss();                    // Shared Preferences
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

            // updating UI from Background Thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            "Status tweeted successfully", Toast.LENGTH_SHORT)
                            .show();
                }
            });
        }
 
    }
    
    private void selectNextResto(){
    	Random r = new Random();
    	if(markersAdded>0){
			selected=r.nextInt(markersAdded);
			System.out.println("kainv1 random number:"+selected);
//			shopList.setText(placeMarkers[selected].getTitle()+"\n"+placeMarkers[selected].getSnippet());
			placeMarkers[selected].showInfoWindow();
			LatLng lastLatLng = placeMarkers[selected].getPosition();
			CameraPosition initialCam = new CameraPosition.Builder()
			.target(lastLatLng)
			.zoom(zoom)
			.bearing(0)
			.tilt(0)
			.build();

			theMap.animateCamera(CameraUpdateFactory.newCameraPosition(initialCam),1000,null);
		}
		else{
			selected=0;
//			shopList.setText("No nearby places found");
		}   	
    }
    
    private void openInMaps(Marker mapItem){
    	if(markersAdded>0){
			LatLng lastLatLng = mapItem.getPosition();
			Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
					Uri.parse("http://maps.google.com/maps?saddr="+lat+","+lng+
							"&daddr="+lastLatLng.latitude+","+lastLatLng.longitude));
			startActivity(intent);
		}
		else{
			selected=0;
		}   	
    }
    
    private void goToUserLocation(){
    	LatLng lastLatLng=new LatLng(lat, lng);
    	CameraPosition initialCam = new CameraPosition.Builder()
		.target(lastLatLng)
		.zoom(zoom)
		.bearing(0)
		.tilt(0)
		.build();

		theMap.animateCamera(CameraUpdateFactory.newCameraPosition(initialCam),1000,null);
		userMarker.showInfoWindow();
	}
    
    private void listPlaces(){
		//populate listview
		arrayAdapter = new PlaceListAdapter(this, placesListView);
		placesList.setAdapter(arrayAdapter);
		placesList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View v, int position, long id){
				Object obj = placesList.getItemAtPosition(position);
				Marker listItem = (Marker)obj;
//				openInMaps(listItem);
				listItem.showInfoWindow();
				placesList.setVisibility(View.INVISIBLE);
				LatLng lastLatLng = listItem.getPosition();
				CameraPosition initialCam = new CameraPosition.Builder()
				.target(lastLatLng)
				.zoom(zoom)
				.bearing(0)
				.tilt(0)
				.build();

				theMap.animateCamera(CameraUpdateFactory.newCameraPosition(initialCam),1000,null);
			}
		});
		if(placesList.getVisibility()==View.INVISIBLE){
			placesList.setVisibility(View.VISIBLE);   	
		}else{
			placesList.setVisibility(View.INVISIBLE);
		}
    }
}