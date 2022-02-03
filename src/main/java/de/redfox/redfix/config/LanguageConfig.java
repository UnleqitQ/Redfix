package de.redfox.redfix.config;

import com.google.gson.JsonPrimitive;

import java.util.Map;

public class LanguageConfig extends ConfigObject {
    public enum Locale {
        DE("de_DE"), EN("en_US");

        String name;

        Locale(String name) {
            this.name = name;
        }
    };

    public LanguageConfig(String path, String file) {
        super(path, file);
        super.set("selected_lang", new JsonPrimitive(Locale.DE.name()));
    }

    public Locale getSelectedLanguage() {
        return Locale.valueOf(super.get("selected_lang").getAsString());
    }

    public void registerMessage(Locale locale, LinkedPath path, String message) {
        super.setDefault(locale.name + "." + LinkedPath.translate(path), new JsonPrimitive(message));
    }

    public void registerMessages(Locale locale, Map<String, String> entries) {
        entries.forEach((k, v) -> super.setDefault(locale.name + "." + k,  new JsonPrimitive(v)));
        super.save();
    }

    public String getMessage(Locale locale, LinkedPath path) {
        return super.get(locale.name + "." + LinkedPath.translate(path)).getAsString();
    }

    public String getMessage(LinkedPath path) {
        return getMessage(LinkedPath.translate(path));
    }

    public String getMessage(String path) {
        return super.get(getSelectedLanguage().name + "." + path).getAsString();
    }
}
