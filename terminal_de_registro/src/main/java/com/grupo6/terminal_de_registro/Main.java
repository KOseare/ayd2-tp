package com.grupo6.terminal_de_registro;

import com.grupo6.security.EncryptionStrategy;
import com.grupo6.security.EncryptionStrategyProvider;

public class Main {
  public static void main(String[] args) {
    EncryptionStrategy encryptionMethod;
    try {
      encryptionMethod = EncryptionStrategyProvider.fromEnvironment();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      return;
    }
    final Controlador controlador = new Controlador(encryptionMethod);
    final RegistrationTerminalFrame ventana = new RegistrationTerminalFrame();
    ventana.setVisible(true);
    controlador.setVista(ventana);
  }
}
