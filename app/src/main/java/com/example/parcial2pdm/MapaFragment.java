package com.example.parcial2pdm;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MapaFragment extends Fragment {

    private TextView txtLatitud, txtLongitud;
    private LinearLayout containerSucursales;
    private MaterialButton btnActualizarUbicacion;
    private FusedLocationProviderClient fusedLocationClient;

    //lista contenedora de nuestras sucursales mapeadas
    private List<SucursalData> listaSucursales;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                if (fineLocationGranted != null && fineLocationGranted) {
                    obtenerUbicacionActual();
                } else {
                    Toast.makeText(getContext(), "Se requieren permisos de ubicación", Toast.LENGTH_LONG).show();
                }
            });

    public MapaFragment() {
        //constructor vacio requerido
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mapa, container, false);

        txtLatitud = view.findViewById(R.id.txtLatitud);
        txtLongitud = view.findViewById(R.id.txtLongitud);
        containerSucursales = view.findViewById(R.id.containerSucursales);
        btnActualizarUbicacion = view.findViewById(R.id.btnActualizarUbicacion);

        //inicializar los objetos de las sucursales vinculando las vistas del XML
        inicializarSucursalesMapeadas(view);

        btnActualizarUbicacion.setOnClickListener(v -> verificarPermisosYObtenerUbicacion());

        verificarPermisosYObtenerUbicacion();

        return view;
    }

    private void inicializarSucursalesMapeadas(View view) {
        listaSucursales = new ArrayList<>();

        // 1.centro (Sivar)
        listaSucursales.add(new SucursalData("Sucursal Centro", 13.6929, -89.2182,
                view.findViewById(R.id.cardCentro),
                view.findViewById(R.id.txtDistanciaCentro),
                view.findViewById(R.id.txtEstadoCentro),
                view.findViewById(R.id.layoutLayoutCentro)));

        // 2.oriente principal (San Miguel Centro)
        listaSucursales.add(new SucursalData("Sucursal Oriente - Principal", 13.4833, -88.1833,
                view.findViewById(R.id.cardOrientePrincipal),
                view.findViewById(R.id.txtDistanciaOrientePrincipal),
                view.findViewById(R.id.txtEstadoOrientePrincipal),
                view.findViewById(R.id.layoutLayoutOrientePrincipal)));

        // 3.oriente norte (DMS convertida)
        listaSucursales.add(new SucursalData("Sucursal Oriente - Norte", 13.4621, -88.1649,
                view.findViewById(R.id.cardOrienteNorte),
                view.findViewById(R.id.txtDistanciaOrienteNorte),
                view.findViewById(R.id.txtEstadoOrienteNorte),
                view.findViewById(R.id.layoutLayoutOrienteNorte)));

        // 4.oriente sur (DMS convertida)
        listaSucursales.add(new SucursalData("Sucursal Oriente - Sur", 13.4399, -88.1575,
                view.findViewById(R.id.cardOrienteSur),
                view.findViewById(R.id.txtDistanciaOrienteSur),
                view.findViewById(R.id.txtEstadoOrienteSur),
                view.findViewById(R.id.layoutLayoutOrienteSur)));
    }

    private void verificarPermisosYObtenerUbicacion() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionActual();
        } else {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void obtenerUbicacionActual() {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double latitud = location.getLatitude();
                            double longitud = location.getLongitude();

                            txtLatitud.setText("Latitud: " + latitud);
                            txtLongitud.setText("Longitud: " + longitud);

                            procesarCercaniaYOrdenarVistas(location);
                        } else {
                            Toast.makeText(getContext(), "GPS no disponible temporalmente", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void procesarCercaniaYOrdenarVistas(Location ubicacionUsuario) {
        // 1.calcular las distancias en metros para cada sucursal
        for (SucursalData sucursal : listaSucursales) {
            Location locSucursal = new Location("");
            locSucursal.setLatitude(sucursal.latitud);
            locSucursal.setLongitude(sucursal.longitud);

            sucursal.distanciaMetros = ubicacionUsuario.distanceTo(locSucursal);
        }

        // 2.ordenar la lista de menor a mayor distancia usando un comparador
        listaSucursales.sort((s1, s2) -> Float.compare(s1.distanciaMetros, s2.distanciaMetros));

        // 3.remover físicamente todas las tarjetas del contenedor en la UI
        containerSucursales.removeAllViews();

        // 4.volver a agregarlas al contenedor ya ordenadas y aplicar los estilos
        for (int i = 0; i < listaSucursales.size(); i++) {
            SucursalData sucursal = listaSucursales.get(i);

            //convertir metros a kilómetros
            float kilometros = sucursal.distanciaMetros / 1000f;
            sucursal.txtDistancia.setText(String.format(Locale.US, "A %.2f km de ti", kilometros));

            //desprender de su padre anterior si es necesario e insertar al contenedor en orden
            if (sucursal.cardView.getParent() != null) {
                ((ViewGroup) sucursal.cardView.getParent()).removeView(sucursal.cardView);
            }
            containerSucursales.addView(sucursal.cardView);

            //reseteo de estilos base
            sucursal.layoutContenedorInterno.setBackgroundColor(Color.WHITE);

            //logica de estados: si esta a menos de 10km se asume que estas operando dentro de esa sucursal
            if (kilometros < 10.0) {
                sucursal.txtEstado.setText("🟢 Operando en esta área");
                sucursal.txtEstado.setTextColor(Color.parseColor("#388E3C")); // Verde
            } else {
                sucursal.txtEstado.setText("❌ Fuera de rango de la sucursal");
                sucursal.txtEstado.setTextColor(Color.parseColor("#757575")); // Gris oscuro
            }

            //resaltar visualmente la tarjeta que quedo en primer lugar (la mas cercana)
            if (i == 0) {
                sucursal.txtEstado.setText("⭐ SUCURSAL MÁS CERCANA");
                sucursal.txtEstado.setTextColor(Color.parseColor("#7B1FA2")); //morado oscuro
                sucursal.layoutContenedorInterno.setBackgroundColor(Color.parseColor("#F3E5F5")); //fondo lila suave
            }
        }
    }

    //estructura contenedora interna (modelo de datos local para el algoritmo de ordenamiento)
    private static class SucursalData {
        String nombre;
        double latitud;
        double longitud;
        float distanciaMetros;
        CardView cardView;
        TextView txtDistancia;
        TextView txtEstado;
        View layoutContenedorInterno;

        SucursalData(String nombre, double latitud, double longitud, CardView cardView, TextView txtDistancia, TextView txtEstado, View layoutContenedorInterno) {
            this.nombre = nombre;
            this.latitud = latitud;
            this.longitud = longitud;
            this.cardView = cardView;
            this.txtDistancia = txtDistancia;
            this.txtEstado = txtEstado;
            this.layoutContenedorInterno = layoutContenedorInterno;
        }
    }
}