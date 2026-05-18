package com.example.parcial2pdm;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parcial2pdm.adapters.ProductoAdapter;
import com.example.parcial2pdm.arcore.ArInventarioManager;
import com.example.parcial2pdm.models.Productos;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CamaraARFragment extends Fragment {

    private ArFragment arFragment;
    private ArInventarioManager arInventarioManager;
    private DatabaseReference productosRef;
    private DatabaseReference catalogoRef;

    private RecyclerView rvProductosParaPosicionar;
    private ProductoAdapter productoAdapter;
    private List<Productos> listaCatalogo;

    private String idSucursalActual = ""; 
    private Productos productoSeleccionadoParaMapear; 

    private boolean inventarioCargado = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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
        productosRef = FirebaseDatabase.getInstance().getReference("productos_imagenes");
        catalogoRef = FirebaseDatabase.getInstance().getReference("productos_imagenes");
        listaCatalogo = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_camara_a_r, container, false);
        
        rvProductosParaPosicionar = root.findViewById(R.id.rvProductosParaPosicionar);
        rvProductosParaPosicionar.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        
        productoAdapter = new ProductoAdapter(listaCatalogo, true, new ProductoAdapter.Listener() {
            @Override
            public void onSelect(Productos producto) {
                setProductoSeleccionadoParaMapear(producto);
            }
            @Override public void onEdit(Productos p) {}
            @Override public void onDelete(Productos p) {}
        });
        rvProductosParaPosicionar.setAdapter(productoAdapter);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cargarCatalogoProductos();

        // Obtener el fragmento de ARCore
        arFragment = (ArFragment) getChildFragmentManager().findFragmentById(R.id.ux_fragment_embed);

        if (arFragment != null) {
            setupArListeners();
            checkArSceneViewReady();
        }
    }

    private void setupArListeners() {
        // Configuración de la sesión
        arFragment.setOnSessionConfigurationListener((session, config) -> {
            config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL);
            config.setFocusMode(Config.FocusMode.AUTO);
            
            // DESACTIVADO: La estimación de luz causa NoSuchMethodError en Sceneform 1.23.0 + ARCore 1.45+
            config.setLightEstimationMode(Config.LightEstimationMode.DISABLED);

            // Desactivar explícitamente la profundidad para evitar el error "spherical_rectifier" en Android 15
            if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                config.setDepthMode(Config.DepthMode.DISABLED);
            }

            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
        });

        // Click en planos detectados
        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            if (productoSeleccionadoParaMapear != null) {
                productoSeleccionadoParaMapear.setIdSucursalAsignada(idSucursalActual);
                arInventarioManager.registrarPosicionEspacialProducto(
                        getContext(), arFragment, hitResult, productoSeleccionadoParaMapear);
            } else {
                Toast.makeText(getContext(), "Selecciona un producto primero.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkArSceneViewReady() {
        if (arFragment != null && arFragment.getArSceneView() != null && arFragment.getArSceneView().getScene() != null) {
            // Ya está listo, agregar el listener de actualización
            arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
                Session session = arFragment.getArSceneView().getSession();
                if (session != null && !inventarioCargado) {
                    if (arFragment.getArSceneView().getArFrame() != null &&
                            arFragment.getArSceneView().getArFrame().getCamera().getTrackingState() == TrackingState.TRACKING) {
                        inventarioCargado = true;
                        cargarProductosExistentes();
                        
                        // Mostrar visualmente que se están buscando planos
                        arFragment.getArSceneView().getPlaneRenderer().setEnabled(true);
                        arFragment.getArSceneView().getPlaneRenderer().setVisible(true);
                    }
                }
            });
        } else {
            // Si no está listo, reintentar en 200ms
            if (isAdded()) {
                mainHandler.postDelayed(this::checkArSceneViewReady, 200);
            }
        }
    }

    private void cargarCatalogoProductos() {
        catalogoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                listaCatalogo.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Productos prod = data.getValue(Productos.class);
                    if (prod != null) listaCatalogo.add(prod);
                }
                productoAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void cargarProductosExistentes() {
        if (idSucursalActual.isEmpty()) return;
        
        productosRef.orderByChild("idSucursalAsignada").equalTo(idSucursalActual)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded() || arFragment == null) return;
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Productos prod = data.getValue(Productos.class);
                            if (prod != null && (prod.getPosicionX() != 0 || prod.getPosicionZ() != 0)) {
                                arInventarioManager.recrearProductoDesdeFirebase(arFragment, prod);
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    public void setProductoSeleccionadoParaMapear(Productos producto) {
        this.productoSeleccionadoParaMapear = producto;
        if (getContext() != null) {
            Toast.makeText(getContext(), "Producto seleccionado: " + producto.getNombreProducto(), Toast.LENGTH_SHORT).show();
        }
    }
}
