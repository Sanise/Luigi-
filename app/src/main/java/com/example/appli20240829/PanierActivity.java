package com.example.appli20240829;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class PanierActivity extends AppCompatActivity {

    private ListView cartListView;
    private Button btnValidateOrder, btnContinueBrowsing;
    private ArrayAdapter<String> cartAdapter;
    private ArrayList<String> cartItems;
    private int customerId = 1; // ID du client test

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panier);

        cartListView = findViewById(R.id.cart_list_view);
        btnValidateOrder = findViewById(R.id.btn_validate_order);
        btnContinueBrowsing = findViewById(R.id.btn_continue_browsing);

        // Récupérer les films du panier
        cartItems = PanierManager.getCart(this);
        if (cartItems == null) {
            cartItems = new ArrayList<>();
        }

        Log.d("PANIER", "Films récupérés : " + cartItems.toString());

        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Votre panier est vide.", Toast.LENGTH_SHORT).show();
        }

        // Afficher la liste des films
        cartAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, cartItems);
        cartListView.setAdapter(cartAdapter);
        cartAdapter.notifyDataSetChanged();

        // Suppression d'un film en appuyant longuement
        cartListView.setOnItemLongClickListener((parent, view, position, id) -> {
            String selectedMovie = cartItems.get(position);
            PanierManager.removeFromCart(this, selectedMovie);
            cartItems.remove(position);
            cartAdapter.notifyDataSetChanged();
            Toast.makeText(this, selectedMovie + " supprimé du panier", Toast.LENGTH_SHORT).show();
            return true;
        });

        // Bouton Valider le panier
        btnValidateOrder.setOnClickListener(v -> {
            if (!cartItems.isEmpty()) {
                envoyerPanierAuServeur();
            }
        });

        // Bouton Poursuivre la recherche
        btnContinueBrowsing.setOnClickListener(v -> {
            Intent intent = new Intent(PanierActivity.this, AfficherListeDvdsActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cartItems.clear();
        cartItems.addAll(PanierManager.getCart(this));
        cartAdapter.notifyDataSetChanged();
    }

    /**
     * Envoie chaque film du panier à l'API pour enregistrement.
     */
    private void envoyerPanierAuServeur() {
        for (String filmTitle : cartItems) {
            int inventoryId = getInventoryIdForFilm(filmTitle);
            if (inventoryId != -1) {
                envoyerLocation(inventoryId);
            } else {
                Toast.makeText(PanierActivity.this, "Erreur : Impossible de récupérer inventory_id pour " + filmTitle, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Simule la récupération d'un inventory_id pour un film.
     * Cette fonction doit être remplacée par une récupération depuis la base de données.
     */
    private int getInventoryIdForFilm(String filmTitle) {
        // Simuler une correspondance filmTitle -> inventoryId
        return 1; // Pour le test, retourne toujours 1
    }

    /**
     * Méthode pour envoyer une location avec HttpURLConnection et AsyncTask.
     */
    private void envoyerLocation(int inventoryId) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    URL url = new URL("http://10.0.2.2:8080/toad/rental/add");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setDoOutput(true);

                    String currentDateTime = getCurrentDateTime();
                    String returnDate = getReturnDate(currentDateTime);

                    String params = "rental_date=" + currentDateTime +
                            "&inventory_id=" + inventoryId +
                            "&customer_id=" + customerId +
                            "&return_date=" + returnDate +
                            "&staff_id=1" +
                            "&last_update=" + currentDateTime;

                    try (OutputStream os = connection.getOutputStream()) {
                        os.write(params.getBytes());
                        os.flush();
                    }

                    int responseCode = connection.getResponseCode();
                    if (responseCode == 200) {
                        return true;
                    } else {
                        Log.e("API_ERROR", "Erreur HTTP : " + responseCode);
                        return false;
                    }
                } catch (Exception e) {
                    Log.e("API_ERROR", "Erreur lors de l'envoi des données", e);
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    cartItems.clear();
                    cartAdapter.notifyDataSetChanged();
                    Toast.makeText(PanierActivity.this, "Commande validée", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PanierActivity.this, "Erreur lors de l'enregistrement", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    private String getCurrentDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private String getReturnDate(String rentalDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try {
            Date date = sdf.parse(rentalDate);
            assert date != null;
            date.setTime(date.getTime() + (7L * 24 * 60 * 60 * 1000)); // Ajouter 7 jours
            return sdf.format(date);
        } catch (Exception e) {
            Log.e("PANIER", "Erreur lors du calcul de la return_date", e);
            return rentalDate;
        }
    }
}
