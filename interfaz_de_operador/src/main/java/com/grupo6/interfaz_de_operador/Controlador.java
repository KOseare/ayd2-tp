package com.grupo6.interfaz_de_operador;

import com.grupo6.conexion_servidor.ConexionServidor;

public class Controlador {
  private static final int RENOTIFY_COOLDOWN_MS = 30_000;
  private final ConexionServidor conexionServidor = new ConexionServidor();
  private IVista vista = null;
  private ModeloVista modelo = new ModeloVista(0, null, null, null, true, true);
  private long renotifyEnabledAtMs;

  public void setVista(IVista vista) {
    this.vista = vista;
    vista.setControlador(this);
    vista.actualizar(modelo);
  }

  public void subscribirse() {
    conexionServidor.subscribeAndListen("SUBSCRIBE_OPERATOR", (String msg) -> handleUpdate(msg),
        (String msg) -> handleError(msg));
  }

  private void handleUpdate(String msg) {
    if (msg != null && msg.startsWith("OK|QUEUE_SIZE|")) {
      String count = msg.substring("OK|QUEUE_SIZE|".length());
      modelo = new ModeloVista(Integer.parseInt(count), modelo.error, modelo.stationId, modelo.currentDni,
          modelo.renotifyBtnEnabled, modelo.finalizeBtnEnabled);
    }
    modelo = new ModeloVista(-1, modelo.error, modelo.stationId, modelo.currentDni, modelo.renotifyBtnEnabled,
        modelo.finalizeBtnEnabled);
    vista.actualizar(modelo);
  }

  private void handleError(String msg) {
    modelo = new ModeloVista(modelo.personasEnCola, msg, modelo.stationId, modelo.currentDni, modelo.renotifyBtnEnabled,
        modelo.finalizeBtnEnabled);
    vista.actualizar(modelo);
  }

  public void callNextClient(InvokeLaterCallback invokeLater) {
    if (modelo.stationId == null || modelo.stationId.isEmpty()) {
      modelo = new ModeloVista(modelo.personasEnCola, "Error: puesto no asignado.", modelo.stationId,
          modelo.currentDni, modelo.renotifyBtnEnabled, modelo.finalizeBtnEnabled);
      vista.actualizar(modelo);
      return;
    }
    runAsync(() -> {
      String response = conexionServidor.sendCommand("CALL_NEXT|" + modelo.stationId);
      invokeLater.invokeLater(() -> handleCallNextResponse(response, invokeLater));
    });
  }

  public void renotifyClient(InvokeLaterCallback invokeLater) {
    if (modelo.stationId == null || modelo.stationId.isEmpty()) {
      modelo = new ModeloVista(modelo.personasEnCola, "Error: puesto no asignado.", modelo.stationId, modelo.currentDni,
          modelo.renotifyBtnEnabled, modelo.finalizeBtnEnabled);
      vista.actualizar(modelo);
      return;
    }
    runAsync(() -> {
      String response = conexionServidor.sendCommand("RENOTIFY|" + modelo.stationId);
      invokeLater.invokeLater(() -> handleRenotifyResponse(response, invokeLater));
    });
  }

  public void finalizeClient(InvokeLaterCallback invokeLater) {
    if (modelo.stationId == null || modelo.stationId.isEmpty()) {
      modelo = new ModeloVista(modelo.personasEnCola, "Error: puesto no asignado.", modelo.stationId, modelo.currentDni,
          modelo.renotifyBtnEnabled, modelo.finalizeBtnEnabled);
      vista.actualizar(modelo);
      return;
    }
    runAsync(() -> {
      String response = conexionServidor.sendCommand("FINALIZE|" + modelo.stationId);
      invokeLater.invokeLater(() -> handleFinalizeResponse(response, invokeLater));
    });
  }

  private void handleCallNextResponse(String response, InvokeLaterCallback invokeLater) {
    if (response.startsWith("OK|CALLED|")) {
      final String currentDni = response.substring("OK|CALLED|".length());
      modelo = new ModeloVista(modelo.personasEnCola, null, modelo.stationId, currentDni, renotifyEnabled(),
          finalizeEnabled());
      vista.actualizar(modelo);
      scheduleRenotifyCooldown();
      refreshQueueCountAsync(invokeLater);
      return;
    }
    if ("OK|NO_PENDING".equals(response)) {
      modelo = new ModeloVista(modelo.personasEnCola, null, modelo.stationId, null, renotifyEnabled(),
          finalizeEnabled());
      vista.actualizar(modelo);
      clearRenotifyCooldown();
      refreshQueueCountAsync(invokeLater);
      return;
    }
    if (response.startsWith("ERROR|NO_PENDING_KEEPING_CURRENT|")) {
      String activeDni = response.substring("ERROR|NO_PENDING_KEEPING_CURRENT|".length());
      modelo = new ModeloVista(modelo.personasEnCola, null, modelo.stationId, activeDni, renotifyEnabled(),
          finalizeEnabled());
      vista.actualizar(modelo);
      refreshQueueCountAsync(invokeLater);
      return;
    }
    modelo = new ModeloVista(modelo.personasEnCola, "Error al llamar siguiente: " + response, modelo.stationId,
        modelo.currentDni, renotifyEnabled(),
        finalizeEnabled());
    vista.actualizar(modelo);
  }

