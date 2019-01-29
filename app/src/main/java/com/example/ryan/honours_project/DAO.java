package com.example.ryan.honours_project;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


public class DAO {
    private String urlAddress = "https://api.openrouteservice.org/";
    private String apiKey = "58d904a497c67e00015b45fcc64a2bb86b5e4b428299a5d1cbb9b2a0";
    RequestQueue queue;


    public DAO(Context context){
        this.queue = Volley.newRequestQueue(context);

    }

    public void getGeoCode(double[] start, String input, final Context context) throws IOException, JSONException {
        input = input + "," + String.valueOf(start[0]) + "," + String.valueOf(start[1]);
        String url = urlBuilder(input, "geocode");
        final String[] geocode = new String[10];

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject output = new JSONObject(response);

                            JSONArray arr = output.getJSONArray("features");
                            for(int i = 0; i < arr.length(); i++){
                                geocode[i] = arr.getJSONObject(i).getJSONObject("properties").getString("label")+ ";" + arr.getJSONObject(i).getJSONObject("properties").getString("distance") + ";" + arr.getJSONObject(i).getJSONObject("geometry").getJSONArray("coordinates").getDouble(1) +"," + arr.getJSONObject(i).getJSONObject("geometry").getJSONArray("coordinates").getDouble(0) ;
                                System.out.println(geocode[i]);

                            }
                            MainActivity ma = (MainActivity)context;
                            ma.setUpList(geocode);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("That didn't work!");
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);


    }

    public void getRoute(double[] start, double[] destination, final Context context){

        String input = String.valueOf(start[0]) + "," + String.valueOf(start[1]) + "|" + String.valueOf(destination[1]) + "," + String.valueOf(destination[0]);
        String url = urlBuilder(input, "directions");

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        MainActivity ma = (MainActivity)context;
                        ma.setUpKML(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("That didn't work!");
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);

    }


    public void handleError(){

    }

    public String getUrlAddress(){
        return urlAddress;
    }

    public String getApiKey(){
        return apiKey;
    }

    public String urlBuilder(String input, String apiCall){
        String url = "";
        switch(apiCall){
           case "geocode":
               String[] info =  input.split(",");
               url =  getUrlAddress() + "geocode/search?api_key=" + getApiKey() + "&text=" + info[0] + "&focus.point.lon=" +info[1] +"&focus.point.lat=" + info[2] + "&boundary.country=GBR";
               break;
           case "directions":
               url =  getUrlAddress() + "directions?api_key=" + getApiKey() + "&coordinates=" + input + "&profile=foot-walking&format=geojson&units=m";
               break;
       }
       return url;
    }


}
