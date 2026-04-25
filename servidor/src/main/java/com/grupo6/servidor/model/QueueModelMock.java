package com.grupo6.servidor.model;

/**
 * Mock of model, we'll implement it later
 */
public class QueueModelMock {

  public String registrarCliente(String dni) {
    return "REGISTRAR_MOCK ticket=REG-001 dni=" + dni;
  }

  public String llamarCliente() {
    return "LLAMAR_MOCK dni=11111111 desk=1";
  }
}
