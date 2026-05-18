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
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Trackable;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ArInventarioManager {

    private final DatabaseReference mDatabase;

    public ArInventarioManager() {
        // Unificamos con el nodo de productos que usas en el resto de la app
        this.mDatabase = FirebaseDatabase.getInstance().getReference("productos_imagenes");
    }

    /**
     * Captura el hitResult del toque en la pantalla física y posiciona el modelo.
     * Se usa el HitResult detectado por el ArFragment para mayor precisión.
     */
    public void registrarPosicionEspacialProducto(Context context, ArFragment arFragment,
                                                  HitResult hit, Productos productoActual) {
        if (arFragment == null || hit == null) return;

        // 1. Crear el ancla espacial fija en el entorno físico actual usando el HitResult
        Anchor anchor = hit.createAnchor();
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());

        // 2. Extraer las posiciones matemáticas X, Y, Z del plano detectado
        Pose pose = hit.getHitPose();
        double x = pose.tx();
        double y = pose.ty();
        double z = pose.tz();

        // 3. Actualizar la instancia local de tu modelo de datos
        productoActual.setPosicionX(x);
        productoActual.setPosicionY(y);
        productoActual.setPosicionZ(z);

        // 4. Persistir las nuevas coordenadas tridimensionales en Firebase en tiempo real
        mDatabase.child(productoActual.getIdProducto()).setValue(productoActual)
                .addOnSuccessListener(aVoid -> Toast.makeText(context,
                        "Ubicación de " + productoActual.getNombreProducto() + " guardada (X,Y,Z)",
                        Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(context,
                        "Error al guardar: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());

        // 5. Instanciar el nodo visual (modelo 3D + etiqueta) en la escena
        crearNodoVisual(arFragment, anchorNode, productoActual);
    }

    /**
     * Carga un producto ya existente recreando su posición espacial
     * a partir de las coordenadas X, Y, Z recuperadas de Firebase.
     */
    public void recrearProductoDesdeFirebase(ArFragment arFragment, Productos producto) {
        if (arFragment == null || arFragment.getArSceneView() == null || arFragment.getArSceneView().getSession() == null) return;

        // Creamos una Pose matemática usando las coordenadas guardadas en el objeto Productos
        Pose poseGuardada = new Pose(
                new float[]{(float) producto.getPosicionX(), (float) producto.getPosicionY(), (float) producto.getPosicionZ()},
                new float[]{0, 0, 0, 1} // Rotación neutra por defecto (Cuaternión)
        );

        // Creamos el ancla fija acoplada a la sesión espacial de ARCore a partir de esa pose
        Anchor anchor = arFragment.getArSceneView().getSession().createAnchor(poseGuardada);
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());

        // Instanciamos el modelo y la tarjeta encima del ancla recreada
        crearNodoVisual(arFragment, anchorNode, producto);
    }

    /**
     * Genera e integra los componentes 3D y 2D flotantes sobre el nodo de anclaje
     */
    private void crearNodoVisual(ArFragment arFragment, AnchorNode anchorNode, Productos producto) {
        // 1. Nodo interactivo para el objeto 3D transformable (rotar/escalar)
        TransformableNode nodoModelo = new TransformableNode(arFragment.getTransformationSystem());
        nodoModelo.setParent(anchorNode);
        nodoModelo.setLocalScale(new Vector3(0.5f, 0.5f, 0.5f)); // Escala base ajustable

        // Cargar el modelo .glb/.gltf desde la URL de Firebase Storage
        cargarModelo3DEnNodo(arFragment.getContext(), nodoModelo, producto.getUrlModelo3D());

        // 2. Nodo para la tarjeta o etiqueta informativa flotante (Requerimiento UX/UI)
        crearEtiquetaFlotanteInformativa(arFragment, anchorNode, producto);
    }

    /**
     * Carga de forma asíncrona el archivo 3D compatible con ARCore utilizando su URL remota
     */
    private void cargarModelo3DEnNodo(Context context, TransformableNode nodo, String urlModelo3D) {
        if (urlModelo3D == null || urlModelo3D.isEmpty()) {
            Toast.makeText(context, "URL del modelo 3D no disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        // Sceneform Maintained (com.gorisse.thomas.sceneform) maneja .glb/.gltf directamente
        // mediante el método setSource(context, Uri) si se habilita setIsFilamentGltf(true).
        ModelRenderable.builder()
                .setSource(context, Uri.parse(urlModelo3D))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(renderable -> {
                    nodo.setRenderable(renderable);
                    nodo.select();
                })
                .exceptionally(throwable -> {
                    // Log the error for debugging
                    android.util.Log.e("AR_ERROR", "Error loading model: " + throwable.getMessage(), throwable);
                    Toast.makeText(context, "Error al cargar el modelo: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                    return null;
                });
    }

    /**
     * Despliega un panel informativo (Card/Etiqueta) flotando arriba del producto en tiempo real
     */
    private void crearEtiquetaFlotanteInformativa(ArFragment arFragment, AnchorNode parentNode, Productos producto) {
        ViewRenderable.builder()
                .setView(arFragment.getContext(), R.layout.layout_tarjeta_ar_producto) // Inflamos el layout de la tarjeta
                .build()
                .thenAccept(viewRenderable -> {
                    // Creamos un nodo secundario posicionado ligeramente arriba del modelo 3D (60cm)
                    AnchorNode etiquetaNode = new AnchorNode();
                    etiquetaNode.setParent(parentNode);
                    etiquetaNode.setLocalPosition(new Vector3(0.0f, 0.6f, 0.0f));
                    etiquetaNode.setRenderable(viewRenderable);

                    // Mapeamos los datos en tiempo real de Firebase a las vistas de la tarjeta
                    View vistaInstanciada = viewRenderable.getView();
                    TextView txtNombre = vistaInstanciada.findViewById(R.id.txtArNombreProducto);
                    TextView txtPrecio = vistaInstanciada.findViewById(R.id.txtArPrecioProducto);
                    TextView txtStock = vistaInstanciada.findViewById(R.id.txtArStockProducto);

                    txtNombre.setText(producto.getNombreProducto());
                    txtPrecio.setText(String.format("$%.2f", producto.getPrecioProducto()));
                    txtStock.setText("Stock: " + producto.getStockProducto());
                })
                .exceptionally(throwable -> {
                    Toast.makeText(arFragment.getContext(), "Error al cargar etiqueta informativa", Toast.LENGTH_SHORT).show();
                    return null;
                });
    }
}