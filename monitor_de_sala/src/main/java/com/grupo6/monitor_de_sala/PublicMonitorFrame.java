package com.grupo6.monitor_de_sala;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import com.grupo6.environment.Environment;
import com.grupo6.ui.AppUiTheme;

public class PublicMonitorFrame extends JFrame {

  private static final long serialVersionUID = 1L;
  private static final String SERVER_HOST = Environment.SERVER_HOST;
  private static final int SERVER_PORT = Environment.SERVER_PORT;
  private static final int HISTORY_LIMIT = 5;

  private final JLabel currentTurnLabel;
  private final JLabel currentStationLabel;
  private final JLabel errorLabel;
  private final List<JLabel> historyRows = new ArrayList<>();
  private final Deque<String> callHistory = new ArrayDeque<>();
  private final Font historyFont;

  public PublicMonitorFrame() {
    setTitle("Monitor de Sala");
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setMinimumSize(new Dimension(520, 520));
    setSize(720, 560);
    setLocationRelativeTo(null);

    Font base = AppUiTheme.baseUiFont();
    historyFont = base.deriveFont(Font.PLAIN, 20f);
    Font statusFont = base.deriveFont(Font.PLAIN, 12f);

    JLabel currentCaption = new JLabel("TURNO ACTUAL", SwingConstants.CENTER);
    currentCaption.setFont(base.deriveFont(Font.BOLD, 11f));
    currentCaption.setForeground(AppUiTheme.TEXT_MUTED);

    currentTurnLabel = new JLabel("-", SwingConstants.CENTER);
    currentTurnLabel.setFont(base.deriveFont(Font.BOLD, 64f));
    currentTurnLabel.setForeground(AppUiTheme.TEXT_HERO_DNI);
    currentTurnLabel.setOpaque(false);

    currentStationLabel = new JLabel("Puesto ID: -", SwingConstants.CENTER);
    currentStationLabel.setFont(base.deriveFont(Font.BOLD, 24f));
    currentStationLabel.setForeground(AppUiTheme.TEXT_BODY);

    JPanel hero = new JPanel(new BorderLayout(0, 10));
    hero.setBackground(AppUiTheme.BG_HERO);
    hero.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(AppUiTheme.BORDER_HERO, 2),
        BorderFactory.createEmptyBorder(20, 24, 28, 24)));
    hero.add(currentCaption, BorderLayout.NORTH);
    hero.add(currentTurnLabel, BorderLayout.CENTER);
    hero.add(currentStationLabel, BorderLayout.SOUTH);

    errorLabel = new JLabel("", SwingConstants.CENTER);
    errorLabel.setFont(statusFont);
    errorLabel.setForeground(new Color(180, 30, 30));

    JLabel historyCaption = new JLabel("HISTORIAL RECIENTE", SwingConstants.CENTER);
    historyCaption.setFont(base.deriveFont(Font.BOLD, 11f));
    historyCaption.setForeground(AppUiTheme.TEXT_MUTED);

    JPanel historyPanel = new JPanel(new GridLayout(HISTORY_LIMIT, 1, 8, 8));
    historyPanel.setOpaque(false);
    for (int index = 0; index < HISTORY_LIMIT; index++) {
      JLabel row = historyRow("-", historyFont);
      historyRows.add(row);
      historyPanel.add(row);
    }

    JPanel historyBlock = new JPanel(new BorderLayout(0, 10));
    historyBlock.setOpaque(false);
    historyBlock.add(historyCaption, BorderLayout.NORTH);
    historyBlock.add(historyPanel, BorderLayout.CENTER);

    JPanel statusWrap = new JPanel(new GridBagLayout());
    statusWrap.setOpaque(false);
    GridBagConstraints gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 0;
    gc.weightx = 1;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.insets = new Insets(0, 0, 8, 0);
    statusWrap.add(errorLabel, gc);

    JPanel south = new JPanel(new BorderLayout(0, 16));
    south.setOpaque(false);
    south.add(statusWrap, BorderLayout.NORTH);
    south.add(historyBlock, BorderLayout.CENTER);

    JPanel root = new JPanel(new BorderLayout(16, 16));
    root.setBackground(AppUiTheme.BG_APP);
    root.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));
    root.add(hero, BorderLayout.CENTER);
    root.add(south, BorderLayout.SOUTH);

    setContentPane(root);
    startMonitorClient();
  }

  private static JLabel historyRow(String doc, Font font) {
    JLabel row = new JLabel(doc, SwingConstants.CENTER);
    row.setFont(font);
    row.setForeground(AppUiTheme.TEXT_BODY);
    row.setOpaque(true);
    row.setBackground(AppUiTheme.BG_CARD);
    row.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(AppUiTheme.BORDER_CARD, 1),
        BorderFactory.createEmptyBorder(8, 12, 8, 12)));
    return row;
  }

  private void startMonitorClient() {
    Thread listenerThread = new Thread(() -> {
      while (!Thread.currentThread().isInterrupted()) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

          writer.println("SUBSCRIBE_MONITOR");
          String subscribedAck = reader.readLine();
          if (subscribedAck == null || !subscribedAck.startsWith("OK|SUBSCRIBED")) {
            SwingUtilities.invokeLater(() -> showError("Error de suscripcion"));
            sleepQuietly(2000);
            continue;
          }

          SwingUtilities.invokeLater(this::clearError);
          String line;
          while ((line = reader.readLine()) != null) {
            String eventLine = line;
            SwingUtilities.invokeLater(() -> handleEvent(eventLine));
          }
        } catch (IOException e) {
          SwingUtilities.invokeLater(() -> showError("Error de conexion con servidor"));
          sleepQuietly(2000);
        }
      }
    }, "monitor-client-listener");

    listenerThread.setDaemon(true);
    listenerThread.start();
  }

  private void handleEvent(String eventLine) {
    String[] parts = eventLine.split("\\|");
    if (parts.length < 2 || !"EVENT".equals(parts[0])) {
      showError("Error: mensaje no valido");
      return;
    }

    if ("CALL".equals(parts[1]) && parts.length >= 4) {
      String dni = parts[2];
      String stationId = parts[3];
      updateTurn(dni, stationId);
      clearError();
      return;
    }

    if ("RENOTIFY".equals(parts[1]) && parts.length >= 5) {
      String dni = parts[2];
      String stationId = parts[3];
      updateCurrentTurnOnly(dni, stationId);
      runPriorityBlink();
      clearError();
      return;
    }

    if ("REMOVED".equals(parts[1]) && parts.length >= 4) {
      updateCurrentTurnOnly("-", "-");
      clearError();
      return;
    }

    if ("FINALIZED".equals(parts[1]) && parts.length >= 4) {
      clearError();
      return;
    }

    showError("Error: evento no soportado");
  }

  private void updateTurn(String dni, String stationId) {
    String rowText = dni + " - Puesto " + stationId;
    callHistory.addFirst(rowText);
    while (callHistory.size() > HISTORY_LIMIT) {
      callHistory.removeLast();
    }

    currentTurnLabel.setText(dni);
    currentStationLabel.setText("Puesto ID: " + stationId);

    int index = 0;
    for (String value : callHistory) {
      historyRows.get(index).setText(value);
      index++;
    }
    while (index < HISTORY_LIMIT) {
      historyRows.get(index).setText("-");
      index++;
    }
  }

  private void updateCurrentTurnOnly(String dni, String stationId) {
    currentTurnLabel.setText(dni);
    currentStationLabel.setText("Puesto ID: " + stationId);
  }

  private void showError(String message) {
    errorLabel.setText(message);
  }

  private void clearError() {
    errorLabel.setText("");
  }

  private void runPriorityBlink() {
    final int[] ticks = { 0 };
    Timer timer = new Timer(200, null);
    timer.addActionListener(e -> {
      if (ticks[0] % 2 == 0) {
        currentTurnLabel.setForeground(AppUiTheme.TEXT_HERO_DNI);
        currentTurnLabel.setOpaque(true);
        currentTurnLabel.setBackground(AppUiTheme.BG_HERO);
      } else {
        currentTurnLabel.setForeground(AppUiTheme.BG_APP);
        currentTurnLabel.setOpaque(true);
        currentTurnLabel.setBackground(AppUiTheme.TEXT_HERO_DNI);
      }
      currentTurnLabel.repaint();
      ticks[0]++;
      if (ticks[0] >= 6) {
        timer.stop();
        currentTurnLabel.setOpaque(false);
        currentTurnLabel.setForeground(AppUiTheme.TEXT_HERO_DNI);
        currentTurnLabel.repaint();
      }
    });
    timer.start();
  }

  private void sleepQuietly(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException interruptedException) {
      Thread.currentThread().interrupt();
    }
  }
}
