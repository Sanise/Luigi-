package com.example.appli20240829;

import android.content.Intent;
import android.content.SharedPreferences;
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
    private int age;
    private SimpleCursorAdapter adapter; // Adapter pour remplir la ListView avec les données des films
    private MatrixCursor dvdCursor; // Curseur temporaire contenant les films affichés
    private Map<String, String> disponibiliteMap = new HashMap<>(); // Map associant un titre à sa disponibilité

    private Map<Integer, JSONObject> filmDetailsMap = new HashMap<>(); // Map associant un ID à l’objet JSON complet du film

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE); //récuperation de ce que contient l'API customer de LoginActivity
        age = sharedPreferences.getInt("age", -1); // récuperer la colonne age depuis l'intent LoginActivity

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

        // Associer les colonnes aux éléments del'interface (activité liste items dvds)
        String[] from = new String[]{"title", "releaseYear", "disponibilite"};
        int[] to = new int[]{R.id.filmName, R.id.filmDate, R.id.filmDisponibilite};

        // Création de l’adapter qui affichera les films dans la ListView
        adapter = new SimpleCursorAdapter(this, R.layout.activity_afficherlisteitemsdvds, dvdCursor, from, to, 0);

        // Initialisation de la ListView
        ListView listViewDvds = findViewById(R.id.listView);
        listViewDvds.setAdapter(adapter); // Liaison avec l’adapter
        listViewDvds.setTextFilterEnabled(true); // Active le filtre texte

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

// Classe interne qui appelle le service REST pour récupérer la disponibilité des films en arrière-plan (AsyncTask)
    private class AppelerServiceRestGETDisponibilite extends AsyncTask<String, Void, JSONArray> {

        @Override
        protected JSONArray doInBackground(String... urls) { // Récupère la première URL passée en paramètre (adresse de l’API)
            String urlString = urls[0]; //récupère l'url
            StringBuilder result = new StringBuilder(); //pour stocker la réponse

            try {
                // Connexion HTTP GET
                URL url = new URL(urlString);// Crée un objet URL à partir de la chaîne de texte
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();//ouvre la connection HTTP à cette URL
                connection.setRequestMethod("GET");  // Spécifie qu'on veut faire une requête de type GET

                // Lire la réponse JSON ligne par ligne
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {  // Boucle pour lire la réponse ligne par ligne
                    result.append(line); //ajoute chaque ligne au résultat final
                }
                reader.close();

                // Convertir le résultat texte en tableau JSON (retourne la réponse)
                return new JSONArray(result.toString());

            } catch (Exception e) {
                Log.e("API_DISPO", "Erreur de connexion ou de lecture : ", e); // Si une erreur survient (connexion, lecture, JSON...), on l'affiche dans les logs
                return null;
            }
        }

    // Exécuté après la récupération des données
        @Override
        protected void onPostExecute(JSONArray filmsDispo) {
            if (filmsDispo == null) {   // Vérifie si la réponse est vide ou null
                Log.e("API_DISPO", "Erreur : données de disponibilité nulles");  // Affiche une erreur dans les logs si les données sont nulles
                return;
            }

            try {
                // Parcours de chaque film pour lire le titre et le nombre dispo (tout les objets du tableau json)
                for (int i = 0; i < filmsDispo.length(); i++) {
                    JSONObject film = filmsDispo.getJSONObject(i);  // Récupère l'objet JSON du film à la position i
                    String title = film.getString("title"); // Récupère le titre du film depuis l'objet JSON
                    int filmsDisponibles = film.getInt("filmsDisponibles");   // Récupère le nombre de films disponibles

                    // Si > 0 : affiche disponible, sinon : indisponible
                    String disponibilite = (filmsDisponibles > 0) ? "Disponible" : "Indisponible";
                    disponibiliteMap.put(title, disponibilite); // Ajoute dans la map le titre du film et sa disponibilité
                }

                String urlFilms;
                if (age<=10) {
                   urlFilms= com.btssio.applicationrftg.DonneesPartagees.getURLConnexion() + "/toad/film/films-enfants";
                }
                else {
                    // Une fois la disponibilité chargée, récupérer la liste des films
                    urlFilms= com.btssio.applicationrftg.DonneesPartagees.getURLConnexion() + "/toad/film/all";
                }
         //       Log.d("AFFICHAGE_FILMS", "URL appelée : " + urlFilms);
                new AppelerServiceRestGETAfficherListeDvdsTask().execute(urlFilms);
            } catch (JSONException e) {
        //        Log.e("API_DISPO", "Erreur de parsing JSON disponibilité : ", e);   // Si une erreur survient pendant la lecture du JSON, on l'affiche dans les logs
            }
        }
    }


    // Classe interne qui appelle le service REST pour afficher la liste complète des films
    private class AppelerServiceRestGETAfficherListeDvdsTask extends AsyncTask<String, Void, JSONArray> {

        //récupération des films
        @Override
        protected JSONArray doInBackground(String... urls) { // Récupère la première URL passée en paramètre (celle de l'API)
            String urlString = urls[0];
            StringBuilder result = new StringBuilder(); // Variable pour stocker la réponse de l'API ligne par ligne

            try {
                URL url = new URL(urlString); // Crée un objet URL à partir de l'adresse
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();  // Ouvre une connexion HTTP vers cette URL
                connection.setRequestMethod("GET");  // Spécifie que la méthode de la requête est GET (lecture uniquement)

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream())); // Crée un lecteur pour lire la réponse ligne par ligne
                String line; // Variable temporaire pour chaque ligne lue
                while ((line = reader.readLine()) != null) { // Lit chaque ligne de la réponse et l'ajoute à la variable "result"
                    result.append(line);
                }
                reader.close();

                return new JSONArray(result.toString()); // Transforme la chaîne de caractères en tableau JSON et la retourne

            } catch (Exception e) {
                Log.e("API_FILMS", "Erreur de connexion ou de lecture : ", e);  // En cas d'erreur (connexion ou parsing), on affiche un message dans les logs
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
