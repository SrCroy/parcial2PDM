package com.example.parcial2pdm;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parcial2pdm.adapters.ProductoAdapter;
import com.example.parcial2pdm.adapters.SucursalAdapter;
import com.example.parcial2pdm.arcore.ArInventarioManager;
import com.example.parcial2pdm.models.Productos;
import com.example.parcial2pdm.models.Sucursal;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;
import com.google.ar.core.Config;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.ux.ArFragment;
import com.gorisse.thomas.sceneform.ArSceneViewKt;
import com.gorisse.thomas.sceneform.light.LightEstimationConfig;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CamaraARFragment extends Fragment {
    private ArFragment arFragment;
    private ArInventarioManager arInventarioManager;
    private DatabaseReference productosRef;
    private DatabaseReference catalogoRef;
    private DatabaseReference sucursalesRef;
    private RecyclerView rvProductosParaPosicionar, rvSucursalesCercanas;
    private ProductoAdapter productoAdapter;
    private SucursalAdapter sucursalAdapter;
    private List<Productos> listaCatalogo;
    private List<Sucursal> listaSucursales;
    private TextView txtInstruccionesAr, txtTituloCatalogo;
    private MaterialButton btnEscanear, btnRegresar;
    private View panelInferiorAr;
    private String idSucursalActual = "";
    private String nombreSucursalActual = "";
    private Productos productoSeleccionadoParaMapear;
    private Location ultimaUbicacion;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean inventarioCargado = false;
    private boolean arConfigurado = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)) {
                    obtenerUbicacionYEscucharSucursales();
                }
            });

    public CamaraARFragment() {
    }

    public static CamaraARFragment newInstance(String idSucursal) {
        CamaraARFragment fragment = new CamaraARFragment();
        Bundle args = new Bundle();
        args.putString("SUCURSAL_ID", idSucursal);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            idSucursalActual = getArguments().getString("SUCURSAL_ID");
        }
        arInventarioManager = new ArInventarioManager();
        FirebaseDatabase db = FirebaseDatabase.getInstance("https://parcial2pdm-80efa-default-rtdb.firebaseio.com/");
        productosRef = db.getReference("productos_imagenes");
        catalogoRef = db.getReference("productos_imagenes");
        sucursalesRef = db.getReference("sucursales");
        
        listaCatalogo = new ArrayList<>();
        listaSucursales = new ArrayList<>();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_camara_a_r, container, false);
        
        rvProductosParaPosicionar = root.findViewById(R.id.rvProductosParaPosicionar);
        rvSucursalesCercanas = root.findViewById(R.id.rvSucursalesCercanas);
        txtInstruccionesAr = root.findViewById(R.id.txtInstruccionesAr);
        txtTituloCatalogo = root.findViewById(R.id.txtTituloCatalogo);
        btnEscanear = root.findViewById(R.id.btnEscanear);
        btnRegresar = root.findViewById(R.id.btnRegresar);
        panelInferiorAr = root.findViewById(R.id.panelInferiorAr);

        rvProductosParaPosicionar.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        // Iniciamos con las sucursales en vertical para que ocupen la pantalla
        rvSucursalesCercanas.setLayoutManager(new LinearLayoutManager(getContext()));

        productoAdapter = new ProductoAdapter(listaCatalogo, true, new ProductoAdapter.Listener() {
            @Override
            public void onSelect(Productos producto) {
                productoSeleccionadoParaMapear = producto;
                Toast.makeText(getContext(), "Producto: " + producto.getNombreProducto(), Toast.LENGTH_SHORT).show();
            }
            @Override public void onEdit(Productos p) {}
            @Override public void onDelete(Productos p) {}
        });
        rvProductosParaPosicionar.setAdapter(productoAdapter);

        sucursalAdapter = new SucursalAdapter(listaSucursales, new SucursalAdapter.Listener() {
            @Override public void onEdit(Sucursal s) {}
            @Override public void onDelete(Sucursal s) {}
            @Override public void onClick(Sucursal s) {
                seleccionarSucursal(s);
            }
        });
        rvSucursalesCercanas.setAdapter(sucursalAdapter);

        btnEscanear.setOnClickListener(v -> {
            if (arFragment != null) {
                arInventarioManager.limpiarEscena(arFragment);
            }
            inventarioCargado = false;
            Toast.makeText(getContext(), "Buscando productos guardados en esta área...", Toast.LENGTH_SHORT).show();
        });

        btnRegresar.setOnClickListener(v -> {
            regresarASucursales();
        });

        return root;
    }

    private void regresarASucursales() {
        this.idSucursalActual = "";
        this.nombreSucursalActual = "";
        this.inventarioCargado = false;
        
        if (arFragment != null) {
            arInventarioManager.limpiarEscena(arFragment);
            getChildFragmentManager().beginTransaction().remove(arFragment).commit();
            arFragment = null;
            arConfigurado = false;
        }

        View root = getView();
        if (root != null) {
            root.findViewById(R.id.ar_container).setVisibility(View.GONE);
            rvSucursalesCercanas.setVisibility(View.VISIBLE);
            btnEscanear.setVisibility(View.GONE);
            btnRegresar.setVisibility(View.GONE);
            panelInferiorAr.setVisibility(View.GONE);
        }
    }

    private void seleccionarSucursal(Sucursal s) {
        if (getView() == null) return;
        this.idSucursalActual = s.getId();
        this.nombreSucursalActual = s.getNombre();
        
        // Mostrar contenedor, botón de escanear, regresar y panel inferior
        View arContainer = getView().findViewById(R.id.ar_container);
        if (arContainer != null) arContainer.setVisibility(View.VISIBLE);
        rvSucursalesCercanas.setVisibility(View.GONE);
        btnEscanear.setVisibility(View.VISIBLE);
        btnRegresar.setVisibility(View.VISIBLE);
        panelInferiorAr.setVisibility(View.VISIBLE);
        
        if (arFragment == null) {
            arFragment = new ArFragment();
            getChildFragmentManager().beginTransaction()
                .replace(R.id.ar_container, arFragment)
                .commit();
            
            checkArSceneViewReady();
        } else {
            arInventarioManager.limpiarEscena(arFragment);
            // Si el fragmento ya existe pero estaba oculto/reutilizado, nos aseguramos de que el listener esté listo
            arConfigurado = false;
            checkArSceneViewReady();
        }

        inventarioCargado = false;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        verificarPermisos();
        cargarCatalogoProductos();

        // Ya no buscamos el fragmento aquí porque se cargará al hacer clic en una sucursal

        if (!idSucursalActual.isEmpty()) {
            rvSucursalesCercanas.setVisibility(View.GONE);
            // El flujo continuará en escucharSucursales cuando encuentre la coincidencia de ID
        } else {
            rvSucursalesCercanas.setVisibility(View.VISIBLE);
            panelInferiorAr.setVisibility(View.GONE);
            btnEscanear.setVisibility(View.GONE);
            btnRegresar.setVisibility(View.GONE);
        }
    }

    private void verificarPermisos() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionYEscucharSucursales();
        } else {
            requestPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
        }
    }

    @SuppressLint("MissingPermission")
    private void obtenerUbicacionYEscucharSucursales() {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    ultimaUbicacion = location;
                    escucharSucursales();
                });
    }

    private void escucharSucursales() {
        sucursalesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaSucursales.clear();
                Sucursal sucursalPreseleccionada = null;
                for (DataSnapshot data : snapshot.getChildren()) {
                    Sucursal s = data.getValue(Sucursal.class);
                    if (s != null) {
                        if (ultimaUbicacion != null) {
                            Location loc = new Location("");
                            loc.setLatitude(s.getLatitud());
                            loc.setLongitude(s.getLongitud());
                            s.setDistanciaMetros(ultimaUbicacion.distanceTo(loc));
                        }
                        listaSucursales.add(s);
                        if (!idSucursalActual.isEmpty() && s.getId().equals(idSucursalActual)) {
                            sucursalPreseleccionada = s;
                        }
                    }
                }
                Collections.sort(listaSucursales, (s1, s2) -> Float.compare(s1.getDistanciaMetros(), s2.getDistanciaMetros()));
                sucursalAdapter.notifyDataSetChanged();
                
                if (sucursalPreseleccionada != null && arFragment == null) {
                    seleccionarSucursal(sucursalPreseleccionada);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupArListeners() {
        if (arFragment.getArSceneView() != null) {
            // Esto soluciona el crash de LightEstimate al desactivarlo explícitamente en la vista
            ArSceneViewKt.setLightEstimationConfig(arFragment.getArSceneView(), LightEstimationConfig.DISABLED);
        }

        arFragment.setOnSessionConfigurationListener((session, config) -> {
            config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL);
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            config.setLightEstimationMode(Config.LightEstimationMode.DISABLED);
        });

        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            if (idSucursalActual.isEmpty()) {
                Toast.makeText(getContext(), "Primero selecciona una sucursal", Toast.LENGTH_SHORT).show();
                return;
            }
            if (productoSeleccionadoParaMapear != null) {
                productoSeleccionadoParaMapear.setIdSucursalAsignada(idSucursalActual);
                arInventarioManager.registrarPosicionEspacialProducto(getContext(), arFragment, hitResult, productoSeleccionadoParaMapear);
                productoSeleccionadoParaMapear = null; 
            } else {
                Toast.makeText(getContext(), "Selecciona un producto del catálogo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkArSceneViewReady() {
        if (arFragment != null && arFragment.getArSceneView() != null && arFragment.getArSceneView().getScene() != null) {
            if (!arConfigurado) {
                setupArListeners();
                arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
                    com.google.ar.core.Frame frame = arFragment.getArSceneView().getArFrame();
                    if (frame == null) return;
                    actualizarInstruccionesUX(frame);
                    if (!inventarioCargado && frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
                        inventarioCargado = true;
                        cargarProductosExistentes();
                    }
                });
                arConfigurado = true;
            }
        } else {
            mainHandler.postDelayed(this::checkArSceneViewReady, 200);
        }
    }

    private void actualizarInstruccionesUX(com.google.ar.core.Frame frame) {
        if (frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
            String prefix = nombreSucursalActual.isEmpty() ? "" : "[" + nombreSucursalActual + "] ";
            if (productoSeleccionadoParaMapear != null) {
                txtInstruccionesAr.setText(prefix + "Toca para poner " + productoSeleccionadoParaMapear.getNombreProducto());
            } else {
                txtInstruccionesAr.setText(prefix + "Busca superficies y selecciona un producto");
            }
        } else {
            txtInstruccionesAr.setText("Mueve el dispositivo para detectar el entorno...");
        }
    }

    private boolean catalogoListenerSet = false;
    private void cargarCatalogoProductos() {
        if (catalogoListenerSet) return;
        catalogoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaCatalogo.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Productos prod = data.getValue(Productos.class);
                    // Solo productos que no tienen posición asignada (están en el catálogo general)
                    if (prod != null && (prod.getPosicionX() == 0 && prod.getPosicionZ() == 0)) {
                        listaCatalogo.add(prod);
                    }
                }
                productoAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
        catalogoListenerSet = true;
    }

    private void cargarProductosExistentes() {
        if (idSucursalActual.isEmpty()) return;
        productosRef.orderByChild("idSucursalAsignada").equalTo(idSucursalActual)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int delay = 0;
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Productos prod = data.getValue(Productos.class);
                            if (prod != null && (prod.getPosicionX() != 0 || prod.getPosicionZ() != 0)) {
                                final int currentDelay = delay;
                                mainHandler.postDelayed(() -> {
                                    if (arFragment != null && arFragment.getContext() != null) {
                                        arInventarioManager.recrearProductoDesdeFirebase(arFragment, prod);
                                    }
                                }, currentDelay);
                                delay += 700; // Delay de 700ms entre cada objeto para que no aparezcan de golpe
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}
