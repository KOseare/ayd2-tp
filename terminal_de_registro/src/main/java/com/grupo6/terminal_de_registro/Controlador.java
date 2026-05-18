package com.grupo6.terminal_de_registro;

import java.util.regex.Pattern;

import com.grupo6.conexion_servidor.ConexionServidor;

public class Controlador {
  private static final Pattern NUMERIC_PATTERN = Pattern.compile("^\\d+$");

  private IVista vista = null;
  private final ConexionServidor conexion = new ConexionServidor();
  private ModeloVista modelo = new ModeloVista(null, null);

  public void setVista(IVista vista) {
    this.vista = vista;
    this.vista.setControlador(this);
  }

  public void registrarse(String dni) {
    String error = validarDni(dni);
    if (error != null) {
      modelo = new ModeloVista(dni, error);
      vista.actualizar(modelo);
      return;
    }
    error = enviarPeticionRegistrarse(dni);
    if (error != null) {
      modelo = new ModeloVista(dni, error);
      vista.actualizar(modelo);
      return;
    }
    modelo = new ModeloVista(null, null);
    vista.actualizar(modelo);
  }

  private String validarDni(String dni) {
    if (dni.isEmpty()) {
      return "Error: el DNI es obligatorio.";
    }

    if (!NUMERIC_PATTERN.matcher(dni).matches()) {
      return "Error: el DNI debe contener solo numeros.";
    }

    if (dni.length() != 7 && dni.length() != 8) {
      return "Error: el DNI debe tener 7 u 8 digitos.";
    }

    return null;
  }

  private String enviarPeticionRegistrarse(String dni) {
    try {
      final String response = conexion.sendCommand("REGISTER|" + dni);
      if (response == null) {
        return "Error: sin respuesta del servidor.";
      }

      if (response == "ERROR|ACTIVE_NODE_NOT_FOUND") {
        return "Error: no se pudo establecer una conexion con el servidor.";
      }

      if (response.startsWith("OK|REGISTERED|")) {
        return null;
      }

      if ("ERROR|ALREADY_IN_QUEUE".equals(response) || "ERROR|ALREADY_IN_ATTENTION".equals(response)) {
        return "Error: el DNI ya existe en la fila.";
      }

      if ("ERROR|INVALID_DNI".equals(response)) {
        return "Error: DNI invalido.";
      }

      return "Error del servidor: " + response;
    } catch (Exception e) {
      return "Hubo un error: " + e.getMessage();
    }
  }
}
