package com.example.parcial2pdm;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextInputLayout tilEmail, tilPassword;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApiKey("AIzaSyBR1nJa4DLe_cHSiMK63jwbira3iT0m-2Q")
                    .setApplicationId("1:646500142633:android:12382b0c98f8ed68775638")
                    .setProjectId("parcial2pdm-80efa")
                    .setStorageBucket("parcial2pdm-80efa.firebasestorage.app")
                    .build();

            FirebaseApp.initializeApp(this, options);
        }

        mAuth = FirebaseAuth.getInstance();

        this.etEmail = findViewById(R.id.etEmail);
        this.etPassword = findViewById(R.id.etPassword);
        this.btnLogin = findViewById(R.id.btnLogin);
        this.progressBar = findViewById(R.id.progressBar);
        this.tilEmail = findViewById(R.id.tilEmail);
        this.tilPassword = findViewById(R.id.tilPassword);

        btnLogin.setEnabled(false);
        btnLogin.setAlpha(0.5f);

        etEmail.addTextChangedListener(watcher);
        etPassword.addTextChangedListener(watcher);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validarLogin();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth != null) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                irAlDashboard();
            }
        }
    }

    private final TextWatcher watcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable editable) {}

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            boolean habilitar = !TextUtils.isEmpty(email) && !TextUtils.isEmpty(password);
            btnLogin.setEnabled(habilitar);
            btnLogin.setAlpha(habilitar ? 1.0f : 0.5f);
        }
    };

    private void validarLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        tilEmail.setError(null);
        tilPassword.setError(null);

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Correo electrónico no válido");
            etEmail.requestFocus();
            return;
        }

        if (password.length() < 6) {
            tilPassword.setError("La contraseña debe tener al menos 6 caracteres");
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        // Llamada clásica de Autenticación con Firebase en Java Puro
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        btnLogin.setAlpha(1.0f);

                        if (task.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "¡Bienvenido!", Toast.LENGTH_SHORT).show();
                            irAlDashboard();
                        } else {
                            tilPassword.setError("Correo o contraseña incorrectos");
                            String errorMsg = task.getException() != null ? task.getException().getMessage() : "Error de autenticación";
                            Toast.makeText(MainActivity.this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void irAlDashboard() {
        Intent intent = new Intent(MainActivity.this, Dashboard.class);
        startActivity(intent);
        finish();
    }
}