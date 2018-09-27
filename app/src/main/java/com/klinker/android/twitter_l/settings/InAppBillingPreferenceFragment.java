package com.klinker.android.twitter_l.settings;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceFragment;
import com.google.android.material.snackbar.Snackbar;
import androidx.preference.PreferenceFragmentCompat;

import com.android.vending.billing.IInAppBillingService;
import com.klinker.android.twitter_l.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class InAppBillingPreferenceFragment extends PreferenceFragment {

    IInAppBillingService mService;

    ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent serviceIntent =
                new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        getActivity().bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);

        try {
            new ConsumeItems().execute();
        } catch (Throwable t) {

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1001) {
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

            if (resultCode == Activity.RESULT_OK) {
                try {
                    JSONObject jo = new JSONObject(purchaseData);
                    String sku = jo.getString("productId");
                    alert("Your support is greatly appreciated. Users like you are the reason I love my job :)");

                    SharedPreferences sharedPreferences = AppSettings.getSharedPreferences(getActivity());

                    sharedPreferences.edit().putBoolean("2018_supporter", true).commit();
                } catch (JSONException e) {
                    alert("Uh oh... Something went wrong with the purchase: Failed to parse purchase data.");
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            getActivity().unbindService(mServiceConn);
        }
    }

    private void alert(String alert) {
        Snackbar
                .make(getActivity().findViewById(android.R.id.content), alert, Snackbar.LENGTH_LONG)
                .show();
    }

    protected void start2016SupporterPurchase(String amount) {
        new StartPurchase("2018_supporter_" + amount).execute();
    }

    class ConsumeItems extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                SharedPreferences sharedPreferences = AppSettings.getSharedPreferences(getActivity());

                String continueToken = "";

                while (continueToken != null) {
                    Bundle ownedItems;
                    if (continueToken.equals(""))
                        ownedItems = mService.getPurchases(3, getActivity().getPackageName(), "inapp", null);
                    else
                        ownedItems = mService.getPurchases(3, getActivity().getPackageName(), "inapp", continueToken);

                    int response = ownedItems.getInt("RESPONSE_CODE");
                    if (response == 0) {
                        ArrayList<String> purchaseDataList =
                                ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                        continueToken = ownedItems.getString("INAPP_CONTINUATION_TOKEN");

                        if (purchaseDataList == null || purchaseDataList.size() == 0) {
                            return null;
                        }

                        for (int i = 0; i < purchaseDataList.size(); ++i) {
                            JSONObject purchaseData = new JSONObject(purchaseDataList.get(i));
                            consumePurchase(purchaseData);

                            sharedPreferences.edit().putBoolean("2018_supporter", true).apply();
                        }

                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }

            return null;
        }

        private void consumePurchase(JSONObject purchaseData) throws JSONException, RemoteException {
            consumePurchase(purchaseData.getString("purchaseToken"));
        }

        private void consumePurchase(String purchaseToken) throws RemoteException {
            try {
                mService.consumePurchase(3, getActivity().getPackageName(), purchaseToken);
            } catch (Throwable t) { }
        }
    }

    class StartPurchase extends AsyncTask<Void, Void, PendingIntent> {
        private ProgressDialog progress;
        private String sku;

        public StartPurchase(String sku) {
            this.sku = sku;
        }

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(getActivity(), "Preparing Purchase...",
                    "Hang tight!", true);
        }

        @Override
        protected PendingIntent doInBackground(Void... arg0) {
            try {
                Bundle buyIntentBundle = mService.getBuyIntent(3, getActivity().getPackageName(),
                        sku, "inapp", "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ");
                return buyIntentBundle.getParcelable("BUY_INTENT");
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(PendingIntent pendingIntent) {
            try {
                getActivity().startIntentSenderForResult(pendingIntent.getIntentSender(),
                        1001, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
                        Integer.valueOf(0));
            } catch (Exception e) {
                e.printStackTrace();
                alert("Uh oh... Something went wrong with the purchase: " + e.getMessage());
            }

            try {
                progress.dismiss();
            } catch (Exception e) { }
        }
    }
}
