/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For 
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * 
 * $Id$
 */

package org.dataone.cn.indexer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlDocumentUtility {

    private static Log log = LogFactory.getLog(XmlDocumentUtility.class);

    private static final String INPUT_ENCODING = "UTF-8";

    //private static DocumentBuilderFactory documentBuilderFactory = null;
    //private static DocumentBuilder builder = null;

    /*static {
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        try {
            builder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }*/

    public static Document loadDocument(String filePath) throws ParserConfigurationException,
            IOException, SAXException {
        return loadDocument(filePath, INPUT_ENCODING);
    }

    private static Document loadDocument(String filePath, String input_encoding)
            throws ParserConfigurationException, IOException, SAXException {
        Document doc = null;
        FileInputStream fis = null;
        InputStreamReader isr = null;
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            fis = new FileInputStream(filePath);
            isr = new InputStreamReader(fis, input_encoding);
            InputSource source = new InputSource(isr);
            doc = builder.parse(source);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error parsing file: " + filePath);
        } finally {
            if (isr != null) {
                isr.close();
            }
            if (fis != null) {
                fis.close();
            }
        }
        return doc;
    }

    public static Document generateXmlDocument(InputStream smdStream) throws SAXException {
        Document doc = null;

        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            doc = builder.parse(smdStream);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } catch (ParserConfigurationException e) {
            log.error(e.getMessage(), e);
        }

        return doc;
    }

    /*public static DocumentBuilder getDocumentBuilder() {
        return builder;
    }*/

}
