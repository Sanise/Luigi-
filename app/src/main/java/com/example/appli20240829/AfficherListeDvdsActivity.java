package com.example.appli20240829;

import android.content.Intent;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class AfficherListeDvdsActivity extends AppCompatActivity {

    private SimpleCursorAdapter adapter;
    private MatrixCursor dvdCursor;
    private Map<String, String> disponibiliteMap = new HashMap<>();
    private Map<Integer, JSONObject> filmDetailsMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_afficherlistedvds);

        Button btnNavigate = findViewById(R.id.btnNavigate);
        btnNavigate.setOnClickListener(v -> {
            Intent intent = new Intent(AfficherListeDvdsActivity.this, PanierActivity.class);
            startActivity(intent);
        });

        // Définition des colonnes pour stocker les films
        String[] columns = new String[]{"_id", "title", "releaseYear", "disponibilite"};
        dvdCursor = new MatrixCursor(columns);

        String[] from = new String[]{"title", "releaseYear", "disponibilite"};
        int[] to = new int[]{R.id.filmName, R.id.filmDate, R.id.filmDisponibilite};

        adapter = new SimpleCursorAdapter(this, R.layout.activity_afficherlisteitemsdvds, dvdCursor, from, to, 0);

        ListView listViewDvds = findViewById(R.id.listView);
        listViewDvds.setAdapter(adapter);
        listViewDvds.setTextFilterEnabled(true);

        // Gestion du clic sur un film
        listViewDvds.setOnItemClickListener((parent, view, position, id) -> {
            dvdCursor.moveToPosition(position);
            int filmId = dvdCursor.getInt(dvdCursor.getColumnIndex("_id"));

            JSONObject filmJson = filmDetailsMap.get(filmId);
            if (filmJson != null) {
                try {
                    Intent intent = new Intent(AfficherListeDvdsActivity.this, FilmDetailsActivity.class);
                    intent.putExtra("filmId", filmId);
                    intent.putExtra("filmTitle", filmJson.getString("title"));
                    intent.putExtra("filmReleaseYear", filmJson.getString("releaseYear"));
                    intent.putExtra("filmDescription", filmJson.optString("description", "Description non disponible"));
                    intent.putExtra("filmRating", filmJson.optString("rating", "Non classé"));
                    intent.putExtra("filmSpecialFeatures", filmJson.optString("specialFeatures", "Aucune information"));

                    startActivity(intent);
                } catch (JSONException e) {
                    Log.e("LISTE_FILMS", "Erreur lors de l'extraction des détails du film", e);
                }
            }
        });

        // Récupérer les données de disponibilité avant de récupérer la liste des films
        new AppelerServiceRestGETDisponibilite().execute(com.btssio.applicationrftg.DonneesPartagees.getURLConnexion() + "/toad/inventory/stockFilm");
    }

    /**
     * Récupère la disponibilité des films
     */
    private class AppelerServiceRestGETDisponibilite extends AsyncTask<String, Void, JSONArray> {

        @Override
        protected JSONArray doInBackground(String... urls) {
            String urlString = urls[0];
            StringBuilder result = new StringBuilder();

            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                return new JSONArray(result.toString());

            } catch (Exception e) {
                Log.e("API_DISPO", "Erreur de connexion ou de lecture : ", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONArray filmsDispo) {
            if (filmsDispo == null) {
                Log.e("API_DISPO", "Erreur : données de disponibilité nulles");
                return;
            }

            try {
                for (int i = 0; i < filmsDispo.length(); i++) {
                    JSONObject film = filmsDispo.getJSONObject(i);
                    String title = film.getString("title");
                    int filmsDisponibles = film.getInt("filmsDisponibles");

                    String disponibilite = (filmsDisponibles > 0) ? "Disponible" : "Indisponible";
                    disponibiliteMap.put(title, disponibilite);
                }

                // Une fois la disponibilité chargée, récupérer la liste des films
                new AppelerServiceRestGETAfficherListeDvdsTask().execute("http://10.0.2.2:8080/toad/film/all");

            } catch (JSONException e) {
                Log.e("API_DISPO", "Erreur de parsing JSON disponibilité : ", e);
            }
        }
    }

    /**
     * Récupère la liste des films
     */
    private class AppelerServiceRestGETAfficherListeDvdsTask extends AsyncTask<String, Void, JSONArray> {

        @Override
        protected JSONArray doInBackground(String... urls) {
            String urlString = urls[0];
            StringBuilder result = new StringBuilder();

            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                return new JSONArray(result.toString());

            } catch (Exception e) {
                Log.e("API_FILMS", "Erreur de connexion ou de lecture : ", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONArray films) {
            if (films == null) {
                Log.e("API_FILMS", "Erreur : la liste films récupérée est nulle");
                return;
            }

            try {
                dvdCursor.close();
                dvdCursor = new MatrixCursor(new String[]{"_id", "title", "releaseYear", "disponibilite"});

                for (int i = 0; i < films.length(); i++) {
                    JSONObject film = films.getJSONObject(i);
                    int filmId = film.getInt("filmId");
                    String title = film.getString("title");
                    String releaseYear = film.optString("releaseYear", "Non disponible");

                    // Stocker les détails du film
                    filmDetailsMap.put(filmId, film);

                    // Récupérer la disponibilité
                    String disponibilite = disponibiliteMap.getOrDefault(title, "Indisponible");

                    dvdCursor.addRow(new Object[]{filmId, title, releaseYear, disponibilite});
                }

                adapter.changeCursor(dvdCursor);

            } catch (JSONException e) {
                Log.e("API_FILMS", "Erreur de parsing du JSON : ", e);
            }
        }
    }
}
