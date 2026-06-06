package com.grupo6.servidor;

public class ServidorActivo extends ServidorState {

  @Override
  public String name() {
    return "ACTIVE";
  }

  @Override
  public void onEnter() {
    context.stopReplicaClientSession();
    context.controlador.clearReplicaSubscribers();
    context.activateAsLeader();
  }

  @Override
  public void onExit() {
  }

  @Override
  public void handleCurrentLeader(int leaderId) {
    if (leaderId != context.id) {
      context.changeState(new ServidorStandby(leaderId));
    }
  }

  @Override
  public void handleStartPromotion() {
  }

  @Override
  public boolean canServeClients() {
    return true;
  }

  @Override
  public boolean canHandlePing() {
    return true;
  }
}
