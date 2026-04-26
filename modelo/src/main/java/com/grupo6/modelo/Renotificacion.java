package com.grupo6.modelo;

import java.util.Optional;

public class Renotificacion {

  public enum Tipo {
    SIN_TURNO_ACTIVO,
    NOTIFICADO,
    REMOVIDO_POR_LIMITE
  }

  private final Tipo tipo;
  private final String dni;
  private final int intentos;

  private Renotificacion(Tipo tipo, String dni, int intentos) {
    this.tipo = tipo;
    this.dni = dni;
    this.intentos = intentos;
  }

  public static Renotificacion sinActivo() {
    return new Renotificacion(Tipo.SIN_TURNO_ACTIVO, null, 0);
  }

  public static Renotificacion notificado(String dni, int intentos) {
    return new Renotificacion(Tipo.NOTIFICADO, dni, intentos);
  }

  public static Renotificacion removidoPorLimite(String dni) {
    return new Renotificacion(Tipo.REMOVIDO_POR_LIMITE, dni, 0);
  }

  public Tipo getTipo() {
    return tipo;
  }

  public Optional<String> getDni() {
    return Optional.ofNullable(dni);
  }

  public int getIntentos() {
    return intentos;
  }
}
