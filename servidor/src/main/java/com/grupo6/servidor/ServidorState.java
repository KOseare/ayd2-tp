package com.grupo6.servidor;

public abstract class ServidorState {
  protected Servidor context;

  public void setContext(Servidor servidor) {
    context = servidor;
  }

  public abstract String name();

  public abstract void onEnter();

  public abstract void onExit();

  public abstract void handleCurrentLeader(int leaderId);

  public abstract void handleStartPromotion();

  public abstract boolean canServeClients();

  public abstract boolean canHandlePing();

  public String serverLogPrefix() {
    return "[" + name() + "]";
  }
}
