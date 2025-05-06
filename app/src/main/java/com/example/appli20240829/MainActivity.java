package com.example.appli20240829;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bouton pour naviguer vers AfficherListeDvdsActivity
        Button buttonAfficherDvds = findViewById(R.id.button_afficher_dvds);

        buttonAfficherDvds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                    intent = new Intent(MainActivity.this, AfficherListeDvdsActivity.class);
                startActivity(intent);
            }
        });
    }
}
