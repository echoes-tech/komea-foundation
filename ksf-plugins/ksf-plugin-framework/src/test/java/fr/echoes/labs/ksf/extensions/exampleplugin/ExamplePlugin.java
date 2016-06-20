package fr.echoes.labs.ksf.extensions.exampleplugin;

import fr.echoes.labs.ksf.plugins.api.PluginPropertiesImpl;
import fr.echoes.labs.pluginfwk.api.extension.IExtension;
import fr.echoes.labs.pluginfwk.api.plugin.PluginDefinition;
import fr.echoes.labs.pluginfwk.api.plugin.PluginProperties;

public class ExamplePlugin implements PluginDefinition {

	public ExamplePlugin() {
		super();
	}

	@Override
	public void destroy() throws Exception {
		//

	}

	@Override
	public String getDescription() {
		return "Example of plugin loaded by the classloader";
	}

	@Override
	public IExtension[] getExtensions() {
		return new IExtension[] { new ExampleExtension() };
	}

	@Override
	public String getId() {
		return "example-classloader-plugin-id";
	}

	@Override
	public String getName() {
		return "Example Classloader plugin";
	}

	@Override
	public PluginProperties getPluginProperties() {
		return new PluginPropertiesImpl();
	}

	@Override
	public void init() {
		//
	}

}
