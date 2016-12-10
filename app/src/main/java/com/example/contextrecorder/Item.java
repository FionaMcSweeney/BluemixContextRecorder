package com.example.contextrecorder;

import com.ibm.mobile.services.data.IBMDataObject;
import com.ibm.mobile.services.data.IBMDataObjectSpecialization;

@IBMDataObjectSpecialization("Item")
public class Item extends IBMDataObject {
	public static final String CLASS_NAME = "Item";
	private static final String ADDRESS = "address";
	private static final String LATITUDE = "latitude";
	private static final String LONGTITUDE = "longtitude";
	private static final String DATE = "date";
	private static final String TIME = "time";
	private static final String VOLUME = "volume";
	private static final String LOCALITY = "locality";
	
	public Item(){	
	}
	
	public String getVolume(){
		return (String) getObject(VOLUME);
	}
	
	public String getLocality(){
		return (String) getObject(LOCALITY);
	}
	
	public void setVolume(String item){
		setObject(VOLUME, (item != null) ? item : "");
	}
	
	public void setLocality(String item){
		setObject(LOCALITY, (item != null) ? item : "");
	}

	public String getLocationAddress(){
		return (String) getObject(ADDRESS);
	}
	
	public void setLocationAddress(String item){
		setObject(ADDRESS, (item != null) ? item : "");
	}
	
	public void setTimeRecorded(String item){
		setObject(TIME, (item != null) ? item : "");
	}
	
	public String getTimeRecorded(){
		return (String) getObject(TIME);
	}
	
	public void setDateRecorded(String item){
		setObject(DATE, (item != null) ? item : "");
	}
	
	public String getDateRecorded(){
		return (String) getObject(DATE);
	}
	
	public String getLocationLatitude(){
		return (String) getObject(LATITUDE);
	}
	
	public void setLocationLatitude(String item){
		setObject(LATITUDE, (item != null) ? item : "");
	}
	
	public String getLocationLongtitude(){
		return (String) getObject(LONGTITUDE);
	}
	
	public void setLocationLongtitude(String item){
		setObject(LONGTITUDE, (item != null) ? item : "");
	}
}
