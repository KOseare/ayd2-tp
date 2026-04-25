package com.grupo6.environment;

public final class QueueProtocol {

  public static final String CMD_LLAMAR_CLIENTE = "LLAMAR_CLIENTE";
  public static final String CMD_REGISTRAR_CLIENTE = "REGISTRAR_CLIENTE";
  public static final String OK_PREFIX = "OK ";
  public static final String ERR_PREFIX = "ERR ";

  private QueueProtocol() {
  }

  public static String responseOk(String payload) {
    return OK_PREFIX + payload;
  }

  public static String responseErr(String message) {
    return ERR_PREFIX + message;
  }
}
