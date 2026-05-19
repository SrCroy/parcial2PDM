package com.example.parcial2pdm.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.parcial2pdm.R;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class Modelo3DAdapter extends RecyclerView.Adapter<Modelo3DAdapter.ViewHolder> {

    private final List<StorageReference> listaModelos;
    private final List<StorageReference> todosLosArchivos;
    private final OnModeloClickListener listener;

    public interface OnModeloClickListener {
        void onModeloClick(StorageReference modelRef);
    }

    public Modelo3DAdapter(List<StorageReference> listaModelos, List<StorageReference> todosLosArchivos, OnModeloClickListener listener) {
        this.listaModelos = listaModelos;
        this.todosLosArchivos = todosLosArchivos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_seleccionar_modelo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StorageReference modelRef = listaModelos.get(position);
        String nombreArchivoModelo = modelRef.getName();
        holder.txtNombre.setText(nombreArchivoModelo.replace(".glb", "").replace(".GLB", ""));

        // Limpiamos la imagen con el icono que te gusta para que no se vea gris vacío
        holder.imgPreview.setImageResource(android.R.drawable.ic_menu_gallery);

        // Sacamos el nombre base (ej: "drill" de "Drill.glb")
        String baseName = nombreArchivoModelo;
        if (baseName.contains(".")) {
            baseName = baseName.substring(0, baseName.lastIndexOf(".")).toLowerCase().trim();
        }

        // Buscamos la imagen en la lista de archivos que ya descargamos
        StorageReference imgRef = null;
        for (StorageReference ref : todosLosArchivos) {
            String fileName = ref.getName().toLowerCase();
            // Verificamos que el nombre coincida y que sea una imagen
            if (fileName.startsWith(baseName) && (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".webp"))) {
                imgRef = ref;
                break;
            }
        }

        if (imgRef != null) {
            imgRef.getDownloadUrl().addOnSuccessListener(uri -> {
                Glide.with(holder.itemView.getContext())
                        .load(uri)
                        .centerCrop() // Esto hace que la imagen llene todo el cuadro y se vea real
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .into(holder.imgPreview);
            });
        }

        holder.itemView.setOnClickListener(v -> listener.onModeloClick(modelRef));
    }

    @Override
    public int getItemCount() {
        return listaModelos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPreview;
        TextView txtNombre;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPreview = itemView.findViewById(R.id.imgModeloPreview);
            txtNombre = itemView.findViewById(R.id.txtNombreModelo);
        }
    }
}
