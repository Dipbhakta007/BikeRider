package com.example.user.bikerider;

import android.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private static final int MY_PERMISSION_REQUEST_CODE=7000;
    private static  final int PLAY_SERVICES_RES_REQUEST=7001;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    LatLng pickupLocation;

    private static int UPDATE_INTERVAL=1000;
    private static int FASTEST_INTERVAL=1000;
    private  int DISPLACEMENT=10;
    Marker mCurrent,mDriver;

    SupportMapFragment mapFragment;
    DatabaseReference ref;
    GeoFire geFire;
    private Button mLogout, mRequest;






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mLogout=(Button)findViewById(R.id.logout);
        mRequest=(Button)findViewById(R.id.request);


        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent=new Intent(CustomerMapActivity.this,MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        ref=FirebaseDatabase.getInstance().getReference("customerRequests");
        geFire=new GeoFire(ref);


        startLocationUpdates();
        displayLocation();

        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Toast.makeText(CustomerMapActivity.this, "Ashche", Toast.LENGTH_SHORT).show();
                displayLocation();
                final double lattitude=mLastLocation.getLatitude();
                final double longitude=mLastLocation.getLongitude();

                pickupLocation=new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude());

                geFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(lattitude, longitude), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {

                        Toast.makeText(CustomerMapActivity.this, "Getting your driver", Toast.LENGTH_SHORT).show();

                    }
                });


                mRequest.setText("Getting Your Driver...");

            }
        });





        setUpLocation();


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case MY_PERMISSION_REQUEST_CODE:
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    buildGoogleApiClient();
                    createLocationRequest();
                    displayLocation();
                }
        }

    }

    private void setUpLocation()
    {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,new String[]{
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
            },MY_PERMISSION_REQUEST_CODE);
        }
        else{
            if(checkPlayServices()){
                buildGoogleApiClient();
                createLocationRequest();
                displayLocation();

            }
        }

    }

    private void createLocationRequest(){
        mLocationRequest=new LocationRequest();

        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);


    }

    private  void buildGoogleApiClient(){
        mGoogleApiClient=new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }


    private boolean checkPlayServices(){
        int resultCode= GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(resultCode!=ConnectionResult.SUCCESS){
            if(GooglePlayServicesUtil.isUserRecoverableError(resultCode)){
                GooglePlayServicesUtil.getErrorDialog(resultCode,this,PLAY_SERVICES_RES_REQUEST).show();
            }
            else{
                Toast.makeText(this,"This device is not compatible",Toast.LENGTH_SHORT).show();
                finish();

            }
            return false;
        }
        return  true;

    }
    private  void displayLocation(){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


            return;
        }
        mLastLocation=LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(mLastLocation!=null){


            final double lattitude=mLastLocation.getLatitude();
            final double longitude=mLastLocation.getLongitude();



            if(mCurrent!=null)mCurrent.remove();
            mCurrent=mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.customer)).position(new LatLng(lattitude,longitude)).title("You"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lattitude,longitude)));

            mMap.animateCamera(CameraUpdateFactory.zoomTo(17));





            //Toast.makeText(this,"hyese",Toast.LENGTH_SHORT).show();
        }
        else{



        }

    }

    private void startLocationUpdates(){

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation=location;
        displayLocation();
      /*  String user_id= FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("driversAvailable");
        GeoFire geoFire=new GeoFire(ref);

        geoFire.setLocation(user_id,new GeoLocation(location.getLatitude(),location.getLongitude()));*/



    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();;
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onStop() {
        super.onStop();


        // geoFire.removeLocation(FirebaseAuth.getInstance().getCurrentUser().getUid());

    }
}