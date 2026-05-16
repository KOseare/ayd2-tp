package com.grupo6.monitor_de_sala;

public class Main {
  public static void main(String[] args) {
    final PublicMonitorFrame vista = new PublicMonitorFrame();
    final Controlador controlador = new Controlador();
    controlador.setVista(vista);
    vista.setVisible(true);
  }
}
