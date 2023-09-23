package org.otaibe.commons.quarkus.core.utils;

import jakarta.annotation.PostConstruct;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.w3c.dom.Document;

/**
 * Created by triphon on 14-6-14.
 */
@Slf4j
@SuppressWarnings("unchecked")
public class XmlUtils {
    public static final String YES = "yes";
    public static final String IDENT_AMOUNT = "{http://xml.apache.org/xslt}indent-amount";

    public static final String TEXT = "text";
    public static final String URL = "url";
    public static final String THUMBNAIL_URL = "thumbnailUrl";
    public static final QName QNAME_TEXT = new QName(TEXT);
    public static final QName QNAME_URL = new QName(URL);

    private TransformerFactory transformerFactory;

    @PostConstruct
    public void init() {
        transformerFactory = TransformerFactory.newInstance();
    }

    //JAXB marshallers are not necessarily thread-safe.
    public String objToXmlString(final Object object, final ThreadLocal<Marshaller> marshaller) throws Exception {
        if (marshaller == null) {
            log.error("null ThreadLocal<Marshaller>");
        }
        final StringWriter stringWriter = new StringWriter();
        final Marshaller marshaller1 = marshaller.get();
        if (marshaller1 == null) {
            log.error("null marshaller");
        }
        marshaller1.marshal(object, new StreamResult(stringWriter));
        return stringWriter.toString();
    }

    public Object toStringLazy(final Object object, final ThreadLocal<Marshaller> marshaller) {
        return new ToStringLazy(object, marshaller, this);
    }

    public <T extends Object> T xmlStringToObject(final String xml, final ThreadLocal<Unmarshaller> unmarshaller) {
        return xmlStringToObject(null, xml, unmarshaller);
    }

    //JAXB unmarshallers are not necessarily thread-safe.
    public <T extends Object> T xmlStringToObject(final Class<T> clazz, final String xml, final ThreadLocal<Unmarshaller> unmarshaller) {
        try {
            final T result = (T) unmarshaller.get().unmarshal(
                    new StreamSource(
                            new StringReader(xml))
            );
            return result;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends Object> T deepClone(final T input, final ThreadLocal<Marshaller> marshaller, final ThreadLocal<Unmarshaller> unmarshaller) {
        final String s = toStringLazy(input, marshaller).toString();
        return xmlStringToObject(s, unmarshaller);
    }

    public <T extends Object> T[] createArray(final Class<T> clazz, final T... objects) {
        final T[] res = (T[]) Array.newInstance(clazz, objects.length);
        for (int i = 0; i < objects.length; i++) {
            res[i] = objects[i];
        }
        return res;
    }

    public XMLGregorianCalendar toXMLGregorianCalendar(final Date date) {
        final GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(date);
        gregorianCalendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
        } catch (final DatatypeConfigurationException e) {
            log.error("unable to create XMLGregorianCalendar");
        }
        return null;
    }

    /*
     * Converts XMLGregorianCalendar to java.util.Date in Java
     */
    public static Date toDate(final XMLGregorianCalendar calendar){
        if(calendar == null) {
            return null;
        }
        return calendar.toGregorianCalendar().getTime();
    }

    public Transformer createTransformer() {
        try {
            final Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, YES);
            transformer.setOutputProperty(IDENT_AMOUNT, "3");
            return transformer;
        } catch (final TransformerConfigurationException e) {
            log.error("unable to create transformer:", e);
            return null;
        }
    }

    public String formatXML(final String input)
    {
        try
        {
            final Transformer transformer = createTransformer();
            if (transformer == null) {
                return input;
            }

            final StreamResult result = new StreamResult(new StringWriter());
            final DOMSource source = new DOMSource(parseXml(input));
            transformer.transform(source, result);
            final String resultString = result.getWriter().toString();
            return resultString;
        } catch (final Exception e)
        {
            log.error("unable to ident xml:", e);
            return input;
        }
    }

    private Document parseXml(final String in)
    {
        try
        {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            final DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(IOUtils.toInputStream(in));
        } catch (final Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public JAXBElement<String> buildStringJAXBElement(final String text) {
        final boolean isUrl = UrlValidator.getInstance().isValid(text);
        final QName qName = isUrl ? QNAME_URL : QNAME_TEXT;
        final String text1 = StringEscapeUtils.unescapeHtml4(text);
        return new JAXBElement<>(qName, String.class, text1);
    }

    @AllArgsConstructor
    private static class ToStringLazy {
        private Object input;
        private ThreadLocal<Marshaller> marshaller;
        private XmlUtils utils;

        @Override
        public String toString() {
            try {
                return utils.objToXmlString(input, marshaller);
            } catch (final Exception e) {
                log.error("unable to serialize", e);
            }
            return StringUtils.EMPTY;
        }
    }

}
