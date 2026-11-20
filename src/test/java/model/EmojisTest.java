package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmojisTest {
    @Test
    void atalhoDeCarinhaViraEmoji() {
        String r = Emojis.converter("oi :)");
        assertFalse(r.contains(":)"));
        assertTrue(r.startsWith("oi "));
        assertNotEquals("oi :)", r);
    }

    @Test
    void choroTemPrioridadeSobreCarinhaTriste() {
        assertFalse(Emojis.converter(":'(").contains("("));
    }

    @Test
    void textoSemAtalhoFicaIgual() {
        assertEquals("bom dia", Emojis.converter("bom dia"));
    }

    @Test
    void nuloContinuaNulo() {
        assertNull(Emojis.converter(null));
    }
}
