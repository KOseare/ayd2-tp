package com.grupo6.servidor;

import com.grupo6.environment.Environment;

public class Main {
  public static void main(String[] args) {
    try {
      QueueServer server = new QueueServer(Environment.SERVER_PORT);
      server.start();
    } catch (Exception e) {
      System.err.println("No se pudo iniciar el servidor: " + e.getMessage());
    }
  }
}
