package com.grupo6.terminal_de_registro.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.grupo6.environment.Environment;
import com.grupo6.environment.QueueProtocol;

/**
 * TCP client to the central queue control server.
 */
public final class QueueServerClient {

  private QueueServerClient() {
  }

  public static String registrarCliente(String dni) throws IOException {
    try (Socket socket = new Socket(Environment.QUEUE_HOST, Environment.QUEUE_PORT);
        PrintWriter out = new PrintWriter(
            new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        BufferedReader in = new BufferedReader(
            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
      out.println(QueueProtocol.CMD_REGISTRAR_CLIENTE);
      out.println(dni == null ? "" : dni);
      return in.readLine();
    }
  }
}
