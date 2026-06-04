package com.grupo6.servidor;

public class ServidorStandby extends ServidorState {

  private final int leaderId;

  public ServidorStandby(int leaderId) {
    this.leaderId = leaderId;
  }

  @Override
  public String name() {
    return "STANDBY";
  }

  @Override
  public void onEnter() {
    context.controlador.clearPersistenciaEntidades();
    context.startReplicaClientSessionIfNeeded(leaderId);
  }

  @Override
  public void onExit() {
    context.stopReplicaClientSession();
  }

  @Override
  public void handleCurrentLeader(int newLeaderId) {
    if (newLeaderId == context.id) {
      context.changeState(new ServidorActivo());
    } else {
      context.changeState(new ServidorStandby(newLeaderId));
    }
  }

  @Override
  public void handleStartPromotion() {
    context.changeState(new ServidorActivo());
  }

  @Override
  public boolean canServeClients() {
    return false;
  }

  @Override
  public boolean canHandlePing() {
    return false;
  }
}
