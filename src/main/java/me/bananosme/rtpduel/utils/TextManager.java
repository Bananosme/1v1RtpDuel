package me.bananosme.rtpduel.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Сообщения из config.yml (секция messages). Форматирование через MiniMessage
 * (<green>, <bold>, <#ff0000>), а также легаси-коды & (&a&l, &#RRGGBB).
 */
public final class TextManager {

    private static final Map<Character, String> LEGACY_CODES = new HashMap<>();

    static {
        LEGACY_CODES.put('0', "black");
        LEGACY_CODES.put('1', "dark_blue");
        LEGACY_CODES.put('2', "dark_green");
        LEGACY_CODES.put('3', "dark_aqua");
        LEGACY_CODES.put('4', "dark_red");
        LEGACY_CODES.put('5', "dark_purple");
        LEGACY_CODES.put('6', "gold");
        LEGACY_CODES.put('7', "gray");
        LEGACY_CODES.put('8', "dark_gray");
        LEGACY_CODES.put('9', "blue");
        LEGACY_CODES.put('a', "green");
        LEGACY_CODES.put('b', "aqua");
        LEGACY_CODES.put('c', "red");
        LEGACY_CODES.put('d', "light_purple");
        LEGACY_CODES.put('e', "yellow");
        LEGACY_CODES.put('f', "white");
        LEGACY_CODES.put('k', "obfuscated");
        LEGACY_CODES.put('l', "bold");
        LEGACY_CODES.put('m', "strikethrough");
        LEGACY_CODES.put('n', "underlined");
        LEGACY_CODES.put('o', "italic");
        LEGACY_CODES.put('r', "reset");
    }

    private final JavaPlugin plugin;

    public TextManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public String messageString(String key) {
        return plugin.getConfig().getString("messages." + key, "");
    }

    private static String format(String template, Map<String, Object> placeholders) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        if (placeholders == null || placeholders.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }

    public Component parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        return MiniMessage.miniMessage().deserialize(convertLegacy(raw));
    }

    public Component message(String key, Map<String, Object> placeholders) {
        return parse(format(messageString(key), placeholders));
    }

    public Component message(String key) {
        return message(key, null);
    }

    /**
     * Сообщение как легаси-строка с §-кодами (для API без Component, например BossBar).
     */
    public String messageLegacy(String key, Map<String, Object> placeholders) {
        return LegacyComponentSerializer.legacySection().serialize(message(key, placeholders));
    }

    private static String convertLegacy(String input) {
        if (input.indexOf('&') < 0 && input.indexOf('§') < 0) {
            return input;
        }
        StringBuilder sb = new StringBuilder(input.length() + 16);
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c != '&' && c != '§') {
                sb.append(c);
                continue;
            }
            if (i + 1 >= chars.length) {
                sb.append(c);
                continue;
            }
            char next = chars[i + 1];
            if (next == '&' || next == '§') {
                sb.append('&');
                i++;
                continue;
            }
            if (next == '#') {
                String hex = parseHex(chars, i + 2);
                if (hex != null) {
                    sb.append('<').append('#').append(hex).append('>');
                    i += 7;
                    continue;
                }
                sb.append(c);
                continue;
            }
            if (next == 'x' || next == 'X') {
                String hex = parseHexFormatted(chars, i + 2);
                if (hex != null) {
                    sb.append('<').append('#').append(hex).append('>');
                    i += 13;
                    continue;
                }
            }
            String tag = LEGACY_CODES.get(next);
            if (tag != null) {
                sb.append('<').append(tag).append('>');
                i++;
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static String parseHex(char[] chars, int start) {
        if (start + 6 > chars.length) {
            return null;
        }
        for (int i = start; i < start + 6; i++) {
            if (!isHexChar(chars[i])) {
                return null;
            }
        }
        return String.copyValueOf(chars, start, 6).toLowerCase();
    }

    private static String parseHexFormatted(char[] chars, int start) {
        if (start + 12 > chars.length) {
            return null;
        }
        StringBuilder hex = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            int amp = start + i * 2;
            if (chars[amp] != '&' && chars[amp] != '§') {
                return null;
            }
            char ch = chars[amp + 1];
            if (!isHexChar(ch)) {
                return null;
            }
            hex.append(ch);
        }
        return hex.toString().toLowerCase();
    }
}