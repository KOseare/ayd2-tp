package com.grupo6.interfaz_de_operador;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import com.grupo6.environment.Environment;
import com.grupo6.ui.AppUiTheme;

public class OperatorFrame extends JFrame {

  private static final long serialVersionUID = 1L;
  private static final String SERVER_HOST = Environment.SERVER_HOST;
  private static final int SERVER_PORT = Environment.SERVER_PORT;
  private static final int RENOTIFY_COOLDOWN_MS = 30_000;

  private String stationId;
  private final JLabel stationLabel;
  private final JLabel queueCountLabel;
  private final JLabel lastCalledCaption;
  private final JLabel lastCalledDniLabel;
  private final JLabel errorLabel;
  private final JButton callNextButton;
  private final JButton renotifyButton;
  private final JButton finalizeButton;
  private final Timer queueRefreshTimer;
  private final Timer buttonsRefreshTimer;
  private long renotifyEnabledAtMs;
  private String currentDni;
  private final Font activeDniFont;
  private final Font idleDniFont;

  public OperatorFrame() {
    stationId = null;
    setTitle("Puesto de Operador");
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setMinimumSize(new Dimension(440, 400));
    setSize(480, 420);
    setLocationRelativeTo(null);

    Font base = AppUiTheme.baseUiFont();
    Font secondaryFont = base.deriveFont(Font.PLAIN, 13f);
    Font statusFont = base.deriveFont(Font.PLAIN, 12f);
    activeDniFont = base.deriveFont(Font.BOLD, 44f);
    idleDniFont = base.deriveFont(Font.BOLD, 24f);

    stationLabel = new JLabel("Puesto: no asignado", SwingConstants.CENTER);
    stationLabel.setFont(secondaryFont);
    stationLabel.setForeground(AppUiTheme.TEXT_MUTED);

    queueCountLabel = new JLabel("Personas en cola: -", SwingConstants.CENTER);
    queueCountLabel.setFont(base.deriveFont(Font.BOLD, 16f));
    queueCountLabel.setForeground(AppUiTheme.TEXT_HERO_DNI);

    lastCalledCaption = new JLabel("ULTIMO LLAMADO", SwingConstants.CENTER);
    lastCalledCaption.setFont(base.deriveFont(Font.BOLD, 11f));
    lastCalledCaption.setForeground(AppUiTheme.TEXT_MUTED);

    lastCalledDniLabel = new JLabel("Sin cliente en atencion", SwingConstants.CENTER);
    lastCalledDniLabel.setFont(idleDniFont);
    lastCalledDniLabel.setForeground(AppUiTheme.TEXT_HERO_DNI);
    lastCalledDniLabel.setOpaque(false);

    errorLabel = new JLabel("", SwingConstants.CENTER);
    errorLabel.setFont(statusFont);
    errorLabel.setForeground(new Color(180, 30, 30));

    callNextButton = new JButton("Llamar Siguiente");
    callNextButton.setFont(base.deriveFont(Font.BOLD, 14f));
    callNextButton.setMargin(new Insets(12, 28, 12, 28));
    callNextButton.addActionListener(e -> callNextClient());

    renotifyButton = new JButton("Re-notificar");
    renotifyButton.setFont(base.deriveFont(Font.BOLD, 14f));
    renotifyButton.setMargin(new Insets(12, 28, 12, 28));
    renotifyButton.addActionListener(e -> renotifyClient());

    finalizeButton = new JButton("Finalizar Atencion");
    finalizeButton.setFont(base.deriveFont(Font.BOLD, 14f));
    finalizeButton.setMargin(new Insets(12, 28, 12, 28));
    finalizeButton.addActionListener(e -> finalizeClient());

    JPanel hero = new JPanel(new BorderLayout(0, 10));
    hero.setBackground(AppUiTheme.BG_HERO);
    hero.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(AppUiTheme.BORDER_HERO, 2),
        BorderFactory.createEmptyBorder(20, 24, 28, 24)));
    hero.add(lastCalledCaption, BorderLayout.NORTH);
    hero.add(lastCalledDniLabel, BorderLayout.CENTER);

    JPanel footer = new JPanel(new BorderLayout(0, 12));
    footer.setOpaque(false);
    footer.setBorder(BorderFactory.createEmptyBorder(4, 20, 0, 20));
    footer.add(errorLabel, BorderLayout.NORTH);
    JPanel buttonRow = new JPanel();
    buttonRow.setOpaque(false);
    buttonRow.add(callNextButton);
    buttonRow.add(renotifyButton);
    buttonRow.add(finalizeButton);
    footer.add(buttonRow, BorderLayout.CENTER);

    JPanel root = new JPanel(new BorderLayout(16, 16));
    root.setBackground(AppUiTheme.BG_APP);
    root.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));
    JPanel header = new JPanel(new BorderLayout(0, 8));
    header.setOpaque(false);
    header.add(stationLabel, BorderLayout.NORTH);
    header.add(queueCountLabel, BorderLayout.SOUTH);
    root.add(header, BorderLayout.NORTH);
    root.add(hero, BorderLayout.CENTER);
    root.add(footer, BorderLayout.SOUTH);

    setContentPane(root);
    queueRefreshTimer = new Timer(3000, e -> refreshQueueCountAsync());
    queueRefreshTimer.start();
    buttonsRefreshTimer = new Timer(1000, e -> updateButtonsState());
    buttonsRefreshTimer.start();
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        queueRefreshTimer.stop();
        buttonsRefreshTimer.stop();
        releaseStationId();
      }
    });
    claimStationIdOrFail();
    refreshQueueCountAsync();
    updateButtonsState();
  }

  private void callNextClient() {
    if (stationId == null || stationId.isEmpty()) {
      showError("Error: puesto no asignado.");
      return;
    }
    runAsync(() -> {
      String response = sendCommand("CALL_NEXT|" + stationId);
      SwingUtilities.invokeLater(() -> handleCallNextResponse(response));
    });
  }

  private void renotifyClient() {
    if (stationId == null || stationId.isEmpty()) {
      showError("Error: puesto no asignado.");
      return;
    }
    runAsync(() -> {
      String response = sendCommand("RENOTIFY|" + stationId);
      SwingUtilities.invokeLater(() -> handleRenotifyResponse(response));
    });
  }

  private void finalizeClient() {
    if (stationId == null || stationId.isEmpty()) {
      showError("Error: puesto no asignado.");
      return;
    }
    runAsync(() -> {
      String response = sendCommand("FINALIZE|" + stationId);
      SwingUtilities.invokeLater(() -> handleFinalizeResponse(response));
    });
  }

  private String sendCommand(String command) {
    try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
      writer.println(command);
      String response = reader.readLine();
      if (response == null) {
        return "ERROR|NO_RESPONSE";
      }
      return response;
    } catch (IOException e) {
      return "ERROR|NETWORK|" + e.getMessage();
    }
  }

  private void refreshQueueCountAsync() {
    runAsync(() -> {
      String response = sendCommand("GET_QUEUE_SIZE");
      SwingUtilities.invokeLater(() -> applyQueueCountResponse(response));
    });
  }

  private void applyQueueCountResponse(String response) {
    if (response != null && response.startsWith("OK|QUEUE_SIZE|")) {
      String count = response.substring("OK|QUEUE_SIZE|".length());
      queueCountLabel.setText("Personas en cola: " + count);
      return;
    }
    queueCountLabel.setText("Personas en cola: sin datos");
  }

  private void handleCallNextResponse(String response) {
    if (response.startsWith("OK|CALLED|")) {
      currentDni = response.substring("OK|CALLED|".length());
      lastCalledDniLabel.setText(currentDni);
      lastCalledDniLabel.setFont(activeDniFont);
      clearError();
      scheduleRenotifyCooldown();
      refreshQueueCountAsync();
      updateButtonsState();
      return;
    }
    if ("OK|NO_PENDING".equals(response)) {
      currentDni = null;
      clearRenotifyCooldown();
      lastCalledDniLabel.setText("Sin cliente en atencion");
      lastCalledDniLabel.setFont(idleDniFont);
      clearError();
      refreshQueueCountAsync();
      updateButtonsState();
      return;
    }
    if (response.startsWith("ERROR|NO_PENDING_KEEPING_CURRENT|")) {
      String activeDni = response.substring("ERROR|NO_PENDING_KEEPING_CURRENT|".length());
      currentDni = activeDni;
      lastCalledDniLabel.setText(activeDni);
      lastCalledDniLabel.setFont(activeDniFont);
      clearError();
      refreshQueueCountAsync();
      updateButtonsState();
      return;
    }
    showError("Error al llamar siguiente: " + response);
  }

  private void handleRenotifyResponse(String response) {
    if (response.startsWith("OK|RENOTIFIED|")) {
      String[] parts = response.split("\\|");
      if (parts.length >= 4) {
        currentDni = parts[2];
        clearError();
      } else {
        clearError();
      }
      scheduleRenotifyCooldown();
      refreshQueueCountAsync();
      updateButtonsState();
      return;
    }
    if (response.startsWith("OK|REMOVED_BY_LIMIT|")) {
      currentDni = null;
      clearRenotifyCooldown();
      lastCalledDniLabel.setText("Sin cliente en atencion");
      lastCalledDniLabel.setFont(idleDniFont);
      clearError();
      refreshQueueCountAsync();
      updateButtonsState();
      return;
    }
    if ("ERROR|NO_ACTIVE_CLIENT".equals(response)) {
      currentDni = null;
      clearRenotifyCooldown();
      lastCalledDniLabel.setText("Sin cliente en atencion");
      lastCalledDniLabel.setFont(idleDniFont);
      clearError();
      refreshQueueCountAsync();
      updateButtonsState();
      return;
    }
    showError("Error al re-notificar: " + response);
  }

  private void handleFinalizeResponse(String response) {
    if (response.startsWith("OK|FINALIZED|")) {
      currentDni = null;
      clearRenotifyCooldown();
      lastCalledDniLabel.setText("Sin cliente en atencion");
      lastCalledDniLabel.setFont(idleDniFont);
      clearError();
      refreshQueueCountAsync();
      updateButtonsState();
      return;
    }
    if ("ERROR|NO_ACTIVE_CLIENT".equals(response)) {
      currentDni = null;
      clearRenotifyCooldown();
      lastCalledDniLabel.setText("Sin cliente en atencion");
      lastCalledDniLabel.setFont(idleDniFont);
      clearError();
      refreshQueueCountAsync();
      updateButtonsState();
      return;
    }
    showError("Error al finalizar: " + response);
  }

  private void showError(String message) {
    errorLabel.setText(message);
  }

  private void clearError() {
    errorLabel.setText("");
  }

  private void scheduleRenotifyCooldown() {
    renotifyEnabledAtMs = System.currentTimeMillis() + RENOTIFY_COOLDOWN_MS;
  }

  private void clearRenotifyCooldown() {
    renotifyEnabledAtMs = 0L;
  }

  private void updateButtonsState() {
    boolean hasCurrent = currentDni != null && !currentDni.isEmpty();
    long now = System.currentTimeMillis();
    boolean renotifyOk =
        hasCurrent && (renotifyEnabledAtMs == 0L || now >= renotifyEnabledAtMs);
    renotifyButton.setEnabled(renotifyOk);
    finalizeButton.setEnabled(hasCurrent);
  }

  private String requestStationIdOrFail() {
    while (true) {
      String input = JOptionPane.showInputDialog(
          null,
          "Ingrese ID de Puesto",
          "ID de Puesto",
          JOptionPane.QUESTION_MESSAGE);
      if (input == null) {
        System.exit(0);
      }
      if (input != null) {
        String trimmed = input.trim();
        if (!trimmed.isEmpty()) {
          return trimmed;
        }
      }
    }
  }

  private void claimStationIdOrFail() {
    while (true) {
      String requestedStation = requestStationIdOrFail();
      String response = sendCommand("CLAIM_STATION|" + requestedStation);
      if (response.startsWith("OK|STATION_CLAIMED|")) {
        stationId = requestedStation;
        stationLabel.setText("Puesto: " + stationId);
        clearError();
        return;
      }
      if ("ERROR|STATION_ID_EXISTS".equals(response)) {
        showError("Error: el Puesto ID ya existe.");
        continue;
      }
      showError("Error al registrar puesto: " + response);
    }
  }

  private void releaseStationId() {
    if (stationId == null || stationId.isEmpty()) {
      return;
    }
    sendCommand("RELEASE_STATION|" + stationId);
  }

  private void runAsync(Runnable action) {
    Thread thread = new Thread(action, "operator-action-thread");
    thread.setDaemon(true);
    thread.start();
  }

}
