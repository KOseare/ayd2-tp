package com.grupo6.servidor.modelo;

public enum EstadoTurno {
  /** Esperando a ser llamado. */
  Esperando,
  /** Llamado a una estacion de servicio (atencion activa). */
  Llamado,
  /** Servicio finalizado. */
  Atendido
}
