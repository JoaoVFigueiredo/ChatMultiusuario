package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {
    @Test
    void nomeValidoEhAparado() {
        assertEquals("Carlos", new Usuario("  Carlos ").getNome());
    }

    @Test
    void nomeVazioLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> new Usuario(""));
    }

    @Test
    void nomeMuitoLongoLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> new Usuario("x".repeat(21)));
    }

    @Test
    void nomeComBarraVerticalLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> new Usuario("a|b"));
    }

    @Test
    void corInvalidaLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> new Usuario("Ana", "azul"));
    }

    @Test
    void trocarCorGeraNovoUsuarioMantendoONome() {
        Usuario u = new Usuario("Ana", "#e91e63");
        Usuario v = u.comCor("#00897b");
        assertEquals("#E91E63", u.getCor());
        assertEquals("#00897B", v.getCor());
        assertEquals("Ana", v.getNome());
    }
}
