package com.grupo6.interfaz_de_operador;

public class Main {
  public static void main(String[] args) {
    final OperatorFrame vista = new OperatorFrame();
    final Controlador controlador = new Controlador();
    controlador.setVista(vista);
    vista.setVisible(true);
  }
}
