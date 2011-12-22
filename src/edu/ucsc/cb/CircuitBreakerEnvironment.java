package edu.ucsc.cb;

import static edu.ucsc.cb.CircuitBreakerEnvironmentProperties.DEBUG;
import static edu.ucsc.cb.CircuitBreakerEnvironmentProperties.CONFIG_FILE;
import static edu.ucsc.cb.CircuitBreakerEnvironmentProperties.FAILED_CALL_THRESHOLD;
import static edu.ucsc.cb.CircuitBreakerEnvironmentProperties.RETRY_THRESHOLD;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * A convenient class for dealing with persisted on file circuit breaker settings.
 *
 * @author hsanchez@cs.ucsc.edu (Huascar A. Sanchez)
 */
public class CircuitBreakerEnvironment {
  private final EnvironmentConfiguration configuration;
  public CircuitBreakerEnvironment() throws IOException {
    this(
        new EnvironmentPropertiesConfiguration(
            getDefaultProperties(),
            CONFIG_FILE
        )
    );
  }

  protected CircuitBreakerEnvironment(EnvironmentConfiguration configuration){
    this.configuration = configuration;
  }

  public static CircuitBreakerEnvironment getInstance(){
    return Installer.INSTANCE;
  }


  public boolean inDebugMode(){
    final Object  prop   = getConfiguration().getProperty(DEBUG);
    return prop == null ? false : Boolean.valueOf(str(prop));
  }

  public long getCircuitBreakerRetryThreshold(){
    return Long.valueOf(
        String.valueOf(getConfiguration().getProperty(
            RETRY_THRESHOLD)));
  }

  public long getCircuitBreakerFailedCallThreshold(){
    return Long.valueOf(
        String.valueOf(getConfiguration().getProperty(
            FAILED_CALL_THRESHOLD)));
  }


  private static Properties getDefaultProperties() {
    return new Properties() {
      private static final long serialVersionUID = 1L;

      {
        setProperty(RETRY_THRESHOLD, Long.toString(900000000000L));
        setProperty(FAILED_CALL_THRESHOLD, String.valueOf(1));
        setProperty(DEBUG, String.valueOf(false));
      }
    };
  }


  protected EnvironmentConfiguration getConfiguration(){
    return configuration;
  }

  static interface EnvironmentConfiguration {
    Object getProperty(String propertyName);
    void setDefaultProperties(Properties defaults);
  }

  static abstract class AbstractEnvironmentConfiguration implements EnvironmentConfiguration {
    private final Map<String, Object> properties = new HashMap<String, Object>();

    AbstractEnvironmentConfiguration() {
    }

    /**
     * This method should be overridden to check whether the properties could maybe have changed,
     * and if yes, to reload them.
     */
    protected abstract void checkForPropertyChanges();

    protected abstract Properties getDefaults();

    protected boolean isDefaultsMode() {
      return false;
    }

    @Override
    public Object getProperty(String propertyName) {
      checkForPropertyChanges();
      synchronized (properties) {
        return !isDefaultsMode() ? properties.get(propertyName)
            : getDefaults().getProperty(propertyName);
      }
    }

    /**
     * setting a property.
     *
     * @param propertyName name of property
     * @param value value of property.
     */
    protected final void setProperty(String propertyName, Object value) {
      synchronized (properties) {
        Object old = properties.get(propertyName);
        if ((value != null && !value.equals(old))
            || value == null && old != null) {
          if(!isDefaultsMode()){
            properties.put(propertyName, value);
          } else {
            getDefaults().setProperty(propertyName, String.valueOf(value));
          }
        }
      }
    }
  }

  private static String str(Object that){
    return that == null ? "" : that.toString();
  }

  static class EnvironmentPropertiesConfiguration extends AbstractEnvironmentConfiguration {
    private final File file;
    private final Properties defaults;

    private boolean useDefaults = false;
    private long lastModified = 0;

    EnvironmentPropertiesConfiguration(Properties defaults, String filename) throws IOException {
      super();
      this.defaults = defaults;
      this.file = new File(filename);
      this.useDefaults = !file.exists();
      loadProperties();
    }

    @Override
    protected void checkForPropertyChanges() {
      if (lastModified != file.lastModified()) {
        try {
          lastModified = file.lastModified();
          loadProperties();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }

    @Override
    protected Properties getDefaults() {
      return defaults;
    }

    @Override
    protected boolean isDefaultsMode() {
      return useDefaults;
    }

    private void loadProperties() throws IOException {
      final Properties properties = new Properties();
      if (!useDefaults) {
        properties.load(new FileInputStream(file));
        setAllProperties(properties);
      } else {
        setAllProperties(getDefaults());
      }
    }

    private void setAllProperties(Properties properties) {
      for (Map.Entry<Object, Object> entry : properties.entrySet()) {
        setProperty(str(entry.getKey()), entry.getValue());
      }
    }

    @Override public void setDefaultProperties(Properties defaults) {
      synchronized (this.defaults){
        this.defaults.clear();
        this.defaults.putAll(defaults);
      }

      try {
        loadProperties();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

    }
  }

  /**
   * Lazy-constructed singleton, which is thread safe
   */
  static class Installer {
    static final CircuitBreakerEnvironment INSTANCE;

    static {
      try {
        INSTANCE = new CircuitBreakerEnvironment();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }
}
