package ederdev.padilla.backgroundservices;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Map;

import ederdev.padilla.backgroundservices.geofencing.Constants;
import ederdev.padilla.backgroundservices.geofencing.GeofenceErrorMessages;
import ederdev.padilla.backgroundservices.geofencing.GeofenceTransitionsIntentService;

/**
 * Created by ederpadilla on 05/03/18.
 */

public class BackgroundService extends IntentService  implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, ResultCallback<Status> {
    public static final String TAG = BackgroundService.class.getSimpleName();
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    protected ArrayList<Geofence> mGeofenceList;
    private boolean isRunning = false;

    /** indicates how to behave if the service is killed */
    int mStartMode;

    /** interface for clients that bind */
    IBinder mBinder;

    /** indicates whether onRebind should be used */
    boolean mAllowRebind;

    public BackgroundService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Util.showLog(TAG,"on create service");

        // Empty list for storing geofences.
        mGeofenceList = new ArrayList<Geofence>();

        // Get the geofences used. Geofence data is hard coded in this sample.
        populateGeofenceList();

        // Kick off the request to build GoogleApiClient.
        buildGoogleApiClient();
    }


    protected synchronized void  buildGoogleApiClient() {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();


    }

    @Override
    protected void onHandleIntent(@Nullable Intent workIntent) {
        // Gets data from the incoming Intent
        String dataString = workIntent.getDataString();
        Util.showLog(TAG,dataString);
    }
    /** The service is starting, due to a call to startService() */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Util.showLog(TAG, "on start command");
        mGoogleApiClient.connect();
        if (mGoogleApiClient.isConnected()){
            try {
                Log.e(TAG, "Entra al try");
                LocationServices.GeofencingApi.addGeofences(
                        mGoogleApiClient,
                        // The GeofenceRequest object.
                        getGeofencingRequest(),
                        // A pending intent that that is reused when calling removeGeofences(). This
                        // pending intent is used to generate an intent when a matched geofence
                        // transition is observed.
                        getGeofencePendingIntent()
                ).setResultCallback(this); // Result processed in onResult().
            } catch (SecurityException securityException) {

                Log.e(TAG, "Entra al catch");
                // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
                securityException.printStackTrace();
            }
        }else{
            Util.showLog(TAG,"google api client not connected "+mGoogleApiClient.isConnected());
        }
        return mStartMode;
    }
    private void populateGeofenceList() {
        for (Map.Entry<String, LatLng> entry : Constants.BAY_AREA_LANDMARKS.entrySet()) {

            mGeofenceList.add(new Geofence.Builder()
                    // Set the request ID of the geofence. This is a string to identify this
                    // geofence.
                    .setRequestId(entry.getKey())

                    // Set the circular region of this geofence.
                    .setCircularRegion(
                            entry.getValue().latitude,
                            entry.getValue().longitude,
                            Constants.GEOFENCE_RADIUS_IN_METERS
                    )

                    // Set the expiration duration of the geofence. This geofence gets automatically
                    // removed after this period of time.
                    .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)

                    // Set the transition types of interest. Alerts are only generated for these
                    // transition. We track entry and exit transitions in this sample.
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_EXIT)

                    // Create the geofence.
                    .build());
        }
    }

    /** A client is binding to the service with bindService() */
    @Override
    public IBinder onBind(Intent intent) {
        Util.showLog(TAG,"ON BIND");
        return mBinder;
    }

    /** Called when all clients have unbound with unbindService() */
    @Override
    public boolean onUnbind(Intent intent) {
        Util.showLog(TAG,"onunbind");
        return mAllowRebind;
    }

    /** Called when a client is binding to the service with bindService()*/
    @Override
    public void onRebind(Intent intent) {
        Util.showLog(TAG,"onRebind");
    }

    /** Called when The service is no longer used and is being destroyed */
    @Override
    public void onDestroy() {
        Util.showLog(TAG,"ondestroy");
        if (mGoogleApiClient.isConnecting() || mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }
    private PendingIntent getGeofencePendingIntent() {
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling addgeoFences()
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Util.showLog(TAG,"Error "+i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Util.showLog(TAG,"Error "+connectionResult.getErrorMessage()+" "+connectionResult.toString());
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.e(TAG,location.toString());
        Util.showLog(TAG," google apli client "+mGoogleApiClient.isConnected());
        if (mGoogleApiClient.isConnected()){
            if (!isRunning){
                Util.showLog(TAG,"aun no esta corriendo "+isRunning);
                try {
                    Log.e(TAG, "Entra al try");
                    LocationServices.GeofencingApi.addGeofences(
                            mGoogleApiClient,
                            // The GeofenceRequest object.
                            getGeofencingRequest(),
                            // A pending intent that that is reused when calling removeGeofences(). This
                            // pending intent is used to generate an intent when a matched geofence
                            // transition is observed.
                            getGeofencePendingIntent()
                    ).setResultCallback(this); // Result processed in onResult().
                } catch (SecurityException securityException) {

                    Log.e(TAG, "Entra al catch");
                    // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
                    securityException.printStackTrace();
                }
                isRunning = true;
            }else{
                Util.showLog(TAG,"not running "+isRunning);
            }
        }
    }
    public void onResult(Status status) {
        Log.e(TAG,"Enrtra a onresult "+status.toString());
        if (status.isSuccess()) {
            Toast.makeText(
                    this,
                    "Geofences Added",
                    Toast.LENGTH_SHORT
            ).show();
            Util.showLog(TAG,"Si agrega los geofences");
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    status.getStatusCode());
            Toast.makeText(
                    this,
                    errorMessage,
                    Toast.LENGTH_SHORT
            ).show();
            Util.showLog(TAG,errorMessage);
        }
    }
}
