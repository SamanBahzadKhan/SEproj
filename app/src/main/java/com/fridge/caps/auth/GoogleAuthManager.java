package com.fridge.caps.auth;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;

/**
 * Google Sign-In configured for Calendar scope + server auth code (exchanged for refresh token on Cloud Functions).
 */
public final class GoogleAuthManager {

    private final GoogleSignInClient googleSignInClient;

    public GoogleAuthManager(@NonNull Activity activity, @NonNull String webClientId) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestServerAuthCode(webClientId, true)
                .requestScopes(new Scope("https://www.googleapis.com/auth/calendar"))
                .build();
        googleSignInClient = GoogleSignIn.getClient(activity, gso);
    }

    @NonNull
    public GoogleSignInClient getGoogleSignInClient() {
        return googleSignInClient;
    }

    @NonNull
    public Intent getSignInIntent() {
        return googleSignInClient.getSignInIntent();
    }

    @Nullable
    public String handleSignInResult(Intent data) throws ApiException {
        com.google.android.gms.tasks.Task<GoogleSignInAccount> task =
                GoogleSignIn.getSignedInAccountFromIntent(data);
        GoogleSignInAccount account = task.getResult(ApiException.class);
        return account != null ? account.getServerAuthCode() : null;
    }
}
