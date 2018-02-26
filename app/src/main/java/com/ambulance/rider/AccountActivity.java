package com.ambulance.rider;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.ambulance.rider.Common.Common;
import com.ambulance.rider.Model.Riders;

public class AccountActivity extends AppCompatActivity implements View.OnClickListener {

    // Declaring variables of views

    EditText emailId, password, regUsername, regPassword, regConfirmPassword, regName, regEmail, regPhone;
    Button loginNow, registerDialog, registerNow, registerCancel;
    ScrollView regRootView;
    RelativeLayout accountActivity;
    AlertDialog dialog;
    static AlertDialog progressDialog;

    // Declaring Firebase Variables
    FirebaseAuth mFirebaseAuth;
    FirebaseDatabase mFirebaseDatabase;
    DatabaseReference riders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        // Initialize Firebase Variables

        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        riders = mFirebaseDatabase.getReference(Common.riderInfo);

        // Initialize Views

        emailId = findViewById(R.id.emailId);
        password = findViewById(R.id.password);
        loginNow = findViewById(R.id.btnLogin);
        accountActivity = findViewById(R.id.accountActivity);
        registerDialog = findViewById(R.id.btnRegisterDialog);

        // Views click listener

        loginNow.setOnClickListener(this);
        registerDialog.setOnClickListener(this);
    }

    public static void showProgressDialog(Context context, String title, String msg) {
        AlertDialog.Builder progress = new AlertDialog.Builder(context);
        progress.setTitle(title);
        progress.setMessage(msg);
        progress.setCancelable(false);
        progressDialog = progress.create();
        progressDialog.show();
    }

    @Override
    public void onClick(View v) {

        // Look for views by their Ids

        switch (v.getId()) {

            case R.id.btnLogin:

                // Handle login button click

                fnLoginNow();

                break;

            case R.id.btnRegisterDialog:

                // Handle register button click and produce a dialog for registration

                fnRegisterDialogBuilder();

                break;

            case R.id.btnRegisterNow:

                // Handle firebase auth and db to store users notification to databases

                fnRegisterNow();

                break;

            case R.id.btnCancel:

                // Handle canceling of dialog

                dialog.dismiss();

                break;
        }

    }

    private void fnRegisterNow() {
        final String username, name, email, phone, password, c_password;
        username = regUsername.getText().toString();
        name = regName.getText().toString();
        email = regEmail.getText().toString();
        phone = regPhone.getText().toString();
        password = regPassword.getText().toString();
        c_password = regConfirmPassword.getText().toString();

        // Validation check

        if (username.length() < 3) {
            regUsername.setError("atleast 3 characters");
        }
        if (name.length() < 3) {
            regName.setError("atleast 3 characters");
        }
        if (email.length() < 3) {
            regEmail.setError("atleast 3 characters");
        }
        if (phone.length() != 10) {
            regPhone.setError("only 10 digits without country code");
        }
        if (password.length() < 6) {
            regPassword.setError("atleast 6 characters");
        }
        if (c_password.length() < 6) {
            regConfirmPassword.setError("atleast 6 characters");
        }

        // After validation(if success)

        if (username.length() >= 3 && name.length() >= 3 && email.length() >= 3 && phone.length() == 10 && password.length() > 5 && c_password.length() > 5) {
            if (password.equals(c_password)) {

                // If all validation is correct

                showProgressDialog(AccountActivity.this, "", "Registering...");
                mFirebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                Riders rider = new Riders();
                                rider.setEmail(email);
                                rider.setUsername(username);
                                rider.setName(name);
                                rider.setPassword(password);
                                rider.setPhone(phone);

                                riders.child(mFirebaseAuth.getCurrentUser().getUid()).setValue(rider)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {

                                                // Reset all fields
                                                regUsername.setText("");
                                                regName.setText("");
                                                regEmail.setText("");
                                                regPassword.setText("");
                                                regConfirmPassword.setText("");
                                                regPhone.setText("");

                                                progressDialog.dismiss();
                                                dialog.dismiss();
                                                Snackbar.make(accountActivity, "Registered Successfully!", Snackbar.LENGTH_SHORT).show();

                                                startActivity(new Intent(AccountActivity.this,MainActivity.class));
                                                finish();

                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {

                                                progressDialog.dismiss();
                                                Snackbar.make(regRootView, "Something went wrong. Try again later.", Snackbar.LENGTH_SHORT).show();

                                            }
                                        });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                                progressDialog.dismiss();
                                Snackbar.make(regRootView, "Something went wrong. Try again later.", Snackbar.LENGTH_SHORT).show();

                            }
                        });

            } else {

                // Error is reported here

                Snackbar.make(regRootView, "Password don't matches", Snackbar.LENGTH_SHORT).show();

            }
        } else {

            // Error is reported here

            Snackbar.make(regRootView, "Check reported errors", Snackbar.LENGTH_SHORT).show();

        }
    }

    private void fnRegisterDialogBuilder() {

        // Dialog Builder

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Inflate other layouts to views

        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View view = layoutInflater.inflate(R.layout.layout_register_dialog, null);

        // finding and initializing views

        regUsername = view.findViewById(R.id.regUsername);
        regName = view.findViewById(R.id.regName);
        regEmail = view.findViewById(R.id.regEmail);
        regPhone = view.findViewById(R.id.regPhone);
        regPassword = view.findViewById(R.id.regPassword);
        regConfirmPassword = view.findViewById(R.id.regConfirmPassword);
        registerNow = view.findViewById(R.id.btnRegisterNow);
        registerCancel = view.findViewById(R.id.btnCancel);
        regRootView = view.findViewById(R.id.registerDialogRoot);

        // Handling register now button click
        registerNow.setOnClickListener(this);
        registerCancel.setOnClickListener(this);

        builder.setView(view);
        builder.setCancelable(false);
        dialog = builder.create();
        dialog.show();
    }

    private void fnLoginNow() {

        String email, pass;
        email = emailId.getText().toString();
        pass = password.getText().toString();

        // Validation Check
        if (email.length() < 3) {
            emailId.setError("atleast 3 characters");
        }
        if (pass.length() < 6) {
            password.setError("atleast 6 characters");
        }

        if (email.length() >= 3 && pass.length() > 5) {

            showProgressDialog(this,"","Signing In...");
            mFirebaseAuth.signInWithEmailAndPassword(email,pass)
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {

                            progressDialog.dismiss();
                            Snackbar.make(accountActivity, "Signed In :)", Snackbar.LENGTH_SHORT).show();

                            startActivity(new Intent(AccountActivity.this,MainActivity.class));
                            finish();

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            progressDialog.dismiss();
                            Snackbar.make(accountActivity, "Something went wrong. Try again later.", Snackbar.LENGTH_SHORT).show();

                        }
                    });

        }else{

            // Error is reported here

            Snackbar.make(accountActivity, "Check reported errors", Snackbar.LENGTH_SHORT).show();

        }

    }
}
