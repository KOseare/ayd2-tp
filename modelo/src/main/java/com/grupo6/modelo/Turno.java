package com.grupo6.modelo;

import java.sql.Timestamp;

public class Turno {

  private final int id;
  private final Cliente cliente;
  private final Timestamp registro;
  private EstadoTurno estado;
  private int nroLlamados;
  private String estacion; // Id de la estacion asignada al turno

  public Turno(int id, Cliente cliente, Timestamp registro) {
    this.id = id;
    this.cliente = cliente;
    this.registro = registro;
    this.estado = EstadoTurno.Esperando;
    this.nroLlamados = 0;
    this.estacion = null;
  }

  public int getId() {
    return id;
  }

  public Cliente getCliente() {
    return cliente;
  }

  public String getDni() {
    return cliente.getDni();
  }

  public Timestamp getRegistro() {
    return registro;
  }

  public EstadoTurno getEstado() {
    return estado;
  }

  public void setEstado(EstadoTurno estado) {
    this.estado = estado;
  }

  public int getNroLlamados() {
    return nroLlamados;
  }

  public void setNroLlamados(int nroLlamados) {
    this.nroLlamados = nroLlamados;
  }

  public void incrementNroLlamados() {
    nroLlamados++;
  }

  public String getEstacion() {
    return estacion;
  }

  public void setEstacion(String estacion) {
    this.estacion = estacion;
  }
}
