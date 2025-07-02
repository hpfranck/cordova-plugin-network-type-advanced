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
                getConnectionType(context, callbackContext);
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
                getConnectionType(context, callbackContext);
            } else {
                callbackContext.error("Permissão READ_PHONE_STATE negada");
            }
        }
    }

    private void getConnectionType(Context context, CallbackContext callback) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Network network = cm.getActiveNetwork();
            if (network != null) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        callback.success("WIFI");
                        return;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        getMobileNetworkType(context, callback);
                        return;
                    }
                }
            }
            callback.success("NONE");
        } else {
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnected()) {
                int type = netInfo.getType();
                if (type == ConnectivityManager.TYPE_WIFI) {
                    callback.success("WIFI");
                } else if (type == ConnectivityManager.TYPE_MOBILE) {
                    getMobileNetworkType(context, callback);
                }
            } else {
                callback.success("NONE");
            }
        }
    }
    
    private void getMobileNetworkType(Context context, CallbackContext callback) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                context.checkSelfPermission(READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                callback.success("CELL");
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                PhoneStateListener listener = new PhoneStateListener() {
                    @Override
                    public void onDisplayInfoChanged(TelephonyDisplayInfo displayInfo) {
                        try {
                            int override = displayInfo.getOverrideNetworkType();
                            int actual = displayInfo.getNetworkType();

                            String result = determineNetworkType(actual, override);
                            
                            callback.success(result);
                            tm.listen(this, PhoneStateListener.LISTEN_NONE); // Remove listener
                        } catch (Exception e) {
                            callback.success("CELL");
                        }
                    }
                };
                
                tm.listen(listener, PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED);
                
                // Timeout para evitar callback infinito
                cordova.getThreadPool().execute(() -> {
                    try {
                        Thread.sleep(2000);
                        tm.listen(listener, PhoneStateListener.LISTEN_NONE);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                });
                
            } else {
                // Para versões anteriores ao Android 11
                int type = tm.getDataNetworkType();
                String result = getNetworkTypeFromInt(type);
                callback.success(result);
            }
        } catch (Exception e) {
            callback.success("CELL");
        }
    }
    
    private String determineNetworkType(int actual, int override) {
        // Verificar 5G primeiro
        if (actual == TelephonyManager.NETWORK_TYPE_NR) {
            return "5G";
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (override == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA) {
                return "5G";
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (override == 5) { 
                    return "5G";
                }
            }
            if (override == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED) {
                return "5G";
            }
        }
        
        return getNetworkTypeFromInt(actual);
    }
    
    private String getNetworkTypeFromInt(int type) {
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_NR:
                return "5G";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return "2G";
            default:
                return "CELL";
        }
    }
}