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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.parcial2pdm.adapters.SucursalAdapter;
import com.example.parcial2pdm.models.Sucursal;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MapaFragment extends Fragment {

    private TextView txtLatitud, txtLongitud;
    private RecyclerView rvSucursales;
    private MaterialButton btnActualizarUbicacion;
    private FloatingActionButton btnAgregarSucursal;

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference mDatabase;

    private List<Sucursal> listaSucursales;
    private SucursalAdapter adapter;
    private Location ultimaUbicacionConocida;

    // Lanzador para solicitar permisos en tiempo de ejecución de forma segura
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                if (fineLocationGranted != null && fineLocationGranted) {
                    obtenerUbicacionActual();
                } else {
                    Toast.makeText(getContext(), "Se requieren permisos de ubicación para calcular cercanías", Toast.LENGTH_LONG).show();
                }
            });

    public MapaFragment() {
        // Constructor vacío obligatorio
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        // CORRECCIÓN DE ARQUITECTURA: Forzamos la referencia directa limpiando el búfer de conexiones anteriores
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://parcial2pdm-80efa-default-rtdb.firebaseio.com/");
        mDatabase = database.getReference("sucursales");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mapa, container, false);

        // Enlace de componentes del XML (fragment_mapa.xml)
        txtLatitud = view.findViewById(R.id.txtLatitud);
        txtLongitud = view.findViewById(R.id.txtLongitud);
        rvSucursales = view.findViewById(R.id.rvSucursales);
        btnActualizarUbicacion = view.findViewById(R.id.btnActualizarUbicacion);
        btnAgregarSucursal = view.findViewById(R.id.btnAgregarSucursal);

        // Configuración inicial del listado dinámico
        listaSucursales = new ArrayList<>();
        rvSucursales.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new SucursalAdapter(listaSucursales, new SucursalAdapter.Listener() {
            @Override
            public void onEdit(Sucursal sucursal) {
                abrirFormulario(sucursal);
            }

            @Override
            public void onDelete(Sucursal sucursal) {
                eliminarSucursal(sucursal);
            }
        });
        rvSucursales.setAdapter(adapter);

        // Activación del puente de escucha en tiempo real con Firebase
        escucharBaseDeDatos();

        // Asignación de oyentes de clics
        btnActualizarUbicacion.setOnClickListener(v -> verificarPermisosYObtenerUbicacion());
        btnAgregarSucursal.setOnClickListener(v -> abrirFormulario(null));

        // Intento de disparo inicial del GPS para poblar la ubicación actual
        verificarPermisosYObtenerUbicacion();

        return view;
    }

    private void escucharBaseDeDatos() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaSucursales.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Sucursal s = data.getValue(Sucursal.class);
                    if (s != null) {
                        listaSucursales.add(s);
                    }
                }
                if (ultimaUbicacionConocida != null) {
                    procesarCercaniaYOrdenar(ultimaUbicacionConocida);
                } else {
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // AGREGA ESTA LÍNEA DE LOG:
                android.util.Log.e("FIREBASE_ERROR", "Detalle del rechazo: " + error.getMessage() + " -> " + error.getDetails());

                Toast.makeText(getContext(), "Error de Firebase: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
        // Forzado de precisión alta omitiendo la memoria caché antigua del emulador
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            ultimaUbicacionConocida = location;
                            txtLatitud.setText("Latitud: " + location.getLatitude());
                            txtLongitud.setText("Longitud: " + location.getLongitude());
                            procesarCercaniaYOrdenar(location);
                        } else {
                            Toast.makeText(getContext(), "GPS inestable. Por favor, intente de nuevo.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void procesarCercaniaYOrdenar(Location ubicacionUsuario) {
        // Cálculo matemático lineal de distancias usando el hardware del dispositivo
        for (Sucursal sucursal : listaSucursales) {
            Location locSucursal = new Location("");
            locSucursal.setLatitude(sucursal.getLatitud());
            locSucursal.setLongitude(sucursal.getLongitud());
            sucursal.setDistanciaMetros(ubicacionUsuario.distanceTo(locSucursal));
        }

        // Ordenamiento por proximidad espacial en la UI (Posición 0 es la más cercana)
        Collections.sort(listaSucursales, new Comparator<Sucursal>() {
            @Override
            public int compare(Sucursal s1, Sucursal s2) {
                return Float.compare(s1.getDistanciaMetros(), s2.getDistanciaMetros());
            }
        });

        adapter.notifyDataSetChanged();
    }

    private void abrirFormulario(@Nullable Sucursal sucursalEditar) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(sucursalEditar == null ? "Nueva Sucursal" : "Editar Sucursal");

        // Construcción estructurada del contenedor del formulario flotante
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 30, 60, 30);

        final EditText etNombre = new EditText(getContext());
        etNombre.setHint("Nombre de la Sucursal");
        layout.addView(etNombre);

        final EditText etLat = new EditText(getContext());
        etLat.setHint("Latitud (Ej: 13.6929)");
        etLat.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        layout.addView(etLat);

        final EditText etLon = new EditText(getContext());
        etLon.setHint("Longitud (Ej: -89.2182)");
        etLon.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        layout.addView(etLon);

        // Inyección de datos previos si el flujo detecta una edición
        if (sucursalEditar != null) {
            etNombre.setText(sucursalEditar.getNombre());
            etLat.setText(String.valueOf(sucursalEditar.getLatitud()));
            etLon.setText(String.valueOf(sucursalEditar.getLongitud()));
        }

        builder.setView(layout);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nombre = etNombre.getText().toString().trim();
            String latStr = etLat.getText().toString().trim();
            String lonStr = etLon.getText().toString().trim();

            if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(latStr) || TextUtils.isEmpty(lonStr)) {
                Toast.makeText(getContext(), "Campos obligatorios vacíos", Toast.LENGTH_SHORT).show();
                return;
            }

            double lat = Double.parseDouble(latStr);
            double lon = Double.parseDouble(lonStr);

            // Generación de llave única persistente si es un registro nuevo
            String id = sucursalEditar != null ? sucursalEditar.getId() : mDatabase.push().getKey();
            if (id != null) {
                Sucursal nuevaSucursal = new Sucursal(id, nombre, lat, lon);
                mDatabase.child(id).setValue(nuevaSucursal)
                        .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Guardado correctamente", Toast.LENGTH_SHORT).show());
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void eliminarSucursal(Sucursal sucursal) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar Sucursal")
                .setMessage("¿Confirmas la eliminación permanente de " + sucursal.getNombre() + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    mDatabase.child(sucursal.getId()).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Eliminada con éxito", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}