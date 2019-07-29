package com.example.fitnesstracker;

import com.google.android.gms.maps.GoogleMap;

import org.jetbrains.annotations.NotNull;

public class GooglemapHelper {

    public static void defaultMapSettings(@NotNull GoogleMap googleMap) {
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setRotateGesturesEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setTiltGesturesEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(false);
        googleMap.setBuildingsEnabled(true);
    }
}