/*
 * Copyright (c) 2011 Evident Solutions Oy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package fi.evident.munchhausen;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public final class Bootstrap {

    private final List<File> libraryDirectories = new ArrayList<File>();
    private final List<File> resourceDirectories = new ArrayList<File>();

    public void addLibraryDirectory(File directory) {
        if (directory == null) throw new IllegalArgumentException("null directory");

        libraryDirectories.add(directory);
    }

    public void addResourceDirectory(File directory) {
        if (directory == null) throw new IllegalArgumentException("null directory");

        resourceDirectories.add(directory);
    }

    public void run(String mainClassName, String[] args) throws BootstrapException, InvocationTargetException {
        if (mainClassName == null) throw new IllegalArgumentException("null mainClassName");

        Thread currentThread = Thread.currentThread();
        ClassLoader oldContextClassLoader = currentThread.getContextClassLoader();
        try {
            ClassLoader classLoader = new URLClassLoader(getClassPathUrls(), getClass().getClassLoader());
            currentThread.setContextClassLoader(classLoader);

            Method main = findMainMethod(mainClassName, classLoader);
            main.invoke(null, new Object[] { args });

        } catch (IllegalAccessException e) {
            throw new BootstrapException("Main method is not accessible.", e);
            
        } finally {
            currentThread.setContextClassLoader(oldContextClassLoader);
        }
    }

    private URL[] getClassPathUrls() {
        List<File> paths = new ArrayList<File>();

        paths.addAll(resourceDirectories);

        for (File directory : libraryDirectories)
            if (directory.isDirectory())
                addArchivesToList(paths, directory);

        URL[] urls = new URL[paths.size()];
        for (int i = 0; i < urls.length; i++)
            urls[i] = toURL(paths.get(i));
        return urls;
    }

    private static URL toURL(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new AssertionError("Conversion of File to URL failed: " + file);
        }
    }

    private static void addArchivesToList(List<File> archives, File directory) {
        for (File child : directory.listFiles())
            if (child.isDirectory())
                addArchivesToList(archives, child);
            else if (child.isFile() && child.getName().endsWith(".jar"))
                archives.add(child);
    }

    private Method findMainMethod(String mainClassName, ClassLoader classLoader) throws BootstrapException {
        try {
            Class<?> mainClass = classLoader.loadClass(mainClassName);
            Method method = mainClass.getMethod("main", String[].class);

            if (!Modifier.isPublic(method.getModifiers()))
                throw new BootstrapException("Main method is not public.");
            
            if (!Modifier.isStatic(method.getModifiers()))
                throw new BootstrapException("Main method is not static.");

            if (method.getReturnType() != Void.TYPE)
                throw new BootstrapException("Main method does not return void.");

            return method;

        } catch (ClassNotFoundException e) {
            throw new BootstrapException("Main class '" + mainClassName + "' not found.", e);
        } catch (NoSuchMethodException e) {
            throw new BootstrapException("Main class '" + mainClassName + "' does not contain main-method.", e);
        }
    }

    private static void printUsageInstructions() {
        System.err.println("Example usage:");
        System.err.println("    java -Dbootstrap.mainclass=foo.FooMain -jar munchhausen-x.y.jar <arguments>");
        System.err.println();
        System.err.println("Properties:");
        System.err.println("    bootstrap.mainclass     The main class of the application. (Required.)");
        System.err.println("    bootstrap.libdir        Root-directory for scanned JAR-files. (Default is current directory.)");
        System.err.println("    bootstrap.resourcedir   Additional directory added to classpath. (Optional.)");
        System.err.println();
    }

    public static void main(String[] args) throws Throwable {
        String mainClassName = System.getProperty("bootstrap.mainclass");
        if (mainClassName == null) {
            System.err.println("Error: main class not specified.\n");
            printUsageInstructions();
            System.exit(1);
        }

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.addLibraryDirectory(new File(System.getProperty("bootstrap.libdir", ".")));

        String resourceDirectory = System.getProperty("bootstrap.resourcedir");
        if (resourceDirectory != null)
            bootstrap.addResourceDirectory(new File(resourceDirectory));

        try {
            bootstrap.run(mainClassName, args);

        } catch (BootstrapException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
            
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
