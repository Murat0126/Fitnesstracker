package com.example.fitnesstracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.widget.Toast;

import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.model.LatLng;

public class MyLocationService extends BroadcastReceiver {

    public static final String ACTION_PROCESS_UPDATE= "package com.example.fitnesstracker.UPDATE_LOCATION";
    private double lat1,lon1,lat2 = 0,lon2 = 0;
    private double distance;
    private MainActivity mainActivity;
    private  Calculator calculator;

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null){
            final String action = intent.getAction();
            if(ACTION_PROCESS_UPDATE.equals(action)){
                LocationResult result = LocationResult.extractResult(intent);
                if(result != null){
                    Location location = result.getLastLocation();

                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                    if (lat1== 0&&lon1 == 0){
                        lat1 = latLng.latitude;
                        lon1 = latLng.longitude;
                    }else {
                        lat1 = lat2;
                        lon1 = lon2;
                    }

                    lat2 = latLng.latitude;
                    lon2 = latLng.longitude;

                    try{

                        MainActivity.getInstance().updateTextView(location);
//                        distance = Calculator.getDistanceFromLatLonInKm(lat1,lon1,lat2,lon2);
                        distance = distFrom(lat1,lon1, lat2,lon2);
                        mainActivity.getDistance(calculator.getDistanceFromLatLonInKm(lat1,lon1,lat2,lon2));
                        Toast.makeText(context, " Calc/ Km: " + calculator.getDistanceFromLatLonInKm(lat1,lon1,lat2,lon2), Toast.LENGTH_SHORT).show();
                        Toast.makeText(context, "distFrom Km: " + distance, Toast.LENGTH_SHORT).show();
                    }catch (Exception e){

//                        distance = calculator.getDistanceFromLatLonInKm(lat1,lon1,lat2,lon2);
                        distance = distFrom(lat1,lon1, lat2,lon2);
                        Toast.makeText(context, " Fitness tracker LatLng: " + latLng + "Km: " + distance, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }

    }

    public static float distFrom (double lat1, double lng1, double lat2, double lng2 )
    {
        double earthRadius = 3958.75;
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double dist = earthRadius * c;

        int meterConversion = 1609;

        return new Float(dist * meterConversion).floatValue();
    }


//    private double calcDistance(Location location) {
//        float distance = 0;
//        float[] distanceArr = new float[2];
//        if (location != null) {
//            Location.distanceBetween(
//                    prevLatitude,
//                    prevLongitude,
//                    location.getLatitude(),
//                    location.getLongitude(), distanceArr);
//            if (ValidateUtil.isValidNumber(distanceArr[0])) {
//                distance = distanceArr[0];
//            }
//        }
//        Log.d(TAG, "calcDistance : distance= " + distance);
//        return distance;
//    }
}
