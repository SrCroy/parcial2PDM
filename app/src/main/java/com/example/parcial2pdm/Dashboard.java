package com.example.parcial2pdm;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

// Importar Firebase Auth para el deslogueo
import com.google.firebase.auth.FirebaseAuth;

public class Dashboard extends AppCompatActivity {
    private BottomNavigationView btnMenu;
    private Toolbar toolbar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.inflateMenu(R.menu.toolbar_menu);
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == R.id.action_logout) {
                        cerrarSesion();
                        return true;
                    }
                    return false;
                }
            });
        }

        this.btnMenu = findViewById(R.id.btnMenu);
        if (savedInstanceState == null){
            cambiarFragment(new MapaFragment());
        }

        btnMenu.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                menuItem.setCheckable(true);
                if (menuItem.getItemId() == R.id.mapa){
                    cambiarFragment(new MapaFragment());
                    return true;
                } else if (menuItem.getItemId() == R.id.producto) {
                    cambiarFragment(new ProductoFragment());
                    return true;
                } else if (menuItem.getItemId() == R.id.inventario) {
                    cambiarFragment(new InventarioFragment());
                    return true;
                } else if (menuItem.getItemId() == R.id.camaraAr){
                    cambiarFragment(new CamaraARFragment());
                    return true;
                }
                return false;
            }
        });
    }

    private void cambiarFragment(Fragment fragment){
        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    private void cerrarSesion() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(Dashboard.this);
        alertDialog.setTitle("Cerrar sesion");
        alertDialog.setMessage("Estas seguro que quieres cerrar sesion");
        alertDialog.setPositiveButton("SI", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (mAuth != null) {
                    mAuth.signOut();
                    Toast.makeText(Dashboard.this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Dashboard.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });
        alertDialog.setNegativeButton("NO",  null);
        alertDialog.show();
    }
}