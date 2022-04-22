package org.killbill.billing.codegen.languages;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

public class ClassUtil {


    public static Map<String, Class> findAPIEnum(final String apiJar) throws IOException, ClassNotFoundException {

        Map<String, Class> result = new HashMap<>();
        JarFile module = null;
        try {
            File file = new File(apiJar);
            URL[] urls = new URL[1];
            urls[0] = new URL(String.format("file:///%s", apiJar)); //Changed to file:/// to support Windows
            final URLClassLoader cl = new URLClassLoader(urls);
            
            module = new JarFile(file);
            final Enumeration<?> files = module.entries();
            while (files != null && files.hasMoreElements()) {
                final String fileName = files.nextElement().toString();

                if (fileName.endsWith(".class")) {
                    final String className = fileName.replaceAll("/", ".").substring(0, fileName.length() - 6);

                    Class<?> theClass;
                    try {
                        theClass = Class.forName(className, false, cl);
                        if (theClass.isEnum()) {
                            result.put(theClass.getSimpleName(), theClass);
                        }
                    } catch (final NoClassDefFoundError e) {
                        continue;
                    }
                }
            }
        } finally {
            if (module != null) {
                try {
                    module.close();
                } catch (final IOException ioe) {
                }
            }
        }
        return result;
    }
}
