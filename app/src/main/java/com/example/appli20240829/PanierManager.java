package com.example.appli20240829;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;

public class PanierManager {
    private static final String CART_PREFS = "cart_prefs";
    private static final String CART_ITEMS = "cart_items";

    // Ajouter un film au panier
    public static void addToCart(Context context, String movieTitle) {
        SharedPreferences prefs = context.getSharedPreferences(CART_PREFS, Context.MODE_PRIVATE);
        ArrayList<String> cartItems = getCart(context);
        cartItems.add(movieTitle);

        // Sauvegarde sous forme de JSON
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(CART_ITEMS, new JSONArray(cartItems).toString());
        editor.apply();

        Log.d("PANIER", "Film ajouté : " + movieTitle);
    }

    // Récupérer les films du panier sous forme de liste
    public static ArrayList<String> getCart(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(CART_PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(CART_ITEMS, "[]");

        ArrayList<String> cartItems = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                cartItems.add(jsonArray.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return cartItems;
    }

    // Supprimer un film du panier
    public static void removeFromCart(Context context, String movieTitle) {
        ArrayList<String> cartItems = getCart(context);
        cartItems.remove(movieTitle);

        // Sauvegarde
        SharedPreferences prefs = context.getSharedPreferences(CART_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(CART_ITEMS, new JSONArray(cartItems).toString());
        editor.apply();
    }

    // Vider le panier
    public static void clearCart(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(CART_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(CART_ITEMS);
        editor.apply();
    }
}
