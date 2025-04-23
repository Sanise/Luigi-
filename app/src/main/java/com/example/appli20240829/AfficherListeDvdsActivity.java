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


// Activité affichant la liste des films (DVDs)
public class AfficherListeDvdsActivity extends AppCompatActivity {

    private SimpleCursorAdapter adapter; // Adapter pour remplir la ListView avec les données des films
    private MatrixCursor dvdCursor; // Curseur temporaire contenant les films affichés
    private Map<String, String> disponibiliteMap = new HashMap<>(); // Map associant un titre à sa disponibilité

    private Map<Integer, JSONObject> filmDetailsMap = new HashMap<>(); // Map associant un ID à l’objet JSON complet du film

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_afficherlistedvds); // Lie l’activité au layout XML

        // Initialisation du bouton "Panier"
        Button btnNavigate = findViewById(R.id.btnNavigate);
        btnNavigate.setOnClickListener(v -> {
            // Lors du clic, passage à l’activité PanierActivity
            Intent intent = new Intent(AfficherListeDvdsActivity.this, PanierActivity.class);
            startActivity(intent);
        });

        // Définition des colonnes pour stocker les films
        String[] columns = new String[]{"_id", "title", "releaseYear", "disponibilite"};
        dvdCursor = new MatrixCursor(columns);

        // Associer les colonnes aux éléments visuels du layout item (activité liste items dvds)
        String[] from = new String[]{"title", "releaseYear", "disponibilite"};
        int[] to = new int[]{R.id.filmName, R.id.filmDate, R.id.filmDisponibilite};

        // Création de l’adapter qui affichera les films dans la ListView
        adapter = new SimpleCursorAdapter(this, R.layout.activity_afficherlisteitemsdvds, dvdCursor, from, to, 0);

        // Initialisation de la ListView
        ListView listViewDvds = findViewById(R.id.listView);
        listViewDvds.setAdapter(adapter); // Liaison avec l’adapter
        listViewDvds.setTextFilterEnabled(true); // Active le filtre texte (optionnel ici)

        // Gestion du clic sur un film de la liste
        listViewDvds.setOnItemClickListener((parent, view, position, id) -> {
            dvdCursor.moveToPosition(position); // Aller à la position cliquée
            int filmId = dvdCursor.getInt(dvdCursor.getColumnIndex("_id")); // Récupérer l’ID du film

            // Récupérer les détails complets du film
            JSONObject filmJson = filmDetailsMap.get(filmId);
            if (filmJson != null) {
                try {
                    // Créer un Intent avec les infos du film pour FilmDetailsActivity
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

        // lancer l'appel API, Récupérer les données de disponibilité avant de récupérer la liste des films
        new AppelerServiceRestGETDisponibilite().execute(com.btssio.applicationrftg.DonneesPartagees.getURLConnexion() + "/toad/inventory/stockFilm");
    }

// Classe interne qui appelle le service REST pour récupérer la disponibilité des films
    private class AppelerServiceRestGETDisponibilite extends AsyncTask<String, Void, JSONArray> {

        @Override
        protected JSONArray doInBackground(String... urls) {
            String urlString = urls[0];
            StringBuilder result = new StringBuilder();

            try {
                // Connexion HTTP GET
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                // Lire la réponse JSON ligne par ligne
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                // Convertir le résultat texte en tableau JSON
                return new JSONArray(result.toString());

            } catch (Exception e) {
                Log.e("API_DISPO", "Erreur de connexion ou de lecture : ", e);
                return null;
            }
        }

    // Exécuté après la récupération des données
        @Override
        protected void onPostExecute(JSONArray filmsDispo) {
            if (filmsDispo == null) {
                Log.e("API_DISPO", "Erreur : données de disponibilité nulles");
                return;
            }

            try {
                // Parcours de chaque film pour lire le titre et le nombre dispo
                for (int i = 0; i < filmsDispo.length(); i++) {
                    JSONObject film = filmsDispo.getJSONObject(i);
                    String title = film.getString("title");
                    int filmsDisponibles = film.getInt("filmsDisponibles");

                    // Si > 0 : disponible, sinon : indisponible
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

    // Classe interne qui appelle le service REST pour afficher la liste complète des DVDs
    private class AppelerServiceRestGETAfficherListeDvdsTask extends AsyncTask<String, Void, JSONArray> {

        //récupération des films
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

        // Une fois les films récupérés, on les affiche dans le curseur
        @Override
        protected void onPostExecute(JSONArray films) {
            if (films == null) {
                Log.e("API_FILMS", "Erreur : la liste films récupérée est nulle");
                return;
            }

            try {
                // Réinitialisation du curseur
                dvdCursor.close();
                dvdCursor = new MatrixCursor(new String[]{"_id", "title", "releaseYear", "disponibilite"});

                for (int i = 0; i < films.length(); i++) {
                    JSONObject film = films.getJSONObject(i);
                    int filmId = film.getInt("filmId");
                    String title = film.getString("title");
                    String releaseYear = film.optString("releaseYear", "Non disponible");

                    // Stocker (sauvgarder) les détails du film
                    filmDetailsMap.put(filmId, film);

                    // Récupérer la disponibilité (On lit la disponibilité depuis la map créée juste avant)
                    String disponibilite = disponibiliteMap.getOrDefault(title, "Indisponible");

                    // Ajout d’une ligne dans le curseur
                    dvdCursor.addRow(new Object[]{filmId, title, releaseYear, disponibilite});
                }

                // Rafraîchir l’adaptateur avec le nouveau curseur
                adapter.changeCursor(dvdCursor);

            } catch (JSONException e) {
                Log.e("API_FILMS", "Erreur de parsing du JSON : ", e);
            }
        }
    }
}
