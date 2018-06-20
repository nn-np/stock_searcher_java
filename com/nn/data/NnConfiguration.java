package com.nn.data;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 * 配置文件读取
 */
public class NnConfiguration {
    private Document m_document;
    public NnConfiguration(String url) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        m_document = builder.parse(url);
    }

    public String getString(String name) {
        return m_document.getElementsByTagName(name).item(0).getTextContent();
    }
}
