package com.grupo6.monitor;

import javax.swing.SwingUtilities;

public class Main {
  public static void main(String[] args) {
    Controlador controlador = new Controlador();
    MonitorFrame frame = new MonitorFrame();
    Monitor monitor = new Monitor();

    controlador.setVista(frame);
    monitor.setControlador(controlador);

    SwingUtilities.invokeLater(() -> frame.setVisible(true));

    monitor.start();
  }
}
