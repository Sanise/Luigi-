package com.example.appli20240829;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class FilmDetailsActivity extends AppCompatActivity {

    private TextView tvMovieTitle, tvMovieDescription, tvMovieReleaseYear, tvMovieRating, tvMovieSpecialFeatures;
    private Button btnAddToCart;
    private String filmTitle;
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

        // Récupération des données passées via l'Intent
        Intent intent = getIntent();
        if (intent != null) {
            filmId = intent.getIntExtra("filmId", -1);
            filmTitle = intent.getStringExtra("filmTitle");

            String filmDescription = intent.getStringExtra("filmDescription");
            String filmReleaseYear = intent.getStringExtra("filmReleaseYear");
            String filmRating = intent.getStringExtra("filmRating");
            String filmSpecialFeatures = intent.getStringExtra("filmSpecialFeatures");

            if (filmTitle == null) {
                filmTitle = "Titre inconnu";
            }

            // Affichage des données dans les TextViews
            tvMovieTitle.setText("Titre : " + filmTitle);
            tvMovieDescription.setText("Description : " + (filmDescription != null ? filmDescription : "Non disponible"));
            tvMovieReleaseYear.setText("Année de sortie : " + (filmReleaseYear != null ? filmReleaseYear : "Non disponible"));
            tvMovieRating.setText("Classification : " + (filmRating != null ? filmRating : "Non classé"));
            tvMovieSpecialFeatures.setText("Fonctionnalités spéciales : " + (filmSpecialFeatures != null ? filmSpecialFeatures : "Aucune information"));

            Log.d("FilmDetailsActivity", "Film chargé : " + filmTitle);
        }

        // Gestion du clic sur "Ajouter au panier"
        btnAddToCart.setOnClickListener(v -> {
            if (filmTitle != null && !filmTitle.isEmpty()) {
                PanierManager.addToCart(FilmDetailsActivity.this, filmTitle);
                Toast.makeText(FilmDetailsActivity.this, filmTitle + " ajouté au panier", Toast.LENGTH_SHORT).show();

                // Affichage d'une boîte de dialogue pour le choix utilisateur
                new android.app.AlertDialog.Builder(FilmDetailsActivity.this)
                        .setMessage("Film ajouté au panier. Que voulez-vous faire ?")
                        .setPositiveButton("Voir le panier", (dialog, which) -> {
                            Intent panierIntent = new Intent(FilmDetailsActivity.this, PanierActivity.class);
                            startActivity(panierIntent);
                            finish(); // Fermer cette activité pour éviter l'empilement
                        })
                        .setNegativeButton("Continuer mes recherches", (dialog, which) -> {
                            Intent listeIntent = new Intent(FilmDetailsActivity.this, AfficherListeDvdsActivity.class);
                            startActivity(listeIntent);
                            finish(); // Fermer cette activité pour éviter l'empilement
                        })
                        .show();
            } else {
                Toast.makeText(FilmDetailsActivity.this, "Erreur : Impossible d'ajouter au panier", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
