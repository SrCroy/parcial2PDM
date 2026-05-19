package com.example.parcial2pdm.arcore;

import android.content.Context;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.parcial2pdm.R;
import com.example.parcial2pdm.models.Productos;
import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.HitResult;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.collision.Box;
import com.google.ar.sceneform.collision.Sphere;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.DpToMetersViewSizer;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ArInventarioManager {

    private final DatabaseReference mDatabase;
    private Node nodoEtiquetaActivo = null;
    private boolean backgroundTapListenerSet = false;

    public ArInventarioManager() {
        this.mDatabase = FirebaseDatabase.getInstance().getReference("productos_imagenes");
    }

    /**
     * Registra un nuevo producto en la escena AR y en Firebase.
     */
    public void registrarPosicionEspacialProducto(Context context, ArFragment arFragment,
                                                  HitResult hit, Productos productoActual) {
        if (arFragment == null || hit == null) return;

        arFragment.getArSceneView().post(() -> {
            try {
                Anchor anchor = hit.createAnchor();
                AnchorNode anchorNode = new AnchorNode(anchor);
                anchorNode.setParent(arFragment.getArSceneView().getScene());

                Pose pose = hit.getHitPose();
                productoActual.setPosicionX(pose.tx());
                productoActual.setPosicionY(pose.ty());
                productoActual.setPosicionZ(pose.tz());

                mDatabase.child(productoActual.getIdProducto()).setValue(productoActual);

                crearNodoVisual(arFragment, anchorNode, productoActual);
                setupBackgroundTapListener(arFragment);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Recrea un producto existente desde los datos de Firebase.
     */
    public void recrearProductoDesdeFirebase(ArFragment arFragment, Productos producto) {
        if (arFragment == null || arFragment.getArSceneView() == null || arFragment.getArSceneView().getSession() == null) return;

        arFragment.getArSceneView().post(() -> {
            try {
                Pose poseGuardada = new Pose(
                        new float[]{(float) producto.getPosicionX(), (float) producto.getPosicionY(), (float) producto.getPosicionZ()},
                        new float[]{0, 0, 0, 1}
                );

                Anchor anchor = arFragment.getArSceneView().getSession().createAnchor(poseGuardada);
                AnchorNode anchorNode = new AnchorNode(anchor);
                anchorNode.setParent(arFragment.getArSceneView().getScene());

                crearNodoVisual(arFragment, anchorNode, producto);
                setupBackgroundTapListener(arFragment);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void setupBackgroundTapListener(ArFragment arFragment) {
        if (backgroundTapListenerSet || arFragment.getArSceneView() == null) return;
        
        arFragment.getArSceneView().getScene().addOnPeekTouchListener((hitTestResult, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                if (hitTestResult.getNode() == null) {
                    arFragment.getArSceneView().post(this::cerrarTarjetaActiva);
                } else if (arFragment.getContext() != null) {
                    // Depuración: Nos dirá qué nodo estamos tocando realmente
                    String nombre = hitTestResult.getNode().getName();
                    if (nombre != null && !nombre.isEmpty()) {
                        Toast.makeText(arFragment.getContext(), "Tocado: " + nombre, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        backgroundTapListenerSet = true;
    }

    private void crearNodoVisual(ArFragment arFragment, AnchorNode anchorNode, Productos producto) {
        TransformableNode nodoModelo = new TransformableNode(arFragment.getTransformationSystem());
        nodoModelo.setParent(anchorNode);
        nodoModelo.setName(producto.getNombreProducto()); // Asignamos nombre para el Toast de depuración
        
        // CONFIGURACIÓN DE TAMAÑO
        nodoModelo.setLocalScale(new Vector3(4.0f, 4.0f, 4.0f)); 
        nodoModelo.getScaleController().setMinScale(0.01f);
        nodoModelo.getScaleController().setMaxScale(25.0f);

        // Colisión balanceada: 0.25m local * escala 4 = 1 metro de área táctil real
        nodoModelo.setCollisionShape(new Sphere(0.25f));

        cargarModelo3DEnNodo(arFragment.getContext(), nodoModelo, producto.getUrlModelo3D());

        // Nodo para la tarjeta informativa (Hijo del modelo para que lo siga)
        Node etiquetaNode = new Node();
        etiquetaNode.setParent(nodoModelo);
        
        // Escala compensada (0.25 * 4 = 1.0 escala real)
        etiquetaNode.setLocalScale(new Vector3(0.25f, 0.25f, 0.25f));
        // Posición: 0.4m local * escala 4 = 1.6 metros de altura real (altura de ojos)
        etiquetaNode.setLocalPosition(new Vector3(0.0f, 0.4f, 0.0f)); 
        etiquetaNode.setEnabled(false);

        // Billboard: La tarjeta siempre mira al usuario
        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            if (etiquetaNode.isEnabled()) {
                Vector3 cameraPosition = arFragment.getArSceneView().getScene().getCamera().getWorldPosition();
                Vector3 nodePosition = etiquetaNode.getWorldPosition();
                Vector3 direction = Vector3.subtract(cameraPosition, nodePosition);
                if (direction.length() > 0.01f) {
                    etiquetaNode.setLookDirection(direction, Vector3.up());
                }
            }
        });

        crearEtiquetaFlotanteInformativa(arFragment, etiquetaNode, producto, anchorNode);

        // Al tocar el modelo 3D, mostramos la tarjeta
        nodoModelo.setOnTapListener((hitTestResult, motionEvent) -> {
            arFragment.getArSceneView().post(() -> {
                if (nodoEtiquetaActivo != null && nodoEtiquetaActivo != etiquetaNode) {
                    nodoEtiquetaActivo.setEnabled(false);
                }
                boolean nuevoEstado = !etiquetaNode.isEnabled();
                etiquetaNode.setEnabled(nuevoEstado);
                nodoEtiquetaActivo = nuevoEstado ? etiquetaNode : null;
                nodoModelo.select();
            });
        });
    }

    public void cerrarTarjetaActiva() {
        if (nodoEtiquetaActivo != null) {
            nodoEtiquetaActivo.setEnabled(false);
            nodoEtiquetaActivo = null;
        }
    }

    private void cargarModelo3DEnNodo(Context context, TransformableNode nodo, String urlModelo3D) {
        if (urlModelo3D == null || urlModelo3D.isEmpty()) return;

        ModelRenderable.builder()
                .setSource(context, Uri.parse(urlModelo3D))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(renderable -> {
                    nodo.setRenderable(renderable);
                    // Colisión de 1 metro real de diámetro
                    nodo.setCollisionShape(new Sphere(0.25f));
                    nodo.select();
                })
                .exceptionally(throwable -> {
                    Toast.makeText(context, "Error cargando modelo: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    return null;
                });
    }

    private void crearEtiquetaFlotanteInformativa(ArFragment arFragment, Node etiquetaNode, 
                                                   Productos producto, AnchorNode parentAnchor) {
        ViewRenderable.builder()
                .setView(arFragment.getContext(), R.layout.layout_tarjeta_ar_producto)
                .build()
                .thenAccept(viewRenderable -> {
                    viewRenderable.setShadowCaster(false);
                    viewRenderable.setShadowReceiver(false);
                    
                    // Aseguramos un tamaño físico real para que sea legible en el mundo AR (250 DP = 1 metro aprox)
                    viewRenderable.setSizer(new DpToMetersViewSizer(250));
                    
                    etiquetaNode.setRenderable(viewRenderable);
                    
                    View vista = viewRenderable.getView();
                    
                    ((TextView) vista.findViewById(R.id.txtArNombreProducto)).setText(producto.getNombreProducto());
                    ((TextView) vista.findViewById(R.id.txtArPrecioProducto)).setText(String.format("$%.2f", producto.getPrecioProducto()));
                    ((TextView) vista.findViewById(R.id.txtArDescripcionProducto)).setText(producto.getDescripcionProducto());
                    ((TextView) vista.findViewById(R.id.txtArStockProducto)).setText("Stock: " + producto.getStockProducto());
                    ((TextView) vista.findViewById(R.id.txtArUbicacionProducto)).setText("Ubicación: " + producto.getUbicacionEstablecimiento());

                    // Listener del botón cerrar (X)
                    vista.findViewById(R.id.btnCerrarTarjeta).setOnClickListener(v -> {
                        arFragment.getArSceneView().post(() -> {
                            etiquetaNode.setEnabled(false);
                            if (nodoEtiquetaActivo == etiquetaNode) {
                                nodoEtiquetaActivo = null;
                            }
                        });
                    });

                    // Botón Quitar del inventario
                    vista.findViewById(R.id.btnQuitarProducto).setOnClickListener(v -> {
                        arFragment.getArSceneView().post(() -> {
                            parentAnchor.setParent(null);
                            if (nodoEtiquetaActivo == etiquetaNode) {
                                nodoEtiquetaActivo = null;
                            }
                            
                            producto.setPosicionX(0);
                            producto.setPosicionY(0);
                            producto.setPosicionZ(0);
                            producto.setIdSucursalAsignada(""); 
                            
                            mDatabase.child(producto.getIdProducto()).setValue(producto)
                                    .addOnSuccessListener(aVoid -> {
                                        if (arFragment.getContext() != null) {
                                            Toast.makeText(arFragment.getContext(), 
                                                "Producto removido", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        });
                    });
                })
                .exceptionally(throwable -> {
                    if (arFragment.getContext() != null) {
                        Toast.makeText(arFragment.getContext(), "Error cargando UI AR", Toast.LENGTH_SHORT).show();
                    }
                    return null;
                });
    }
}
