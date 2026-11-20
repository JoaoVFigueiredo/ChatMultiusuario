package server;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AssistenteIASkillTest {
    @Test
    void normalizaRespostaSemPerderParagrafosEListas() {
        String resposta = AssistenteIASkill.normalizarResposta(
            "## Plano simples\n\n- Consulte um profissional.\n"
                + "- Café da manhã: **aveia + fruta**\n"
                + "| Etapa | Ação |\n|---|---|\n| 1 | Comece devagar |");

        assertEquals(
            "Plano simples\n\n• Consulte um profissional.\n• Café da manhã: aveia + fruta\n"
                + "Etapa | Ação\n1 | Comece devagar",
            resposta);
        assertFalse(resposta.contains("**"));
    }

    @Test
    void preservaTextoDeLinksESeparacaoEntreParagrafos() {
        String resposta = AssistenteIASkill.normalizarResposta(
            "### Resposta\n\n- Item A\n- Item com hífen - e **ênfase**\n\n"
                + "Consulte [o guia](https://exemplo.org).");

        assertEquals(
            "Resposta\n\n• Item A\n• Item com hífen - e ênfase\n\n"
                + "Consulte o guia (https://exemplo.org).",
            resposta);
    }

    @Test
    void informaQuandoNaoHaTokenConfigurado() throws Exception {
        AssistenteIASkill assistente = new AssistenteIASkill(
            "", ConfiguracaoAssistentes.MODELO_IA_PADRAO);

        assertEquals(
            List.of("A conversa com a Assistente IA está desativada: falta configurar "
                + "GROQ_API_KEY ou ASSISTENTE_IA_TOKEN no arquivo .env."),
            assistente.responder("olá", new BolaContext(List.of(), List.of())));
    }
}
