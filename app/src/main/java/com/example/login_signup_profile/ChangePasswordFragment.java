package com.example.login_signup_profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bliss.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordFragment extends Fragment {

    private Button btnCancelPassword, btnDone;
    private TextInputEditText etNewPassword, etConfirmPassword;
    private FirebaseAuth auth;
    private FirebaseUser user;

    public ChangePasswordFragment() {
        // 必须保留的空构造函数
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 使用你提供的 XML 布局文件
        return inflater.inflate(R.layout.fragment_change_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //--初始化视图--
        btnCancelPassword = view.findViewById(R.id.btnCancelPassword);
        btnDone = view.findViewById(R.id.btnDone);
        etNewPassword = view.findViewById(R.id.etNewPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        //--取消按钮逻辑--
        btnCancelPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 返回上一个 Fragment (ProfileFragment)
                if (getParentFragmentManager() != null) {
                    getParentFragmentManager().popBackStack();
                }
            }
        });

        //--完成按钮逻辑--
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newPass = etNewPassword.getText().toString();
                String confirmPass = etConfirmPassword.getText().toString();

                if (newPass.isEmpty() || confirmPass.isEmpty()) {
                    Toast.makeText(getContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!newPass.equals(confirmPass)) {
                    Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(newPass.length() < 6){
                    Toast.makeText(getContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (user != null) {
                    user.updatePassword(newPass).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(getContext(), "Password Reset Successfully", Toast.LENGTH_SHORT).show();
                            // 修改成功后自动回退
                            if (getParentFragmentManager() != null) {
                                getParentFragmentManager().popBackStack();
                            }
                        }
                    }).addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
}