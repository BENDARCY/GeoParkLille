package com.example.dl05.geoparklille;

import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LocationManager locationManager = null;
    private Location pos = null;
    private LatLng position = null;
    Button btnListeParkings = null;
    Button btnCarteParkings = null;
    Boolean gps_enabled = null;
    double[] coordGPS = null;

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public void setProgressBar(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    public ProgressBar progressBar = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnListeParkings = (Button)findViewById(R.id.button);
        btnCarteParkings = (Button)findViewById(R.id.button2);
        progressBar =  (ProgressBar)findViewById(R.id.progressBar);

        //Gestion de la géolocalisation - à vérifier
        locationManager = (LocationManager) getSystemService(getApplicationContext().LOCATION_SERVICE);

        try {
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        if(!gps_enabled)
        {
            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }

        ArrayList<LocationProvider> providers = new ArrayList<LocationProvider>();
        List<String> names = locationManager.getProviders(true);

        for (String name : names) {
            providers.add(locationManager.getProvider(name));
        }

        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == 0) {
            pos = locationManager.getLastKnownLocation(providers.get(0).getName());
        }

        //Récupération des coordonnées lat,lng de l'appareil utilisateur
        if (pos != null) {
            coordGPS = new double[2];
            position = new LatLng(pos.getLatitude(), pos.getLongitude());
            coordGPS[0] = pos.getLatitude();
            coordGPS[1] = pos.getLongitude();
        }
        else
        {
            coordGPS = new double[2];
            coordGPS[0] = 50.3333;
            coordGPS[1] = 3.7777;
        }

        //Gestion des deux boutons du menu principal
        btnListeParkings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Appel d'un thread en fonction d'une url générée en fonction des coordonnées utilisateur
                System.out.println(coordGPS[0]);
                Intent intent = new Intent(MainActivity.this, ListActivity.class);
                intent.putExtra("coordgeo", coordGPS);
                startActivity(intent);
            }
        });

        btnCarteParkings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GetAllParkings thread = new GetAllParkings(getApplicationContext(), MainActivity.this);
                thread.execute();
            }
        });

    }
}
