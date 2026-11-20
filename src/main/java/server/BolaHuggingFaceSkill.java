package server;

import model.Mensagem;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

final class BolaHuggingFaceSkill implements BolaSkill {
    static final String ENDPOINT_HUGGING_FACE =
        "https://router.huggingface.co/v1/chat/completions";
    static final String ENDPOINT_GROQ = "https://api.groq.com/openai/v1/chat/completions";
    private static final int LIMITE_RESPOSTA_PADRAO = 480;
    private static final int LIMITE_TOKENS_RESPOSTA_PADRAO = 220;
    private static final String NOME_BOLA = "Bola";
    private static final String NOME_ASSISTENTE_IA = "Assistente IA";
    private final String nomeAssistente;
    private final String instrucaoSistema;
    private final String avisoSemToken;
    private final String token;
    private final String modeloPadrao;
    private final String endpoint;
    private final String provedor;
    private final int limiteTokensResposta;
    private final int limiteRespostaCaracteres;

    BolaHuggingFaceSkill() {
        this(NOME_BOLA, "Você é Bola, assistente especializado exclusivamente em futebol. "
            + "Responda em português sobre times, jogadores, campeonatos, regras e táticas. "
            + "Se a pergunta não for sobre futebol, explique brevemente que esse assunto é para "
            + "a Assistente IA e não tente respondê-la. Não invente resultados, tabelas, escalações "
            + "ou notícias atuais. Quando pedirem informações atuais, informe que a busca de notícias "
            + "de futebol está disponível neste chat.",
            "Recebi sua pergunta, mas a conversa livre com IA está desativada: "
                + "falta configurar BOLA_TOKEN no servidor. Notícias continuam disponíveis sem token; "
                + "experimente \"notícias do Flamengo\".",
            System.getenv("BOLA_TOKEN"), System.getenv("BOLA_MODEL"));
    }

    BolaHuggingFaceSkill(String nomeAssistente, String instrucaoSistema, String avisoSemToken) {
        this(nomeAssistente, instrucaoSistema, avisoSemToken,
            System.getenv("BOLA_TOKEN"), System.getenv("BOLA_MODEL"));
    }

    BolaHuggingFaceSkill(String nomeAssistente, String instrucaoSistema, String avisoSemToken,
                         String token, String modeloPadrao) {
        this(nomeAssistente, instrucaoSistema, avisoSemToken, token, modeloPadrao,
            ENDPOINT_HUGGING_FACE, "Hugging Face");
    }

    BolaHuggingFaceSkill(String nomeAssistente, String instrucaoSistema, String avisoSemToken,
                         String token, String modeloPadrao, String endpoint, String provedor) {
        this(nomeAssistente, instrucaoSistema, avisoSemToken, token, modeloPadrao,
            endpoint, provedor, LIMITE_TOKENS_RESPOSTA_PADRAO, LIMITE_RESPOSTA_PADRAO);
    }

    BolaHuggingFaceSkill(String nomeAssistente, String instrucaoSistema, String avisoSemToken,
                         String token, String modeloPadrao, String endpoint, String provedor,
                         int limiteTokensResposta, int limiteRespostaCaracteres) {
        if (limiteTokensResposta < 1 || limiteRespostaCaracteres < 1) {
            throw new IllegalArgumentException("Os limites de resposta devem ser positivos.");
        }
        this.nomeAssistente = nomeAssistente;
        this.instrucaoSistema = instrucaoSistema;
        this.avisoSemToken = avisoSemToken;
        this.token = token;
        this.modeloPadrao = modeloPadrao == null || modeloPadrao.isBlank()
            ? ConfiguracaoAssistentes.MODELO_BOLA_PADRAO : modeloPadrao.trim();
        this.endpoint = endpoint;
        this.provedor = provedor;
        this.limiteTokensResposta = limiteTokensResposta;
        this.limiteRespostaCaracteres = limiteRespostaCaracteres;
    }

    @Override
    public boolean atende(String consulta) {
        return true;
    }

