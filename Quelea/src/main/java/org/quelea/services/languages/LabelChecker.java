/*
 * This file is part of Quelea, free projection software for churches.
 * 
 * Copyright (C) 2019 Michael Berry
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.quelea.services.languages;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.quelea.utils.NoDuplicateProperties;

/**
 * Run as a standalone script - checks to see whether the language files are
 * complete, and for those that aren't shows the missing labels that need
 * translating.
 * <p/>
 * @author Michael
 */
public class LabelChecker {

    private NoDuplicateProperties labels;
    private NoDuplicateProperties engLabels;
    private String name;
    private String langName;

    public LabelChecker(String name) {
        this.name = name;
        labels = new NoDuplicateProperties();
        engLabels = new NoDuplicateProperties();
        File langFile = new File("languages", name);
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(langFile), "UTF-8")) {
            labels.load(reader);
            langName = labels.getProperty("LANGUAGENAME");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        File englangFile = new File("languages", "gb.lang");
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(englangFile), "UTF-8")) {
            engLabels.load(reader);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public List<String> getMissingLabels() {
        List<String> missing = new ArrayList<>();
        boolean first = false;
        System.err.print("\nChecking \"" + name + "\"...");
        for (Object okey : engLabels.keySet()) {
            String key = (String) okey;
            String prop = labels.getProperty(key);
            if (prop == null) {
                if (!first) {
                    first = true;
                    System.err.println();
                    System.err.println("MISSING LABELS:");
                }
                String val = key + "=" + engLabels.getProperty(key).replace("\n", "\\n");
                missing.add(val);
                System.err.println(val);
            }
        }
        if (missing.isEmpty()) {
            System.err.println("All good.");
        }
        return missing;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Checking language files:");
        boolean someLabelsMissing = false;
        Map<String, List<String>> missingMap = new HashMap<>();
        for (File file : new File("languages").listFiles()) {
            if (file.getName().endsWith("lang")
                    && !file.getName().equals("gb.lang")
                    && !file.getName().equals("us.lang")) { //Exclude GB english file since this is what we work from, and US file because it gets translated automatically!
                System.out.println("Checking " + file.getName());
                LabelChecker lc = new LabelChecker(file.getName());
                List<String> missingLabels = lc.getMissingLabels();
                missingMap.put(lc.langName, missingLabels);
                if (!missingLabels.isEmpty()) {
                    someLabelsMissing = true;
                }
            }
        }
        String json = new Gson().toJson(missingMap);
        
        File missingLabelFile = new File("dist/missinglabels.js");
        new File("dist").mkdir();
        missingLabelFile.createNewFile();
        try (PrintWriter out = new PrintWriter(missingLabelFile, StandardCharsets.UTF_8.displayName())) {
            out.println("var mls = " + json + ";");
        }
        
        if (someLabelsMissing) {
            System.err.println("\nWARNING: Some language files have missing labels. "
                    + "This is normal for intermediate builds and development releases, "
                    + "but for final releases this should be fixed if possible. ");
        }
    }
}
