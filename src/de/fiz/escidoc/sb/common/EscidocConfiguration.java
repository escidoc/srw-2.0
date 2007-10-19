package de.fiz.escidoc.sb.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;

/**
 * Handles properties.
 * 
 * @author Michael Hoppe
 * 
 */
public final class EscidocConfiguration {

    public static final String ESCIDOC_BASEURL = "escidoc.baseurl";

    private static final String CATALINA_HOME = "catalina.home";

    private static EscidocConfiguration instance = null;

    private Properties properties;

    private static final String PROPERTIES_BASEDIR =
        System.getProperty(CATALINA_HOME) + "/";

    private static final String PROPERTIES_DIR = PROPERTIES_BASEDIR + "conf/";

    private static final String PROPERTIES_FILENAME = "escidoc.properties";

    private static final String PROPERTIES_DEFAULT_FILENAME =
        PROPERTIES_FILENAME + ".default";

    /**
     * Private Constructor, in order to prevent instantiation of this utility
     * class. read the Properties and fill it in properties attribute.
     * 
     * @throws EscidocException
     *             e
     * 
     * @common
     */
    private EscidocConfiguration() throws IOException {
        this.properties = loadProperties();
    }

    /**
     * Returns and perhabs initializes Object.
     * 
     * @return EscidocConfiguration self
     * @throws IOException
     *             e
     * 
     * @common
     */
    public static synchronized EscidocConfiguration getInstance()
        throws IOException {
        if (instance == null) {
            instance = new EscidocConfiguration();
        }
        return instance;
    }

    /**
     * Returns the property with the given name or null if property was not
     * found.
     * 
     * @param name
     *            The name of the Property.
     * @return Value of the given Property as String.
     * @common
     */
    public synchronized String get(final String name) {
        return (String) properties.get(name);
    }

    /**
     * Returns the property with the given name or the second parameter as
     * default value if property was not found.
     * 
     * @param name
     *            The name of the Property.
     * @param defaultValue
     *            The default vaule if property isn't given.
     * @return Value of the given Property as String.
     * @common
     */
    public synchronized String get(final String name, final String defaultValue) {
        String prop = (String) properties.get(name);

        if (prop == null) {
            return (defaultValue);
        }
        return (prop);
    }

    /**
     * Loads the Properties from the possible files. First loads properties from
     * the file escidoc.properties.default. Afterwards tries to load specific
     * properties from the file escidoc.properties and merges them with the
     * default properties. If any key is included in default and specific
     * properties, the value of the specific property will overwrite the default
     * property.
     * 
     * @throws EscidocException
     *             If the loading of the default properties (file
     *             escidoc.properties.default) fails.
     * 
     * @return The properties
     * @throws EscidocException
     * @common
     */
    private synchronized Properties loadProperties() throws IOException {
        Properties result;
        result = getProperties(PROPERTIES_DEFAULT_FILENAME);

        Properties specific = null;
        try {
            specific = getProperties(PROPERTIES_FILENAME);
        }
        catch (IOException e) {
            try {
                specific =
                    getProperties(PROPERTIES_BASEDIR + PROPERTIES_FILENAME);
            }
            catch (IOException e1) {
                try {
                    specific =
                        getProperties(PROPERTIES_DIR + PROPERTIES_FILENAME);
                }
                catch (IOException e2) {
                    specific = new Properties();
                }
            }
        }
        result.putAll(specific);
        // set Properties as System-Variables
        Iterator iter = result.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            String value = result.getProperty(key);
            value = replaceEnvVariables(value);
            System.setProperty(key, value);
        }
        return result;
    }

    /**
     * Get the properties from a file.
     * 
     * @param filename
     *            The name of the properties file.
     * @return The properties.
     * @throws IOException
     *             If access to the specified file fails.
     */
    private synchronized Properties getProperties(final String filename)
        throws IOException {

        Properties result = new Properties();
        InputStream propertiesStream = getInputStream(filename);
        result.load(propertiesStream);
        return result;
    }

    /**
     * Get an InputStream for the given file.
     * 
     * @param filename
     *            The name of the file.
     * @return The InputStream or null if the file could not be located.
     * @throws FileNotFoundException
     *             If access to the specified file fails.
     */
    private synchronized InputStream getInputStream(final String filename)
        throws FileNotFoundException {

        InputStream inputStream =
            getClass().getClassLoader().getResourceAsStream(filename);
        if (inputStream == null) {
            inputStream = new FileInputStream(new File(filename));
        }
        return inputStream;
    }

    /**
     * Retrieves the Properties from File.
     * 
     * @param property
     *            value of property with env-variable-syntax (eg ${java.home})
     * @return String replaced env-variables
     * 
     * @common
     */
    private synchronized String replaceEnvVariables(final String property) {
        String replacedProperty = property;
        if (property.indexOf("${") > -1) {
            String[] envVariables = property.split("\\}.*?\\$\\{");
            if (envVariables != null) {
                for (int i = 0; i < envVariables.length; i++) {
                    envVariables[i] =
                        envVariables[i].replaceFirst(".*?\\$\\{", "");
                    envVariables[i] = envVariables[i].replaceFirst("\\}.*", "");
                    if (System.getProperty(envVariables[i]) != null
                        && !System.getProperty(envVariables[i]).equals("")) {
                        String envVariable =
                            System.getProperty(envVariables[i]);
                        envVariable = envVariable.replaceAll("\\\\", "/");
                        replacedProperty =
                            replacedProperty.replaceAll("\\$\\{"
                                + envVariables[i] + "}", envVariable);
                    }
                }
            }
        }
        return replacedProperty;
    }

}
