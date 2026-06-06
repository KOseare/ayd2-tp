package com.grupo6.servidor;

import com.grupo6.environment.Environment;
import com.grupo6.environment.ServerAddress;
import com.grupo6.persistencia.PersistenciaFactory;
import com.grupo6.persistencia.PersistenciaFactoryProvider;
import com.grupo6.security.EncryptionStrategy;
import com.grupo6.security.EncryptionStrategyProvider;

public class Main {

  public static void main(String[] args) {
    final int id = Integer.parseInt(args[0]);
    final ServerAddress addr = Environment.nodosServidores.get(id);
    final int port = addr.port;
    final PersistenciaFactory persistenciaFactory = PersistenciaFactoryProvider.createFromEnvironment();
    EncryptionStrategy encryptionMethod;
    try {
      encryptionMethod = EncryptionStrategyProvider.fromEnvironment();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      return;
    }
    final Controlador controlador = new Controlador(encryptionMethod);

    final Servidor servidor = new Servidor(id, port, persistenciaFactory, controlador);

    servidor.start();
  }
}
