package com.grupo6.monitor;

import javax.swing.SwingUtilities;

public class Controlador {
  private IVista vista;

  public void setVista(IVista vista) {
    this.vista = vista;
    vista.setControlador(this);
  }

  public void actualizarEstado(ModeloVista modelo) {
    if (vista != null) {
      SwingUtilities.invokeLater(() -> vista.actualizar(modelo));
    }
  }
}