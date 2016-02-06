package com.afollestad.polarupgradetool;

import java.io.*;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Aidan Follestad (afollestad)
 */
public class XmlElementExtractor {

    private static final String REGEX = "<%s name=\"%s\">[\\s\\S]*?<\\/%s>";

    private final File mFile;
    private final Pattern[] mPatterns;

    public XmlElementExtractor(File xmlFile, String[] tagNames, String[] names) {
        if (tagNames == null || names == null || tagNames.length != names.length)
            throw new IllegalArgumentException("tagNames and names must be non-null matching length arrays.");
        mFile = xmlFile;
        mPatterns = new Pattern[tagNames.length];
        for (int i = 0; i < mPatterns.length; i++) {
            mPatterns[i] = Pattern.compile(String.format(REGEX,
                    tagNames[i], names[i], tagNames[i]));
        }
    }

    public HashMap<String, String> find() {
        if (!mFile.exists()) {
            Main.LOG("File %s does not exist.", mFile.getAbsolutePath());
            return null;
        }

        InputStream is = null;
        BufferedReader reader = null;

        final HashMap<String, String> result = new HashMap<>(mPatterns.length);

        try {
            is = new FileInputStream(mFile);
            reader = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = reader.readLine()) != null) {
                for (Pattern p : mPatterns) {
                    Matcher matcher = p.matcher(line);
                    if (matcher.find()) {
                        String name = AttributeExtractor.getAttributeValue("name", line);
                        String value = AttributeExtractor.getElementValue(line);
                        result.put(name, value);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Main.LOG("[ERROR] Failed to read %s: %s", mFile.getAbsolutePath(), e.getMessage());
            return null;
        } finally {
            Util.closeQuietely(reader);
            Util.closeQuietely(is);
        }

        return result;
    }
}