package com.grupo6.monitor;

import javax.swing.SwingUtilities;

import com.grupo6.shared.Subscriber;

public class Controlador implements Subscriber<ModeloVista> {
  private IVista vista = null;

  public Controlador(Monitor monitor) {
    monitor.subscribe(this);
  }

  public void setVista(IVista vista) {
    this.vista = vista;
    vista.setControlador(this);
  }

  public void actualizarEstado(ModeloVista modelo) {
    if (vista != null) {
      SwingUtilities.invokeLater(() -> vista.actualizar(modelo));
    }
  }

  @Override
  public void update(ModeloVista context) {
    actualizarEstado(context);
  }
}
