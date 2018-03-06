package ederdev.padilla.backgroundservices;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.location.Geofence;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    private String TAG = MainActivity.class.getSimpleName();
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.fab)FloatingActionButton fab;
    @BindView(R.id.container) ConstraintLayout constraintLayout;
    Intent serviceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        Util.showLog(TAG,"oncreate");
        if ( checkPermission() ) {
            Util.showLog(TAG,"Tiene permiso");
        }
        else askPermission();
    }
    @OnClick(R.id.fab)
    public void showSnackBar(){
        Snackbar.make(constraintLayout, "Service is running "+isMyServiceRunning(BackgroundService.class), Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Util.showLog(TAG,"destroy");
        if (!isMyServiceRunning(BackgroundService.class)){
            Util.showLog(TAG,"service is not running");
            serviceIntent = new Intent(MainActivity.this,BackgroundService.class);
            startService(serviceIntent);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Util.showLog(TAG,"stop");
        if (!isMyServiceRunning(BackgroundService.class)){
            Util.showLog(TAG,"service is not running");
            serviceIntent = new Intent(MainActivity.this,BackgroundService.class);
            startService(serviceIntent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Util.showLog(TAG,"Pause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Util.showLog(TAG,"Resume");
        if (isMyServiceRunning(BackgroundService.class)){
            Util.showLog(TAG,"service is running");
            stopService();
        }else{
            Util.showLog(TAG,"service is not running");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Util.showLog(TAG,"Start");
    }
    public void stopService() {
        stopService(new Intent(getBaseContext(), BackgroundService.class));
    }
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    private final int REQ_PERMISSION = 999;

    private boolean checkPermission() {
        Log.d(TAG, "checkPermission()");
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED );
    }

    private void askPermission() {
        Log.d(TAG, "askPermission()");
        ActivityCompat.requestPermissions(
                this,
                new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                REQ_PERMISSION
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult()");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch ( requestCode ) {
            case REQ_PERMISSION: {
                if ( grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED ){

                } else {
                    permissionsDenied();
                }
                break;
            }
        }
    }

    private void permissionsDenied() {
        Log.w(TAG, "permissionsDenied()");
    }
}
