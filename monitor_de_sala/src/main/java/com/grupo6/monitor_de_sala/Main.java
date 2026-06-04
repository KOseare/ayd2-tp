package com.grupo6.monitor_de_sala;

import com.grupo6.security.EncryptionStrategy;
import com.grupo6.security.EncryptionStrategyProvider;

public class Main {
  public static void main(String[] args) {
    final PublicMonitorFrame vista = new PublicMonitorFrame();
    EncryptionStrategy encryptionMethod;
    try {
      encryptionMethod = EncryptionStrategyProvider.fromEnvironment();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      return;
    }
    final Controlador controlador = new Controlador(encryptionMethod);
    controlador.setVista(vista);
    vista.setVisible(true);
  }
}
