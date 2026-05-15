package com.example.parcial2pdm;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextInputLayout tilEmail, tilPassword;
    private ProgressBar progressBar;
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

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);

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

    private final TextWatcher watcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable editable) {

        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            boolean habilitar = !TextUtils.isEmpty(email) && !TextUtils.isEmpty(password);
            btnLogin.setEnabled(habilitar);
            btnLogin.setAlpha(habilitar ? 1.0f : 0.5f);
        }

    };


    private void validarLogin(){
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        tilEmail.setError(null);
        tilPassword.setError(null);

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            tilEmail.setError("Correo electronico no vaido");
            etEmail.requestFocus();
            return;
        }

        if (password.length() > 6){
            tilPassword.setError("La contrasenia debe de tener al menos 6 caracteres");
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean exito = email.equals("admin@gmail.com") && password.equals("123456");
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                btnLogin.setAlpha(1.0f);

                if (exito) {
                    Intent intent = new Intent(MainActivity.this, Dashboard.class);
                    startActivity(intent);
                } else {
                    tilPassword.setError("Correo o contraseña incorrectos");
                    Toast.makeText(MainActivity.this, "Credenciales inválidas", Toast.LENGTH_LONG).show();
                }
            }
        }, 200);
    }
}