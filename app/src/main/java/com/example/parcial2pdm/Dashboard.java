package com.example.parcial2pdm;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class Dashboard extends AppCompatActivity {
    private BottomNavigationView btnMenu;
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
}