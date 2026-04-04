package com.grupo6.terminal_de_registro;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import com.grupo6.environment.Environment;
import com.grupo6.ui.AppUiTheme;

public class RegistrationTerminalFrame extends JFrame {

  private static final long serialVersionUID = 1L;
  private static final String OPERATOR_HOST = Environment.OPERATOR_HOST;
  private static final int OPERATOR_PORT = Environment.OPERATOR_PORT;
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
          JOptionPane.ERROR_MESSAGE);
    }
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

    statusLabel = new JLabel("Estado: esperando registro...", SwingConstants.CENTER);
    statusLabel.setFont(statusFont);
    statusLabel.setForeground(AppUiTheme.TEXT_BODY);

    JButton joinButton = new JButton("Unirse a la lista de espera");
    joinButton.setFont(base.deriveFont(Font.BOLD, 14f));
    joinButton.setMargin(new Insets(12, 28, 12, 28));
    joinButton.addActionListener(e -> registerClient());

    JPanel footer = new JPanel(new BorderLayout(0, 12));
    footer.setOpaque(false);
    footer.setBorder(BorderFactory.createEmptyBorder(4, 20, 0, 20));
    footer.add(statusLabel, BorderLayout.NORTH);
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

    JPanel root = new JPanel(new BorderLayout(16, 16));
    root.setBackground(AppUiTheme.BG_APP);
    root.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));
    root.add(hintPanel, BorderLayout.NORTH);
    root.add(hero, BorderLayout.CENTER);
    root.add(footer, BorderLayout.SOUTH);

    setContentPane(root);
  }
}
