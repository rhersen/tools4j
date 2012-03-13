package org.deephacks.tools4j.config.admin.rcp;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
    private Logger logger = LoggerFactory.getLogger(Activator.class);
    public static final String ACCEPT_IMAGE = "icons/accept.png";
    public static final String CANCEL_IMAGE = "icons/cancel.png";
    public static final String ADD_IMAGE = "icons/add.png";
    public static final String DELETE_IMAGE = "icons/delete.png";

    // The plug-in ID
    public static final String PLUGIN_ID = "config-admin-rcp-plugin";

    // The shared instance
    private static Activator plugin;

    /**
     * The constructor
     */
    public Activator() {
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception {
        plugin = this;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
        plugin = null;
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given
     * plug-in relative path
     *
     * @param path the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    @Override
    protected void initializeImageRegistry(ImageRegistry reg) {
        super.initializeImageRegistry(reg);
        Bundle bundle = Platform.getBundle(PLUGIN_ID);
        addImage(ACCEPT_IMAGE, reg, bundle);
        addImage(CANCEL_IMAGE, reg, bundle);
        addImage(ADD_IMAGE, reg, bundle);
        addImage(DELETE_IMAGE, reg, bundle);
    }

    public static Image getImage(String imagePath) {
        ImageRegistry imageRegistry = plugin.getImageRegistry();
        return imageRegistry.get(imagePath);
    }

    private void addImage(String imagePath, ImageRegistry reg, Bundle bundle) {
        ImageDescriptor myImage = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path(
                imagePath), null));
        reg.put(imagePath, myImage);
    }
}
