package org.jboss.arquillian.android;

public class AndroidConfigurationException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public AndroidConfigurationException(String message) {
        super(message);
    }

    public AndroidConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}
