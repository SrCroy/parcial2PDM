package com.example.parcial2pdm.models;

public class Productos {
    private String idProducto;
    private String nombreProducto;
    private String categoriaProducto;
    private String descripcionProducto;
    private double precioProducto;
    private int stockProducto;
    private String urlImagenReferencia;
    private String urlModelo3D;
    private String ubicacionEstablecimiento;
    private String idSucursalAsignada;
    private double posicionX;
    private double posicionY;
    private double posicionZ;

    public Productos() {
    }

    public Productos(String idProducto, String nombreProducto, String categoriaProducto,
                     String descripcionProducto, double precioProducto, int stockProducto,
                     String urlImagenReferencia, String urlModelo3D, String ubicacionEstablecimiento,
                     String idSucursalAsignada, double posicionX, double posicionY, double posicionZ) {
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.categoriaProducto = categoriaProducto;
        this.descripcionProducto = descripcionProducto;
        this.precioProducto = precioProducto;
        this.stockProducto = stockProducto;
        this.urlImagenReferencia = urlImagenReferencia;
        this.urlModelo3D = urlModelo3D;
        this.ubicacionEstablecimiento = ubicacionEstablecimiento;
        this.idSucursalAsignada = idSucursalAsignada;
        this.posicionX = posicionX;
        this.posicionY = posicionY;
        this.posicionZ = posicionZ;
    }

    public String getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(String idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public String getCategoriaProducto() {
        return categoriaProducto;
    }

    public void setCategoriaProducto(String categoriaProducto) {
        this.categoriaProducto = categoriaProducto;
    }

    public String getDescripcionProducto() {
        return descripcionProducto;
    }

    public void setDescripcionProducto(String descripcionProducto) {
        this.descripcionProducto = descripcionProducto;
    }

    public double getPrecioProducto() {
        return precioProducto;
    }

    public void setPrecioProducto(double precioProducto) {
        this.precioProducto = precioProducto;
    }

    public int getStockProducto() {
        return stockProducto;
    }

    public void setStockProducto(int stockProducto) {
        this.stockProducto = stockProducto;
    }

    public String getUrlImagenReferencia() {
        return urlImagenReferencia;
    }

    public void setUrlImagenReferencia(String urlImagenReferencia) {
        this.urlImagenReferencia = urlImagenReferencia;
    }

    public String getUrlModelo3D() {
        return urlModelo3D;
    }

    public void setUrlModelo3D(String urlModelo3D) {
        this.urlModelo3D = urlModelo3D;
    }

    public String getUbicacionEstablecimiento() {
        return ubicacionEstablecimiento;
    }

    public void setUbicacionEstablecimiento(String ubicacionEstablecimiento) {
        this.ubicacionEstablecimiento = ubicacionEstablecimiento;
    }

    public String getIdSucursalAsignada() {
        return idSucursalAsignada;
    }

    public void setIdSucursalAsignada(String idSucursalAsignada) {
        this.idSucursalAsignada = idSucursalAsignada;
    }

    public double getPosicionX() {
        return posicionX;
    }

    public void setPosicionX(double posicionX) {
        this.posicionX = posicionX;
    }

    public double getPosicionY() {
        return posicionY;
    }

    public void setPosicionY(double posicionY) {
        this.posicionY = posicionY;
    }

    public double getPosicionZ() {
        return posicionZ;
    }

    public void setPosicionZ(double posicionZ) {
        this.posicionZ = posicionZ;
    }
}