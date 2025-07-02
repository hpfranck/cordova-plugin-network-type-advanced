package com.moblink.networktype;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.os.Build;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;

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
        callbackContext.error("Erro Java: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        return false;
    }
}

    private String getConnectionType(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            int type = netInfo.getType();
            if (type == ConnectivityManager.TYPE_WIFI) {
                return "WIFI";
            } else if (type == ConnectivityManager.TYPE_MOBILE) {
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.PERMISSION_GRANTED) != PackageManager.PERMISSION_GRANTED) {
                    callbackContext.error("Permissão PERMISSION_GRANTED não concedida");
                    return "NONE";
                }
                int networkType = tm.getDataNetworkType();

                switch (networkType) {
                    case TelephonyManager.NETWORK_TYPE_NR:
                        return "5G";
                    case TelephonyManager.NETWORK_TYPE_LTE:
                        return "4G";
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                    case TelephonyManager.NETWORK_TYPE_HSPA:
                    case TelephonyManager.NETWORK_TYPE_HSPAP:
                    case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    case TelephonyManager.NETWORK_TYPE_EVDO_A:
                        return "3G";
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                    case TelephonyManager.NETWORK_TYPE_GPRS:
                    case TelephonyManager.NETWORK_TYPE_CDMA:
                        return "2G";
                    default:
                        return "CELL";
                }
            } else {
                return "UNKNOWN";
            }
        } else {
            return "NONE";
        }
    }
}
