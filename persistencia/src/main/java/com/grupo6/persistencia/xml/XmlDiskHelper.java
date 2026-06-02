package com.grupo6.persistencia.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public final class XmlDiskHelper {

  public Document readRoot(String filePath) throws IOException {
    final File target = new File(filePath);
    if (!target.isFile() || target.length() == 0) {
      return newEmptyRoot();
    }
    try {
      final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      try (FileInputStream in = new FileInputStream(target)) {
        return builder.parse(in);
      }
    } catch (ParserConfigurationException | SAXException e) {
      throw new IOException(e);
    }
  }

  public void writeRoot(String filePath, Document root) throws IOException {
    if (root == null || root.getDocumentElement() == null) {
      throw new IllegalArgumentException("Root must not be null");
    }
    final File target = new File(filePath);
    final File parent = target.getParentFile();
    if (parent != null) {
      Files.createDirectories(parent.toPath());
    }
    final File temp = new File(target.getAbsolutePath() + ".tmp");
    try {
      final Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
      transformer.setOutputProperty(OutputKeys.INDENT, "no");
      try (FileWriter writer = new FileWriter(temp, StandardCharsets.UTF_8)) {
        transformer.transform(new DOMSource(root), new StreamResult(writer));
        writer.write('\n');
      }
    } catch (TransformerException e) {
      throw new IOException(e);
    }
    Files.move(
        temp.toPath(),
        target.toPath(),
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.ATOMIC_MOVE);
  }

  Document newEmptyRoot() throws IOException {
    try {
      final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      doc.appendChild(doc.createElement("root"));
      return doc;
    } catch (ParserConfigurationException e) {
      throw new IOException(e);
    }
  }

}
