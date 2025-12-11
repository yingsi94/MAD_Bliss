package com.example.bliss;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private Button btnCancelPassword, btnDone;
    private TextInputEditText etNewPassword, etConfirmPassword;
    FirebaseAuth auth;
    FirebaseUser user;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);


        btnCancelPassword = findViewById(R.id.btnCancelPassword);
        btnDone = findViewById(R.id.btnDone);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        auth = FirebaseAuth.getInstance();

        btnCancelPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newPass = etNewPassword.getText().toString();
                String confirmPass = etConfirmPassword.getText().toString();

                userId = auth.getCurrentUser().getUid();
                user = auth.getCurrentUser();

                if (newPass.isEmpty() || confirmPass.isEmpty()) {
                    Toast.makeText(ChangePasswordActivity.this, "Fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!newPass.equals(confirmPass)) {
                    Toast.makeText(ChangePasswordActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(newPass.length() < 6){
                    Toast.makeText(ChangePasswordActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                    return;
                }

                    user.updatePassword(newPass).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(ChangePasswordActivity.this, "Password Reset Successfully", Toast.LENGTH_SHORT).show();
                        }
                    });
                startActivity(new Intent(ChangePasswordActivity.this, ProfileActivity.class));
                finish();

            }
        });
    }
}
