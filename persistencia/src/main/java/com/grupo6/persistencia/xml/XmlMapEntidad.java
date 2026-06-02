package com.grupo6.persistencia.xml;

import com.grupo6.persistencia.entidad.MapEntidad;
import java.io.IOException;
import java.util.Optional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class XmlMapEntidad implements MapEntidad {

  private final XmlDiskHelper disk;
  private final String filePath;

  public XmlMapEntidad(XmlDiskHelper disk, String filePath) {
    this.disk = disk;
    this.filePath = filePath;
  }

  @Override
  public void save(String mapLine) throws IOException {
    if (mapLine == null || !mapLine.startsWith("MAP:")) {
      throw new IllegalArgumentException("Map line must start with MAP:");
    }
    final String mapPart = mapLine.substring("MAP:".length());
    final Document doc = disk.newEmptyRoot();
    final Element root = doc.getDocumentElement();
    final Element assignments = doc.createElement("assignments");
    if (!mapPart.isEmpty()) {
      for (String entry : mapPart.split("~", -1)) {
        if (entry.isEmpty()) {
          continue;
        }
        final String[] kv = entry.split("=", 2);
        if (kv.length != 2) {
          throw new IllegalArgumentException("Invalid MAP entry: " + entry);
        }
        final Element assignment = doc.createElement("assignment");
        final Element station = doc.createElement("station");
        station.setTextContent(unescapePipe(kv[0]));
        assignment.appendChild(station);
        assignment.appendChild(XmlTurnCodec.lineRecordToElement(doc, kv[1]));
        assignments.appendChild(assignment);
      }
    }
    root.appendChild(assignments);
    disk.writeRoot(filePath, doc);
  }

  @Override
  public Optional<String> load() throws IOException {
    final Document doc = disk.readRoot(filePath);
    final Element assignments = (Element) doc.getElementsByTagName("assignments").item(0);
    if (assignments == null) {
      return Optional.empty();
    }
    final StringBuilder line = new StringBuilder("MAP:");
    final NodeList items = assignments.getElementsByTagName("assignment");
    for (int i = 0; i < items.getLength(); i++) {
      if (i > 0) {
        line.append('~');
      }
      final Element assignment = (Element) items.item(i);
      line.append(escapePipe(textChild(assignment, "station")));
      line.append('=');
      line.append(XmlTurnCodec.elementToLineRecord((Element) assignment.getElementsByTagName("turn").item(0)));
    }
    return Optional.of(line.toString());
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
