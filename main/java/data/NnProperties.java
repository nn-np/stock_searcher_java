package main.java.data;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.util.Properties;

/**
 * 注意，如果配置文件的后缀名为.xml，则创建xml文件，并且提交也为xml格式，否则以默认格式提交
 * @author nnns
 */
public class NnProperties extends Properties {
    private File mFile;
    private boolean isFromXML;
    public NnProperties(String path) throws IOException, XMLStreamException {
        mFile = new File(path);
        String name = mFile.getName();
        isFromXML = name.substring(name.lastIndexOf(".") + 1).equals("xml");
        if (!mFile.exists()) {
            if (isFromXML) {
                initXML();
            } else {
                mFile.createNewFile();
            }
        }
        InputStream in = new BufferedInputStream(new FileInputStream(mFile));
        if (isFromXML) {
            this.loadFromXML(in);
        } else {
            this.load(in);
        }
        in.close();
    }

    private void initXML() throws FileNotFoundException, XMLStreamException {
        OutputStream out = new FileOutputStream(mFile);
        XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(out,"UTF-8");
        writer.writeStartDocument();
        writer.writeDTD("<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">");
        writer.writeStartElement("properties");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();
        writer.close();
    }

    private void submitToXML(String comments) throws IOException {
        OutputStream out = new FileOutputStream(mFile);
        storeToXML(out,comments);
        out.close();
    }

    private void submitTODefault(String comments) throws IOException {
        OutputStream out = new FileOutputStream(mFile);
        store(out, comments);
        out.close();
    }

    public void submit() throws IOException {
        if (isFromXML) {
            submitToXML(null);
        } else {
            submitTODefault(null);
        }
    }

    public void submit(String comments) throws IOException {
        if (isFromXML) {
            submitToXML(comments);
        } else {
            submitTODefault(comments);
        }
    }
}
