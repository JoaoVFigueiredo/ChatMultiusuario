package model;

import java.util.LinkedHashMap;
import java.util.Map;

/** Atalhos de texto que viram emoji (:) vira carinha, <3 vira coração...) e o painel do seletor. */
public final class Emojis {
    private static final Map<String, String> ATALHOS = new LinkedHashMap<>();

    static {
        // A ordem importa: ":'(" precisa vir antes de ":("
        ATALHOS.put(":'(", "😢");
        ATALHOS.put(":-)", "🙂");
        ATALHOS.put(":)", "🙂");
        ATALHOS.put(":D", "😄");
        ATALHOS.put(":(", "🙁");
        ATALHOS.put(";)", "😉");
        ATALHOS.put("<3", "❤️");
        ATALHOS.put("(y)", "👍");
        ATALHOS.put(":coracao:", "❤️");
        ATALHOS.put(":riso:", "😂");
        ATALHOS.put(":fogo:", "🔥");
        ATALHOS.put(":festa:", "🎉");
        ATALHOS.put(":legal:", "😎");
        ATALHOS.put(":palmas:", "👏");
        ATALHOS.put(":pensando:", "🤔");
        ATALHOS.put(":ok:", "👌");
    }

    public static final String[] PAINEL = {
        "😀","😁","😂","🤣","😊","😍","😘","😎",
        "🤔","😅","😭","😡","😱","🥳","😴","🤗",
        "🙄","😉","🙂","🙃","😇","🤩","😋","😜",
        "🥺","😢","👍","👎","👏","🙌","🙏","💪",
        "👌","✌️","🤝","❤️","💔","💙","💚","💛",
        "💜","🔥","🎉","✨","⭐","🎂","🍕","🍔",
        "☕","⚽","🎮","🚀","🌈","🌞","🌙","🐶","🐱"
    };

    private Emojis() { }

    public static String converter(String texto) {
        if (texto == null) {
            return null;
        }
        String r = texto;
        for (Map.Entry<String, String> e : ATALHOS.entrySet()) {
            r = r.replace(e.getKey(), e.getValue());
        }
        return r;
    }
}
