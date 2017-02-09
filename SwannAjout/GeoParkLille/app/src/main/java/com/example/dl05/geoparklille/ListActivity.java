package com.example.dl05.geoparklille;

import android.content.Intent;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.model.LatLng;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class ListActivity extends AppCompatActivity {
    Double lat = null;
    Double lng = null;
    private Location pos = null;
    private LatLng position = null;

    double[] lattitudes = null;
    double[] longitudes = null;
    double[] coordGPS = null;

    HttpURLConnection con = null;
    InputStream is = null;
    //Url de l'API
    String url = "https://opendata.lillemetropole.fr/api/records/1.0/search/?dataset=disponibilite-parkings&facet=libelle&facet=ville&facet=etat&refine.etat=OUVERT&exclude.dispo=0&timezone=Europe%2FParis&rows=24";

    ArrayList<HashMap<String, String>> parking = new ArrayList<>();

    Double myLat = 0.00;
    Double myLng = 0.00;
    HashMap<String, String> donnees = new HashMap<>();
    HttpURLConnection connection = null;
    InputStream stream = null;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_activity);


        Intent intent = getIntent();
        coordGPS = intent.getDoubleArrayExtra("coordgeo");

        //On instancie la class AsyncTask => Tache asynchrone
        AsyncTask reqHttp = new AsyncTask();
        reqHttp.execute();//On lance la tâche


        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    //AsyncTask pour utilisation de l'API open-data MEL
    private class AsyncTask extends android.os.AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                //Permet de récupérer les requêtes HTTP
                con = (HttpURLConnection) (new URL(url)).openConnection();
                con.setRequestMethod("GET");
                con.setDoInput(true);
                con.setDoOutput(false);
                con.connect();

                StringBuffer buffer = new StringBuffer();

                is = con.getInputStream();//Flux de donnée

                BufferedReader br = new BufferedReader(new InputStreamReader(is));//Récupére en brut
                String line = null;
                while ((line = br.readLine()) != null)
                    buffer.append(line + "\r\n");

                is.close();
                con.disconnect();
                return buffer.toString();

            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (Throwable t) {
                }
                try {
                    con.disconnect();
                } catch (Throwable t) {
                }
            }
            return null;
        }

        //String result Récupére le résultat de DoInBackground
        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject jsonObject = new JSONObject(result);//Typage de string result en Objet Json

                JSONArray parkings = jsonObject.getJSONArray("records");

                lattitudes = new double[parkings.length()];
                longitudes = new double[parkings.length()];

                for (int i = 0; i < parkings.length(); i++) {

                    JSONObject c = parkings.getJSONObject(i);
                    JSONObject fields = c.getJSONObject("fields");
                    JSONArray coordgeo = fields.getJSONArray("coordgeo");

                    lat = coordgeo.getDouble(0);
                    lng = coordgeo.getDouble(1);

                    HashMap<String, String> champ = new HashMap<>();

                    String adresse = "Adresse: " + fields.getString("adresse");
                    String ville = "Ville: " + fields.getString("ville");
                    String name = "Parking: " + fields.getString("libelle");
                    int place_dispo = fields.getInt("dispo");
                    int place_max = fields.getInt("max");
                    String place_dispo_string = "Nombre de places disponibles : ";
                    String place_max_string = "Nombre de places maximum : "+ String.valueOf(place_max);

                    String etat = null;

                    if (ville.equals("Roubaix")) {
                        etat = "Non disponible";
                        place_dispo_string += "Information non disponible";
                    } else {
                        etat = fields.getString("etat");
                        place_dispo_string += String.valueOf(place_dispo);
                    }

                    //Ajouts des éléments dans le HashMap
                    champ.put("Libelle", name);
                    champ.put("Adresse", adresse);
                    champ.put("Etat", etat);
                    champ.put("Ville", ville);
                    champ.put("Dispo", place_dispo_string);
                    champ.put("Max", place_max_string);
                    champ.put("Lattitude", lat.toString());
                    champ.put("Longitude", lng.toString());

                    lattitudes[i] = lat;
                    longitudes[i] = lng;

                    parking.add(champ);

                }
                //Coordonnées utilisateur à récupérer via intent

                GetDistance reqHttp2 = new GetDistance();
                reqHttp2.execute(coordGPS, lattitudes, longitudes);


            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public class GetDistance extends android.os.AsyncTask<double[], Void, JSONObject> {
        InputStream stream = null;
        HttpURLConnection connection = null;

        @Override
        protected JSONObject doInBackground(double[]... params) {

            try {
                int i;
                String url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" + params[0][0] + "," + params[0][1] + "&destinations=";
                System.out.println(url);
                for (i = 0; i < params[1].length; i++) {
                    System.out.println(url);
                    url += String.valueOf(params[1][i]) + "," + String.valueOf(params[2][i]) + "|";
                }
                url += "&AIzaSyC59DRu1peMbaWybCxz0nmTsnJWKXRASK8";
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
                while ((ligne = buffer.readLine()) != null)
                    chaine += (ligne + "\r\n");

                JSONObject json = new JSONObject(chaine);
                System.out.println(json.toString());
                stream.close();
                connection.disconnect();

                return json;
            } catch (Throwable t) {
                t.printStackTrace();
                return null;
            } finally {
                try {
                    stream.close();
                } catch (Throwable t) {
                }
                try {
                    connection.disconnect();
                } catch (Throwable t) {
                }
            }
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);

            int i = 0;
            try {
                JSONObject rows = null;
                do {
                    String distance = null;
                    String duration = null;
                    String valeur = null;

                    rows = jsonObject.getJSONArray("rows").getJSONObject(0);
                    distance = rows.getJSONArray("elements").getJSONObject(i).getJSONObject("distance").getString("text");
                    duration = rows.getJSONArray("elements").getJSONObject(i).getJSONObject("duration").getString("text");
                    valeur = rows.getJSONArray("elements").getJSONObject(i).getJSONObject("duration").getString("value");

                    parking.get(i).put("Distance", distance);
                    parking.get(i).put("Duration", duration);
                    parking.get(i).put("ValeurDuree", valeur.toString());
                    System.out.println("Arraylist distance : " + parking.get(i).get("ValeurDuree"));

                    i++;
                } while (i < rows.getJSONArray("elements").length());


                // And then sort it using collections.sort().
                Collections.sort(parking, distanceComparator);

                for (HashMap<String, String> hm : parking) {
                    System.out.println(hm.get("Libelle") + " " + hm.get("ValeurDuree"));
                }

                ListAdapter adapter = new SimpleAdapter(ListActivity.this, parking, R.layout.item_list, new String[]{"Libelle", "Adresse", "Ville", "Dispo", "Max", "Distance", "Duration"}, new int[]{R.id.libelle, R.id.adresse, R.id.ville, R.id.place_dispo, R.id.place_max, R.id.distance, R.id.duree});
                final ListView listView = (ListView) findViewById(R.id.listView);
                listView.setAdapter(adapter);

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                        HashMap<String, String> map = (HashMap<String, String>) listView.getItemAtPosition(position);
                        Intent intent = new Intent(ListActivity.this, MapsActivityList.class);
                        intent.putExtra("parking", map);
                        startActivity(intent);
                    }
                });

            } catch (Throwable t) {
            }
        }
    }

    Comparator<HashMap<String, String>> distanceComparator = new Comparator<HashMap<String, String>>() {

        @Override
        public int compare(HashMap<String, String> o1, HashMap<String, String> o2) {
            // Get the distance and compare the distance.
            Integer distance1 = Integer.parseInt(o1.get("ValeurDuree"));
            Integer distance2 = Integer.parseInt(o2.get("ValeurDuree"));

            return distance1.compareTo(distance2);
        }
    };
}