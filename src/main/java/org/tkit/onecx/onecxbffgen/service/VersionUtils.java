package org.tkit.onecx.onecxbffgen.service;

public final class VersionUtils {

    private VersionUtils() {
    }

    public static int compare(String left, String right) {
        int[] l = parse(left);
        int[] r = parse(right);
        for (int i = 0; i < Math.max(l.length, r.length); i++) {
            int li = i < l.length ? l[i] : 0;
            int ri = i < r.length ? r[i] : 0;
            if (li != ri) {
                return Integer.compare(li, ri);
            }
        }
        return 0;
    }

    private static int[] parse(String version) {
        String[] parts = version.replaceFirst("^[^0-9]+", "").split("[.-]");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Integer.parseInt(parts[i].replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                result[i] = 0;
            }
        }
        return result;
    }
}



