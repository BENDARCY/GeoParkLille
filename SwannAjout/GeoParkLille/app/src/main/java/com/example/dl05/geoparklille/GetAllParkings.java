package com.example.dl05.geoparklille;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

/**
 * Created by DL05 on 01/02/2017.
 */

public class GetAllParkings extends AsyncTask<Void, Void, JSONObject> {

    private Context context;
    WeakReference<Activity> activity;
    InputStream stream = null;
    HttpURLConnection connection = null;
    ProgressBar progressBar = null;
    TextView textView = null;
    String heure = null;


    public GetAllParkings(Context context, Activity activity){
        this.context= context;
        this.activity = new WeakReference<Activity>(activity);
    }

    @Override
    protected void onPreExecute()
    {
        Activity act = activity.get();
        if(act != null && act.getClass() == MainActivity.class)
        {
            progressBar = (ProgressBar)act.findViewById(R.id.progressBar);
            textView = (TextView)act.findViewById(R.id.textView2);
            textView.setVisibility(VISIBLE);
            progressBar.setVisibility(VISIBLE);
            progressBar.incrementProgressBy(30);
        }
        super.onPreExecute();
    }

    @Override
    protected JSONObject doInBackground(Void... params) {

        try
        {
            connection = (HttpURLConnection) (new URL("https://opendata.lillemetropole.fr/api/records/1.0/search/?dataset=disponibilite-parkings&facet=libelle&facet=ville&facet=etat&refine.etat=OUVERT&exclude.dispo=0&timezone=Europe%2FParis&rows=24")).openConnection();
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
        ArrayList<ArrayList<String>> parkings = new ArrayList<ArrayList<String>>();
        try
        {
            JSONObject records = null;
            JSONObject fields = null;

            //JSON à récuperer erreur nombre de résultat
            do{
                records = jsonObject.getJSONArray("records").getJSONObject(i);
                fields = records.getJSONObject("fields");
                heure = records.getString("record_timestamp");
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

        }
        catch(Throwable t){}


        Activity act = activity.get();
        if(act != null && act.getClass() == MainActivity.class) {
            progressBar.incrementProgressBy(30);
            textView.setVisibility(INVISIBLE);
            progressBar.setVisibility(INVISIBLE);
        }

        //Passage à l'activité suivante en envoyant la liste des parkings
        Intent intent = new Intent(context, MapsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("parkings", parkings);
        intent.putExtra("datemaj", heure);
        context.startActivity(intent);


    }
}
