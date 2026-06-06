package com.grupo6.monitor;

import javax.swing.SwingUtilities;

public class Main {
  public static void main(String[] args) {
    MonitorFrame frame = new MonitorFrame();
    Monitor monitor = new Monitor();
    Controlador controlador = new Controlador(monitor);

    controlador.setVista(frame);

    SwingUtilities.invokeLater(() -> frame.setVisible(true));

    monitor.start();
  }
}
