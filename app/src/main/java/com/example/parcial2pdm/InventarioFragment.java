package com.example.parcial2pdm;

import android.os.Bundle;
import android.util.Log;
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
import com.example.parcial2pdm.models.Productos;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class InventarioFragment extends Fragment {

    private RecyclerView rvInventario;
    private ProductoAdapter adapter;
    private List<Productos> listaProductos;
    private DatabaseReference mDatabase;

    public InventarioFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDatabase = FirebaseDatabase.getInstance().getReference("productos_imagenes");
        listaProductos = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inventario, container, false);
        rvInventario = view.findViewById(R.id.rvInventario);
        rvInventario.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new ProductoAdapter(listaProductos, new ProductoAdapter.Listener() {
            @Override
            public void onSelect(Productos productos) {
                // Podría mostrar detalle o permitir edición rápida de stock
                Toast.makeText(getContext(), "Producto: " + productos.getNombreProducto(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onEdit(Productos productos) {
                // Redirigir o abrir formulario de edición (se puede compartir lógica con ProductoFragment)
            }

            @Override
            public void onDelete(Productos productos) {
            }
        });
        
        rvInventario.setAdapter(adapter);
        escucharCambiosInventario();
        
        return view;
    }

    private void escucharCambiosInventario() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaProductos.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Productos p = data.getValue(Productos.class);
                    if (p != null) {
                        listaProductos.add(p);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("InventarioFragment", "Error: " + error.getMessage());
            }
        });
    }
}