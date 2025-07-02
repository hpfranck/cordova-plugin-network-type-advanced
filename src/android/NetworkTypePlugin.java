package com.moblink.networktype;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkCapabilities;
import android.net.Network;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.PhoneStateListener;
import android.os.Build;
import android.Manifest;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PermissionHelper;

import org.json.JSONArray;
import org.json.JSONException;

public class NetworkTypePlugin extends CordovaPlugin {
    
    private static final String READ_PHONE_STATE = Manifest.permission.READ_PHONE_STATE;
    private static final int REQUEST_CODE = 1;
    private CallbackContext callbackContext;
    
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        try {
            this.callbackContext = callbackContext;
            
            if (action.equals("getType")) {
                if (!PermissionHelper.hasPermission(this, READ_PHONE_STATE)) {
                    PermissionHelper.requestPermission(this, REQUEST_CODE, READ_PHONE_STATE);
                    return true;
                }
                
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
    
    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Context context = this.cordova.getActivity().getApplicationContext();
                String tipo = getConnectionType(context);
                callbackContext.success(tipo);
            } else {
                callbackContext.error("Permissão READ_PHONE_STATE negada");
            }
        }
    }

    private String getConnectionType(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Network network = cm.getActiveNetwork();
            if (network != null) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        return "WIFI";
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        return getMobileNetworkType(context);
                    }
                }
            }
            return "NONE";
        } else {
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnected()) {
                int type = netInfo.getType();
                if (type == ConnectivityManager.TYPE_WIFI) {
                    return "WIFI";
                } else if (type == ConnectivityManager.TYPE_MOBILE) {
                    return getMobileNetworkType(context);
                }
            }
            return "NONE";
        }
    }
    
    private String getMobileNetworkType(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            
            // Verifica permissão antes de acessar
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    return "CELL";
                }
            }
            
            // Para Android 12+ usa TelephonyDisplayInfo para detectar 5G
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                final String[] networkType = {"CELL"};
                
                tm.listen(new PhoneStateListener() {
                    @Override
                    public void onDisplayInfoChanged(TelephonyDisplayInfo displayInfo) {
                        if (displayInfo.getNetworkType() == TelephonyManager.NETWORK_TYPE_NR ||
                            displayInfo.getOverrideNetworkType() == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA ||
                            displayInfo.getOverrideNetworkType() == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE) {
                            networkType[0] = "5G";
                        }
                    }
                }, PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED);
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
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                    return "3G";
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_IDEN:
                case TelephonyManager.NETWORK_TYPE_GSM:
                    return "2G";
                default:
                    return "CELL";
            }
        } catch (SecurityException e) {
            // Se ainda houver problema de segurança, retorna tipo genérico
            return "CELL";
        }
    }
}