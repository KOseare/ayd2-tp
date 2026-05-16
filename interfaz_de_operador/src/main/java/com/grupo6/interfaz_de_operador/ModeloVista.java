package com.grupo6.interfaz_de_operador;

public class ModeloVista {
  public final int personasEnCola;
  public final String currentDni;
  public final String stationId;
  public final String error;
  public final boolean renotifyBtnEnabled;
  public final boolean finalizeBtnEnabled;

  public ModeloVista(int personasEnCola, String error, String stationId, String currentDni, boolean renotifyBtnEnabled,
      boolean finalizeBtnEnabled) {
    this.personasEnCola = personasEnCola;
    this.error = error;
    this.stationId = stationId;
    this.currentDni = currentDni;
    this.renotifyBtnEnabled = renotifyBtnEnabled;
    this.finalizeBtnEnabled = finalizeBtnEnabled;
  }
}
