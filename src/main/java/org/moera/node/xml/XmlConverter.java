package org.moera.node.xml;

import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class XmlConverter {

    public static void convert(String xml, ContentHandler handler) throws XmlConverterException {
        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            SAXParser saxParser = saxParserFactory.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();
            xmlReader.setContentHandler(handler);
            xmlReader.parse(new InputSource(new StringReader(XmlUtils.delicateAmps(xml, false).toString())));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new XmlConverterException(e);
        }
    }

}
