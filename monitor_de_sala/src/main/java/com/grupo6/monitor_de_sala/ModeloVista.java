package com.grupo6.monitor_de_sala;

import java.util.Deque;

public class ModeloVista {
  public final String dniTurnoActual;
  public final String idPuestoTurnoActual;
  public final Deque<String> historialDeLlamadas;
  public final String error;
  public final boolean runPriorityBlink;

  public ModeloVista(String dniTurnoActual, String idPuestoTurnoActual, Deque<String> historialDeLlamadas,
      String error, boolean runPriorityBlink) {
    this.dniTurnoActual = dniTurnoActual;
    this.idPuestoTurnoActual = idPuestoTurnoActual;
    this.historialDeLlamadas = historialDeLlamadas;
    this.error = error;
    this.runPriorityBlink = runPriorityBlink;
  }
}
