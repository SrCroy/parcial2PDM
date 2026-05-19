package com.example.parcial2pdm;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.parcial2pdm.models.Sucursal;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class MapaFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DatabaseReference mDatabase;
    private FusedLocationProviderClient fusedLocationClient;
    private Map<String, Marker> markersMap = new HashMap<>();

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                if (fineLocationGranted != null && fineLocationGranted) {
                    if (mMap != null) habilitarMiUbicacion();
                }
            });

    public MapaFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://parcial2pdm-80efa-default-rtdb.firebaseio.com/");
        mDatabase = database.getReference("sucursales");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mapa, container, false);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        view.findViewById(R.id.btnAgregarSucursal).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Mantén presionado en el mapa para agregar una sucursal", Toast.LENGTH_LONG).show();
        });

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        verificarPermisosYConfigurarMapa();
        escucharSucursales();

        mMap.setOnMapLongClickListener(latLng -> abrirFormulario(null, latLng));

        mMap.setOnMarkerClickListener(marker -> {
            Sucursal sucursal = (Sucursal) marker.getTag();
            if (sucursal != null) {
                mostrarOpcionesSucursal(sucursal);
            }
            return false;
        });
    }

    private void verificarPermisosYConfigurarMapa() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            habilitarMiUbicacion();
        } else {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void habilitarMiUbicacion() {
        if (mMap != null) {
            mMap.setMyLocationEnabled(true);
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            LatLng miPos = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(miPos, 15));
                        }
                    });
        }
    }

    private void escucharSucursales() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (Marker marker : markersMap.values()) {
                    marker.remove();
                }
                markersMap.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Sucursal s = data.getValue(Sucursal.class);
                    if (s != null) {
                        LatLng pos = new LatLng(s.getLatitud(), s.getLongitud());
                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(pos)
                                .title(s.getNombre()));
                        if (marker != null) {
                            marker.setTag(s);
                            markersMap.put(s.getId(), marker);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarOpcionesSucursal(Sucursal sucursal) {
        String[] opciones = {"Editar", "Eliminar", "Ver en AR"};
        new AlertDialog.Builder(requireContext())
                .setTitle(sucursal.getNombre())
                .setItems(opciones, (dialog, which) -> {
                    switch (which) {
                        case 0: abrirFormulario(sucursal, new LatLng(sucursal.getLatitud(), sucursal.getLongitud())); break;
                        case 1: eliminarSucursal(sucursal); break;
                        case 2: abrirAR(sucursal); break;
                    }
                })
                .show();
    }

    private void abrirAR(Sucursal sucursal) {
        if (getActivity() instanceof Dashboard) {
            CamaraARFragment fragment = CamaraARFragment.newInstance(sucursal.getId());
            ((Dashboard) getActivity()).getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void abrirFormulario(@Nullable Sucursal sucursalEditar, LatLng latLng) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(sucursalEditar == null ? "Nueva Sucursal" : "Editar Sucursal");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 30, 60, 30);

        final EditText etNombre = new EditText(getContext());
        etNombre.setHint("Nombre de la Sucursal");
        if (sucursalEditar != null) etNombre.setText(sucursalEditar.getNombre());
        layout.addView(etNombre);

        builder.setView(layout);
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nombre = etNombre.getText().toString().trim();
            if (TextUtils.isEmpty(nombre)) {
                Toast.makeText(getContext(), "Nombre obligatorio", Toast.LENGTH_SHORT).show();
                return;
            }

            String id = sucursalEditar != null ? sucursalEditar.getId() : mDatabase.push().getKey();
            if (id != null) {
                Sucursal nueva = new Sucursal(id, nombre, latLng.latitude, latLng.longitude);
                mDatabase.child(id).setValue(nueva)
                        .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Guardado", Toast.LENGTH_SHORT).show());
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void eliminarSucursal(Sucursal sucursal) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar")
                .setMessage("¿Eliminar " + sucursal.getNombre() + "?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    mDatabase.child(sucursal.getId()).removeValue();
                })
                .setNegativeButton("No", null)
                .show();
    }
}
