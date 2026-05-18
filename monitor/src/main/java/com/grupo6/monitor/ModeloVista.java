package com.grupo6.monitor;

import java.util.List;

public class ModeloVista {
  public final int activeNodeId;
  public final List<String> nombresNodos;
  public final List<String> estadosNodos;

  public ModeloVista(int activeNodeId, List<String> nombresNodos, List<String> estadosNodos) {
    this.activeNodeId = activeNodeId;
    this.nombresNodos = nombresNodos;
    this.estadosNodos = estadosNodos;
  }
}
