package client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormatadorRespostaIATest {
    @Test
    void preservaParagrafosTituloEListaComFormatacao() {
        FormatadorRespostaIA.Resultado resultado = FormatadorRespostaIA.formatar(
            "## Ideias para estudar\n\n- Leia **um capítulo** por dia 😊\n"
                + "- Revise as anotações na sexta-feira.");

        assertTrue(resultado.texto().startsWith(
            "Ideias para estudar.\n\n• Leia um capítulo"));
        assertTrue(resultado.texto().contains("😊"));
        assertTrue(resultado.negrito().size() == 1);
        assertTrue(resultado.titulos().size() == 1);
        assertTrue(resultado.texto().contains("\n• Revise"));
        assertFalse(resultado.texto().contains("- Revise"));
        assertFalse(resultado.texto().contains("##"));
        assertFalse(resultado.texto().contains("**"));
    }

    @Test
    void transformaTabelaMarkdownEmUmParagrafoSemSintaxeMarkdown() {
        FormatadorRespostaIA.Resultado resultado = FormatadorRespostaIA.formatar(
            "## Ideias\n"
                + "| # | Ideia | Como colocar em prática | Por que funciona | Dica |\n"
                + "|---|---|---|---|---|\n"
                + "| 1 | **Grupo temático** | Abra um grupo no app | Foco em um assunto | Combine horários |");

        assertTrue(resultado.texto().contains("Ideias.\n• Grupo temático"));
        assertTrue(resultado.texto().contains("Como colocar em prática: Abra um grupo no app"));
        assertTrue(resultado.texto().contains("Por que funciona: Foco em um assunto"));
        assertTrue(resultado.texto().contains("Dica: Combine horários"));
        assertTrue(resultado.texto().contains("\n"));
        assertFalse(resultado.texto().contains("-"));
        assertFalse(resultado.texto().contains("|"));
        assertFalse(resultado.texto().contains("---"));
        assertFalse(resultado.texto().contains("**"));
    }

    @Test
    void mantemUnderscoresEmNomesDeVariaveis() {
        FormatadorRespostaIA.Resultado resultado =
            FormatadorRespostaIA.formatar("Configure GROQ_API_KEY e __mantenha o destaque__.");

        assertTrue(resultado.texto().contains("GROQ_API_KEY"));
        assertTrue(resultado.texto().contains("mantenha o destaque"));
    }
}
