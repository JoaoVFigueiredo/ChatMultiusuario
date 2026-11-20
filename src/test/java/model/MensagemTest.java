package model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class MensagemTest {
    @Test
    void formatarIncluiHoraRemetenteEConteudo() {
        Mensagem m = new Mensagem("Ana", "oi pessoal", LocalDateTime.of(2026, 8, 10, 14, 30));
        assertEquals("[14:30] Ana: oi pessoal", m.formatar());
    }

    @Test
    void conteudoVazioLancaExcecao() {
        assertThrows(IllegalArgumentException.class,
            () -> new Mensagem("Ana", "   ", LocalDateTime.now()));
    }

    @Test
    void remetenteNuloLancaExcecao() {
        assertThrows(IllegalArgumentException.class,
            () -> new Mensagem(null, "oi", LocalDateTime.now()));
    }

    @Test
    void conteudoEhAparado() {
        Mensagem m = Mensagem.nova("Bia", "  olá  ");
        assertEquals("olá", m.getConteudo());
    }
}
