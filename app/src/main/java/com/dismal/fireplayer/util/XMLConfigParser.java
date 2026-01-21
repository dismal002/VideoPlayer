package com.dismal.fireplayer.util;

import android.os.Environment;
import android.util.Log;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import com.dismal.fireplayer.provider.LocalMediaProviderContract;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class XMLConfigParser {
    static final String CFG_PATH = "etc/cfg-videoplayer.xml";
    static final String TAG = "XMLConfigParser";
    private HashMap<String, String> ConfigsMap = new HashMap<>();

    public boolean containsCarrier(String carrier) {
        return this.ConfigsMap.containsKey(carrier);
    }

    public String getCfg(String carrier) {
        return this.ConfigsMap.get(carrier);
    }

    public boolean loadCfgOverrides() {
        try {
            FileReader cfgReader = new FileReader(new File(Environment.getRootDirectory(), CFG_PATH));
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(cfgReader);
                XmlUtils.beginDocument(parser, "cfgOverrides");
                while (true) {
                    XmlUtils.nextElement(parser);
                    if (!"cfgOverride".equals(parser.getName())) {
                        break;
                    }
                    this.ConfigsMap.put(parser.getAttributeValue((String) null, LocalMediaProviderContract.NAME_COLUMN), parser.getAttributeValue((String) null, "value"));
                }
            } catch (XmlPullParserException e) {
                Log.w(TAG, "Exception in xml config parser " + e);
            } catch (IOException e2) {
                Log.w(TAG, "Exception in xml config parser " + e2);
            }
            return true;
        } catch (FileNotFoundException e3) {
            Log.w(TAG, "can't open " + Environment.getRootDirectory() + "/" + CFG_PATH);
            return false;
        }
    }
}
