package org.tkit.onecx.onecxbffgen.model;

import java.util.LinkedHashMap;
import java.util.Map;

public record SchemaModel(String name, Map<String, String> fields) {

    public static SchemaModel empty(String name) {
        return new SchemaModel(name, new LinkedHashMap<>());
    }
}



