package server;

import model.Mensagem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BolaAssistantTest {
    private final BolaAssistant assistant = new BolaAssistant();

    @Test
    void selecionaHabilidadeLocalParaResumoClimaESorteio() throws Exception {
        BolaContext contexto = new BolaContext(
            List.of(Mensagem.nova("Ana", "Vamos falar do jogo de domingo.")),
            List.of("Ana", "Bia"));

        List<String> resumo = assistant.responder("resumo", contexto);
        List<String> clima = assistant.responder("clima em São Paulo", contexto);
        List<String> sorteio = assistant.responder("sortear", contexto);
        List<String> ajudaClassificacao = assistant.responder("classificação brasileira", contexto);

        assertAll(
            () -> assertTrue(resumo.get(0).contains("jogo de domingo")),
            () -> assertTrue(clima.get(0).contains("previsão do tempo")),
            () -> assertTrue(sorteio.get(0).contains("Ana")),
            () -> assertTrue(sorteio.get(0).contains("Bia")),
            () -> assertTrue(ajudaClassificacao.get(0).contains("Série A")
                && ajudaClassificacao.get(0).contains("Série B"))
        );
    }

    @Test
    void avisaQuandoNaoHaProvedorConfiguradoParaPerguntasLivres() throws Exception {
        BolaAssistant semProvedor = new BolaAssistant("", "", "", "");

        List<String> resposta = semProvedor.responder(
            "Explique a regra do impedimento", new BolaContext(List.of(), List.of()));

        assertTrue(resposta.get(0).contains("GROQ_API_KEY ou BOLA_TOKEN"));
    }
}
