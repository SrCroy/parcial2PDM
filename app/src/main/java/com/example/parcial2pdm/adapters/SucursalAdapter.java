package com.example.parcial2pdm.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parcial2pdm.R;
import com.example.parcial2pdm.models.Sucursal;

import java.util.List;
import java.util.Locale;

public class SucursalAdapter extends RecyclerView.Adapter<SucursalAdapter.SucursalViewHolder> {

    private List<Sucursal> listaSucursales;
    private Listener listener;

    public SucursalAdapter(List<Sucursal> listaSucursales, Listener listener) {
        this.listaSucursales = listaSucursales;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SucursalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sucursales_items, parent, false);
        return new SucursalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SucursalViewHolder holder, int position) {
        Sucursal sucursal = listaSucursales.get(position);

        holder.txtNombre.setText(sucursal.getNombre());

        float kilometros = sucursal.getDistanciaMetros() / 1000f;
        holder.txtDistancia.setText(String.format(Locale.US, "A %.2f km de ti", kilometros));

        holder.layoutContenedor.setBackgroundColor(Color.WHITE);

        if (kilometros < 10.0) {
            if (position == 0) {
                holder.txtEstado.setText("⭐ SUCURSAL MÁS CERCANA");
                holder.txtEstado.setTextColor(Color.parseColor("#7B1FA2"));
                holder.layoutContenedor.setBackgroundColor(Color.parseColor("#F3E5F5"));
            } else {
                holder.txtEstado.setText("🟢 Operando en esta área");
                holder.txtEstado.setTextColor(Color.parseColor("#388E3C"));
            }
        } else {
            holder.txtEstado.setText("❌ Fuera de rango de la sucursal");
            holder.txtEstado.setTextColor(Color.parseColor("#757575"));
        }

        holder.btnEditar.setOnClickListener(v -> listener.onEdit(sucursal));
        holder.btnEliminar.setOnClickListener(v -> listener.onDelete(sucursal));
    }

    @Override
    public int getItemCount() {
        return listaSucursales.size();
    }

    public class SucursalViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre, txtDistancia, txtEstado;
        ImageButton btnEditar, btnEliminar;
        LinearLayout layoutContenedor;

        public SucursalViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNombre = itemView.findViewById(R.id.txtNombreSucursal);
            txtDistancia = itemView.findViewById(R.id.txtDistanciaSucursal);
            txtEstado = itemView.findViewById(R.id.txtEstadoSucursal);
            btnEditar = itemView.findViewById(R.id.btnEditarSucursal);
            btnEliminar = itemView.findViewById(R.id.btnEliminarSucursal);
            layoutContenedor = itemView.findViewById(R.id.layoutLayoutSucursal);
        }
    }

    public interface Listener {
        void onEdit(Sucursal sucursal);
        void onDelete(Sucursal sucursal);
    }
}