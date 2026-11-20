package server;

import java.io.IOException;
import java.util.List;

/** Serviço da Assistente IA; credenciais e modelo vêm da configuração isolada. */
final class HuggingFaceService {
    private final AssistenteIASkill assistente;

    HuggingFaceService(String token, String modelo) {
        assistente = new AssistenteIASkill(token, modelo);
    }

    HuggingFaceService(String groqApiKey, String groqModel,
                       String token, String modelo) {
        assistente = new AssistenteIASkill(groqApiKey, groqModel, token, modelo);
    }

    String enviarMensagem(String promptDoUsuario) throws IOException, InterruptedException {
        return enviarMensagem(promptDoUsuario, new BolaContext(List.of(), List.of()));
    }

    String enviarMensagem(String promptDoUsuario, BolaContext contexto)
            throws IOException, InterruptedException {
        return String.join("\n", assistente.responder(promptDoUsuario, contexto));
    }
}
