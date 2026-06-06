package com.grupo6.persistencia.xml;

import com.grupo6.persistencia.entidad.StationsEntidad;
import java.io.IOException;
import java.util.Optional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class XmlStationsEntidad implements StationsEntidad {

  private final XmlDiskHelper disk;
  private final String filePath;

  public XmlStationsEntidad(XmlDiskHelper disk, String filePath) {
    this.disk = disk;
    this.filePath = filePath;
  }

  @Override
  public void save(String stationsBlock) throws IOException {
    final Document doc = disk.newEmptyRoot();
    final Element root = doc.getDocumentElement();
    final Element stations = doc.createElement("stations");
    final String block = stationsBlock == null ? "" : stationsBlock;
    for (String line : block.split("\n", -1)) {
      if (!line.isEmpty()) {
        final Element station = doc.createElement("station");
        station.setTextContent(line);
        stations.appendChild(station);
      }
    }
    root.appendChild(stations);
    disk.writeRoot(filePath, doc);
  }

  @Override
  public Optional<String> load() throws IOException {
    final Document doc = disk.readRoot(filePath);
    final Element stations = (Element) doc.getElementsByTagName("stations").item(0);
    if (stations == null) {
      return Optional.empty();
    }
    final NodeList items = stations.getElementsByTagName("station");
    if (items.getLength() == 0) {
      return Optional.of("");
    }
    final StringBuilder block = new StringBuilder();
    for (int i = 0; i < items.getLength(); i++) {
      if (i > 0) {
        block.append('\n');
      }
      block.append(items.item(i).getTextContent());
    }
    return Optional.of(block.toString());
  }
}
