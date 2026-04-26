package com.grupo6.servidor.modelo;

public enum EstadoTurno {
  /** Esperando a ser llamado. */
  Esperando,
  /** Llamado a una estación de servicio (atención activa). */
  Llamado,
  /** Servicio finalizado. */
  Atendido
}
