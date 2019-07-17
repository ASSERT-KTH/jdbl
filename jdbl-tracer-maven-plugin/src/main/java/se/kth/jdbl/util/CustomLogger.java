package se.kth.jdbl.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class CustomLogger {

    public void logElementRemoved(String pathToLogFile, HashSet<String> elementsRemoved) throws IOException {
        List<String> elementsRemovedList = new ArrayList<>(elementsRemoved);
        Collections.sort(elementsRemovedList);
        FileWriter writer = new FileWriter(pathToLogFile);
        for (String removedElement : elementsRemovedList) {
            writer.write(removedElement + "\n");
        }
        writer.close();
    }
}
