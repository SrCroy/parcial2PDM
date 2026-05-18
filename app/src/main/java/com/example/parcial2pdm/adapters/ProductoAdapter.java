package com.example.parcial2pdm.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.parcial2pdm.R;
import com.example.parcial2pdm.models.Productos;

import java.util.List;
import java.util.Locale;

public class ProductoAdapter extends RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder> {
    private List<Productos> listaProducto;
    private Listener listener;
    private boolean esModoHorizontal = false;

    public ProductoAdapter(List<Productos> listaProducto, Listener listener) {
        this.listaProducto = listaProducto;
        this.listener = listener;
    }

    public ProductoAdapter(List<Productos> listaProducto, boolean esModoHorizontal, Listener listener) {
        this.listaProducto = listaProducto;
        this.esModoHorizontal = esModoHorizontal;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductoAdapter.ProductoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = esModoHorizontal ? R.layout.item_producto_ar_horizontal : R.layout.productos_items;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ProductoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductoAdapter.ProductoViewHolder holder, int position) {
        Productos productos = listaProducto.get(position);

        Glide.with(holder.itemView.getContext())
                .load(productos.getUrlImagenReferencia())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.stat_notify_error)
                .into(holder.imgProducto);

        holder.txtNombre.setText(productos.getNombreProducto());
        if (holder.txtCategoria != null) holder.txtCategoria.setText(productos.getCategoriaProducto());
        holder.txtPrecio.setText(String.format(Locale.US, "$%.2f", productos.getPrecioProducto()));
        if (holder.txtStock != null) holder.txtStock.setText("Stock: " + productos.getStockProducto());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onSelect(productos);
            }
        });

        if (holder.btnEditar != null) {
            holder.btnEditar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onEdit(productos);
                }
            });
        }

        if (holder.btnEliminar != null) {
            holder.btnEliminar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onDelete(productos);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return listaProducto.size();
    }

    public class ProductoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProducto;
        TextView txtNombre, txtCategoria, txtPrecio, txtStock;
        ImageButton btnEditar, btnEliminar;
        public ProductoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProducto = itemView.findViewById(R.id.imgProducto);
            txtNombre = itemView.findViewById(R.id.txtNombre);
            txtCategoria = itemView.findViewById(R.id.txtCategoria);
            txtPrecio = itemView.findViewById(R.id.txtPrecio);
            txtStock = itemView.findViewById(R.id.txtStock);
            btnEditar = itemView.findViewById(R.id.btnEditar);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }

    public interface Listener {
        void onSelect(Productos productos);
        void onEdit(Productos productos);
        void onDelete(Productos productos);
    }
}