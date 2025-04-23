package com.example.appli20240829;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;

public class PanierManager {
    private static final String CART_PREFS = "cart_prefs"; // Clé de nom du fichier de préférences
    private static final String CART_ITEMS = "cart_items";  // Clé de la liste des éléments stockés dans le panier

    // Ajouter un film au panier
    public static void addToCart(Context context, String movieTitle) {
        // On récupère les préférences partagées (fichier local de stockage)
        SharedPreferences prefs = context.getSharedPreferences(CART_PREFS, Context.MODE_PRIVATE);
        // On récupère la liste actuelle du panier
        ArrayList<String> cartItems = getCart(context);
        // On ajoute le nouveau film à la liste
        cartItems.add(movieTitle);

        // On sauvegarde la nouvelle liste dans les préférences (sous forme de tableau JSON)
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(CART_ITEMS, new JSONArray(cartItems).toString());
        editor.apply(); // Applique les modifications

        // Affiche une trace dans le logcat pour le suivi
        Log.d("PANIER", "Film ajouté : " + movieTitle);
    }

    // Récupérer les films du panier sous forme de liste
    public static ArrayList<String> getCart(Context context) {
        // On ouvre le fichier des préférences
        SharedPreferences prefs = context.getSharedPreferences(CART_PREFS, Context.MODE_PRIVATE);

        // On récupère la chaîne JSON associée à la clé "cart_items"
        String json = prefs.getString(CART_ITEMS, "[]");

        ArrayList<String> cartItems = new ArrayList<>();
        try {
            // On reconstruit un tableau à partir de la chaîne JSON
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                cartItems.add(jsonArray.getString(i)); // On ajoute chaque élément à la liste
            }
        } catch (JSONException e) {
            e.printStackTrace(); // Affiche une erreur si la chaîne est mal formée
        }

        return cartItems; // Retourne la liste reconstituée
    }

    // Supprimer un film précis du panier
    public static void removeFromCart(Context context, String movieTitle) {
        // Récupère la liste actuelle
        ArrayList<String> cartItems = getCart(context);

        // Supprime l’élément correspondant (si trouvé)
        cartItems.remove(movieTitle);

        //  // Sauvegarde la liste mise à jour dans les préférences
        SharedPreferences prefs = context.getSharedPreferences(CART_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(CART_ITEMS, new JSONArray(cartItems).toString());
        editor.apply();
    }

    // Vider entièrement le panier
    public static void clearCart(Context context) {
        // Ouvre le fichier des préférences
        SharedPreferences prefs = context.getSharedPreferences(CART_PREFS, Context.MODE_PRIVATE);

        // Supprime complètement la clé contenant les éléments
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(CART_ITEMS);
        editor.apply();
    }
}
