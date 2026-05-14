package com.grupo6.terminal_de_registro;

public class Main {
  public static void main(String[] args) {
    final Controlador controlador = new Controlador();
    final RegistrationTerminalFrame ventana = new RegistrationTerminalFrame();
    ventana.setVisible(true);
    controlador.setVista(ventana);
  }
}
