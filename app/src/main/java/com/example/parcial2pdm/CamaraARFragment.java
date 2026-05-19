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
    private android.widget.TextView txtInstruccionesAr;
    private com.google.android.material.button.MaterialButton btnEscanear;

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
        txtInstruccionesAr = root.findViewById(R.id.txtInstruccionesAr);
        btnEscanear = root.findViewById(R.id.btnEscanear);

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

        btnEscanear.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Apunta la cámara a la imagen de referencia del producto", Toast.LENGTH_LONG).show();
        });

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
            
            // Habilitar base de datos de imágenes para el Escáner (Requerimiento)
            configurarAugmentedImages(session, config);

            // DESACTIVADO: La estimación de luz causa NoSuchMethodError en Sceneform 1.23.0 + ARCore 1.45+
            config.setLightEstimationMode(Config.LightEstimationMode.DISABLED);

            // Desactivar explícitamente la profundidad para evitar el error "spherical_rectifier" en Android 15
            if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                config.setDepthMode(Config.DepthMode.DISABLED);
            }

            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
        });

        // Click en planos detectados (Suelo o Paredes)
        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            if (productoSeleccionadoParaMapear != null) {
                productoSeleccionadoParaMapear.setIdSucursalAsignada(idSucursalActual);
                arInventarioManager.registrarPosicionEspacialProducto(
                        getContext(), arFragment, hitResult, productoSeleccionadoParaMapear);
                
                // Limpiar selección después de posicionar
                productoSeleccionadoParaMapear = null; 
            } else {
                Toast.makeText(getContext(), "Selecciona un producto del catálogo abajo.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configurarAugmentedImages(Session session, Config config) {
        // En una implementación real, aquí descargarías las imágenes de Firebase
        // y las añadirías a aid.addImage(nombre, bitmap).
        com.google.ar.core.AugmentedImageDatabase aid = new com.google.ar.core.AugmentedImageDatabase(session);
        config.setAugmentedImageDatabase(aid);
    }

    private void checkArSceneViewReady() {
        if (arFragment != null && arFragment.getArSceneView() != null && arFragment.getArSceneView().getScene() != null) {
            arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
                com.google.ar.core.Frame frame = arFragment.getArSceneView().getArFrame();
                if (frame == null) return;

                // Actualizar Instrucciones UX (Requerimiento)
                actualizarInstruccionesUX(frame);

                // Procesar Imágenes Escaneadas (Requerimiento Escáner)
                procesarAugmentedImages(frame);

                if (!inventarioCargado && frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
                    inventarioCargado = true;
                    cargarProductosExistentes();
                    arFragment.getArSceneView().getPlaneRenderer().setEnabled(true);
                }
            });
        } else {
            if (isAdded()) {
                mainHandler.postDelayed(this::checkArSceneViewReady, 200);
            }
        }
    }

    private void actualizarInstruccionesUX(com.google.ar.core.Frame frame) {
        if (frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
            if (productoSeleccionadoParaMapear != null) {
                txtInstruccionesAr.setText("Toca un plano para posicionar " + productoSeleccionadoParaMapear.getNombreProducto());
            } else {
                txtInstruccionesAr.setText("Superficie detectada. Selecciona un producto del catálogo.");
            }
        } else {
            txtInstruccionesAr.setText("Mueve el dispositivo para reconocer el entorno...");
        }
    }

    private void procesarAugmentedImages(com.google.ar.core.Frame frame) {
        java.util.Collection<com.google.ar.core.AugmentedImage> images = frame.getUpdatedTrackables(com.google.ar.core.AugmentedImage.class);
        for (com.google.ar.core.AugmentedImage image : images) {
            if (image.getTrackingState() == TrackingState.TRACKING) {
                // Lógica de escáner: Buscar si el nombre de la imagen coincide con un producto
                verificarProductoEscaneado(image.getName());
            }
        }
    }

    private void verificarProductoEscaneado(String nombreImagen) {
        // Buscamos en toda la base de datos si el producto existe y si está en esta sucursal
        productosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean encontradoEnSucursal = false;
                Productos productoEncontrado = null;

                for (DataSnapshot data : snapshot.getChildren()) {
                    Productos p = data.getValue(Productos.class);
                    if (p != null && p.getNombreProducto().equalsIgnoreCase(nombreImagen)) {
                        productoEncontrado = p;
                        if (p.getIdSucursalAsignada().equals(idSucursalActual)) {
                            encontradoEnSucursal = true;
                            break;
                        }
                    }
                }

                if (productoEncontrado != null) {
                    if (encontradoEnSucursal) {
                        Toast.makeText(getContext(), "Producto detectado: " + productoEncontrado.getNombreProducto() + " en esta sucursal.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "El producto " + productoEncontrado.getNombreProducto() + " NO está en esta sucursal.", Toast.LENGTH_LONG).show();
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void cargarCatalogoProductos() {
        catalogoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                listaCatalogo.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Productos prod = data.getValue(Productos.class);
                    // FILTRO: Solo mostrar productos que NO tengan posición asignada (Requerimiento)
                    if (prod != null && (prod.getPosicionX() == 0 && prod.getPosicionZ() == 0)) {
                        listaCatalogo.add(prod);
                    }
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
