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

    private EditText editTextEmail, editTextPassword, editTextUrl;
    private Button buttonLogin;
    private Spinner spinnerURLs;
    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextEmail = findViewById(R.id.etEmail);
        editTextPassword = findViewById(R.id.etPassword);
        buttonLogin = findViewById(R.id.btnLogin);
        editTextUrl = findViewById(R.id.editTextURL);
        spinnerURLs = findViewById(R.id.spinnerURLs);

        String[] listeURLs = getResources().getStringArray(R.array.listeURLs);
        ArrayAdapter<CharSequence> adapterListeURLs = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, listeURLs);
        adapterListeURLs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerURLs.setAdapter(adapterListeURLs);
        spinnerURLs.setOnItemSelectedListener(this);

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editTextEmail.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();
                String urlConnexion = com.btssio.applicationrftg.DonneesPartagees.getURLConnexion();

                if (!editTextUrl.getText().toString().isEmpty()) {
                    urlConnexion = editTextUrl.getText().toString().trim();
                }

                if (!email.isEmpty() && !password.isEmpty()) {
                    fetchCustomerByEmail(email, password, urlConnexion);
                } else {
                    Toast.makeText(LoginActivity.this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void fetchCustomerByEmail(String email, String password, String urlConnexion) {
        String url = urlConnexion + "/toad/customer/getByEmail?email=" + email;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(LoginActivity.this, "Erreur de connexion", Toast.LENGTH_SHORT).show());
                Log.e("LoginActivity", "Échec de la connexion : ", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        Log.d("LoginActivity", "Réponse du serveur : " + responseBody);

                        // Correction : Vérification si la réponse est vide avant de l'analyser
                        if (responseBody == null || responseBody.isEmpty()) {
                            runOnUiThread(() ->
                                    Toast.makeText(LoginActivity.this, "Utilisateur non trouvé", Toast.LENGTH_SHORT).show());
                            return;
                        }

                        JSONObject jsonObject = new JSONObject(responseBody);

                        // VERIFICATION DE L'EMAIL ET DU PASSWORD
                        if (jsonObject.has("password") && jsonObject.getString("password").equals(password)) {
                            int userId = jsonObject.getInt("customerId");
                            runOnUiThread(() -> {
                                Toast.makeText(LoginActivity.this, "Connexion réussie", Toast.LENGTH_SHORT).show();

                                // STOCKAGE DE l'ID DE L'UTILISATEUR
                                SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putInt("customerId", userId);
                                editor.apply();

                                // REDIRIGE VERS LA LISTE DES FILMS
                                Intent intent = new Intent(LoginActivity.this, AfficherListeDvdsActivity.class);
                                intent.putExtra("USER_ID", userId);
                                startActivity(intent);
                                finish();
                            });
                        } else {
                            runOnUiThread(() ->
                                    Toast.makeText(LoginActivity.this, "Mot de passe incorrect", Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        Log.e("LoginActivity", "Erreur lors du traitement des données", e);
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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // Correction de l'URL
        String selectedURL = parent.getItemAtPosition(position).toString();
        com.btssio.applicationrftg.DonneesPartagees.setURLConnexion(selectedURL);
        editTextUrl.setText(selectedURL);
        Toast.makeText(getApplicationContext(), "URL sélectionnée : " + selectedURL, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Ne rien faire
    }
}
