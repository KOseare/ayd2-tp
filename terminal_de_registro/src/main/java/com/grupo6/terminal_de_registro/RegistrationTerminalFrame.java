package com.grupo6.terminal_de_registro;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import com.grupo6.ui.AppUiTheme;

public class RegistrationTerminalFrame extends JFrame implements IVista {
  private static final long serialVersionUID = 1L;

  private Controlador controlador = null;
  private final JTextField documentField;
  private final JLabel errorLabel;

  private void showError(String message) {
    errorLabel.setText(message);
  }

  private void clearError() {
    errorLabel.setText("");
  }

  private String getDocumentText() {
    return documentField.getText() == null ? "" : documentField.getText().trim();
  }

  public RegistrationTerminalFrame() {
    setTitle("Terminal de Registro");
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setMinimumSize(new Dimension(440, 380));
    setSize(480, 400);
    setLocationRelativeTo(null);

    Font base = AppUiTheme.baseUiFont();
    Font statusFont = base.deriveFont(Font.PLAIN, 12f);

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

    JButton joinButton = new JButton("Registrarse");
    joinButton.setFont(base.deriveFont(Font.BOLD, 14f));
    joinButton.setMargin(new Insets(12, 28, 12, 28));
    joinButton.addActionListener(e -> controlador.registrarse(getDocumentText()));

    JPanel footer = new JPanel(new BorderLayout(0, 12));
    footer.setOpaque(false);
    footer.setBorder(BorderFactory.createEmptyBorder(4, 20, 0, 20));
    footer.add(errorLabel, BorderLayout.NORTH);
    JPanel buttonRow = new JPanel();
    buttonRow.setOpaque(false);
    buttonRow.add(joinButton);
    footer.add(buttonRow, BorderLayout.CENTER);

    GridBagConstraints gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 0;
    gc.weightx = 1;
    gc.fill = GridBagConstraints.HORIZONTAL;

    JPanel root = new JPanel(new BorderLayout(16, 16));
    root.setBackground(AppUiTheme.BG_APP);
    root.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));
    root.add(hero, BorderLayout.CENTER);
    root.add(footer, BorderLayout.SOUTH);

    setContentPane(root);
  }

  @Override
  public void setControlador(Controlador controlador) {
    this.controlador = controlador;
  }

  @Override
  public void actualizar(ModeloVista modelo) {
    if (modelo.error == null) {
      clearError();
    } else {
      showError(modelo.error);
    }
    if (modelo.dni == null) {
      documentField.setText("");
    } else {
      documentField.setText(modelo.dni);
    }
  }
}
