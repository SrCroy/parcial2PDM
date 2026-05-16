package com.example.parcial2pdm;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;

public class MapaFragment extends Fragment {

    private TextView txtLatitud, txtLongitud, txtSucursal;
    private MaterialButton btnActualizarUbicacion;
    private FusedLocationProviderClient fusedLocationClient;

    //launcher para solicitar permisos de ubi al usuario
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if (fineLocationGranted != null && fineLocationGranted) {
                    //permiso concedido, procedemos a obtener la ubi
                    obtenerUbicacionActual();
                } else {
                    Toast.makeText(getContext(), "Se requieren permisos de ubicación para esta función", Toast.LENGTH_LONG).show();
                    txtSucursal.setText("Permiso Denegado");
                }
            });

    public MapaFragment() {
        // constructor vacio requerido
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // inicializa el cliente de ubicacion de Google Play Services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mapa, container, false);

        txtLatitud = view.findViewById(R.id.txtLatitud);
        txtLongitud = view.findViewById(R.id.txtLongitud);
        txtSucursal = view.findViewById(R.id.txtSucursal);
        btnActualizarUbicacion = view.findViewById(R.id.btnActualizarUbicacion);

        btnActualizarUbicacion.setOnClickListener(v -> verificarPermisosYObtenerUbicacion());

        // intentar obtener la ubi nada mas abrir el fragmento
        verificarPermisosYObtenerUbicacion();

        return view;
    }

    private void verificarPermisosYObtenerUbicacion() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionActual();
        } else {
            // pedimos el permiso en pantalla
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    @SuppressLint("MissingPermission") // se suprime la advertencia porque ya verificamos el permiso arriba
    private void obtenerUbicacionActual() {
        txtSucursal.setText("Calculando...");

        // MODIFICACION: forzar la peticion directa al hardware GPS omitiendo la cache antigua
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double latitud = location.getLatitude();
                            double longitud = location.getLongitude();

                            txtLatitud.setText("Latitud: " + latitud);
                            txtLongitud.setText("Longitud: " + longitud);

                            identificarSucursalContextual(latitud, longitud);
                        } else {
                            Toast.makeText(getContext(), "No se pudo obtener la ubicación exacta. Intenta de nuevo.", Toast.LENGTH_SHORT).show();
                            txtSucursal.setText("GPS no disponible");
                        }
                    }
                });
    }

    private void identificarSucursalContextual(double latUsuario, double lonUsuario) {
        //coordenadas de prueba (San Salvador y San Miguel)
        Location sucursalCentro = new Location("");
        sucursalCentro.setLatitude(13.6929);
        sucursalCentro.setLongitude(-89.2182);

        Location sucursalOriente = new Location("");
        sucursalOriente.setLatitude(13.4833);
        sucursalOriente.setLongitude(-88.1833);

        Location ubicacionUsuario = new Location("");
        ubicacionUsuario.setLatitude(latUsuario);
        ubicacionUsuario.setLongitude(lonUsuario);

        //calcular distancia en metros
        float distanciaCentro = ubicacionUsuario.distanceTo(sucursalCentro);
        float distanciaOriente = ubicacionUsuario.distanceTo(sucursalOriente);

        // logicc de decision: si está a menos de 10km de la sucursal
        if (distanciaCentro < 10000) {
            txtSucursal.setText("🏢 Sucursal Centro (Activa)");
        } else if (distanciaOriente < 10000) {
            txtSucursal.setText("🏢 Sucursal Oriente (Activa)");
        } else {
            txtSucursal.setText("⚠️(Fuera de Sucursal)");
        }
    }
}