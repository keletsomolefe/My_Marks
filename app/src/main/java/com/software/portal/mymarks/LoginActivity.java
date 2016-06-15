package com.software.portal.mymarks;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.widget.Toast;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {
        // UI references.
    private EditText mSNumberView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mSNumberView = (EditText) findViewById(R.id.sNumber);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.portal_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        mSNumberView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        final String sNumber = mSNumberView.getText().toString();
        final String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError("This field is required");
            focusView = mPasswordView;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            mPasswordView.setError("Password must contain at least 4 characters.");
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid student number address.
        if (TextUtils.isEmpty(sNumber)) {
            mSNumberView.setError(getString(R.string.error_field_required));
            focusView = mSNumberView;
            cancel = true;
        } else if (!isEmailValid(sNumber)) {
            mSNumberView.setError("Incorrect student number format.");
            focusView = mSNumberView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);

            new GetMarksTask(sNumber, password).execute();
        }
    }

    private boolean isEmailValid(String email) {
        return email.matches("u\\d{8}+");
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private class GetMarksTask extends AsyncTask<Void, String, String> {
        private final String sNumber;
        private final String pwd;
        private UPPortal portal;

        GetMarksTask(String _sNumber, String _pwd) {
            sNumber = _sNumber.substring(1);
            pwd = _pwd;
            portal = new UPPortal();
        }

        @Override
        protected String doInBackground(Void ... params) {
            String result;
            try {
                publishProgress("Loggin in.");
                if (portal.login(sNumber, pwd)) {
                    publishProgress("Retrieving marks.");
                    result = portal.getMarks();
                } else {
                    publishProgress("Loggin failed, check student number and password.");
                    return null;
                }

            } catch (Exception e) {
                publishProgress("Permanant Failure, Check Internet Connection.");
                return null;
            } finally {
                portal.logout();
            }

            return result;
        }

        protected void onProgressUpdate(String... progress) {
            Toast.makeText(getBaseContext(), progress[0], Toast.LENGTH_SHORT).show();
        }

        protected void onPostExecute(String result) {
            if (result ==  null) {
                showProgress(false);
                return;
            }

            if (result.startsWith("<?xml")) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("XML", result);
                startActivity(intent);
            } else {
                showProgress(false);
                Toast.makeText(getBaseContext(), "Failed to retrieve marks.", Toast.LENGTH_LONG).show();
            }
        }
    }
}

