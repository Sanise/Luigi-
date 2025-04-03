package com.example.appli20240829;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
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
    private int customerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panier);

        // 🔐 Récupérer l'ID du client connecté
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        customerId = sharedPreferences.getInt("customerId", -1);

        if (customerId == -1) {
            Toast.makeText(this, "Erreur : ID client introuvable. Veuillez vous reconnecter.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        cartListView = findViewById(R.id.cart_list_view);
        btnValidateOrder = findViewById(R.id.btn_validate_order);
        btnContinueBrowsing = findViewById(R.id.btn_continue_browsing);

        cartItems = PanierManager.getCart(this);
        if (cartItems == null) {
            cartItems = new ArrayList<>();
        }

        Log.d("PANIER", "Films récupérés : " + cartItems.toString());

        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Votre panier est vide.", Toast.LENGTH_SHORT).show();
        }

        // 💬 Afficher uniquement les titres dans la ListView
        ArrayList<String> displayItems = new ArrayList<>();
        for (String item : cartItems) {
            String[] parts = item.split("\\|");
            displayItems.add(parts.length > 1 ? parts[1] : item);
        }

        cartAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayItems);
        cartListView.setAdapter(cartAdapter);

        // ❌ Suppression d'un film sur appui long
        cartListView.setOnItemLongClickListener((parent, view, position, id) -> {
            String titleClicked = cartAdapter.getItem(position);
            String fullEntry = null;

            for (String item : cartItems) {
                String[] parts = item.split("\\|");
                if (parts.length > 1 && parts[1].equals(titleClicked)) {
                    fullEntry = item;
                    break;
                }
            }

            if (fullEntry != null) {
                PanierManager.removeFromCart(this, fullEntry);
                cartItems.remove(fullEntry);
                Toast.makeText(this, titleClicked + " supprimé du panier", Toast.LENGTH_SHORT).show();

                // Mettre à jour l'affichage
                ArrayList<String> updatedDisplayItems = new ArrayList<>();
                for (String item : cartItems) {
                    String[] parts = item.split("\\|");
                    updatedDisplayItems.add(parts.length > 1 ? parts[1] : item);
                }

                cartAdapter.clear();
                cartAdapter.addAll(updatedDisplayItems);
                cartAdapter.notifyDataSetChanged();
            }

            return true;
        });

        // ✅ Valider la commande
        btnValidateOrder.setOnClickListener(v -> {
            if (!cartItems.isEmpty()) {
                envoyerPanierAuServeur();
            }
        });

        // ↩️ Retour à la liste des DVDs
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

    // 📤 Envoi de chaque film au serveur
    private void envoyerPanierAuServeur() {
        for (String entry : cartItems) {
            String[] parts = entry.split("\\|");
            if (parts.length >= 2) {
                int inventoryId = Integer.parseInt(parts[0]);
                envoyerLocation(inventoryId);
            } else {
                Toast.makeText(this, "Entrée invalide : " + entry, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 📨 Envoi d'une location
    private void envoyerLocation(int inventoryId) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    URL url = new URL(com.btssio.applicationrftg.DonneesPartagees.getURLConnexion() + "/toad/rental/add");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setDoOutput(true);

                    String rentalDateTime = getCurrentDateTime();
                    String returnDate = getReturnDate(rentalDateTime);

                    String params = "rental_date=" + rentalDateTime +
                            "&inventory_id=" + inventoryId +
                            "&customer_id=" + customerId +
                            "&return_date=" + returnDate +
                            "&staff_id=1" +
                            "&last_update=" + rentalDateTime;

                    try (OutputStream os = connection.getOutputStream()) {
                        os.write(params.getBytes());
                        os.flush();
                    }

                    int responseCode = connection.getResponseCode();
                    return responseCode == 200;

                } catch (Exception e) {
                    Log.e("API_ERROR", "Erreur lors de l'envoi des données", e);
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    PanierManager.clearCart(PanierActivity.this);
                    cartItems.clear();
                    cartAdapter.clear();
                    cartAdapter.notifyDataSetChanged();
                    Toast.makeText(PanierActivity.this, "Commande validée", Toast.LENGTH_SHORT).show();
                }
                else {
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
            date.setTime(date.getTime() + (7L * 24 * 60 * 60 * 1000)); // +7 jours
            return sdf.format(date);
        } catch (Exception e) {
            Log.e("PANIER", "Erreur lors du calcul de la return_date", e);
            return rentalDate;
        }
    }
}
