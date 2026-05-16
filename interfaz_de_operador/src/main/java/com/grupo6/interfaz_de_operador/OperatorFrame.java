package com.grupo6.interfaz_de_operador;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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

import com.grupo6.ui.AppUiTheme;

public class OperatorFrame extends JFrame implements IVista {
  private static final long serialVersionUID = 1L;

  private final JLabel stationLabel;
  private final JLabel queueCountLabel;
  private final JLabel lastCalledCaption;
  private final JLabel lastCalledDniLabel;
  private final JLabel errorLabel;
  private final JButton callNextButton;
  private final JButton renotifyButton;
  private final JButton finalizeButton;
  private Timer queueRefreshTimer = null;
  private Timer buttonsRefreshTimer = null;
  private final Font activeDniFont;
  private final Font idleDniFont;

  public OperatorFrame() {
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

    renotifyButton = new JButton("Re-notificar");
    renotifyButton.setFont(base.deriveFont(Font.BOLD, 14f));
    renotifyButton.setMargin(new Insets(12, 28, 12, 28));

    finalizeButton = new JButton("Finalizar Atencion");
    finalizeButton.setFont(base.deriveFont(Font.BOLD, 14f));
    finalizeButton.setMargin(new Insets(12, 28, 12, 28));

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
  }

  private void showError(String message) {
    errorLabel.setText(message);
  }

  private void clearError() {
    errorLabel.setText("");
  }

  private void updateButtonsState(boolean renotifyEnabled, boolean finalizeEnabled) {
    renotifyButton.setEnabled(renotifyEnabled);
    finalizeButton.setEnabled(finalizeEnabled);
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

  @Override
  public void setControlador(Controlador controlador) {
    callNextButton
        .addActionListener(e -> controlador.callNextClient((runnable) -> SwingUtilities.invokeLater(runnable)));
    renotifyButton
        .addActionListener(e -> controlador.renotifyClient((runnable) -> SwingUtilities.invokeLater(runnable)));
    finalizeButton
        .addActionListener(e -> controlador.finalizeClient((runnable) -> SwingUtilities.invokeLater(runnable)));
    queueRefreshTimer = new Timer(3000,
        e -> controlador.refreshQueueCountAsync((runnable) -> SwingUtilities.invokeLater(runnable)));
    queueRefreshTimer.start();
    buttonsRefreshTimer = new Timer(1000, e -> controlador.cooldownUpdate());
    buttonsRefreshTimer.start();
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        // No se invoca si se cierra con CTRL+C desde la terminal
        queueRefreshTimer.stop();
        buttonsRefreshTimer.stop();
        controlador.releaseStationId();
      }
    });
    controlador.claimStationIdOrFail(() -> requestStationIdOrFail());
    controlador.refreshQueueCountAsync((runnable) -> SwingUtilities.invokeLater(runnable));
    controlador.cooldownUpdate();
    controlador.subscribirse();
  }

  @Override
  public void actualizar(ModeloVista modelo) {
    if (modelo.currentDni != null) {
      lastCalledDniLabel.setText(modelo.currentDni);
      lastCalledDniLabel.setFont(activeDniFont);
    } else {
      lastCalledDniLabel.setText("Sin cliente en atención");
      lastCalledDniLabel.setFont(idleDniFont);
    }
    if (modelo.error == null) {
      clearError();
    } else {
      showError(modelo.error);
    }
    if (modelo.personasEnCola >= 0) {
      queueCountLabel.setText("Personas en cola: " + modelo.personasEnCola);
    } else {
      queueCountLabel.setText("Personas en cola: sin datos");
    }
    if (modelo.stationId != null) {
      stationLabel.setText("Puesto: " + modelo.stationId);
    }
    updateButtonsState(modelo.renotifyBtnEnabled, modelo.finalizeBtnEnabled);
  }
}
