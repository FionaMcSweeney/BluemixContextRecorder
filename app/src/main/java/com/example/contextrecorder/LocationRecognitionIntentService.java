package com.example.contextrecorder;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import bolts.Continuation;
import bolts.Task;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.ibm.mobile.services.data.IBMDataException;
import com.ibm.mobile.services.data.IBMDataObject;
import com.ibm.mobile.services.data.IBMQuery;

@SuppressLint("SimpleDateFormat")
public class LocationRecognitionIntentService extends IntentService implements
	GooglePlayServicesClient.ConnectionCallbacks,
	GooglePlayServicesClient.OnConnectionFailedListener,
	com.google.android.gms.location.LocationListener{

	List<Item> itemList;
	ContextRecorderApplication crApplication;
	ArrayAdapter<Item> lvArrayAdapter;
	
	LocationClient mLocationClient;
	
	// Define an object that holds accuracy and frequency parameters
	LocationRequest mLocationRequest;
	
	// Variable to hold the location
	String mocLocationProvider= LocationManager.GPS_PROVIDER;//.NETWORK_PROVIDER;
	Location defaultLocation = createMocks(mocLocationProvider, 51.8449269, -8.492791699999998, 3.0f);
	Location currentLocation = defaultLocation;
	
	public static final String CLASS_NAME="MainActivity";
	
	// Milliseconds per second
	private static final int MILLISECONDS_PER_SECOND = 10;
	// Update frequency in seconds
	public static final int UPDATE_INTERVAL_IN_SECONDS = 10;
	// Update frequency in milliseconds
	private static final long UPDATE_INTERVAL =
	    MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
	// The fastest update frequency, in seconds
	private static final int FASTEST_INTERVAL_IN_SECONDS = 10;
	// A fast frequency ceiling in milliseconds
	private static final long FASTEST_INTERVAL =
	    MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;
	
	public static final int DETECTION_INTERVAL_SECONDS = 40;
    public static final int DETECTION_INTERVAL_MILLISECONDS =
            MILLISECONDS_PER_SECOND * DETECTION_INTERVAL_SECONDS;

    private long lastLocationUpdate;
    int bufferSize = 0;
    AudioRecord audio = null;
    Thread thread;
    
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    
    Geocoder geocoder;
	boolean processingData = false;
    
	public LocationRecognitionIntentService() {
		super("");
	}
	
	@Override
	public void onCreate(){
		/* Use application class to maintain global state. */
		crApplication = (ContextRecorderApplication) getApplication();
		// Create the LocationRequest object
        mLocationRequest = LocationRequest.create();
        // Use high accuracy
        mLocationRequest.setPriority(
                LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the update interval to 5 seconds
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        // Set the fastest update interval to 1 second
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        // connect to the location client
        connectToLocationClient();
        
        lastLocationUpdate = new Date().getTime();
        
        geocoder = new Geocoder(this, Locale.getDefault());
        
        // setup audio 
        int sampleRate = 8000;
        try {
            bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
            audio = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        } catch (Exception e) {
            android.util.Log.e("TrackingFlow", "Exception", e);
        }
        
        // Thread to create mock Locations
    	thread = new Thread(new Runnable()
    	{
    			
    		@SuppressWarnings("static-access")
			@SuppressLint("NewApi")
    		public void run()
    		{
    			// Create a new Location to inject into Location Services
    		    int pauseInterval = 100;
    		    mLocationClient.setMockMode(true);
    		    String mocLocationProvider= LocationManager.NETWORK_PROVIDER;
    		    /*
                * Wait to allow the test to switch to the app under test, by putting the thread
                * to sleep.
                */
                try {
                	thread.sleep((long) (pauseInterval * 1000));
                } catch (InterruptedException e) {
                    return;
                }
    		    
                // setup mock Locations
                Location mock1 = createMocks(mocLocationProvider, 51.898158, -8.472828, 3.0f);

		    	currentLocation = mock1;
		    	mLocationClient.setMockLocation(currentLocation);
		    	 try {
		         	thread.sleep((long) (DETECTION_INTERVAL_MILLISECONDS));
		         } catch (InterruptedException e) {
		             return;
		         }
	            onLocationChanged(mLocationClient.getLastLocation());
    		}
    		
    	});
        
	}

	@Override
	public void onLocationChanged(Location location) {
		long currentTime = new Date().getTime();
		long timeInLocation = currentTime - lastLocationUpdate;
		 // Report to the UI that the location was updated
        currentLocation = location;
        
        if (audio != null){
        	audio.startRecording();
        }
        getLocationsItems();			
        createContextRecord(location, timeInLocation);
        lastLocationUpdate = currentTime;
              
	}

	@SuppressLint("NewApi")
	public void createContextRecord(Location location, long timeInLocation){		
		Item context = new Item();
		List<Address> addresses = null;
		try {
			addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
			if (addresses != null && !addresses.isEmpty()) {
				String address = addresses.get(0).getAddressLine(0);
				String locality = addresses.get(0).getLocality() != null 
						? addresses.get(0).getLocality().replace(".", "").replace(" ", "_") 
								: addresses.get(0).getAdminArea();
				context.setLocationAddress(address);
				context.setLocality(locality);
			}
			context.setLocationLatitude(Double.toString(location.getLatitude()));
			context.setLocationLongtitude(Double.toString(location.getLongitude()));  
			context.setDateRecorded((dateFormat.format(new Date())).toString());
			context.setTimeRecorded((timeFormat.format(new Date())).toString());
			context.setVolume(Double.toString(getEnvironmentVolume()));
			
			if (audio != null){
				audio.stop();
	        }
			
			addLocationItemToCloud(context);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	private double getEnvironmentVolume(){
		short[] buffer = new short[bufferSize];
	    int bufferReadResult = 1;
	    double lastLevel = 0.0;
	    if (audio != null) {

	        // Sense the voice...
	        bufferReadResult = audio.read(buffer, 0, bufferSize);
	        double sumLevel = 0;
	        for (int i = 0; i < bufferReadResult; i++) {
	            sumLevel += buffer[i] *  buffer[i];
	        }
	        
	        sumLevel /=bufferReadResult;
	        lastLevel = Math.sqrt(sumLevel);
	    }
		return lastLevel;
	    
	}
	
	private void addLocationItemToCloud(Item item) {
		// Use the IBMDataObject to create and persist the Item object.
		item.save().continueWith(new Continuation<IBMDataObject, Void>() {

			@Override
			public Void then(Task<IBMDataObject> task) throws Exception {
                // Log if the save was cancelled.
                if (task.isCancelled()){
                    Log.e(CLASS_NAME, "Exception : Task " + task.toString() + " was cancelled.");
                }
				 // Log error message, if the save task fails.
				else if (task.isFaulted()) {
					Log.e(CLASS_NAME, "Exception : " + task.getError().getMessage());
				}
                // If the result succeeds, load the list.
				else {
					Log.i(CLASS_NAME, "Item has been logged");
				}
				return null;
			}

		});
	}
	
	@Override
	public void onConnectionFailed(ConnectionResult result) {		
    	Log.e("Connection", 
    			"Location client failed to connect");
    	Toast.makeText(this, "Unable to retrieve location updates at this time",
                Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onConnected(Bundle connectionHint) {	
		Toast.makeText(this, "Listening for location updates",
                Toast.LENGTH_SHORT).show();		
		mLocationClient.requestLocationUpdates(mLocationRequest, this);
		getLocationsItems();
	    thread.start();	    	    
	}

	@Override
	public void onDisconnected() {
		Toast.makeText(this, "Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
	}
	
	private void connectToLocationClient() {
        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.
                        isGooglePlayServicesAvailable(this);
        Log.i("ResultCode", Integer.toString(resultCode));
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
        	mLocationClient = new LocationClient(this, this, this);
			mLocationClient.connect();			
            // log the status
            Log.i("Location Updates",
                    "Google Play services is available.");            
        // Google Play services was not available for some reason.
        // resultCode holds the error code.
        } else {
        	Log.e("Location Updates", 
        			"Google Play services is not available.");           
        }
		
    }

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {		
		return Service.START_REDELIVER_INTENT;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@SuppressLint("NewApi")
	private Location createMocks(String provider, double lat, double lng, float accuracy){
        // Create a new mock Location
        Location newLocation = new Location(provider);
        newLocation.setLatitude(lat);
        newLocation.setLongitude(lng);
        newLocation.setAccuracy(accuracy);        
        newLocation.setTime(System.currentTimeMillis());

        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            newLocation.setElapsedRealtimeNanos(
                 SystemClock.elapsedRealtimeNanos());
        }
        return newLocation;
	}
	
	public void getLocationsItems() {
		try {
			
			IBMQuery<Item> query = IBMQuery.queryForClass(Item.class);
			
			// get locality
			String locality = "";
			List<Address> address;
			try {
				address = geocoder.getFromLocation(currentLocation.getLatitude(), currentLocation.getLongitude(), 1);
				if (address != null && !address.isEmpty()) {
					locality = address.get(0).getLocality() != null 
							? address.get(0).getLocality().replace(".", "").replace(" ", "_") 
									: address.get(0).getAdminArea();
				}			
			} catch (IOException e) {
				e.printStackTrace();
			}
			String date = "";
			date = dateFormat.format(new Date()).toString();
			query.whereKeyEqualsTo("date", date);
			query.whereKeyEqualsTo("locality", locality);
			query.find().continueWith(new Continuation<List<Item>, Void>() {
			
				@Override
				public Void then(Task<List<Item>> task) throws Exception {
                    final ArrayList<Item> objects = (ArrayList<Item>) task.getResult();
                     // Log if the find was cancelled.
                    if (task.isCancelled()){
                        Log.e("DATA", "Exception : Task " + task.toString() + " was cancelled.");
                    }
					 // Log error message, if the find task fails.
					else if (task.isFaulted()) {
						Log.e("DATA", "Exception : " + task.getError().getMessage());
					}					
					 // If the result succeeds, load the list.
					else {
						Log.i("in else", objects.toString());
                        postLocationsToActivity(objects); 
					}
					return null;
				}
			},Task.UI_THREAD_EXECUTOR);
			
		}  catch (IBMDataException error) {
			Log.e("DATA", "Exception : " + error.getMessage());
		}
	}
	
	@SuppressWarnings("unchecked")
	public void postLocationsToActivity(ArrayList<Item> objects) {
		Intent intent = new Intent("my-event");
        intent.putParcelableArrayListExtra("message", (ArrayList<? extends Parcelable>) objects);
        intent.putExtra("currentLocation", currentLocation);
        LocalBroadcastManager
        	.getInstance(this).sendBroadcast(intent);
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
	}
	
}