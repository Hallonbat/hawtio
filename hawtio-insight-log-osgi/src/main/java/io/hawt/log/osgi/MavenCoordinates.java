package io.hawt.log.osgi;

import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import static io.hawt.log.support.MavenCoordinates.appendMavenCoordinateFromPomProperties;

public class MavenCoordinates {

    private static Map<String, String> MAVEN_COORDINATES = new ConcurrentHashMap<String, String>();

    public static void addMavenCoord(Map<String, String> properties) {
        if (properties.get("maven.coordinates") == null) {
            String mavenCoord = getMavenCoordinates(properties);
            if (mavenCoord != null && !mavenCoord.isEmpty()) {
                properties.put("maven.coordinates", mavenCoord);
            }
        }
    }

    public static String[] addMavenCoord(String[] throwable) {
        if (throwable != null) {
            String[] newThrowable = new String[throwable.length];
            for (int i = 0; i < newThrowable.length; i++) {
                newThrowable[i] = addMavenCoord(throwable[i]);
            }
            return newThrowable;
        }
        return throwable;
    }

    private static String addMavenCoord(String line) {
        if (line.endsWith("]")) {
            int index = line.lastIndexOf('[');
            if (index > 0) {
                String str = line.substring(index + 1, line.length() - 1);
                index = str.indexOf(':');
                if (index > 0) {
                    String idStr = str.substring(0, index);
                    String mvn = getMavenCoordinates(idStr);
                    if (mvn != null) {
                        return line + "[" + mvn + "]";
                    }
                }
            }
        }
        return line;
    }

    private static String getMavenCoordinates(Map props) {
        Object id = (props != null) ? props.get("bundle.id") : null;
        if (id == null) {
            return null;
        }
        return getMavenCoordinates(id.toString());
    }

    private static String getMavenCoordinates(String bundleIdStr) {
        long bundleId;
        try {
            bundleId = Long.parseLong(bundleIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
        return getMavenCoordinates(bundleId);
    }

    public static Bundle getBundle(long bundleId) {
        Bundle logBundle = FrameworkUtil.getBundle(Logs.class);
        BundleContext bundleContext = logBundle != null ? logBundle.getBundleContext() : null;
        Bundle bundle = bundleContext != null ? bundleContext.getBundle(bundleId) : null;
        return bundle;
    }

    public static String getMavenCoordinates(long bundleId) {
        Bundle bundle = getBundle(bundleId);
        if (bundle == null) {
            // Not sure why can't we find the bundleId?
            return null;
        }
        String id = Long.toString(bundle.getBundleId()) + ":" + Long.toString(bundle.getLastModified());
        String maven = MAVEN_COORDINATES.get(id);
        if (maven == null) {
            if (bundle.getState() >= Bundle.RESOLVED) {
                try {
                    Enumeration<URL> e = bundle.findEntries("META-INF/maven/", "pom.properties", true);
                    StringBuilder buf = new StringBuilder();
                    while (e != null && e.hasMoreElements()) {
                        URL url = e.nextElement();
                        appendMavenCoordinateFromPomProperties(url.openStream(), buf);
                    }
                    maven = buf.toString();
                } catch (Throwable t) {
                    // Ignore
                    maven = "";
                }
                MAVEN_COORDINATES.put(id, maven);
            } else {
                maven = "";
            }
        }
        return maven;
    }
}
