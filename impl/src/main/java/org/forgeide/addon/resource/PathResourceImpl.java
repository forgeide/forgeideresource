package org.forgeide.addon.resource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.forgeide.addon.resources.PathResource;
import org.jboss.forge.addon.facets.FacetNotFoundException;
import org.jboss.forge.addon.resource.AbstractResource;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceException;
import org.jboss.forge.addon.resource.ResourceFacet;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.resource.ResourceFilter;
import org.jboss.forge.addon.resource.monitor.ResourceMonitor;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.util.OperatingSystemUtils;
import org.jboss.forge.furnace.util.Streams;
import org.jboss.forge.furnace.util.Visitor;

/**
 * PathResource implementation
 *
 * @author Shane Bryzak
 *
 * @param <T>
 */
public class PathResourceImpl<T extends PathResource<T>> extends AbstractResource<Path> 
    implements PathResource<T> 
{
    protected Path path;
    protected FileTime lastModification;

    protected PathResourceImpl(final ResourceFactory factory, final Path file) {
        super(factory, null);

        if ((this.path = path) != null) {
            try {
                this.lastModification = Files.getLastModifiedTime(path);
            } catch (IOException ex) {
                throw new ResourceException(ex);
            }
        }
    }

    @Override
    public boolean delete() throws UnsupportedOperationException {
        try {
            return Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new ResourceException(ex);
        }
    }

    @Override
    public PathResource getParent()
    {
       return path.getParent() != null ? getResourceFactory().create(PathResource.class, path.getParent())
                : null;
    }

    @Override
    public boolean delete(boolean recursive) throws UnsupportedOperationException {
        if (recursive) {
            try {
               Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                   @Override
                   public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                       throws IOException
                   {
                       Files.delete(file);
                       return FileVisitResult.CONTINUE;
                   }
                   @Override
                   public FileVisitResult postVisitDirectory(Path dir, IOException e)
                       throws IOException
                   {
                       if (e == null) {
                           Files.delete(dir);
                           return FileVisitResult.CONTINUE;
                       } else {
                           // directory iteration failed
                           throw e;
                       }
                   }
                 });
               return true;
            } catch (IOException ex) {
                throw new ResourceException(ex);
            }
        } else {
            return delete();
        }
    }

    @Override
    public String getName() {
        return path.getFileName().toString();
    }

    @Override
    public Resource<Path> createFrom(Path file) {
        return new PathResourceImpl(getResourceFactory(), file);
    }

    @Override
    public Path getUnderlyingResourceObject() {
        return path;
    }

    @Override
    public InputStream getResourceInputStream() {
        try {
            return Files.newInputStream(path);
        } catch (IOException ex) {
            throw new ResourceException(ex);
        }
    }

    private OutputStream getResourceOutputStream() {
        try {
            return Files.newOutputStream(path);
        } catch (IOException ex) {
            throw new ResourceException(ex);
        }
    }

    @Override
    public Resource<Path> getChild(String name) {
        return getResourceFactory().create(path.resolve(name));
    }

    @Override
    public boolean exists() {
        return Files.exists(path);
    }

    @Override
    public PathResource<T> setContents(String data) {
        if (data == null)
        {
           data = "";
        }
        return setContents(data.toCharArray());
    }

    @Override
    public PathResource<T> setContents(String data, Charset charset) {
        if (data == null)
        {
           data = "";
        }
        return setContents(data.toCharArray(), charset);
    }

    @Override
    public PathResource<T> setContents(final char[] data) {
        return setContents(new ByteArrayInputStream(new String(data).getBytes()));
    }

    @Override
    public PathResource<T> setContents(final char[] data, Charset charset) {
        return setContents(new ByteArrayInputStream(new String(data).getBytes(charset)));
    }

    @Override
    public PathResource<T> setContents(InputStream data) {
        Assert.notNull(data, "InputStream must not be null.");

        try
        {
           if (!exists())
           {
               getParent().mkdirs();
              if (!createNewFile())
              {
                 throw new IOException("Failed to create path: " + path);
              }
           }

           OutputStream out = getResourceOutputStream();
           try
           {
              Streams.write(data, out);
           }
           finally
           {
              Streams.closeQuietly(data);
              out.flush();
              Streams.closeQuietly(out);
              if (OperatingSystemUtils.isWindows())
              {
                 System.gc();
              }
           }
        }
        catch (IOException e)
        {
           throw new ResourceException(e);
        }
        return (T) this;
    }

    @Override
    public boolean isDirectory() {
        return Files.isDirectory(path);
    }

    @Override
    public boolean isStale() {
        try {
            return lastModification.compareTo(Files.getLastModifiedTime(path)) != 0;
        } catch (IOException ex) {
            throw new ResourceException(ex);
        }
    }

    @Override
    public void refresh() {
        try {
            lastModification = Files.getLastModifiedTime(path);
        } catch (IOException ex) {
            throw new ResourceException(ex);
        }
    }

    @Override
    public boolean mkdir() {
        try {
            return (path = Files.createDirectory(path)) != null;
        } catch (IOException ex) {
            throw new ResourceException(ex);
        }
    }

    @Override
    public boolean mkdirs() {
        try {
            return (path = Files.createDirectories(path)) != null;
        } catch (IOException ex) {
            throw new ResourceException(ex);
        }
    }

    @Override
    public void deleteOnExit() {
        path.toFile().deleteOnExit();
    }

    @Override
    public boolean createNewFile() {
        try
        {
           getParent().mkdirs();
           if (Files.createFile(path) != null)
           {
              return true;
           }
           return false;
        }
        catch (IOException e)
        {
           throw new ResourceException(e);
        }
    }

    @Override
    public T createTempResource() {
        try
        {
           T result = (T) createFrom(Files.createTempFile("forgetemp", ""));
           return result;
        }
        catch (IOException e)
        {
           throw new ResourceException(e);
        }
    }

    @Override
    public boolean renameTo(String pathspec) {
        try {
            return (path = Files.move(path, path.resolveSibling(pathspec))) != null;
        } catch (IOException ex) {
            throw new ResourceException(ex);
        }
    }

    @Override
    public boolean renameTo(PathResource<?> target) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public long getSize() {
        try {
            return Files.size(path);
        } catch (IOException ex) {
            throw new ResourceException(ex);
        }
    }

    @Override
    public boolean isWritable() {
        return Files.isWritable(path);
    }

    @Override
    public boolean isReadable() {
        return Files.isReadable(path);
    }

    @Override
    public boolean isExecutable() {
        return Files.isExecutable(path);
    }

    @Override
    public ResourceMonitor monitor() {
        // TODO implement this with a watcher service, don't use a thread
        
        //path.register(watcher, events)
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResourceMonitor monitor(ResourceFilter filter) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getLastModified() {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ex) {
            throw new ResourceException(ex);
        }
    }

    @Override
    public void setLastModified(long currentTimeMillis) {
        try {
            Files.setLastModifiedTime(path, FileTime.fromMillis(currentTimeMillis));
        } catch (IOException ex) {
            throw new ResourceException(ex);
        }
    }

    private volatile List<Resource<?>> listCache;

    @Override
    protected List<Resource<?>> doListResources() {
        if (Files.isDirectory(path)) {
            if (isStale())
            {
               listCache = null;
            }

            if (listCache == null)
            {
               listCache = new LinkedList<>();

               DirectoryStream<Path> stream = null;

               try {
                   stream = Files.newDirectoryStream(path, new DirectoryStream.Filter<Path>() {
                       @Override
                       public boolean accept(Path entry) throws IOException 
                       {
                           return true;
                           //Files.isDirectory(entry);
                       }
                   });
               } catch (IOException ex) {
                   throw new ResourceException(ex);
               }

               for (Path entry: stream) {
                   listCache.add(getResourceFactory().create(entry));
               }
            }

            return listCache;
        } else {
            return Collections.emptyList();
        }
    }

}
