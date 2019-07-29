package com.example.fitnesstracker;

import com.example.fitnesstracker.interfaces.StepListener;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity
        implements
        OnMyLocationButtonClickListener,
        OnMyLocationClickListener,
        OnMapReadyCallback, SensorEventListener, StepListener {


    private StepDetector simpleStepDetector;
    private SensorManager sensorManager;
    private Sensor accel;
    private static final String TEXT_NUM_STEPS = "Number of Steps: ";
    private int numSteps;
    private double distance;
    private int seconds;


    private FusedLocationProviderClient fusedLocationProviderClient ;
    public static final String TAG = "StepCounter";
    private static final int REQUEST_OAUTH_REQUEST_CODE = 0x1001;

    private GoogleMap mMap;
    private DBAdapter helper;
    static MainActivity instance;
    private LocationRequest locationRequest;
    private LinearLayout linearLayout;
    private LinearLayout steps_layout;
    private LinearLayout distance_layout;
    private LinearLayout time_layout;
    private Button btn_start;
    private Button btn_stop;
    private Button btn_pause;
    private TextView text_steps;
    private TextView txt_distance;
    private TextView txt_time;

    boolean start_clicked = false;
    boolean stop_clicked = false;
    boolean pause_clicked = false;
    boolean  running = false;

    public static MainActivity getInstance() {
        return instance;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        instance = this;

        new GpsUtils(this).turnGPSOn(new GpsUtils.onGpsListener() {
            @Override
            public void gpsStatus(boolean isGPSEnable) {
            }
        });

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        updateLocation();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MainActivity.this, "you must accept this location", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }


                }).check();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener((StepListener) this);


        linearLayout = (LinearLayout) findViewById(R.id.dates_line);
        steps_layout = (LinearLayout) findViewById(R.id.steps_layout);
        distance_layout = (LinearLayout) findViewById(R.id.distance_layout);
        time_layout = (LinearLayout) findViewById(R.id.time_layout);

        btn_start = findViewById(R.id.btn_start);
        btn_stop = findViewById(R.id.btn_stop);
        btn_pause = findViewById(R.id.btn_pause);



            btn_start.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onClick(View v) {
                    if(!start_clicked) {

                        numSteps = 0;
                        sensorManager.registerListener((SensorEventListener) MainActivity.this, accel, SensorManager.SENSOR_DELAY_FASTEST);

                        text_steps = new TextView(MainActivity.this);
                        text_steps.setText("steps: ");
                        text_steps.setTextSize(30);

                        txt_distance = new TextView(MainActivity.this);
                        txt_distance.setText("distance: " + distance);
                        txt_distance.setTextSize(30);

                        txt_time = new TextView(MainActivity.this);
                        txt_time.setText("time: ");
                        txt_time.setTextSize(30);

                        steps_layout.addView(text_steps);
                        distance_layout.addView(txt_distance);
                        time_layout.addView(txt_time);



                            start_clicked = true;
                        runtimer();

                    }

                }
            });

            btn_stop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sensorManager.unregisterListener((SensorEventListener) MainActivity.this);
                    start_clicked = false;

                }
            });


            btn_pause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });




        helper = new DBAdapter(this);


    }

    private void runtimer(){
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                int hours = seconds/3600;
                int minutes = (seconds%3600)/60;
                int second = seconds%60;

                @SuppressLint("DefaultLocale") String time = String.format("%d:%02d:%02d", hours, minutes, second);
                txt_time.setText("time: " + time);
                if (start_clicked){
                    seconds++;
                }
                handler.postDelayed(this,1000);
            }
        });
    }


    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        enableMyLocation();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onResume() {
        super.onResume();
        running = true;
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if(countSensor != null){
            sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
        }else {
            Toast.makeText(this, "Сенсор не найден!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        running = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector.updateAccel(
                    event.timestamp, event.values[0], event.values[1], event.values[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @SuppressLint("SetTextI18n")
    @Override
    public void step(long timeNs) {
        numSteps++;
        text_steps.setText(TEXT_NUM_STEPS + numSteps);
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private void updateLocation() {
        buildLocationRequest();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, getPendingIntent());
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, MyLocationService.class);
        intent.setAction(MyLocationService.ACTION_PROCESS_UPDATE);
        return  PendingIntent.getBroadcast(this, 0,intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(500);
        locationRequest.setFastestInterval(300);
        locationRequest.setSmallestDisplacement(10f);
    }

    public  void  updateTextView(final Location location){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String loc = String.valueOf(location);
                txt_distance.setText(loc);
                Toast.makeText(MainActivity.this,"MOving the camera to: lat: " + location.getLatitude() + "lng: "+ location.getLongitude(), Toast.LENGTH_SHORT).show();
                addLocation(location);
                moveCamera(location,15f);
            }
        });
    }


    private void moveCamera(Location location, float zoom){
        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
        Toast.makeText(MainActivity.this,"MOving the camera to: lat: " + location.getLatitude() + "lng: "+ location.getLongitude(), Toast.LENGTH_SHORT).show();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,zoom));
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission toaccess the location is missing.

        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);

        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {

            Intent i = new Intent(MainActivity.this, MainActivity.class);
            finish();
            overridePendingTransition(0, 0);
            startActivity(i);
            overridePendingTransition(0, 0);


        }
    }

    public void getDistance(double distance){
        this.distance = distance;
    }

    public void addLocation(Location latLng)
    {
        double latitude = latLng.getLatitude();
        double longitude = latLng.getLongitude();
        String lat = String.valueOf(latitude);
        String lon = String.valueOf(longitude);

            long id = helper.insertData(lat,lon);
        if(id<=0)
        {
            Message.message(getApplicationContext(),"Insertion Unsuccessful"+ lat + " - " + lon);
        } else
        {
            Message.message(getApplicationContext(),"Insertion Successful"+ lat + " - " + lon);
        }

    }

}
