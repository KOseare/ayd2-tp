package com.grupo6.persistencia.xml;

import com.grupo6.persistencia.entidad.HistEntidad;
import java.io.IOException;
import java.util.Optional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class XmlHistEntidad implements HistEntidad {

  private final XmlDiskHelper disk;
  private final String filePath;

  public XmlHistEntidad(XmlDiskHelper disk, String filePath) {
    this.disk = disk;
    this.filePath = filePath;
  }

  @Override
  public void save(String histLine) throws IOException {
    if (histLine == null || !histLine.startsWith("HIST:")) {
      throw new IllegalArgumentException("Hist line must start with HIST:");
    }
    final String histPart = histLine.substring("HIST:".length());
    final Document doc = disk.newEmptyRoot();
    final Element root = doc.getDocumentElement();
    final Element history = doc.createElement("history");
    if (!histPart.isEmpty()) {
      for (String rec : histPart.split("~", -1)) {
        if (!rec.isEmpty()) {
          history.appendChild(XmlTurnCodec.lineRecordToElement(doc, rec));
        }
      }
    }
    root.appendChild(history);
    disk.writeRoot(filePath, doc);
  }

  @Override
  public Optional<String> load() throws IOException {
    final Document doc = disk.readRoot(filePath);
    final Element history = (Element) doc.getElementsByTagName("history").item(0);
    if (history == null) {
      return Optional.empty();
    }
    final StringBuilder line = new StringBuilder("HIST:");
    final NodeList turns = history.getElementsByTagName("turn");
    for (int i = 0; i < turns.getLength(); i++) {
      if (i > 0) {
        line.append('~');
      }
      line.append(XmlTurnCodec.elementToLineRecord((Element) turns.item(i)));
    }
    return Optional.of(line.toString());
  }
}
