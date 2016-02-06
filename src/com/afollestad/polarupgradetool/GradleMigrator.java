package com.afollestad.polarupgradetool;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Aidan Follestad (afollestad)
 */
public class GradleMigrator {

    private final File mFile;
    private final ArrayList<String> mPropertyNames;
    private final ArrayList<String> mPropertyValues;

    public GradleMigrator(File file, String[] propertyNames, String[] propertyValues) {
        mFile = file;
        mPropertyNames = new ArrayList<>(propertyNames.length);
        Collections.addAll(mPropertyNames, propertyNames);
        mPropertyValues = new ArrayList<>(propertyValues.length);
        Collections.addAll(mPropertyValues, propertyValues);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public boolean process() {
        final ArrayList<String> lines = new ArrayList<>();
        InputStream is = null;
        BufferedReader reader = null;

        try {
            is = new FileInputStream(mFile);
            reader = new BufferedReader(new InputStreamReader(is));
            String line;

            while ((line = reader.readLine()) != null) {
                for (int i = 0; i < mPropertyNames.size(); i++) {
                    final String propertyName = mPropertyNames.get(i);
                    int start = line.indexOf(propertyName + " ");
                    if (start == -1) continue;
                    line = propertyName + " " + mPropertyValues.get(i);
                }
                lines.add(line);
            }
        } catch (Exception e) {
            Main.LOG("[ERROR]: Failed to migrate a Gradle file: %s", e.getMessage());
            return false;
        } finally {
            Util.closeQuietely(reader);
            Util.closeQuietely(is);
        }

        mFile.delete();
        OutputStream os = null;
        BufferedWriter writer = null;

        try {
            os = new FileOutputStream(mFile);
            writer = new BufferedWriter(new OutputStreamWriter(os));

            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) writer.newLine();
                writer.write(lines.get(i));
            }
        } catch (Exception e) {
            Main.LOG("[ERROR]: Failed to migrate a Gradle file: %s", e.getMessage());
            return false;
        } finally {
            Util.closeQuietely(writer);
            Util.closeQuietely(os);
        }

        Main.LOG("[INFO] Migrated Gradle file %s", Main.cleanupPath(mFile.getAbsolutePath()));
        return true;
    }
}