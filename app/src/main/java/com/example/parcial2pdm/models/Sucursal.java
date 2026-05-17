package com.example.parcial2pdm.models;

import com.google.firebase.database.Exclude;

public class Sucursal {
    private String id;
    private String nombre;
    private double latitud;
    private double longitud;
    private float distanciaMetros;

    // CRÍTICO: Firebase necesita este constructor vacío obligatoriamente
    public Sucursal() {
    }

    public Sucursal(String id, String nombre, double latitud, double longitud) {
        this.id = id;
        this.nombre = nombre;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }
    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }

    @Exclude
    public float getDistanciaMetros() { return distanciaMetros; }
    @Exclude
    public void setDistanciaMetros(float distanciaMetros) { this.distanciaMetros = distanciaMetros; }
}