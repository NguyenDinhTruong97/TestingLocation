package com.example.test;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.google.android.gms.common.util.Strings;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by deepshikha on 24/11/16.
 */

public class GoogleService extends Service implements LocationListener {

    boolean isGPSEnable = false;
    boolean isNetworkEnable = false;
    boolean isServiceContinue = true;
    double latitude, longitude;
    LocationManager locationManager;
    Location location;
    private Handler mHandler = new Handler();
    private Timer mTimer = null;
    long notify_interval = 5000;
    public static String str_receiver = "servicetutorial.service.receiver";
    Intent intent;
    Geocoder geocoder;
    BroadcastReceiver broadcastReceiver;

    public GoogleService() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return START_REDELIVER_INTENT;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mTimer = new Timer();

        mTimer.schedule(new TimerTaskToGetLocation(), 5, notify_interval);
        intent = new Intent(str_receiver);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (Objects.requireNonNull(action).equals(AppConstants.BROADCAST_KEY_INTENT)) {
                    if(intent.getStringExtra(AppConstants.STOP_SERVICE).equals(AppConstants.STOP_VALUE)){
                        stopService();
                    }
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AppConstants.BROADCAST_KEY_INTENT);
        registerReceiver(broadcastReceiver, intentFilter);

        startForeground(1,createNotification());
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if(isServiceContinue) {
            Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
            restartServiceIntent.setPackage(getPackageName());

            PendingIntent restartServicePendingIntent = PendingIntent.getService(getApplicationContext(), 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
            AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            alarmService.set(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime(),
                    restartServicePendingIntent);
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    private void fn_getlocation() {
        locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        isGPSEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnable = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        geocoder = new Geocoder(this, Locale.getDefault());

        if (!isGPSEnable && !isNetworkEnable) {

        } else {

            if (isNetworkEnable) {
                location = null;
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, this);
                if (locationManager!=null){
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (location!=null){

                        Log.e("latitude",location.getLatitude()+"");
                        Log.e("longitude",location.getLongitude()+"");

                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        String city = "";
                        try {
                            city = geocoder.getFromLocation(latitude, longitude, 1).get(0).getAdminArea();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        Toast.makeText(this, "latitude " + location.getLatitude() +  "longitude " + location.getLongitude() + " " +city , Toast.LENGTH_SHORT).show();
//                        sendDataToServer();
//                        fn_update(location);
                    }
                }

            }


            if (isGPSEnable){
                location = null;
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this);
                if (locationManager!=null){
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location!=null){
                        Log.e("latitude",location.getLatitude()+"");
                        Log.e("longitude",location.getLongitude()+"");
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
//                        sendDataToServer();
//                        fn_update(location);
                    }
                }
            }


        }
    }

    private class TimerTaskToGetLocation extends TimerTask{
        @Override
        public void run() {
            mHandler.post(() -> fn_getlocation());
        }
    }

    private Notification createNotification(){
        String CHANNEL_ID = "ENDLESS SERVICE";
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,"Endless Service notifications channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Endless Service channel");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);


           notification= new Notification.Builder(getApplicationContext(),CHANNEL_ID)
                    .setContentText("Check location")
                    .setContentTitle("latitude longitude")
                    .setContentIntent(pendingIntent)
                    .setChannelId(CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.sym_action_chat)
                    .build();

            notificationManager.createNotificationChannel(channel);
            return notification;
//            notificationManager.notify(1,notification);
        }else{
           return new Notification.Builder(this)
                   .setContentTitle("Endless Service")
                    .setContentText("This is your favorite endless service working")
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(android.R.drawable.sym_action_chat)
                    .setTicker("Ticker text")
                    .build();
        }

    }

    private void sendDataToServer() {
        AndroidNetworking.post("Server api")
                .setPriority(Priority.MEDIUM)
                .addBodyParameter("latitude", latitude+"")
                .addBodyParameter("logitude", longitude+"")
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                    }
                    @Override
                    public void onError(ANError anError) {
                        System.out.println("Error " + anError);
                    }
                });
    }

    private void stopService(){
        stopForeground(true);
        stopSelf();
        mTimer.cancel();
        isServiceContinue = false;
    }


//    private void fn_update(Location location){
//
//        intent.putExtra("latutide",location.getLatitude()+"");
//        intent.putExtra("longitude",location.getLongitude()+"");
//        sendBroadcast(intent);
//    }


}