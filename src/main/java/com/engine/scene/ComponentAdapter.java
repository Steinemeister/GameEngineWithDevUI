package com.engine.scene;

import com.google.gson.*;

import java.lang.reflect.Type;

public class ComponentAdapter implements JsonSerializer<Component>, JsonDeserializer<Component> {
    @Override
    public JsonElement serialize(Component src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        // Wir schreiben den vollstaendigen Java-Klassennamen als "type"-Attribut in das JSON
        jsonObject.addProperty("type", src.getClass().getName());
        // Die restlichen Daten der Komponente werden ganz normal gespiegelt
        jsonObject.add("properties", context.serialize(src));
        return jsonObject;
    }

    @Override
    public Component deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        // Wir lesen den Klassennamen aus dem "type"-Attribut wieder aus
        String className = jsonObject.get("type").getAsString();
        JsonElement properties = jsonObject.get("properties");

        try {
            // Wir zwingen Java, die echte, konkrete Klasse (z.B. RotationComponent) im RAM zu suchen
            Class<?> clazz = Class.forName(className);
            return context.deserialize(properties, clazz);
        } catch (ClassNotFoundException e) {
            throw new JsonParseException("Unbekannte Komponente geladen: " + className, e);
        }
    }
}
