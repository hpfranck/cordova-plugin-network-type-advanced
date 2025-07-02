package com.moblink.networktype;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;

public class NetworkTypePlugin extends CordovaPlugin {
    
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        try {
            if (action.equals("getType")) {
                Context context = this.cordova.getActivity().getApplicationContext();
                String tipo = getConnectionType(context);
                callbackContext.success(tipo);
                return true;
            } else {
                callbackContext.error("Ação desconhecida: " + action);
                return false;
            }
        } catch (Exception e) {
            callbackContext.error("Erro: " + e.getMessage());
            return false;
        }
    }

    private String getConnectionType(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return "NONE";
            
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            if (capabilities == null) return "NONE";
            
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return "WIFI";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return "CELL";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return "ETHERNET";
            } else {
                return "UNKNOWN";
            }
        } else {
            // Para Android < 6.0
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnected()) {
                int type = netInfo.getType();
                if (type == ConnectivityManager.TYPE_WIFI) {
                    return "WIFI";
                } else if (type == ConnectivityManager.TYPE_MOBILE) {
                    return "CELL";
                } else {
                    return "UNKNOWN";
                }
            } else {
                return "NONE";
            }
        }
    }
}