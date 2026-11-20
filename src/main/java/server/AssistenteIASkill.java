package server;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Integra a Assistente IA geral aos provedores configurados. */
final class AssistenteIASkill {
    private static final String ENDPOINT_GROQ = BolaHuggingFaceSkill.ENDPOINT_GROQ;
    private static final Pattern LINHA_COM_TABELA = Pattern.compile("(?m)^([^\\r\\n]*\\|[^\\r\\n]*)$");
    private static final Pattern SEPARADOR_TABELA = Pattern.compile(":?-{2,}:?");
    private static final Pattern LINK_MARKDOWN =
        Pattern.compile("\\[([^\\]]+)]\\((https?://[^\\s)]+)\\)");
    private static final String INSTRUCAO_SISTEMA =
        "Você é uma assistente virtual muito educada, atenciosa, paciente, inteligente e prestativa. "
            + "Converse de forma natural, acolhedora e respeitosa, como alguém que realmente quer ajudar. "
            + "Responda a qualquer mensagem, inclusive conversa casual, e retribua cumprimentos como oi, "
            + "olá, bom dia, boa tarde e boa noite antes de ajudar. Nunca exija um formato especial "
            + "de pergunta. "
            + "Responda em português, a menos que a pessoa prefira outro idioma. "
            + "Você é uma assistente generalista: ajude com matemática escolar e avançada, matemática "
            + "computacional, algoritmos numéricos, estatística, ciências, história, geografia, línguas, "
            + "literatura, cultura, estudos, escrita, tecnologia, programação, trabalho, organização, "
            + "criatividade, alimentação, dieta e outros assuntos. "
            + "Explique conceitos com clareza e exemplos simples; adapte a profundidade ao pedido, "
            + "mostre os passos de cálculos quando forem úteis e confira o resultado. "
            + "Respeite o tom, o nível de detalhe e o formato que a pessoa pedir; se não estiver claro, "
            + "responda diretamente e pergunte como ela prefere explicações futuras. Seja sincera e gentil: "
            + "não concorde só para agradar, corrija erros com respeito e não prometa uma resposta desejada "
            + "se ela não for verdadeira. "
            + "Use parágrafos e quebras de linha quando ajudarem a leitura. Organize instruções e opções "
            + "em listas simples, sem tabelas nem formatação Markdown. Seja clara e suficientemente "
            + "completa, sem ser prolixa. Não invente fatos nem finja ter acesso a notícias ou dados atuais; "
            + "quando houver incerteza, diga isso com honestidade. Em assuntos médicos, jurídicos ou "
            + "financeiros, ofereça informação geral e recomende um profissional para decisões importantes. "
            + "Ao falar de dieta ou alimentação, dê orientação geral equilibrada, não recomende dietas "
            + "extremas, restrição perigosa ou suplementos como tratamento, e considere alergias e condições "
            + "de saúde perguntando detalhes relevantes; encaminhe necessidades clínicas a nutricionista "
            + "ou médico.";

    private static final int LIMITE_RESPOSTA_CARACTERES = 6000;
    private static final int LIMITE_TOKENS_RESPOSTA = 3000;

    private final BolaHuggingFaceSkill cliente;

    AssistenteIASkill(String token, String modelo) {
        this("", "", token, modelo);
    }

