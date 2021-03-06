package org.ldg.mcpd;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import java.io.*;
import java.util.*;

public class Remapper extends org.objectweb.asm.commons.Remapper implements ClassHandler {
    List<String> exemptions;
    InheritanceGraph inheritance;
    String default_package = null;
    Map<String, String> packages = new HashMap<String, String>();
    Map<String, String> classes = new HashMap<String, String>();
    Map<String, String> fields = new HashMap<String, String>();
    Map<String, String> methods = new HashMap<String, String>();

    public Remapper(File configfile, List<String> exclude, InheritanceGraph inheritanceGraph, boolean invert) throws IOException {
        exemptions = exclude;
        if (exemptions == null) {
            exemptions = new ArrayList<String>();
        }
        inheritance = inheritanceGraph;

        FileInputStream fis = new FileInputStream(configfile);
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader config = new BufferedReader(isr);

        String line = config.readLine();
        while (line != null) {
            line = line.split("#")[0];
            String[] pieces = line.trim().split(" ");

            if (pieces[0].equals("PK:")) {
                if (pieces.length != 3) {
                    System.out.println("Bad config line: " + line);
                }

                String sourcePackage = pieces[1];
                String destPackage = pieces[2];
                if (invert) {
                    sourcePackage = pieces[2];
                    destPackage = pieces[1];
                }

                if (sourcePackage.equals(".")) {
                    default_package = destPackage;
                } else {
                    packages.put(sourcePackage, destPackage);
                }
            } else if (pieces[0].equals("CL:")) {
                if (pieces.length != 3) {
                    System.out.println("Bad config line: " + line);
                }

                String sourceClass = pieces[1];
                String destClass = pieces[2];
                if (invert) {
                    sourceClass = pieces[2];
                    destClass = pieces[1];
                }

                classes.put(sourceClass, destClass);
            } else if (pieces[0].equals("FD:")) {
                if (pieces.length != 3) {
                    System.out.println("Bad config line: " + line);
                }

                String sourceField = pieces[1];

                // Trim off the class name.
                String[] subpieces = pieces[2].split("/");

                if (invert) {
                    sourceField = pieces[2];
                    subpieces = pieces[1].split("/");
                }

                String destField = subpieces[subpieces.length - 1];

                fields.put(sourceField, destField);
            } else if (pieces[0].equals("MD:")) {
                if (pieces.length != 5) {
                    System.out.println("Bad config line: " + line);
                }

                String sourceMethod = pieces[1];
                String sourceSignature = pieces[2];

                // Trim off the class name.
                String[] subpieces = pieces[3].split("/");

                if (invert) {
                    sourceMethod = pieces[3];
                    sourceSignature = pieces[4];
                    subpieces = pieces[1].split("/");
                }

                String destMethod = subpieces[subpieces.length - 1];

                methods.put(sourceMethod + ";" + sourceSignature, destMethod);
            }
            line = config.readLine();
        }
    }

    public String map(String name) {
        if (classes.containsKey(name)) {
            return classes.get(name);
        } else if (default_package != null && !name.contains("/")) {
            return default_package + "/" + name;
        } else {
            String best_match = "";

            for (String pkg : packages.keySet()) {
                if (pkg.length() > best_match.length()
                    && name.startsWith(pkg + "/")) {
                    best_match = pkg;
                }
            }

            if (best_match.length() > 0) {
                String mapping = packages.get(best_match);
                if (mapping.equals(".")) {
                    return name.substring(best_match.length() + 1);
                } else {
                    return mapping + name.substring(best_match.length());
                }
            }
        }

        return name;
    }

    public String mapFieldName(String cls, String name, String descriptor) {
        // Check the class itself first.
        String key = cls + "/" + name;
        if (fields.containsKey(key)) {
            return fields.get(key);
        }

        // Then all of its ancestors
        for (String ancestor : inheritance.getAncestors(cls)) {
            key = ancestor + "/" + name;
            if (fields.containsKey(key)) {
                return fields.get(key);
            }
        }
        return name;
    }

    public String mapMethodName(String cls, String name, String descriptor) {
        // Check the class itself first.
        String key = cls + "/" + name + ";" + descriptor;
        if (methods.containsKey(key)) {
            return methods.get(key);
        }

        // Then all of its ancestors
        for (String ancestor : inheritance.getAncestors(cls)) {
            key = ancestor + "/" + name + ";" + descriptor;
            if (methods.containsKey(key)) {
                return methods.get(key);
            }
        }
        return name;
    }

    public String mapFilename(String infile) {
        // Trim ".class"
        String inclass = infile.substring(0, infile.length() - 6);

        // Apply standard remapping.
        String outclass = map(inclass);

        return outclass + ".class";
    }

    public boolean isExempt(String infile) {
        if (infile.toLowerCase().endsWith(".class")) {
            String inclass = infile.substring(0, infile.length() - 6);

            for (String exempt : exemptions) {
                if (inclass.startsWith(exempt + "/")) {
                    return true;
                }
            }

            // Non-exempt class file.
            return false;
        }

        // Non-class files are always exempt.
        return true;
    }

    public boolean needsOutput() {
        return true;
    }

    public String processZippedFilename(String filename) {
        if (isExempt(filename)) {
            return null;  // Do not process this file; copy it directly.
        } else {
            return mapFilename(filename);
        }
    }

    public void handleClass(InputStream in, OutputStream out) throws IOException {
        // Set up the ASM prerequisites.
        ClassReader cr = new ClassReader(in);
        NonloadingClassWriter cw = new NonloadingClassWriter(inheritance);
        RemappingClassAdapter visitor = new RemappingClassAdapter(cw, this);

        // Do the actual remapping.
        cr.accept(visitor, cr.SKIP_FRAMES);

        // Write out the translated class.
        out.write(cw.toByteArray());
    }
}
