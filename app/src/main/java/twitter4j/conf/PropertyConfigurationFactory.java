package twitter4j.conf;

/**
 * ConfigurationFactory implementation for PropertyConfiguration.
 * Currently getInstance calls concrete constructor each time. No caching at all.
 *
 * @author Yusuke Yamamoto - yusuke at mac.com
 */
class PropertyConfigurationFactory implements ConfigurationFactory {
    private static final PropertyConfiguration ROOT_CONFIGURATION;

    static {
        ROOT_CONFIGURATION = new PropertyConfiguration();
        // calling ROOT_CONFIGURATION.dumpConfiguration() will cause ExceptionInInitializerError as Logger has not been initialized.
        // as a quick and dirty solution, static initializer of twitter4j.Logger will call dumpConfiguration() on behalf.
    }

    @Override
    public Configuration getInstance() {
        return ROOT_CONFIGURATION;
    }

    // It may be preferable to cache the config instance

    @Override
    public Configuration getInstance(String configTreePath) {
        PropertyConfiguration conf = new PropertyConfiguration(configTreePath);
        conf.dumpConfiguration();
        return conf;
    }

    @Override
    public void dispose() {
        // nothing to do for property based configuration
    }
}