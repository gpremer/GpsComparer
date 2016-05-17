package com.ictects.gpscomparer;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.*;

import java.util.Locale;

public class MainActivity
        extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = "GpsMain";

    private Button androidBtn;
    private Button playServicesBtn;
    private Button stopBtn;
    private TextView measurements;
    private TextView currentDistance;
    private Location lastLocation;
    private double totalDistance;
    private LocationListener androidLocationListener;
    private GoogleApiClient googleApiClient;
    private com.google.android.gms.location.LocationListener playServiceslocationListener;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        androidBtn = (Button) findViewById(R.id.android);
        playServicesBtn = (Button) findViewById(R.id.playServices);
        stopBtn = (Button) findViewById(R.id.stopRecording);
        measurements = (TextView) findViewById(R.id.measurements);
        currentDistance = (TextView) findViewById(R.id.currentDistance);

        androidBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAndroidGpsRecording();
            }
        });
        playServicesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                startGoogleLocationRecording();
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopGpsRecording();
            }
        });
        stopBtn.setEnabled(false);

        playServicesBtn.setEnabled(false);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        initPlayServices();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void initPlayServices() {
        Log.d(TAG, "Connecting to Google Play Services");
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    private void startAndroidGpsRecording() {
        initLocationRecording();

        // Acquire a reference to the system Location Manager
        androidLocationListener = new LocationListener() {
            public void onLocationChanged(final Location location) {
                // Called when a new location is found by the network location provider.
                updateDistance(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        if (checkLocationPermission()) return;
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000, 5, androidLocationListener);
    }

    private void startGoogleLocationRecording() {
        initLocationRecording();

        if (checkLocationPermission()) return;
        playServiceslocationListener = new com.google.android.gms.location.LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateDistance(location);
            }
        };
        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient,
                LocationRequest
                        .create()
                        .setInterval(4000)
                        .setSmallestDisplacement(5)
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY),
                playServiceslocationListener
        );
    }

    private boolean checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return true;
        }
        return false;
    }

    private void initLocationRecording() {
        lastLocation = null;
        totalDistance = 0;
        currentDistance.setText("0.0");
        stopBtn.setEnabled(true);
        androidBtn.setEnabled(false);
        playServicesBtn.setEnabled(false);
    }

    private void stopGpsRecording() {
        stopBtn.setEnabled(false);
        androidBtn.setEnabled(true);
        playServicesBtn.setEnabled(true);
        String type = "";
        if (androidLocationListener != null) {
            type = "A: ";
            if (checkLocationPermission()) return;
            locationManager.removeUpdates(androidLocationListener);
            androidLocationListener = null;
        }
        if (playServiceslocationListener != null) {
            type = "G: ";
            if (checkLocationPermission()) return;
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, playServiceslocationListener);
            playServiceslocationListener = null;
        }
        measurements.setText(type + totalDistance + "\n" + measurements.getText());
    }

    private void updateDistance(final Location location) {
        if (lastLocation == null) {
            lastLocation = location;
        } else {
            totalDistance += location.distanceTo(lastLocation);
            currentDistance.setText(String.format(Locale.GERMANY, "%.1f", totalDistance));
            lastLocation = location;
        }
    }

    @Override
    public void onConnected(@Nullable final Bundle bundle) {
        Log.d(TAG, "Connected to Google Play Services");
        playServicesBtn.setEnabled(true);
    }

    @Override
    public void onConnectionSuspended(final int i) {
        Log.d(TAG, "Google Play Services connection suspended");
        playServicesBtn.setEnabled(false);
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult connectionResult) {
        Log.w(TAG, "Could not connect to Google Play Services " + connectionResult.getErrorMessage());

        Toast.makeText(MainActivity.this, "Oops no Google Play Services connection", Toast.LENGTH_LONG).show();
    }
}
