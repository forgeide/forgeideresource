package org.forgeide.addon.resources;

import java.nio.file.Path;

import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFilter;
import org.jboss.forge.addon.resource.WriteableResource;
import org.jboss.forge.addon.resource.monitor.ResourceMonitor;

/**
 * A Resource that represents a (NIO Files API) Path
 *
 * @author Shane Bryzak
 *
 * @param <T>
 */
public interface PathResource<T extends PathResource<T>> extends Resource<Path>,
    WriteableResource<PathResource<T>, Path> {

    /**
     * Return true if this {@link FileResource} exists and is actually a directory, otherwise return false;
     */
    public boolean isDirectory();

    /**
     * Returns true if the underlying resource has been modified on the file system since it was initially loaded.
     * 
     * @return boolean true if resource is changed.
     */
    public boolean isStale();

    /**
     * Re-read the file-system meta-data for this resource (such as last modified time-stamp, and permissions.)
     */
    public void refresh();

    /**
     * Create a new single directory for this resource. This will not succeed if any parent directories needed for this
     * resource to exist are missing. You should consider using {@link #mkdirs()}
     */
    public boolean mkdir();

    /**
     * Create all directories required for this resource to exist.
     */
    public boolean mkdirs();

    /**
     * Requests that the file or directory denoted by this resource be deleted when the virtual machine terminates.
     * <p>
     * Once deletion has been requested, it is not possible to cancel the request. This method should therefore be used
     * with care.
     */
    public void deleteOnExit();

    /**
     * Create the file in the underlying resource system. Necessary directory paths will be created automatically.
     */
    public boolean createNewFile();

    /**
     * Create a temporary {@link FileResource}
     */
    public T createTempResource();

    /**
     * Rename this {@link Resource} to the given path.
     */
    public boolean renameTo(final String pathspec);

    /**
     * Rename this {@link Resource} to the given {@link FileResource}
     */
    public boolean renameTo(final PathResource<?> target);

    /**
     * Returns the size of the file denoted by this abstract pathname
     */
    public long getSize();

    /**
     * Returns if a file is writable
     */
    public boolean isWritable();

    /**
     * Returns if a file is readable
     */
    public boolean isReadable();

    /**
     * Returns if a file is executable
     */
    public boolean isExecutable();

    /**
     * A parent for a Path is always another Path
     */
    @Override
    public Resource<Path> getParent();

    /**
     * Monitors this FileResource
     */
    ResourceMonitor monitor();

    /**
     * Monitors this FileResource using the given filter
     */
    ResourceMonitor monitor(ResourceFilter filter);

    /**
     * Get the last modified time-stamp of this resource.
     */
    public long getLastModified();

    /**
     * Set the last modified time-stamp of this resource.
     */
    public void setLastModified(long currentTimeMillis);
}
