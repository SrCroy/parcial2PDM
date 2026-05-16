package com.example.parcial2pdm;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.parcial2pdm.adapters.ProductoAdapter;
import com.example.parcial2pdm.models.Productos;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ProductoFragment extends Fragment {
    private ProductoAdapter productoAdapter;
    private Productos productosSeleccionado;
    private List<Productos> listaProductos;
    private RecyclerView rvProductos;
    private FloatingActionButton btnAgregar;
    private DatabaseReference mDatabase;
    private FirebaseStorage mStorage;

    private ImageView imgDialogPreview;
    private String rutaImagenSeleccionada = "";

    private String rutaCamara;
    private Uri uriImageCamara;


    private final ActivityResultLauncher<PickVisualMediaRequest> seleccionarImagen =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    if (uri != null && imgDialogPreview != null) {
                        String newRoute = copiarImagenApp(uri);
                        if (newRoute != null) {
                            rutaImagenSeleccionada = newRoute;
                            imgDialogPreview.setImageURI(Uri.fromFile(new File(newRoute)));
                        }
                    }
                }
            });

    private final ActivityResultLauncher<String> seleccionaImagenCamara2 =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean resultado) {
                    if (resultado) {
                        abrirCamara();
                    } else {
                        Toast.makeText(getContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private final ActivityResultLauncher<Uri> seleccionaImagenCamara =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean resultado) {
                    if (resultado && imgDialogPreview != null) {
                        rutaImagenSeleccionada = rutaCamara;
                        imgDialogPreview.setImageURI(uriImageCamara);
                    } else {
                        Toast.makeText(getContext(), "No se tomó la foto", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    public ProductoFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDatabase = FirebaseDatabase.getInstance().getReference("productos_imagenes");
        mStorage = FirebaseStorage.getInstance("gs://parcial2pdm-80efa.firebasestorage.app");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_producto, container, false);
        this.rvProductos = view.findViewById(R.id.rvProductos);
        this.btnAgregar = view.findViewById(R.id.btnAgregar);

        this.listaProductos = new ArrayList<>();

        productoAdapter = new ProductoAdapter(listaProductos, new ProductoAdapter.Listener() {
            @Override
            public void onSelect(Productos productos) {
                productosSeleccionado = productos;
                mostrarInformacion(productos);
            }

            @Override
            public void onEdit(Productos productos) {
                productosSeleccionado = productos;
                abrirFomulario(productos);
            }

            @Override
            public void onDelete(Productos productos) {
                productosSeleccionado = productos;
                eliminar(productos);
            }
        });

        rvProductos.setLayoutManager(new LinearLayoutManager(getContext()));
        rvProductos.setAdapter(productoAdapter);

        escucharBaseDeDatos();

        btnAgregar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                abrirFomulario(null);
            }
        });

        return view;
    }

    private void escucharBaseDeDatos() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaProductos.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Productos prod = dataSnapshot.getValue(Productos.class);
                    if (prod != null) {
                        listaProductos.add(prod);
                    }
                }

                if (getContext() != null) {
                    Log.d("FIREBASE_CHECK", "Productos recibidos: " + listaProductos.size());
                    if (listaProductos.isEmpty()) {
                        Toast.makeText(getContext(), "Conectado a Firebase, pero el nodo 'Productos' está vacío", Toast.LENGTH_SHORT).show();
                    }
                }
                productoAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void abrirFomulario(@Nullable Productos productoAEditar) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View viewDialog = LayoutInflater.from(getContext()).inflate(R.layout.dialog_formulario_producto, null);
        builder.setView(viewDialog);
        Button btnGaleria = viewDialog.findViewById(R.id.btnGaleria);
        Button btnCamara = viewDialog.findViewById(R.id.btnCamara);
        imgDialogPreview = viewDialog.findViewById(R.id.imgPreview);
        TextInputEditText etNombre = viewDialog.findViewById(R.id.etNombre);
        TextInputEditText etCategoria = viewDialog.findViewById(R.id.etCategoria);
        TextInputEditText etDescripcion = viewDialog.findViewById(R.id.etDescripcion);
        TextInputEditText etPrecio = viewDialog.findViewById(R.id.etPrecio);
        TextInputEditText etStock = viewDialog.findViewById(R.id.etStock);
        TextInputEditText etUbicacion = viewDialog.findViewById(R.id.etUbicacion);
        TextInputEditText etUrlModelo3D = viewDialog.findViewById(R.id.etUrlModelo3D);
        rutaImagenSeleccionada = "";
        btnGaleria.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                abrirGaleria();
            }
        });
        btnCamara.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validarPermisoCamara();
            }
        });

        boolean esEdicion = (productoAEditar != null);
        if (esEdicion) {
            builder.setTitle("Modificar Producto");
            etNombre.setText(productoAEditar.getNombreProducto());
            etCategoria.setText(productoAEditar.getCategoriaProducto());
            etDescripcion.setText(productoAEditar.getDescripcionProducto());
            etPrecio.setText(String.valueOf(productoAEditar.getPrecioProducto()));
            etStock.setText(String.valueOf(productoAEditar.getStockProducto()));
            etUbicacion.setText(productoAEditar.getUbicacionEstablecimiento());
            etUrlModelo3D.setText(productoAEditar.getUrlModelo3D());

            rutaImagenSeleccionada = productoAEditar.getUrlImagenReferencia();
            if (!TextUtils.isEmpty(rutaImagenSeleccionada)) {
                Glide.with(viewDialog.getContext())
                        .load(rutaImagenSeleccionada)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(imgDialogPreview);
            }
        } else {
            builder.setTitle("Registrar Nuevo Producto");
        }

        builder.setPositiveButton(esEdicion ? "Actualizar" : "Guardar", null);
        builder.setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nombre = etNombre.getText().toString().trim();
                String categoria = etCategoria.getText().toString().trim();
                String descripcion = etDescripcion.getText().toString().trim();
                String precioStr = etPrecio.getText().toString().trim();
                String stockStr = etStock.getText().toString().trim();
                String ubicacion = etUbicacion.getText().toString().trim();
                String urlModelo3D = etUrlModelo3D.getText().toString().trim();

                if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(categoria) || TextUtils.isEmpty(precioStr) || TextUtils.isEmpty(stockStr)) {
                    Toast.makeText(getContext(), "Por favor rellena los datos principales", Toast.LENGTH_SHORT).show();
                    return;
                }

                double precio = Double.parseDouble(precioStr);
                int stock = Integer.parseInt(stockStr);

                String id = esEdicion ? productoAEditar.getIdProducto() : mDatabase.push().getKey();

                if (id != null) {
                    if (!TextUtils.isEmpty(rutaImagenSeleccionada) && !rutaImagenSeleccionada.startsWith("http")) {
                        Toast.makeText(getContext(), "Subiendo imagen a la nube...", Toast.LENGTH_SHORT).show();
                        subirImagenAFirebaseStorage(id, nombre, categoria, descripcion, precio, stock, urlModelo3D, ubicacion, dialog);
                    } else {
                        Productos producto = new Productos(id, nombre, categoria, descripcion, precio, stock, rutaImagenSeleccionada, urlModelo3D, ubicacion, "", 0.0, 0.0, 0.0);
                        mDatabase.child(id).setValue(producto);
                        Toast.makeText(getContext(), esEdicion ? "Producto actualizado" : "Producto guardado", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }
            }
        });
    }

    private void subirImagenAFirebaseStorage(final String id, final String nombre, final String categoria, final String descripcion,
                                             final double precio, final int stock, final String url3D, final String ubicacion, final AlertDialog dialog) {
        File archivoLocal = new File(rutaImagenSeleccionada);

        Bitmap bitmap = BitmapFactory.decodeFile(archivoLocal.getAbsolutePath());
        if (bitmap == null) {
            Toast.makeText(getContext(), "Error al procesar archivo local", Toast.LENGTH_SHORT).show();
            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] datosImagen = baos.toByteArray();

        final StorageReference refStorage = mStorage.getReference().child("productos_imagenes/" + id + ".jpg");

        refStorage.putBytes(datosImagen)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        refStorage.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uriInternet) {
                                String urlRealDeInternet = uriInternet.toString();
                                Productos producto = new Productos(id, nombre, categoria, descripcion, precio, stock, urlRealDeInternet, url3D, ubicacion, "", 0.0, 0.0, 0.0);
                                mDatabase.child(id).setValue(producto);

                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "¡Producto registrado con éxito!", Toast.LENGTH_SHORT).show();
                                }
                                dialog.dismiss();
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error al subir foto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private String copiarImagenApp(Uri uriOriginal) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uriOriginal);
            String nombreArchivo = "IMG_" + System.currentTimeMillis() + ".jpg";
            File directorio = new File(requireContext().getFilesDir(), "imagenes");
            if (!directorio.exists()) {
                directorio.mkdirs();
            }

            File archivoDestino = new File(directorio, nombreArchivo);
            OutputStream outputStream = new FileOutputStream(archivoDestino);

            byte[] buffer = new byte[4096];
            int bytesLeidos;

            while ((bytesLeidos = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesLeidos);
            }

            inputStream.close();
            outputStream.close();

            return archivoDestino.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void abrirGaleria() {
        seleccionarImagen.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void validarPermisoCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
        } else {
            seleccionaImagenCamara2.launch(Manifest.permission.CAMERA);
        }
    }

    private void abrirCamara() {
        try {
            File archivoImagen = crearArchivoImagenCamara();
            rutaCamara = archivoImagen.getAbsolutePath();

            uriImageCamara = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".provider",
                    archivoImagen
            );

            seleccionaImagenCamara.launch(uriImageCamara);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File crearArchivoImagenCamara() throws IOException {
        String nombreArchivo = "CAM_" + System.currentTimeMillis() + ".jpg";
        File directorio = new File(requireContext().getFilesDir(), "imagenes");
        if (!directorio.exists()) {
            directorio.mkdirs();
        }
        return new File(directorio, nombreArchivo);
    }

    private void eliminar(Productos producto) {
        AlertDialog.Builder alert = new AlertDialog.Builder(requireContext());
        alert.setTitle("Eliminar Producto");
        alert.setMessage("¿Estás completamente seguro de eliminar a " + producto.getNombreProducto() + "?");

        alert.setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mDatabase.child(producto.getIdProducto()).removeValue();
                mStorage.getReference().child("productos_imagenes/" + producto.getIdProducto() + ".jpg").delete();
                Toast.makeText(getContext(), "Producto removido con éxito", Toast.LENGTH_SHORT).show();
            }
        });

        alert.setNegativeButton("Cancelar", null);
        alert.show();
    }

    private void mostrarInformacion(Productos producto) {
        AlertDialog.Builder info = new AlertDialog.Builder(requireContext());
        info.setTitle(producto.getNombreProducto());
        info.setMessage("Descripción: " + producto.getDescripcionProducto() + "\n" +
                "Ubicación en tienda: " + producto.getUbicacionEstablecimiento() + "\n" +
                "Enlace 3D: " + producto.getUrlModelo3D());
        info.setPositiveButton("Entendido", null);
        info.show();
    }
}