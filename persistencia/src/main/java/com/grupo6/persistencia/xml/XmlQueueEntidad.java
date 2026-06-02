package com.grupo6.persistencia.xml;

import com.grupo6.persistencia.entidad.QueueEntidad;
import java.io.IOException;
import java.util.Optional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class XmlQueueEntidad implements QueueEntidad {

  private final XmlDiskHelper disk;
  private final String filePath;

  public XmlQueueEntidad(XmlDiskHelper disk, String filePath) {
    this.disk = disk;
    this.filePath = filePath;
  }

  @Override
  public void save(String queueBlock) throws IOException {
    if (queueBlock == null || queueBlock.trim().isEmpty()) {
      throw new IllegalArgumentException("Queue block must not be empty");
    }
    int next = -1;
    String queuePart = null;
    for (String line : queueBlock.split("\n", -1)) {
      if (line.startsWith("NEXT:")) {
        next = Integer.parseInt(line.substring("NEXT:".length()).trim());
      } else if (line.startsWith("QUEUE:")) {
        queuePart = line.substring("QUEUE:".length());
      }
    }
    if (next < 0 || queuePart == null) {
      throw new IllegalArgumentException("Queue block must contain NEXT and QUEUE lines");
    }
    final Document doc = disk.newEmptyRoot();
    final Element root = doc.getDocumentElement();
    final Element nextEl = doc.createElement("next");
    nextEl.setTextContent(String.valueOf(next));
    root.appendChild(nextEl);
    final Element queue = doc.createElement("queue");
    if (!queuePart.isEmpty()) {
      for (String rec : queuePart.split("~", -1)) {
        if (!rec.isEmpty()) {
          queue.appendChild(XmlTurnCodec.lineRecordToElement(doc, rec));
        }
      }
    }
    root.appendChild(queue);
    disk.writeRoot(filePath, doc);
  }

  @Override
  public Optional<String> load() throws IOException {
    final Document doc = disk.readRoot(filePath);
    final Element root = doc.getDocumentElement();
    final Element nextEl = (Element) root.getElementsByTagName("next").item(0);
    final Element queueEl = (Element) root.getElementsByTagName("queue").item(0);
    if (nextEl == null || queueEl == null) {
      return Optional.empty();
    }
    final StringBuilder block = new StringBuilder();
    block.append("NEXT:").append(nextEl.getTextContent().trim()).append('\n');
    block.append("QUEUE:");
    final NodeList turns = queueEl.getElementsByTagName("turn");
    for (int i = 0; i < turns.getLength(); i++) {
      if (i > 0) {
        block.append('~');
      }
      block.append(XmlTurnCodec.elementToLineRecord((Element) turns.item(i)));
    }
    return Optional.of(block.toString());
  }
}
