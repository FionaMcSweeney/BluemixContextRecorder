package com.example.contextrecorder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends FragmentActivity {

	//private static final android.R.attr R = ;
	// Google Map
    private GoogleMap googleMap;
    // An instance of the status broadcast receiver
    DownloadStateReceiver mDownloadStateReceiver;
    Geocoder geocoder;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		geocoder = new Geocoder(this, Locale.getDefault());
		// The filter's action is BROADCAST_ACTION
        IntentFilter statusIntentFilter = new IntentFilter(
                "my-event");
        
        // Sets the filter's category to DEFAULT
        statusIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        
        // Instantiates a new DownloadStateReceiver
        mDownloadStateReceiver = new DownloadStateReceiver();
        
        // Registers the DownloadStateReceiver and its intent filters
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mDownloadStateReceiver,
                statusIntentFilter);
        
        try {
            // Loading map
            initilizeMap();
 
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	/*
     * Called when the Activity becomes visible.
     */
    @Override
    protected void onStart() {
        super.onStart();
        // use this to start and trigger a service
        Intent i= new Intent(this,LocationRecognitionIntentService.class);
        // potentially add data to the intent
        i.putExtra("KEY1", "Value to be used by the service");
        this.startService(i); 
    }
    
    /**
     * This class uses the BroadcastReceiver framework to detect and handle status messages from
     * the service that downloads URLs.
     */
    private class DownloadStateReceiver extends BroadcastReceiver {
        
        private DownloadStateReceiver() {
            
            // prevents instantiation by other packages.
        }
        /**
         *
         * This method is called by the system when a broadcast Intent is matched by this class'
         * intent filters
         *
         * @param context An Android context
         * @param intent The incoming broadcast Intent
         */
        @Override
        public void onReceive(Context context, Intent intent) {
        	googleMap.clear();
        	// Extract data included in the Intent
            ArrayList<Parcelable> items = intent.getParcelableArrayListExtra("message");
            Location location = intent.getParcelableExtra("currentLocation");
            // create marker
            MarkerOptions marker = new MarkerOptions()
            .position(new LatLng(location.getLatitude(), location.getLongitude()));
            // adding marker
            marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            .title("You are here");
            googleMap.addMarker(marker);
            CameraPosition cameraPosition = new CameraPosition.Builder().target(
                   new LatLng(location.getLatitude(), location.getLongitude())).zoom(12).build();    
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            List<Item> listOfLocations = getItemsToShow(items, location);
            for (Item z : listOfLocations) {
            	double lat = Double.parseDouble(z.getLocationLatitude());
            	double lon = Double.parseDouble(z.getLocationLongtitude());
            	String address = "Unknown";
            	String volumeString = z.getVolume();
            	try {
					List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
					if (addresses != null && !addresses.isEmpty()) {
						address = addresses.get(0).getAddressLine(0);
					}
					int decimalPointIndex = volumeString.indexOf(".");
	            	volumeString = volumeString.substring(0, decimalPointIndex + 2);
				} catch (IOException e) {
					e.printStackTrace();
				}
            	String message = "Address: " + address + " Volume: " + volumeString + "dB";            	
            	MarkerOptions marker2 = new MarkerOptions().position(new LatLng(lat, lon));
            	float vol = Float.parseFloat(z.getVolume());
            	if (vol <= 40) {
            		marker2.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            		.title(message);
            	} else if (vol > 40 && vol <= 60) {
            		marker2.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            		.title(message);;
            	} else {
            		marker2.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            		.title(message);;
            	}          	
                 googleMap.addMarker(marker2);
            }
        }
    }
    
    public List<Item> getItemsToShow(ArrayList<Parcelable> objects, Location currentLocation) {
    	List<Item> locationToShow = new ArrayList<Item>();
    	for (int i = 0; i < objects.size(); i++) {
    		Item item = (Item) objects.get(i);
        	String currentLat = Double.toString(currentLocation.getLatitude());
        	String currentLong = Double.toString(currentLocation.getLongitude());
           if (!item.getLocationLatitude().contentEquals(currentLat)
        		   && !item.getLocationLongtitude().contentEquals(currentLong)) {
        	   
        		   if (locationToShow.isEmpty()) {
        			   locationToShow.add(item);
        		   } else {
        			   int count = 0;
        			   double volumeAvg = Double.parseDouble(item.getVolume());               			   
        			   if (locationToShow.contains(item)) {
        				   for (Item loc : locationToShow) {
                			   if (loc.getLocationLatitude().contentEquals(item.getLocationLatitude()) 
                					   && loc.getLocationLongtitude().contentEquals(item.getLocationLongtitude())) {
                				   count ++;
                				   volumeAvg = volumeAvg + Double.parseDouble(loc.getVolume()) / count;
                				   loc.setVolume(Double.toString(volumeAvg));
                			   } 
                		   }
        			   } else {
        				   locationToShow.add(item);
        			   }
        			   
        		   }

           } else {
        	   objects.remove(item);
           }
           
        }
		return locationToShow;
    }
    
    /**
     * function to load map. If map is not created it will create it for you
     * */
    private void initilizeMap() {
        if (googleMap == null) {
            googleMap = ((MapFragment) getFragmentManager().findFragmentById(
                    R.id.map)).getMap();
 
            // check if map is created successfully or not
            if (googleMap == null) {
                Toast.makeText(getApplicationContext(),
                        "Sorry! unable to create maps", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }
    
    /*
     * Called when the Activity is no longer visible.
     */
    @Override
    protected void onStop() {
        super.onStop();
    }


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	

}
