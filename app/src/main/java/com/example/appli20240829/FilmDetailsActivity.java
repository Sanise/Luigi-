package com.example.appli20240829;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class FilmDetailsActivity extends AppCompatActivity {

    // Déclaration des composants de l’interface (TextView + bouton)
    private TextView tvMovieTitle, tvMovieDescription, tvMovieReleaseYear, tvMovieRating, tvMovieSpecialFeatures;
    private Button btnAddToCart;
    // Variable pour stocker l’ID du film reçu depuis l’intent
    private int filmId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_film_details); // Liaison avec le layout XML


        // Initialisation des vues
        tvMovieTitle = findViewById(R.id.movie_title);
        tvMovieDescription = findViewById(R.id.movie_description);
        tvMovieReleaseYear = findViewById(R.id.movie_release_year);
        tvMovieRating = findViewById(R.id.movie_rating);
        tvMovieSpecialFeatures = findViewById(R.id.movie_special_features);
        btnAddToCart = findViewById(R.id.btnAddToCart);

        // Récupération de l'ID du film passé en Intent (l'écran précedent)
        filmId = getIntent().getIntExtra("filmId", -1);

        // Si on a bien reçu un ID de film
        if (filmId != -1) {
            detailFilm(filmId); // Récupère les détails du film via l’API
            filmDispo(filmId); // Vérifie s’il est disponible à la location
        } else {
            Log.e("FilmDetailsActivity", "filmId non fourni");
            showToast("Erreur : ID du film manquant");
        }
    }

    //Cette méthode fait un appel API pour récupérer les détails du film
    private void detailFilm(int filmId) {
        // URL pour l’API REST qui renvoie un film par son ID
        String url = com.btssio.applicationrftg.DonneesPartagees.getURLConnexion() + "/toad/film/getById?id=" + filmId;
        Log.d("FilmDetailsActivity", "URL de récupération d'un film : " + url);

        // Création d’une file de requêtes HTTP avec Volley
        RequestQueue queue = Volley.newRequestQueue(this);

        // Création de la requête GET, attente d’un objet JSON
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    Log.d("FilmDetailsActivity", "Réponse JSON film : " + response.toString());
                    updateFilmDetail(response); // Met à jour les vues avec les données
                },
                error -> {
                    Log.e("FilmDetailsActivity", "Erreur lors de la récupération du film", error);
                    showToast("Erreur lors du chargement des détails du film.");
                });

        // Ajout de la requête à la file (elle sera exécutée automatiquement)
        queue.add(request);
    }

    private void updateFilmDetail(JSONObject response) {
        Log.d("FilmDetailsActivity", "Mise à jour des détails du film");
        // Mise à jour des TextView avec les champs du JSON
        tvMovieTitle.setText("Titre : " + response.optString("title", "N/A"));
        tvMovieDescription.setText("Description : " + response.optString("description", "N/A"));
        tvMovieReleaseYear.setText("Année de sortie : " + response.optInt("releaseYear", 0));
        tvMovieRating.setText("Note : " + response.optString("rating", "N/A"));
        tvMovieSpecialFeatures.setText("Fonctionnalités spéciales : " + response.optString("specialFeatures", "N/A"));
    }

    //Cette méthode appelle l’API qui vérifie si le film est disponible, elle prend la réponse JSON du film et affiche les données à l’écran
    private void filmDispo(int filmId) {
        String url = com.btssio.applicationrftg.DonneesPartagees.getURLConnexion() + "/toad/inventory/available/getById?id=" + filmId;
        Log.d("FilmDetailsActivity", "URL de vérification disponibilité : " + url);

        RequestQueue queue = Volley.newRequestQueue(this);
        // Requête GET simple (attend une chaîne de caractères)
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    Log.d("FilmDetailsActivity", "Réponse brute de l'API : " + response);

                    // Si la réponse est vide ou nulle : le film n’est pas disponible
                    if (response == null || response.trim().isEmpty()) {
                        Log.e("FilmDetailsActivity", "Réponse vide → film indisponible");
                        disablePanierButton();
                        return;
                    }

                    try {
                        // On attend un entier (inventoryId) dans la réponse
                        int inventoryId = Integer.parseInt(response.trim());
                        enablePanierButton(inventoryId); // Le film est dispo
                    } catch (NumberFormatException e) {
                        Log.e("FilmDetailsActivity", "Réponse invalide, pas un ID numérique", e);
                        disablePanierButton(); // En cas d’erreur réseau
                    }
                },
                error -> {
                    Log.e("FilmDetailsActivity", "Erreur lors de la vérification de la disponibilité", error);
                    disablePanierButton();
                });

        queue.add(request); // Ajout de la requête dans la file
    }


//Active le bouton et gère l’ajout au panier avec l’inventoryId
    private void enablePanierButton(int inventoryId) {
        Log.d("FilmDetailsActivity", "Le film est disponible (inventoryId = " + inventoryId + ")");
        btnAddToCart.setEnabled(true); // Bouton activé
        btnAddToCart.setAlpha(1.0f);  // Apparence normale
        btnAddToCart.setText("Ajouter au panier");

        // Clic sur le bouton => ajout au panier avec inventoryId et titre
        btnAddToCart.setOnClickListener(v -> {
            String filmTitle = tvMovieTitle.getText().toString().replace("Titre : ", "").trim();
            String entry = inventoryId + "|" + filmTitle; // Format : id|titre

            PanierManager.addToCart(FilmDetailsActivity.this, entry);
            Toast.makeText(FilmDetailsActivity.this, "Ajouté au panier : " + filmTitle, Toast.LENGTH_SHORT).show();
        });
    }


//Désactive le bouton s’il n’y a pas de stock
    private void disablePanierButton() {
        Log.d("FilmDetailsActivity", "Film indisponible, bouton désactivé");
        btnAddToCart.setEnabled(false); // Désactivation fonctionnelle
        btnAddToCart.setAlpha(0.5f);  // Grisé
        btnAddToCart.setText("DVD non dispo");
    }

//Affiche un petit message temporaire à l’écran
    private void showToast(String message) {
        Toast.makeText(FilmDetailsActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}
