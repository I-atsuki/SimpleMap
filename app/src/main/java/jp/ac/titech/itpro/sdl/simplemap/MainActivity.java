package jp.ac.titech.itpro.sdl.simplemap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private final static String TAG = "MainActivity";

    private TextView infoView;
    private GoogleMap googleMap;
    private GoogleApiClient googleApiClient;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private enum UpdatingState {STOPPED, REQUESTING, STARTED}

    private UpdatingState state = UpdatingState.STOPPED;

    private final static String[] PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private final static int REQCODE_PERMISSIONS = 1234;
    private boolean mflag = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        infoView = findViewById(R.id.info_view);
        MapFragment mapFragment =
                (MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onResume");
                    startLocationUpdate(true);
                }
        });

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = new LocationRequest();
        //locationRequest.setInterval(10000L);
        //locationRequest.setFastestInterval(5000L);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.d(TAG, "onLocationResult");
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    infoView.setText(getString(R.string.latlng_format,
                            latLng.latitude, latLng.longitude));
                    if (googleMap != null)
                        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

                }
            }
        };


    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        googleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (state != UpdatingState.STARTED && googleApiClient.isConnected())
            startLocationUpdate(true);
        else
            state = UpdatingState.REQUESTING;
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        if (state == UpdatingState.STARTED)
            stopLocationUpdate();
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        googleApiClient.disconnect();
        super.onStop();
    }

    private Marker mk1;
    private Marker mk2;

    @Override
    public void onMapReady(GoogleMap map) {
            Log.d(TAG, "onMapReady");
            map.moveCamera(CameraUpdateFactory.zoomTo(15f));
            googleMap = map;
        googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng longpushLocation) {
                if(mk1 != null) {
                    mk1.remove();
                }
                if(mk2 != null){
                    mk1 = mk2;
                }
                LatLng newlocation = new LatLng(longpushLocation.latitude, longpushLocation.longitude);
                mk2 = googleMap.addMarker(new MarkerOptions().position(newlocation).title(""+longpushLocation.latitude+" :"+ longpushLocation.longitude));
                //googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newlocation, 14));
                if(mk1 != null){
                    LatLng po1 = mk1.getPosition();
                    LatLng po2 = mk2.getPosition();
                    float[] re = new float[1];
                    Location.distanceBetween(po1.latitude,po1.longitude,po2.latitude,po2.longitude,re);
                    infoView.setText("距離は"+(int)re[0]+"mです");
                }
            }

        });


    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected");
        if (state == UpdatingState.REQUESTING)
            startLocationUpdate(true);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed");
    }

    private void startLocationUpdate(boolean reqPermission) {
        Log.d(TAG, "startLocationUpdate");
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                if (reqPermission)
                    ActivityCompat.requestPermissions(this, PERMISSIONS, REQCODE_PERMISSIONS);
                else
                    Toast.makeText(this,
                            getString(R.string.toast_requires_permission_format, permission),
                            Toast.LENGTH_SHORT).show();
                return;
            }
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        state = UpdatingState.STARTED;
    }

    @Override
    public void onRequestPermissionsResult(int reqCode,
                                           @NonNull String[] permissions, @NonNull int[] grants) {
        Log.d(TAG, "onRequestPermissionsResult");
        switch (reqCode) {
        case REQCODE_PERMISSIONS:
            startLocationUpdate(false);
            break;
        }
    }

    private void stopLocationUpdate() {
        Log.d(TAG, "stopLocationUpdate");
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        state = UpdatingState.STOPPED;
    }
}
