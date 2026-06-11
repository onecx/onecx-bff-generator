package org.tkit.onecx.onecxbffgen.model;

import org.tkit.onecx.onecxbffgen.service.VersionUtils;

public enum DependencyProfile {
    LEGACY_UP_TO_2_5,
    TRANSITION_2_6_TO_3_0,
    MODERN_3_1_PLUS;

    public static DependencyProfile fromParentVersion(String version) {
        if (VersionUtils.compare(version, "3.1.0") >= 0) {
            return MODERN_3_1_PLUS;
        }
        if (VersionUtils.compare(version, "2.5.0") <= 0) {
            return LEGACY_UP_TO_2_5;
        }
        return TRANSITION_2_6_TO_3_0;
    }
}