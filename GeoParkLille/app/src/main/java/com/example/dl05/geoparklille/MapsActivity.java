package com.example.dl05.geoparklille;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    String maj = null;
    String heure = null;
    ArrayList<ArrayList<String>> parkings = null;
    TextView textView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //Récupération des données
        Intent intent = getIntent();
        heure = intent.getStringExtra("datemaj");

        textView = (TextView)findViewById(R.id.heure);
        maj = FormatHeure(heure);
        textView.setText("Dernière mise à jour à " + maj);

        parkings = new ArrayList<ArrayList<String>>();
        parkings = (ArrayList<ArrayList<String>>) intent.getSerializableExtra("parkings");


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    //Formater le timestamp pour l'afficher en bas de la map
    public String FormatHeure(String heure)
    {
        String[] tab = new String[2];
        int j = 0;
        for(String h : heure.split("T"))
        {
            System.out.println(h);
            tab[j]  = h;
            j++;
        }
        heure = tab[1];
        System.out.println(heure);
        tab = new String[2];
        j = 0;
        for(String h : heure.split("\\+"))
        {
            System.out.println(h);
            tab[j] = h;
            j++;
        }
        return tab[0];
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        int compteur = 3;

        for(List<String> parking : parkings)
        {
            String libelle = "", adresse = "";
            Double lat = 0.00, lng = 0.00;
            int i = 0;
            for(String data : parking)
            {
                System.out.println(data);
                if(i == 0)
                {
                    libelle = data;
                }
                else if(i==2)
                {
                    lat = Double.parseDouble(data);
                }
                else if(i==3)
                {
                    lng = Double.parseDouble(data);
                }
                i++;
            }

            String compteurStr = "P-" + compteur;
            String titre = compteurStr + " - " + libelle;

            LatLng position = new LatLng(lat,lng);
            mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(titre)
                    );

            compteur++;
        }

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                //A chaque appui sur un marqueur, on met à jour la liste des parkings - A ETUDIER
                Refresh task = new Refresh();
                task.execute();

                String affichage ="";
                System.out.println(parkings.size());

                for(int i = 3; i<parkings.size()+3; i++)
                {
                    String compteurStr = "P-" + i;
                    System.out.println("Compteur = " +compteurStr);
                    System.out.println("Marqueur = "+marker.getTitle());
                    if(marker.getTitle().startsWith(compteurStr))
                    {
                        for(int j = 0; j<parkings.size(); j++)
                        {
                            if(parkings.get(j).get(0).toString().contains(marker.getTitle().substring(8)))
                            {
                                affichage = "Adresse : " + parkings.get(j).get(1).toString() + "\n" + "Places disponibles : " + parkings.get(j).get(5).toString() + "" +
                                        "\n Places maximum : "  + parkings.get(j).get(6).toString();
                                Toast.makeText(MapsActivity.this, affichage, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
                textView.setText("Dernière mise à jour à " + maj);
                return false;
            }
        });

        LatLng lille = new LatLng(50.633333, 3.066667);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(lille));
        mMap.setMinZoomPreference(15);
    }

    public class Refresh extends AsyncTask<Void, Void, JSONObject>
    {
        InputStream stream = null;
        HttpURLConnection connection = null;

        @Override
        protected JSONObject doInBackground(Void... params) {

            try
            {
                connection = (HttpURLConnection) (new URL("https://opendata.lillemetropole.fr/api/records/1.0/search/?dataset=disponibilite-parkings&facet=libelle&facet=ville&facet=etat&refine.etat=OUVERT&exclude.dispo=0&timezone=Europe%2FParis")).openConnection();
                connection.setRequestMethod("GET");
                connection.setDoInput(true);
                connection.setDoOutput(false);
                connection.connect();

                StringBuffer reponse = new StringBuffer();
                stream = connection.getInputStream();
                String chaine = new String();

                BufferedReader buffer = new BufferedReader(new InputStreamReader(stream));
                String ligne = null;
                while((ligne = buffer.readLine()) != null)
                    chaine+=(ligne + "\r\n");

                JSONObject json = new JSONObject(chaine);
                stream.close();
                connection.disconnect();

                return json;
            }
            catch(Throwable t)
            {
                t.printStackTrace();
                return null;
            }
            finally
            {
                try{ stream.close(); } catch(Throwable t){}
                try{ connection.disconnect(); } catch(Throwable t){}
            }
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);

            int i = 0;
            parkings = new ArrayList<ArrayList<String>>();
            try
            {
                JSONObject records = null;
                JSONObject fields = null;
                String now = "";

                //JSON à récuperer erreur nombre de résultat
                do{
                    records = jsonObject.getJSONArray("records").getJSONObject(i);
                    fields = records.getJSONObject("fields");
                    now = records.getString("record_timestamp");
                    ArrayList<String> parking = new ArrayList<String>();

                    if(fields.getString("ville").equals("Roubaix"))
                    {
                        parking.add(fields.getString("libelle"));
                        parking.add(fields.getString("adresse"));
                        parking.add(String.valueOf(fields.getJSONArray("coordgeo").getDouble(0)));
                        parking.add(String.valueOf(fields.getJSONArray("coordgeo").getDouble(1)));
                        parking.add("Information non disponible");
                        parking.add("Information non disponible");
                        parking.add(String.valueOf(fields.getInt("max")));

                    }
                    else
                    {
                        parking.add(fields.getString("libelle"));
                        parking.add(fields.getString("adresse"));
                        parking.add(String.valueOf(fields.getJSONArray("coordgeo").getDouble(0)));
                        parking.add(String.valueOf(fields.getJSONArray("coordgeo").getDouble(1)));
                        parking.add(fields.getString("etat"));
                        parking.add(String.valueOf(fields.getInt("dispo")));
                        parking.add(String.valueOf(fields.getInt("max")));
                    }

                    parkings.add(parking);
                    i++;
                }while(jsonObject.getJSONArray("records").getJSONObject(i) != null);

                maj = FormatHeure(now);


            }
            catch(Throwable t){}
        }
    }

    public class Distance extends AsyncTask<Double[], Void, JSONObject>
    {
        InputStream stream = null;
        HttpURLConnection connection = null;

        @Override
        protected JSONObject doInBackground(Double[]... params) {

            try
            {
                int i;
                String url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins="+params[0][0]+","+params[0][1]+"&destinations=";
                for(i = 0; i < params[1].length-1; i++)
                {
                    url+=String.valueOf(params[1][i])+","+String.valueOf(params[2][i])+"|";
                }
                url+=String.valueOf(params[1][i+1])+","+String.valueOf(params[2][i+1]);
                url+="&AIzaSyC59DRu1peMbaWybCxz0nmTsnJWKXRASK8";

                System.out.println(url);

                connection = (HttpURLConnection) (new URL(url)).openConnection();
                connection.setRequestMethod("GET");
                connection.setDoInput(true);
                connection.setDoOutput(false);
                connection.connect();

                StringBuffer reponse = new StringBuffer();
                stream = connection.getInputStream();
                String chaine = new String();

                BufferedReader buffer = new BufferedReader(new InputStreamReader(stream));
                String ligne = null;
                while((ligne = buffer.readLine()) != null)
                    chaine+=(ligne + "\r\n");

                JSONObject json = new JSONObject(chaine);
                stream.close();
                connection.disconnect();

                return json;
            }
            catch(Throwable t)
            {
                t.printStackTrace();
                return null;
            }
            finally
            {
                try{ stream.close(); } catch(Throwable t){}
                try{ connection.disconnect(); } catch(Throwable t){}
            }
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);

            int i = 0;
            try
            {
                JSONObject rows = null;
                String distance = null;
                String duration = null;

                do{
                    rows = jsonObject.getJSONArray("rows").getJSONObject(0);
                    distance = rows.getJSONArray("elements").getJSONObject(i).getJSONObject("distance").getString("text");
                    duration = rows.getJSONArray("elements").getJSONObject(i).getJSONObject("duration").getString("text");

                    //parkings.get(i).put("Distance", distance);
                    //parkings.get(i).put("Duration", duration);

                    i++;
                }while(rows.getJSONArray("elements").getJSONObject(i) != null);



            }
            catch(Throwable t){}
        }
    }
}
