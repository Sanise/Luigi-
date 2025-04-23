package com.example.appli20240829;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    // Champs de saisie pour email, mot de passe, et URL personnalisée
    private EditText editTextEmail, editTextPassword, editTextUrl;
    private Button buttonLogin; // Bouton de connexion
    private Spinner spinnerURLs; // Liste déroulante des URLs prédéfinies
    private OkHttpClient client = new OkHttpClient();  // Client HTTP (librairie OkHttp) pour envoyer les requêtes API

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // Lie cette activité à son layout XML

        // Liaison des composants visuels du layout XML
        editTextEmail = findViewById(R.id.etEmail);
        editTextPassword = findViewById(R.id.etPassword);
        buttonLogin = findViewById(R.id.btnLogin);
        editTextUrl = findViewById(R.id.editTextURL);
        spinnerURLs = findViewById(R.id.spinnerURLs);

        // Initialisation du Spinner avec les URLs prédéfinies (définies dans strings.xml)
        String[] listeURLs = getResources().getStringArray(R.array.listeURLs);
        ArrayAdapter<CharSequence> adapterListeURLs = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, listeURLs);
        adapterListeURLs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerURLs.setAdapter(adapterListeURLs);
        spinnerURLs.setOnItemSelectedListener(this); // écouteur de sélection d’URL

        // Lorsqu’on clique sur le bouton de connexion
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Récupère les valeurs saisies
                String email = editTextEmail.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();
                String urlConnexion = com.btssio.applicationrftg.DonneesPartagees.getURLConnexion();

                // Si l’utilisateur a saisi manuellement une URL, on l’utilise
                if (!editTextUrl.getText().toString().isEmpty()) {
                    urlConnexion = editTextUrl.getText().toString().trim();
                }

                // Vérifie que tous les champs sont remplis
                if (!email.isEmpty() && !password.isEmpty()) {
                    // Lancer la requête pour chercher le client par email
                    fetchCustomerByEmail(email, password, urlConnexion);
                } else {
                    // Affiche un message si un champ est vide
                    Toast.makeText(LoginActivity.this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Cette méthode lance une requête HTTP GET vers /getByEmail pour récupérer les infos d’un client
    private void fetchCustomerByEmail(String email, String password, String urlConnexion) {
        String url = urlConnexion + "/toad/customer/getByEmail?email=" + email;

        // Création de la requête HTTP GET avec OkHttp
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        // Envoie de la requête en asynchrone (appel réseau en arrière-plan)
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // En cas d’échec réseau → message d’erreur
                runOnUiThread(() ->
                        Toast.makeText(LoginActivity.this, "Erreur de connexion", Toast.LENGTH_SHORT).show());
                Log.e("LoginActivity", "Échec de la connexion : ", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string(); // Corps brut de la réponse
                        Log.d("LoginActivity", "Réponse du serveur : " + responseBody);

                        // Correction : Vérification si la réponse est vide avant de l'analyser
                        if (responseBody == null || responseBody.isEmpty()) {
                            runOnUiThread(() ->
                                    Toast.makeText(LoginActivity.this, "Utilisateur non trouvé", Toast.LENGTH_SHORT).show());
                            return;
                        }

                        // Conversion de la réponse en JSON
                        JSONObject jsonObject = new JSONObject(responseBody);

                        // Vérifie que le mot de passe correspond à celui enregistré
                        if (jsonObject.has("password") && jsonObject.getString("password").equals(password)) {
                            int userId = jsonObject.getInt("customerId");
                            // Si tout est bon → on affiche un message + on enregistre l’ID
                            runOnUiThread(() -> {
                                Toast.makeText(LoginActivity.this, "Connexion réussie", Toast.LENGTH_SHORT).show();

                                // Stockage de l’ID utilisateur dans les préférences partagées
                                SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putInt("customerId", userId);
                                editor.apply();

                                // REDIRIGE VERS LA LISTE DES FILMS
                                Intent intent = new Intent(LoginActivity.this, AfficherListeDvdsActivity.class);
                                intent.putExtra("USER_ID", userId);
                                startActivity(intent);
                                finish(); // Termine LoginActivity
                            });
                        } else {
                            // Mauvais mot de passe
                            runOnUiThread(() ->
                                    Toast.makeText(LoginActivity.this, "Mot de passe incorrect", Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) { // Erreur de parsing ou autre
                        Log.e("LoginActivity", "Erreur lors du traitement des données", e);
                        // Réponse HTTP autre que 200
                        runOnUiThread(() ->
                                Toast.makeText(LoginActivity.this, "Erreur lors du traitement des données", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(LoginActivity.this, "Erreur serveur", Toast.LENGTH_SHORT).show());
                    Log.e("LoginActivity", "Erreur serveur : " + response.code());
                }
            }
        });
    }

    // Méthode déclenchée quand l'utilisateur sélectionne une URL dans le spinner
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // Correction de l'URL
        String selectedURL = parent.getItemAtPosition(position).toString(); // Récupère l’URL choisie
        com.btssio.applicationrftg.DonneesPartagees.setURLConnexion(selectedURL); // Mise à jour globale
        editTextUrl.setText(selectedURL); // Affiche l’URL dans le champ
        Toast.makeText(getApplicationContext(), "URL sélectionnée : " + selectedURL, Toast.LENGTH_SHORT).show();
    }

    // Obligatoire, mais on ne fait rien si rien n’est sélectionné
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Ne rien faire
    }
}
