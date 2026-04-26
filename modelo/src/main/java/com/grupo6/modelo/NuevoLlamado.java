package com.grupo6.modelo;

import java.util.Optional;

public class NuevoLlamado {

  public enum Tipo {
    /** Fila vacia, no hay nadie en la estacion. */
    SIN_PENDIENTES,
    /** Fila vacia, el cliente actual en la estacion se mantiene. */
    SIN_PENDIENTES_MANTENER_ACTUAL,
    /** Se toma el siguiente cliente de la fila y se asigna a la estacion. */
    ASIGNADO
  }

  private final Tipo tipo;
  private final Turno reemplazado;
  private final Turno asignado;
  private final String dniActualMantenido;

  private NuevoLlamado(Tipo tipo, Turno reemplazado, Turno asignado, String dniActualMantenido) {
    this.tipo = tipo;
    this.reemplazado = reemplazado;
    this.asignado = asignado;
    this.dniActualMantenido = dniActualMantenido;
  }

  public static NuevoLlamado sinPendientes() {
    return new NuevoLlamado(Tipo.SIN_PENDIENTES, null, null, null);
  }

  public static NuevoLlamado sinPendientesMantenerActual(String dni) {
    return new NuevoLlamado(Tipo.SIN_PENDIENTES_MANTENER_ACTUAL, null, null, dni);
  }

  public static NuevoLlamado asignado(Turno reemplazado, Turno asignado) {
    return new NuevoLlamado(Tipo.ASIGNADO, reemplazado, asignado, null);
  }

  public Tipo getTipo() {
    return tipo;
  }

  public Optional<Turno> getReemplazado() {
    return Optional.ofNullable(reemplazado);
  }

  public Optional<Turno> getAsignado() {
    return Optional.ofNullable(asignado);
  }

  public Optional<String> getDniActualMantenido() {
    return Optional.ofNullable(dniActualMantenido);
  }
}
