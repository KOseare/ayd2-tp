package com.grupo6.servidor;

public class ServidorInicial extends ServidorState {

  @Override
  public String name() {
    return "INITIAL";
  }

  @Override
  public void onEnter() {
  }

  @Override
  public void onExit() {
  }

  @Override
  public void handleCurrentLeader(int leaderId) {
    if (leaderId == context.id) {
      context.changeState(new ServidorActivo());
    } else {
      context.changeState(new ServidorStandby(leaderId));
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
