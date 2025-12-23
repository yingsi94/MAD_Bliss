package com.example.login_signup_profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bliss.R;
// 导入 MainActivity
import com.example.main.MainActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword, tvSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignUp = findViewById(R.id.tvSignUp);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = etEmail.getText().toString();
                String password = etPassword.getText().toString();

                if (!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    if(!password.isEmpty()){
                        auth.signInWithEmailAndPassword(email,password)
                                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                                    @Override
                                    public void onSuccess(AuthResult authResult)  {
                                        Toast.makeText(LoginActivity.this,"Login Successful", Toast.LENGTH_SHORT).show();

                                        // --- 修改后的跳转逻辑 ---
                                        // 跳转到 MainActivity，而不是 Fragment
                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                        // 清空栈，防止返回键回到登录界面
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish(); // 销毁当前的登录页面
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(LoginActivity.this, "Login Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        etPassword.setError("Password required");
                    }
                } else if (email.isEmpty()) {
                    etEmail.setError("Email required");
                } else {
                    etEmail.setError("Invalid Email");
                }
            }
        });

        tvForgotPassword.setOnClickListener(view ->
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));

        tvSignUp.setOnClickListener(view ->
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class)));
    }
}