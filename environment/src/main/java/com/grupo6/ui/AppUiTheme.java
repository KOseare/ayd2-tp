package com.grupo6.ui;

import java.awt.Color;
import java.awt.Font;

import javax.swing.UIManager;

/**
 * Shared colors and font baseline for queue-management Swing apps.
 */
public final class AppUiTheme {

  public static final Color BG_APP = new Color(248, 249, 251);
  public static final Color BG_HERO = new Color(240, 247, 255);
  public static final Color BORDER_HERO = new Color(120, 170, 220);
  public static final Color TEXT_MUTED = new Color(100, 108, 120);
  public static final Color TEXT_HERO_DNI = new Color(20, 45, 90);
  public static final Color TEXT_BODY = new Color(70, 78, 90);
  public static final Color BG_CARD = new Color(252, 253, 255);
  public static final Color BORDER_CARD = new Color(200, 214, 235);

  private AppUiTheme() {}

  public static Font baseUiFont() {
    Font f = UIManager.getFont("Label.font");
    return f != null ? f : new Font(Font.SANS_SERIF, Font.PLAIN, 12);
  }
}
