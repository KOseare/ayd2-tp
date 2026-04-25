package com.grupo6.terminal_de_registro;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import com.grupo6.environment.Environment;
import com.grupo6.ui.AppUiTheme;

public class RegistrationTerminalFrame extends JFrame {

  private static final long serialVersionUID = 1L;
  private static final String SERVER_HOST = Environment.SERVER_HOST;
  private static final int SERVER_PORT = Environment.SERVER_PORT;
  private static final Pattern NUMERIC_PATTERN = Pattern.compile("^\\d+$");

  private final JTextField documentField;
  private final JLabel errorLabel;
  private final JLabel queueCountLabel;
  private final Timer queueRefreshTimer;

  private void registerClient() {
    String dni = documentField.getText() == null ? "" : documentField.getText().trim();
    if (dni.isEmpty()) {
      showError("Error: el DNI es obligatorio.");
      return;
    }

    if (!NUMERIC_PATTERN.matcher(dni).matches()) {
      showError("Error: el DNI debe contener solo numeros.");
      return;
    }

    if (dni.length() != 7 && dni.length() != 8) {
      showError("Error: el DNI debe tener 7 u 8 digitos.");
      return;
    }

    sendData(dni);
  }

  private void sendData(String dni) {
    try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
      out.println("REGISTER|" + dni);
      String response = in.readLine();
      if (response == null) {
        showError("Error: sin respuesta del servidor.");
        return;
      }

      if (response.startsWith("OK|REGISTERED|")) {
        documentField.setText("");
        clearError();
        refreshQueueCountAsync();
        return;
      }

      if ("ERROR|ALREADY_IN_QUEUE".equals(response) || "ERROR|ALREADY_IN_ATTENTION".equals(response)) {
        showError("Error: el DNI ya existe en la fila.");
        return;
      }

      if ("ERROR|INVALID_DNI".equals(response)) {
        showError("Error: DNI invalido.");
        return;
      }

      showError("Error del servidor: " + response);
    } catch (UnknownHostException e) {
      showError("Error de red: host servidor invalido.");
    } catch (IOException e) {
      showError("No se pudo conectar al servidor.");
      JOptionPane.showMessageDialog(
          this,
          "No fue posible enviar el DNI porque el servidor no responde.\n" + e.getMessage(),
          "Error de conexion",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  private String sendSimpleCommand(String command) {
    try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
      out.println(command);
      return in.readLine();
    } catch (IOException e) {
      return "ERROR|NETWORK";
    }
  }

  private void refreshQueueCountAsync() {
    Thread thread = new Thread(() -> {
      String response = sendSimpleCommand("GET_QUEUE_SIZE");
      SwingUtilities.invokeLater(() -> applyQueueCountResponse(response));
    }, "registration-queue-refresh");
    thread.setDaemon(true);
    thread.start();
  }

  private void applyQueueCountResponse(String response) {
    if (response == null) {
      queueCountLabel.setText("Personas en cola: -");
      showError("Error: sin respuesta del servidor.");
      return;
    }
    if (response.startsWith("OK|QUEUE_SIZE|")) {
      String count = response.substring("OK|QUEUE_SIZE|".length());
      queueCountLabel.setText("Personas en cola: " + count);
      return;
    }
    queueCountLabel.setText("Personas en cola: -");
  }

  private void showError(String message) {
    errorLabel.setText(message);
  }

  private void clearError() {
    errorLabel.setText("");
  }

  public RegistrationTerminalFrame() {
    setTitle("Terminal de Registro");
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setMinimumSize(new Dimension(440, 380));
    setSize(480, 400);
    setLocationRelativeTo(null);

    Font base = AppUiTheme.baseUiFont();
    Font secondaryFont = base.deriveFont(Font.PLAIN, 13f);
    Font statusFont = base.deriveFont(Font.PLAIN, 12f);

    JLabel hint = new JLabel("Solo numeros, sin puntos ni espacios.", SwingConstants.CENTER);
    hint.setFont(secondaryFont);
    hint.setForeground(AppUiTheme.TEXT_MUTED);

    queueCountLabel = new JLabel("Personas en cola: -", SwingConstants.CENTER);
    queueCountLabel.setFont(secondaryFont);
    queueCountLabel.setForeground(AppUiTheme.TEXT_BODY);

    JLabel heroCaption = new JLabel("INGRESE SU DOCUMENTO", SwingConstants.CENTER);
    heroCaption.setFont(base.deriveFont(Font.BOLD, 11f));
    heroCaption.setForeground(AppUiTheme.TEXT_MUTED);

    documentField = new JTextField();
    documentField.setHorizontalAlignment(SwingConstants.CENTER);
    documentField.setFont(base.deriveFont(Font.PLAIN, 28f));
    documentField.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(AppUiTheme.BORDER_HERO, 2),
        BorderFactory.createEmptyBorder(12, 16, 12, 16)));

    JPanel fieldWrap = new JPanel(new BorderLayout());
    fieldWrap.setOpaque(false);
    fieldWrap.add(documentField, BorderLayout.CENTER);

    JPanel hero = new JPanel(new BorderLayout(0, 12));
    hero.setBackground(AppUiTheme.BG_HERO);
    hero.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(AppUiTheme.BORDER_HERO, 2),
        BorderFactory.createEmptyBorder(20, 24, 24, 24)));
    hero.add(heroCaption, BorderLayout.NORTH);
    hero.add(fieldWrap, BorderLayout.CENTER);

    errorLabel = new JLabel("", SwingConstants.CENTER);
    errorLabel.setFont(statusFont);
    errorLabel.setForeground(new java.awt.Color(180, 30, 30));

    JButton joinButton = new JButton("Unirse a la lista de espera");
    joinButton.setFont(base.deriveFont(Font.BOLD, 14f));
    joinButton.setMargin(new Insets(12, 28, 12, 28));
    joinButton.addActionListener(e -> registerClient());

    JPanel footer = new JPanel(new BorderLayout(0, 12));
    footer.setOpaque(false);
    footer.setBorder(BorderFactory.createEmptyBorder(4, 20, 0, 20));
    footer.add(errorLabel, BorderLayout.NORTH);
    JPanel buttonRow = new JPanel();
    buttonRow.setOpaque(false);
    buttonRow.add(joinButton);
    footer.add(buttonRow, BorderLayout.CENTER);

    JPanel hintPanel = new JPanel(new GridBagLayout());
    hintPanel.setOpaque(false);
    GridBagConstraints gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 0;
    gc.weightx = 1;
    gc.fill = GridBagConstraints.HORIZONTAL;
    hintPanel.add(hint, gc);
    gc.gridy = 1;
    gc.insets = new Insets(6, 0, 0, 0);
    hintPanel.add(queueCountLabel, gc);

    JPanel root = new JPanel(new BorderLayout(16, 16));
    root.setBackground(AppUiTheme.BG_APP);
    root.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));
    root.add(hintPanel, BorderLayout.NORTH);
    root.add(hero, BorderLayout.CENTER);
    root.add(footer, BorderLayout.SOUTH);

    setContentPane(root);

    queueRefreshTimer = new Timer(3000, e -> refreshQueueCountAsync());
    queueRefreshTimer.start();
    refreshQueueCountAsync();
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        queueRefreshTimer.stop();
      }
    });
  }
}
