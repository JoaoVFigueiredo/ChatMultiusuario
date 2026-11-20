package server;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HuggingFaceServiceTest {
    @Test
    void retornaAvisoDeConfiguracaoQuandoNaoHaToken() throws Exception {
        HuggingFaceService service = new HuggingFaceService(
            "", ConfiguracaoAssistentes.MODELO_IA_PADRAO);

        assertEquals(
            "A conversa com a Assistente IA está desativada: falta configurar "
                + "GROQ_API_KEY ou ASSISTENTE_IA_TOKEN no arquivo .env.",
            service.enviarMensagem("Olá", new BolaContext(List.of(), List.of())));
    }
}
