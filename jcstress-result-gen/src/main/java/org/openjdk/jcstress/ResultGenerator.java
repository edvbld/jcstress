/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.jcstress;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class ResultGenerator {
    private final Set<String> generatedResults = new HashSet<>();
    private final String srcRoot;

    public ResultGenerator(String srcRoot) {
        this.srcRoot = srcRoot;
    }

    public String generateResult(Class<?>... args) {
        String name = "";
        for (Class k : args) {
            if (k.isPrimitive()) {
                if (k == boolean.class) name += "Z";
                if (k == byte.class)    name += "B";
                if (k == short.class)   name += "S";
                if (k == char.class)    name += "C";
                if (k == int.class)     name += "I";
                if (k == long.class)    name += "J";
                if (k == float.class)   name += "F";
                if (k == double.class)  name += "D";
            } else {
                name += "L";
            }
        }
        name += "_Result";

        // already generated
        if (!generatedResults.add(name))
            return name;

        PrintWriter pw;
        try {
            Path dir = Paths.get(srcRoot, "org.openjdk.jcstress.infra.results.".split("\\."));
            Path file = dir.resolve(name + ".java");
            Files.createDirectories(dir);
            pw = new PrintWriter(file.toFile());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        pw.println("package org.openjdk.jcstress.infra.results;");
        pw.println("");
        pw.println("import java.io.Serializable;");
        pw.println("import org.openjdk.jcstress.annotations.Result;");
        pw.println("");
        pw.println("@Result");
        pw.println("public class " + name + " implements Serializable {");

        {
            int n = 1;
            for (Class k : args) {
                pw.println("    @sun.misc.Contended");
                pw.println("    @jdk.internal.vm.annotation.Contended");
                pw.println("    public " + k.getSimpleName() + " r" + n + ";");
                pw.println();
                n++;
            }
        }

        pw.println("    public int hashCode() {");
        pw.println("        int result = 0;");
        {
            int n = 1;
            for (Class k : args) {
                if (k == boolean.class) {
                    pw.println("        result = 31*result + (r" + n + " ? 1 : 0);");
                }
                if (k == byte.class || k == short.class || k == char.class || k == int.class) {
                    pw.println("        result = 31*result + r" + n + ";");
                }
                if (k == long.class) {
                    pw.println("        result = 31*result + (int) (r" + n + " ^ (r" + n + " >>> 32));");
                }
                if (k == double.class) {
                    pw.println("        result = 31*result + (int) (Double.doubleToLongBits(r" + n + ") ^ (Double.doubleToLongBits(r" + n + ") >>> 32));");
                }
                if (k == float.class) {
                    pw.println("        result = 31*result + (int) (Float.floatToIntBits(r" + n + ") ^ (Float.floatToIntBits(r" + n + ") >>> 32));");
                }
                n++;
            }
        }
        pw.println("        return result;");
        pw.println("    }");
        pw.println();
        pw.println("    public boolean equals(Object o) {");
        pw.println("        if (this == o) return true;");
        pw.println("        if (o == null || getClass() != o.getClass()) return false;");
        pw.println();
        pw.println("        " + name + " that = (" + name + ") o;\n");

        {
            int n = 1;
            for (Class k : args) {
                if (k == boolean.class || k == byte.class || k == short.class || k == char.class
                        || k == int.class || k == long.class) {
                    pw.println("        if (r" + n + " != that.r" + n + ") return false;");
                }
                if (k == double.class) {
                    pw.println("        if (Double.compare(r" + n + ", that.r" + n + ") != 0) return false;");
                }
                if (k == float.class) {
                    pw.println("        if (Float.compare(r" + n + ", that.r" + n + ") != 0) return false;");
                }
                n++;
            }
        }

        pw.println("        return true;");
        pw.println("    }");

        pw.println("    public String toString() {");
        pw.print("        return \"\" + ");

        {
            int n = 1;
            for (Class k : args) {
                if (n != 1)
                    pw.print(" + \", \" + ");
                pw.print("r" + n);
                n++;
            }
            pw.println(";");
        }

        pw.println("    }");

        pw.println("}");
        pw.close();

        return name;
    }
}
