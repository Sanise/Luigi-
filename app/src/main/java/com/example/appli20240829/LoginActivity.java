package com.example.appli20240829;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        requestQueue = Volley.newRequestQueue(this);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                } else {
                    verifierUtilisateur(email, password);
                }
            }
        });
    }

    private void verifierUtilisateur(String email, String password) {
        String url = "http://10.0.2.2:8080/toad/customer/getByEmail?email=" + email;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            int customerId = response.getInt("customerId");
                            String passwordBDD = response.getString("password");

                            if (password.equals(passwordBDD)) {
                                Toast.makeText(LoginActivity.this, "Connexion réussie !", Toast.LENGTH_SHORT).show();

                                // Redirection avec l'ID du client
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.putExtra("CUSTOMER_ID", customerId);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this, "Mot de passe incorrect", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Log.e("LOGIN", "Erreur JSON : " + e.getMessage());
                            Toast.makeText(LoginActivity.this, "Erreur de connexion", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("LOGIN", "Erreur Volley : " + error.toString());
                        Toast.makeText(LoginActivity.this, "Utilisateur introuvable", Toast.LENGTH_SHORT).show();
                    }
                });

        requestQueue.add(request);
    }
}
