package com.grupo6.interfaz_de_operador;

import com.grupo6.security.EncryptionStrategy;
import com.grupo6.security.EncryptionStrategyProvider;

public class Main {
  public static void main(String[] args) {
    final OperatorFrame vista = new OperatorFrame();
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
