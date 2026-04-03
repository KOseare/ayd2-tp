package com.grupo6.terminal_de_registro;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

public class RegistrationTerminalFrame extends JFrame {

  private static final long serialVersionUID = 1L;
  private static final String OPERATOR_HOST = "127.0.0.1";
  private static final int OPERATOR_PORT = 3001;
  private static final Pattern NUMERIC_PATTERN = Pattern.compile("^\\d+$");

  private final JTextField documentField;
  private final JLabel statusLabel;

  private void registerClient() {
    String dni = documentField.getText() == null ? "" : documentField.getText().trim();
    if (dni.isEmpty()) {
      statusLabel.setText("Error: el DNI es obligatorio.");
      return;
    }

    if (!NUMERIC_PATTERN.matcher(dni).matches()) {
      statusLabel.setText("Error: el DNI debe contener solo numeros.");
      return;
    }

    sendData(dni);
  }

  private void sendData(String dni) {
    try (Socket socket = new Socket(OPERATOR_HOST, OPERATOR_PORT);
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
      out.println(dni);
      statusLabel.setText("Registro exitoso. DNI enviado: " + dni);
      documentField.setText("");
    } catch (UnknownHostException e) {
      statusLabel.setText("Error de red: host operador invalido.");
    } catch (IOException e) {
      statusLabel.setText("No se pudo conectar al operador. Esta encendido?");
      JOptionPane.showMessageDialog(
          this,
          "No fue posible enviar el DNI porque la interfaz de operador no responde.\n" + e.getMessage(),
          "Error de conexion",
          JOptionPane.ERROR_MESSAGE
      );
    }
  }

  public RegistrationTerminalFrame() {
    setTitle("Terminal de Registro");
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setSize(440, 220);
    setLocationRelativeTo(null);

    JLabel title = new JLabel("Ingrese su numero de documento");
    JLabel hint = new JLabel("Ingrese solo numeros, sin puntos ni espacios.");

    documentField = new JTextField();
    documentField.setColumns(18);
    statusLabel = new JLabel("Estado: esperando registro...");

    JButton joinButton = new JButton("Unirse a la lista de espera");

    JPanel form = new JPanel(new GridLayout(5, 1, 8, 8));
    form.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
    form.add(title);
    form.add(hint);
    form.add(documentField);
    form.add(joinButton);
    form.add(statusLabel);

    setLayout(new BorderLayout());
    add(form, BorderLayout.CENTER);

    joinButton.addActionListener(e -> registerClient());
  }
}