    @Override
    public List<String> executar(String consulta, BolaContext contexto)
            throws IOException, InterruptedException {
        if (token == null || token.isBlank()) {
            return List.of(avisoSemToken);
        }
        String corpo = criarCorpo(modeloPadrao, consulta, contexto);
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
            .timeout(Duration.ofSeconds(45))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8))
            .build();
        HttpResponse<String> response = enviarComTentativas(request);

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String detalhe = response.body();
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new IOException("Chave do " + provedor + " inválida ou sem permissão "
                    + "(HTTP " + response.statusCode() + ").");
            }
            if (response.statusCode() == 429) {
                throw new IOException("Limite de requisições do " + provedor
                    + " atingido (HTTP 429). Aguarde e tente novamente.");
            }
            if (detalhe == null || detalhe.isBlank()) {
                detalhe = "Sem detalhes fornecidos pela API.";
            }
            String detalheNormalizado = detalhe.toLowerCase(java.util.Locale.ROOT);
            if (provedor.equals("Hugging Face") && response.statusCode() == 400
                    && detalheNormalizado.contains("not supported by any provider")) {
                throw new IOException("O modelo não está disponível para os provedores habilitados "
                    + "nesta conta Hugging Face. Confira se o token específico deste assistente permite "
                    + "chamadas aos Inference Providers e se o provedor configurado está habilitado; "
                    + "depois reinicie o servidor. "
                    + "Detalhe da API: " + detalhe);
            }
            if (detalhe.length() > 1000) {
                detalhe = detalhe.substring(0, 1000);
            }
            throw new IOException(provedor + " HTTP " + response.statusCode() + ": " + detalhe);
        }

        String resposta = extrairConteudo(response.body());
        if (resposta == null || resposta.isBlank()) {
            throw new IOException("A resposta do " + provedor + " não continha texto.");
        }
        resposta = resposta.trim();
        if (resposta.length() > limiteRespostaCaracteres) {
            resposta = resposta.substring(0, limiteRespostaCaracteres - 3) + "...";
        }
        return List.of(resposta);
    }

    /** Repete até 3 vezes em queda de conexão (ex.: Connection reset) ou erro 5xx. */
    private static HttpResponse<String> enviarComTentativas(HttpRequest request)
            throws IOException, InterruptedException {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        for (int tentativa = 1; ; tentativa++) {
            try {
                HttpResponse<String> resposta = http.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (resposta.statusCode() < 500 || tentativa == 3) return resposta;
            } catch (IOException e) {
                if (tentativa == 3) throw e;
            }
            Thread.sleep(700L * tentativa);
        }
    }

    String criarCorpo(String modelo, String consulta, BolaContext contexto) {
        StringBuilder corpo = new StringBuilder("{\"model\":\"")
            .append(escaparJson(modelo))
            .append("\",\"messages\":[{\"role\":\"system\",\"content\":\"")
            .append(escaparJson(instrucaoSistema)).append("\"}");

        int inicio = Math.max(0, contexto.historico().size() - 12);
        for (int i = inicio; i < contexto.historico().size(); i++) {
            Mensagem mensagem = contexto.historico().get(i);
            String remetente = mensagem.getRemetente();
            String role;
            String conteudo;
            if (remetente.equalsIgnoreCase(nomeAssistente)) {
                role = "assistant";
                conteudo = mensagem.getConteudo();
            } else if (ehOutroAssistente(remetente)) {
                continue;
            } else {
                role = "user";
                conteudo = remetente + ": " + mensagem.getConteudo();
            }
            corpo.append(",{\"role\":\"").append(role).append("\",\"content\":\"")
                .append(escaparJson(conteudo)).append("\"}");
        }
        corpo.append(",{\"role\":\"user\",\"content\":\"")
            .append(escaparJson(consulta))
            .append("\"}],\"max_tokens\":")
            .append(limiteTokensResposta)
            .append(",\"temperature\":0.6");
        // gpt-oss é modelo de raciocínio: sem isso o "pensamento" consome os tokens e a resposta vem vazia.
        if (provedor.equals("Groq") && modelo.toLowerCase(java.util.Locale.ROOT).contains("gpt-oss")) {
            corpo.append(",\"reasoning_effort\":\"low\"");
        }
        corpo.append("}");
        return corpo.toString();
    }

    private boolean ehOutroAssistente(String remetente) {
        if (nomeAssistente.equalsIgnoreCase(NOME_BOLA)) {
            return remetente.equalsIgnoreCase(NOME_ASSISTENTE_IA);
        }
        if (nomeAssistente.equalsIgnoreCase(NOME_ASSISTENTE_IA)) {
            return remetente.equalsIgnoreCase(NOME_BOLA);
        }
        return remetente.equalsIgnoreCase(NOME_BOLA)
            || remetente.equalsIgnoreCase(NOME_ASSISTENTE_IA);
    }

    /** Extrai apenas choices[0].message.content, evitando campos content de outros objetos. */
    private static String extrairConteudo(String json) throws IOException {
        int choices = json.indexOf("\"choices\"");
        if (choices < 0) throw new IOException("Resposta sem choices do provedor.");
        int array = json.indexOf('[', choices + 9);
        if (array < 0) throw new IOException("Lista choices inválida.");
        int message = json.indexOf("\"message\"", array);
        if (message < 0) throw new IOException("Resposta sem message.");
        int content = json.indexOf("\"content\"", message + 9);
        if (content < 0) throw new IOException("Resposta sem content.");
        int colon = json.indexOf(':', content + 9);
        if (colon < 0) throw new IOException("Campo content inválido.");
        int pos = colon + 1;
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;
        if (pos >= json.length() || json.charAt(pos) != '"') {
            throw new IOException("A API não retornou conteúdo textual.");
        }
        StringBuilder result = new StringBuilder();
        for (int i = pos + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') return result.toString();
            if (c != '\\') { result.append(c); continue; }
            if (++i >= json.length()) break;
            char e = json.charAt(i);
            switch (e) {
                case '"', '\\', '/' -> result.append(e);
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                case 't' -> result.append('\t');
                case 'b' -> result.append('\b');
                case 'f' -> result.append('\f');
                case 'u' -> {
                    if (i + 4 >= json.length()) throw new IOException("Unicode incompleto.");
                    try { result.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16)); }
                    catch (NumberFormatException ex) { throw new IOException("Unicode inválido.", ex); }
                    i += 4;
                }
                default -> throw new IOException("Escape JSON inválido.");
            }
        }
        throw new IOException("Resposta JSON incompleta.");
    }

    private static String escaparJson(String valor) {
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            switch (c) {
                case '"' -> resultado.append("\\\"");
                case '\\' -> resultado.append("\\\\");
                case '\b' -> resultado.append("\\b");
                case '\f' -> resultado.append("\\f");
                case '\n' -> resultado.append("\\n");
                case '\r' -> resultado.append("\\r");
                case '\t' -> resultado.append("\\t");
                default -> {
                    if (c < 0x20) resultado.append(String.format("\\u%04x", (int) c));
                    else resultado.append(c);
                }
            }
        }
        return resultado.toString();
    }
}
