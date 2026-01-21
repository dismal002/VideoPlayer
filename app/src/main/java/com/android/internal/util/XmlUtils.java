package com.android.internal.util;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import java.io.IOException;

public class XmlUtils {
    public static void beginDocument(XmlPullParser parser, String firstElementName)
            throws XmlPullParserException, IOException {
        int type;
        while ((type = parser.next()) != 2 && type != 1) {
        }
        if (type != 2) {
            throw new XmlPullParserException("No start tag found");
        } else if (!parser.getName().equals(firstElementName)) {
            throw new XmlPullParserException(
                    "Unexpected start tag: found " + parser.getName() + ", expected " + firstElementName);
        }
    }

    public static void nextElement(XmlPullParser parser) throws XmlPullParserException, IOException {
        int type;
        while ((type = parser.next()) != 2 && type != 1) {
        }
    }
}
