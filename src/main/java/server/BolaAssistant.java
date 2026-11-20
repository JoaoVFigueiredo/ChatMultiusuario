package server;

import java.io.IOException;
import java.util.List;

/** Encaminha cada pedido para a primeira habilidade capaz de atendê-lo. */
final class BolaAssistant {
    private final List<BolaSkill> habilidades;
    private static final String INSTRUCAO_SISTEMA =
        "Você é Bola, assistente especializado exclusivamente em futebol. "
            + "Responda em português sobre times, jogadores, campeonatos, regras e táticas. "
            + "Se a pergunta não for sobre futebol, explique brevemente que esse assunto é para "
            + "a Assistente IA e não tente respondê-la. Não invente resultados, tabelas, escalações "
            + "ou notícias atuais. Para informações de jogos recentes, prefira a busca de notícias "
            + "disponível neste chat e deixe claro quando ela não confirmar um resultado.";
    private static final String AVISO_SEM_TOKEN =
        "As respostas livres do Bola estão desativadas: configure GROQ_API_KEY ou BOLA_TOKEN "
            + "no arquivo .env. Notícias e classificação continuam disponíveis.";

    BolaAssistant() {
        this(System.getenv("GROQ_API_KEY"), System.getenv("GROQ_MODEL"),
            System.getenv("BOLA_TOKEN"), System.getenv("BOLA_MODEL"));
    }

    BolaAssistant(String token, String modelo) {
        this("", "", token, modelo);
    }

    BolaAssistant(String groqApiKey, String groqModel, String token, String modelo) {
        habilidades = List.of(
            new BolaComandosSkill(),
            new BolaClassificacaoSkill(),
            new BolaNoticiasSkill(),
            criarAssistente(groqApiKey, groqModel, token, modelo)
        );
    }

    private static BolaHuggingFaceSkill criarAssistente(
            String groqApiKey, String groqModel, String token, String modelo) {
        if (groqApiKey != null && !groqApiKey.isBlank()) {
            return new BolaHuggingFaceSkill(
                "Bola",
                INSTRUCAO_SISTEMA,
                AVISO_SEM_TOKEN,
                groqApiKey,
                groqModel == null || groqModel.isBlank()
                    ? ConfiguracaoAssistentes.MODELO_GROQ_PADRAO : groqModel,
                BolaHuggingFaceSkill.ENDPOINT_GROQ,
                "Groq",
                1500,
                1600);
        }
        return new BolaHuggingFaceSkill(
            "Bola",
            INSTRUCAO_SISTEMA,
            AVISO_SEM_TOKEN,
            token,
            modelo,
            BolaHuggingFaceSkill.ENDPOINT_HUGGING_FACE,
            "Hugging Face",
            1500,
            1600);
    }

    List<String> responder(String consulta, BolaContext contexto)
            throws IOException, InterruptedException {
        for (BolaSkill habilidade : habilidades) {
            if (habilidade.atende(consulta)) {
                return habilidade.executar(consulta, contexto);
            }
        }
        throw new IllegalStateException("Nenhuma habilidade do Bola aceitou a consulta.");
    }
}
