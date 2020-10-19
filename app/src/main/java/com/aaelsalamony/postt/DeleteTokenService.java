package com.aaelsalamony.postt;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;

import java.io.IOException;

public class DeleteTokenService extends IntentService
{
    public static final String TAG = DeleteTokenService.class.getSimpleName();

    public DeleteTokenService()
    {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        try
        {
            // Check for current token
            String originalToken = getTokenFromPrefs();
            Log.d(TAG, "Token before deletion: " + originalToken);

            // Resets Instance ID and revokes all tokens.
            FirebaseInstanceId.getInstance().deleteInstanceId();
            FirebaseInstanceId.getInstance().getToken();
            FirebaseInstanceId.getInstance().getToken();

            // Clear current saved token
            saveTokenToPrefs("");

            // Check for success of empty token
            String tokenCheck = getTokenFromPrefs();
            Log.d(TAG, "Token deleted. Proof: " + tokenCheck);

            // Now manually call onTokenRefresh()
            Log.d(TAG, "Getting new token");
            FirebaseInstanceId.getInstance().getToken();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void saveTokenToPrefs(String _token)
    {
        // Access Shared Preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();

        // Save to SharedPreferences
        editor.putString("registration_id", _token);
        editor.apply();
    }

    private String getTokenFromPrefs()
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getString("registration_id", null);
    }
}