  public void handleFinalizeResponse(String response, InvokeLaterCallback invokeLater) {
    if (response.startsWith("OK|FINALIZED|")) {
      modelo = new ModeloVista(modelo.personasEnCola, null, modelo.stationId, null, renotifyEnabled(),
          finalizeEnabled());
      vista.actualizar(modelo);
      clearRenotifyCooldown();
      refreshQueueCountAsync(invokeLater);
      return;
    }
    if ("ERROR|NO_ACTIVE_CLIENT".equals(response)) {
      modelo = new ModeloVista(modelo.personasEnCola, null, modelo.stationId, null, renotifyEnabled(),
          finalizeEnabled());
      vista.actualizar(modelo);
      clearRenotifyCooldown();
      refreshQueueCountAsync(invokeLater);
      return;
    }
    modelo = new ModeloVista(modelo.personasEnCola, "Error al finalizar: " + response, modelo.stationId,
        modelo.currentDni, renotifyEnabled(),
        finalizeEnabled());
    vista.actualizar(modelo);
  }

  private boolean renotifyEnabled() {
    final boolean hasCurrent = modelo.currentDni != null && !modelo.currentDni.isEmpty();
    final long now = System.currentTimeMillis();
    return hasCurrent && (renotifyEnabledAtMs == 0L || now >= renotifyEnabledAtMs);
  }

  private boolean finalizeEnabled() {
    return modelo.currentDni != null && !modelo.currentDni.isEmpty();
  }

  private void scheduleRenotifyCooldown() {
    renotifyEnabledAtMs = System.currentTimeMillis() + RENOTIFY_COOLDOWN_MS;
  }

  private void clearRenotifyCooldown() {
    renotifyEnabledAtMs = 0L;
  }

  public void refreshQueueCountAsync(InvokeLaterCallback invokeLater) {
    runAsync(() -> {
      String response = conexionServidor.sendCommand("GET_QUEUE_SIZE");
      invokeLater.invokeLater(() -> applyQueueCountResponse(response));
    });
  }

  private void applyQueueCountResponse(String response) {
    if (response != null && response.startsWith("OK|QUEUE_SIZE|")) {
      String count = response.substring("OK|QUEUE_SIZE|".length());
      modelo = new ModeloVista(Integer.parseInt(count), modelo.error, modelo.stationId, modelo.currentDni,
          modelo.renotifyBtnEnabled, modelo.finalizeBtnEnabled);
      vista.actualizar(modelo);
      return;
    }
    modelo = new ModeloVista(-1, modelo.error, modelo.stationId, modelo.currentDni, modelo.renotifyBtnEnabled,
        modelo.finalizeBtnEnabled);
    vista.actualizar(modelo);
  }

  public void claimStationIdOrFail(GetCallback requestStationId) {
    while (true) {
      String requestedStation = requestStationId.get();
      String response = conexionServidor.sendCommand("CLAIM_STATION|" + requestedStation);
      if (response.startsWith("OK|STATION_CLAIMED|")) {
        modelo = new ModeloVista(modelo.personasEnCola, null, requestedStation, modelo.currentDni,
            modelo.renotifyBtnEnabled, modelo.finalizeBtnEnabled);
        vista.actualizar(modelo);
        return;
      }
      if ("ERROR|STATION_ID_EXISTS".equals(response)) {
        modelo = new ModeloVista(modelo.personasEnCola, "Error: el Puesto ID ya existe.", modelo.stationId,
            modelo.currentDni,
            modelo.renotifyBtnEnabled, modelo.finalizeBtnEnabled);
        vista.actualizar(modelo);
        continue;
      }
      modelo = new ModeloVista(modelo.personasEnCola, "Error al registrar puesto: " + response, modelo.stationId,
          modelo.currentDni,
          modelo.renotifyBtnEnabled, modelo.finalizeBtnEnabled);
      vista.actualizar(modelo);
    }
  }

  public void cooldownUpdate() {
    modelo = new ModeloVista(modelo.personasEnCola, modelo.error, modelo.stationId, modelo.currentDni,
        renotifyEnabled(), finalizeEnabled());
    vista.actualizar(modelo);
  }

  public void handleRenotifyResponse(String response, InvokeLaterCallback invokeLater) {
    if (response.startsWith("OK|RENOTIFIED|")) {
      String[] parts = response.split("\\|");
      if (parts.length >= 4) {
        modelo = new ModeloVista(modelo.personasEnCola, null, modelo.stationId, parts[2], renotifyEnabled(),
            finalizeEnabled());
        vista.actualizar(modelo);
      } else {
        modelo = new ModeloVista(modelo.personasEnCola, null, modelo.stationId, modelo.currentDni,
            renotifyEnabled(),
            finalizeEnabled());
        vista.actualizar(modelo);
      }
      scheduleRenotifyCooldown();
      refreshQueueCountAsync(invokeLater);
      return;
    }
    if (response.startsWith("OK|REMOVED_BY_LIMIT|")) {
      modelo = new ModeloVista(modelo.personasEnCola, null, modelo.stationId, null, renotifyEnabled(),
          finalizeEnabled());
      vista.actualizar(modelo);
      clearRenotifyCooldown();
      refreshQueueCountAsync(invokeLater);
      return;
    }
    if ("ERROR|NO_ACTIVE_CLIENT".equals(response)) {
      modelo = new ModeloVista(modelo.personasEnCola, null, modelo.stationId, null, renotifyEnabled(),
          finalizeEnabled());
      vista.actualizar(modelo);
      clearRenotifyCooldown();
      refreshQueueCountAsync(invokeLater);
      return;
    }
    modelo = new ModeloVista(modelo.personasEnCola, "Error al re-notificar: " + response, modelo.stationId,
        modelo.currentDni, renotifyEnabled(),
        finalizeEnabled());
    vista.actualizar(modelo);
  }

  public void releaseStationId() {
    if (modelo.stationId == null || modelo.stationId.isEmpty()) {
      return;
    }
    conexionServidor.sendCommand("RELEASE_STATION|" + modelo.stationId);
  }

  private void runAsync(Runnable action) {
    Thread thread = new Thread(action, "operator-action-thread");
    thread.setDaemon(true);
    thread.start();
  }
}
