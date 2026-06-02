package com.grupo6.persistencia.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

final class XmlTurnCodec {

  private XmlTurnCodec() {}

  static Element lineRecordToElement(Document doc, String rec) {
    final String[] p = rec.split("\\|", -1);
    if (p.length != 6) {
      throw new IllegalArgumentException("Invalid turn record: " + rec);
    }
    final Element turn = doc.createElement("turn");
    appendChild(doc, turn, "id", p[0]);
    appendChild(doc, turn, "dni", unescapePipe(p[1]));
    appendChild(doc, turn, "estado", p[2]);
    appendChild(doc, turn, "nroLlamados", p[3]);
    appendChild(doc, turn, "estacion", p[4].isEmpty() ? "" : unescapePipe(p[4]));
    appendChild(doc, turn, "registro", p[5]);
    return turn;
  }

  static String elementToLineRecord(Element turn) {
    final String estacion = textChild(turn, "estacion");
    return new StringBuilder()
        .append(textChild(turn, "id"))
        .append('|')
        .append(escapePipe(textChild(turn, "dni")))
        .append('|')
        .append(textChild(turn, "estado"))
        .append('|')
        .append(textChild(turn, "nroLlamados"))
        .append('|')
        .append(estacion.isEmpty() ? "" : escapePipe(estacion))
        .append('|')
        .append(textChild(turn, "registro"))
        .toString();
  }

  private static void appendChild(Document doc, Element parent, String tag, String value) {
    final Element child = doc.createElement(tag);
    child.setTextContent(value);
    parent.appendChild(child);
  }

  private static String textChild(Element parent, String tag) {
    final Element child = (Element) parent.getElementsByTagName(tag).item(0);
    if (child == null || child.getTextContent() == null) {
      return "";
    }
    return child.getTextContent();
  }

  private static String escapePipe(String s) {
    return s.replace("\\", "\\\\").replace("|", "\\|");
  }

  private static String unescapePipe(String s) {
    final StringBuilder out = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '\\') {
        i++;
        if (i >= s.length()) {
          break;
        }
        out.append(s.charAt(i));
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }
}
