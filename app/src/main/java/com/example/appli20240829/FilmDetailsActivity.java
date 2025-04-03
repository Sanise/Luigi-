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

    private TextView tvMovieTitle, tvMovieDescription, tvMovieReleaseYear, tvMovieRating, tvMovieSpecialFeatures;
    private Button btnAddToCart;
    private int filmId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_film_details);

        // Initialisation des vues
        tvMovieTitle = findViewById(R.id.movie_title);
        tvMovieDescription = findViewById(R.id.movie_description);
        tvMovieReleaseYear = findViewById(R.id.movie_release_year);
        tvMovieRating = findViewById(R.id.movie_rating);
        tvMovieSpecialFeatures = findViewById(R.id.movie_special_features);
        btnAddToCart = findViewById(R.id.btnAddToCart);

        // Récupération de l'ID du film passé en Intent
        filmId = getIntent().getIntExtra("filmId", -1);

        if (filmId != -1) {
            detailFilm(filmId);
            filmDispo(filmId);
        } else {
            Log.e("FilmDetailsActivity", "filmId non fourni");
            showToast("Erreur : ID du film manquant");
        }
    }

    private void detailFilm(int filmId) {
        String url = com.btssio.applicationrftg.DonneesPartagees.getURLConnexion() + "/toad/film/getById?id=" + filmId;
        Log.d("FilmDetailsActivity", "URL de récupération d'un film : " + url);

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    Log.d("FilmDetailsActivity", "Réponse JSON film : " + response.toString());
                    updateFilmDetail(response);
                },
                error -> {
                    Log.e("FilmDetailsActivity", "Erreur lors de la récupération du film", error);
                    showToast("Erreur lors du chargement des détails du film.");
                });

        queue.add(request);
    }

    private void updateFilmDetail(JSONObject response) {
        Log.d("FilmDetailsActivity", "Mise à jour des détails du film");
        tvMovieTitle.setText("Titre : " + response.optString("title", "N/A"));
        tvMovieDescription.setText("Description : " + response.optString("description", "N/A"));
        tvMovieReleaseYear.setText("Année de sortie : " + response.optInt("releaseYear", 0));
        tvMovieRating.setText("Note : " + response.optString("rating", "N/A"));
        tvMovieSpecialFeatures.setText("Fonctionnalités spéciales : " + response.optString("specialFeatures", "N/A"));
    }

    private void filmDispo(int filmId) {
        String url = com.btssio.applicationrftg.DonneesPartagees.getURLConnexion() + "/toad/inventory/available/getById?id=" + filmId;
        Log.d("FilmDetailsActivity", "URL de vérification disponibilité : " + url);

        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    Log.d("FilmDetailsActivity", "Réponse brute de l'API : " + response);

                    if (response == null || response.trim().isEmpty()) {
                        Log.e("FilmDetailsActivity", "Réponse vide → film indisponible");
                        disablePanierButton();
                        return;
                    }

                    try {
                        int inventoryId = Integer.parseInt(response.trim());
                        enablePanierButton(inventoryId); // Le film est dispo ✅
                    } catch (NumberFormatException e) {
                        Log.e("FilmDetailsActivity", "Réponse invalide, pas un ID numérique", e);
                        disablePanierButton();
                    }
                },
                error -> {
                    Log.e("FilmDetailsActivity", "Erreur lors de la vérification de la disponibilité", error);
                    disablePanierButton();
                });

        queue.add(request);
    }



    private void enablePanierButton(int inventoryId) {
        Log.d("FilmDetailsActivity", "Le film est disponible (inventoryId = " + inventoryId + ")");
        btnAddToCart.setEnabled(true);
        btnAddToCart.setAlpha(1.0f);
        btnAddToCart.setText("Ajouter au panier");

        btnAddToCart.setOnClickListener(v -> {
            String filmTitle = tvMovieTitle.getText().toString().replace("Titre : ", "").trim();
            String entry = inventoryId + "|" + filmTitle;

            PanierManager.addToCart(FilmDetailsActivity.this, entry);
            Toast.makeText(FilmDetailsActivity.this, "Ajouté au panier : " + filmTitle, Toast.LENGTH_SHORT).show();
        });
    }



    private void disablePanierButton() {
        Log.d("FilmDetailsActivity", "Film indisponible, bouton désactivé");
        btnAddToCart.setEnabled(false);
        btnAddToCart.setAlpha(0.5f);
        btnAddToCart.setText("DVD non dispo");
    }




    private void showToast(String message) {
        Toast.makeText(FilmDetailsActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}
