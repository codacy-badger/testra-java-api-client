package tech.testra.java.api.client.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class PropertyHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(PropertyHelper.class);
  private static Properties properties = new Properties();
  private static List<String> resourceFileNames = new ArrayList<>();

  private PropertyHelper() {
  }

  public static synchronized void loadPropertiesFromAbsolute(String resourceFileName) {
    try {
      if (!resourceFileNames.contains(resourceFileName)) {
        LOGGER.info("Loading property resource : {}", resourceFileName);
        FileInputStream in = new FileInputStream(resourceFileName);
        properties.load(in);
        resourceFileNames.add(resourceFileName);
      }
    } catch (IOException ex) {
      LOGGER.error(".testra file not found");
      ex.fillInStackTrace();
      throw new RuntimeException(
          String.format("Property resource loading error for %s", resourceFileName));
    }
  }


  public static String prop(String value) {
    if(System.getProperty(value) !=null){
      return System.getProperty(value);
    }
    return properties.getProperty(value);
  }

  public static String prop(String value, String defaultValue) {
    if(System.getProperty(value) !=null){
      return System.getProperty(value);
    }
    return properties.getProperty(value, defaultValue);
  }
}
