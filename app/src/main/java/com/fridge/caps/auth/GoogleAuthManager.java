package com.fridge.caps.auth;



/**
 * Purpose: Handles authentication provider integration and sign-in flow.
 * Depends on: Firebase Auth and Google sign-in SDK components.
 * Notes: Bridges account sign-in results to app auth state.
 */
/**
 * Purpose: Handles authentication provider integration and sign-in flow.
 * Depends on: Firebase Auth and Google sign-in SDK components.
 * Notes: Bridges account sign-in results to app auth state.
 */
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