    AssistenteIASkill(String groqApiKey, String groqModel, String token, String modelo) {
        if (groqApiKey != null && !groqApiKey.isBlank()) {
            cliente = new BolaHuggingFaceSkill(
                "Assistente IA",
                INSTRUCAO_SISTEMA,
                "A conversa com a Assistente IA está desativada: configure GROQ_API_KEY "
                    + "ou ASSISTENTE_IA_TOKEN no arquivo .env.",
                groqApiKey,
                groqModel == null || groqModel.isBlank()
                    ? ConfiguracaoAssistentes.MODELO_GROQ_PADRAO : groqModel,
                ENDPOINT_GROQ,
                "Groq",
                LIMITE_TOKENS_RESPOSTA,
                LIMITE_RESPOSTA_CARACTERES);
            return;
        }
        cliente = new BolaHuggingFaceSkill(
            "Assistente IA",
            INSTRUCAO_SISTEMA,
            "A conversa com a Assistente IA está desativada: falta configurar "
                + "GROQ_API_KEY ou ASSISTENTE_IA_TOKEN no arquivo .env.",
            token,
            modelo,
            BolaHuggingFaceSkill.ENDPOINT_HUGGING_FACE,
            "Hugging Face",
            LIMITE_TOKENS_RESPOSTA,
            LIMITE_RESPOSTA_CARACTERES);
    }

    List<String> responder(String pergunta, BolaContext contexto)
            throws IOException, InterruptedException {
        String resposta = normalizarResposta(String.join(" ", cliente.executar(pergunta, contexto)));
        if (resposta.isBlank()) {
            throw new IOException("A resposta da Assistente IA ficou vazia após a formatação.");
        }
        return List.of(resposta);
    }

    static String normalizarResposta(String resposta) {
        if (resposta == null || resposta.isBlank()) return "";
        String preparada = resposta
            .replaceAll("(?m)^[ \\t]*#{1,6}[ \\t]*", "")
            .replaceAll("(?m)^[ \\t]*(?:```|~~~)[^\\r\\n]*$", "")
            .replaceAll("(?m)^[ \\t]*(?:-{3,}|\\*{3,}|_{3,})[ \\t]*$", "")
            .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
            .replaceAll("(?<!\\*)\\*([^*\\r\\n]+)\\*(?!\\*)", "$1")
            .replaceAll("`{1,3}([^`\\r\\n]+)`{1,3}", "$1")
            .replaceAll("(?m)^[ \\t]*[-*+][ \\t]+", "• ");
        Matcher links = LINK_MARKDOWN.matcher(preparada);
        StringBuffer respostaComLinks = new StringBuffer();
        while (links.find()) {
            links.appendReplacement(respostaComLinks,
                Matcher.quoteReplacement(links.group(1) + " (" + links.group(2) + ")"));
        }
        links.appendTail(respostaComLinks);
        preparada = respostaComLinks.toString()
            .replaceAll("(?m)^[ \\t]*\\|?[ \\t]*:?-{2,}:?[ \\t]*"
                + "(?:\\|[ \\t]*:?-{2,}:?[ \\t]*)+\\|?[ \\t]*\\R?", "");
        Matcher linhasTabela = LINHA_COM_TABELA.matcher(preparada);
        StringBuffer respostaFormatada = new StringBuffer();
        while (linhasTabela.find()) {
            List<String> celulas = Arrays.stream(linhasTabela.group().split("\\|"))
                .map(String::trim)
                .filter(celula -> !celula.isEmpty())
                .toList();
            String trecho = celulas.stream().allMatch(SEPARADOR_TABELA.asPredicate())
                ? ""
                : String.join(" | ", celulas);
            linhasTabela.appendReplacement(respostaFormatada, Matcher.quoteReplacement(trecho));
        }
        linhasTabela.appendTail(respostaFormatada);
        String[] linhas = respostaFormatada.toString().replace('\r', '\n').split("\\n", -1);
        List<String> linhasNormalizadas = new java.util.ArrayList<>();
        boolean linhaEmBranco = false;
        for (String linha : linhas) {
            String linhaLimpa = linha.replaceAll("[\\t\\x0B\\f ]+", " ").trim();
            if (linhaLimpa.isEmpty()) {
                if (!linhasNormalizadas.isEmpty() && !linhaEmBranco) {
                    linhasNormalizadas.add("");
                }
                linhaEmBranco = true;
                continue;
            }
            linhasNormalizadas.add(linhaLimpa);
            linhaEmBranco = false;
        }
        return String.join("\n", linhasNormalizadas).trim();
    }
}
