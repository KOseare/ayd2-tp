package com.grupo6.monitor_de_sala;

import java.util.ArrayDeque;
import java.util.Deque;

import com.grupo6.conexion_servidor.ConexionServidor;
import com.grupo6.security.AESEncryptionStrategy;
import com.grupo6.security.EncryptionStrategy;

public class Controlador {
  private static final int HISTORY_LIMIT = 5;
  private final ConexionServidor conexionServidor = new ConexionServidor();
  private final EncryptionStrategy encryptionStrategy;
  private IVista vista = null;
  private ModeloVista modelo = new ModeloVista(null, null, new ArrayDeque<>(), null, false);

  public Controlador() {
    this(new AESEncryptionStrategy());
  }

  public Controlador(EncryptionStrategy encryptionStrategy) {
    this.encryptionStrategy = encryptionStrategy;
  }

  public void setVista(IVista vista) {
    this.vista = vista;
    this.vista.setControlador(this);
    this.vista.actualizar(modelo);
  }

  public void startMonitorClient(InvokeLaterCallback invokeLater) {
    conexionServidor.subscribeAndListen("SUBSCRIBE_MONITOR",
        (String msg) -> {
          invokeLater.invokeLater(() -> handleEvent(msg));
        }, (String err) -> {
          invokeLater.invokeLater(() -> handleConnectionError(err));
        });
  }

  private void handleEvent(String event) {
    if ("OK|CONNECTED".equals(event)) {
      clearError();
      return;
    }
    String[] parts = event.split("\\|");
    System.out.println("Controlador Monitor de Sala: Evento recibido - " + event);
    if (parts.length < 2 || !"EVENT".equals(parts[0])) {
      showError("Error: mensaje no valido");
      return;
    }

    if ("CALL".equals(parts[1]) && parts.length >= 4) {
      String dni = decryptDniForDisplay(parts[2]);
      if (dni == null) {
        return;
      }
      String stationId = parts[3];
      updateTurn(dni, stationId);
      clearError();
      return;
    }

    if ("RENOTIFY".equals(parts[1]) && parts.length >= 5) {
      String dni = decryptDniForDisplay(parts[2]);
      if (dni == null) {
        return;
      }
      String stationId = parts[3];
      updateCurrentTurnOnly(dni, stationId);
      runPriorityBlink();
      clearError();
      return;
    }

    if ("REMOVED".equals(parts[1]) && parts.length >= 4) {
      // updateCurrentTurnOnly("-", "-");
      clearError();
      return;
    }

    if ("FINALIZED".equals(parts[1]) && parts.length >= 4) {
      clearError();
      return;
    }

    showError("Error: evento no soportado");
  }

  private void showError(String error) {
    modelo = new ModeloVista(modelo.dniTurnoActual, modelo.idPuestoTurnoActual, modelo.historialDeLlamadas, error,
        false);
    vista.actualizar(modelo);
  }

  private void handleConnectionError(String error) {
    modelo = new ModeloVista("-", "-", modelo.historialDeLlamadas, error, false);
    vista.actualizar(modelo);
  }

  private void updateTurn(String dni, String stationId) {
    String rowText = dni + " - Puesto " + stationId;
    final Deque<String> callHistory = modelo.historialDeLlamadas;
    callHistory.addFirst(rowText);
    while (callHistory.size() > HISTORY_LIMIT) {
      callHistory.removeLast();
    }
    modelo = new ModeloVista(dni, stationId, callHistory, modelo.error, false);
    vista.actualizar(modelo);
  }

  private void clearError() {
    modelo = new ModeloVista(modelo.dniTurnoActual, modelo.idPuestoTurnoActual, modelo.historialDeLlamadas, null,
        false);
    vista.actualizar(modelo);
  }

  private void runPriorityBlink() {
    modelo = new ModeloVista(modelo.dniTurnoActual, modelo.idPuestoTurnoActual, modelo.historialDeLlamadas,
        modelo.error,
        true);
    vista.actualizar(modelo);
  }

  private void updateCurrentTurnOnly(String dni, String idPuesto) {
    modelo = new ModeloVista(dni, idPuesto, modelo.historialDeLlamadas, modelo.error, false);
    vista.actualizar(modelo);
  }

  private String decryptDniForDisplay(String encryptedDni) {
    try {
      return encryptionStrategy.decrypt(encryptedDni);
    } catch (RuntimeException e) {
      showError("Error: no se pudo descifrar el DNI");
      return null;
    }
  }
}
