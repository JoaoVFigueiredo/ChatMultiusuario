package server;

import model.Mensagem;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BolaHuggingFaceSkillTest {
    @Test
    void usaEndpointGroqComChaveEModeloConfigurados() throws Exception {
        AtomicReference<String> autorizacao = new AtomicReference<>();
        AtomicReference<String> corpo = new AtomicReference<>();
        HttpServer servidorHttp = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidorHttp.createContext("/v1/chat/completions", requisicao -> {
            autorizacao.set(requisicao.getRequestHeaders().getFirst("Authorization"));
            corpo.set(new String(requisicao.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] resposta = "{\"choices\":[{\"message\":{\"content\":\"Resposta Groq\"}}]}"
                .getBytes(StandardCharsets.UTF_8);
            requisicao.sendResponseHeaders(200, resposta.length);
            requisicao.getResponseBody().write(resposta);
            requisicao.close();
        });
        servidorHttp.start();
        try {
            String endpoint = "http://127.0.0.1:" + servidorHttp.getAddress().getPort()
                + "/v1/chat/completions";
            BolaHuggingFaceSkill cliente = new BolaHuggingFaceSkill(
                "Assistente IA", "Responda em português.", "Sem chave.",
                "chave-de-teste", "modelo-de-teste", endpoint, "Groq");

            assertEquals(
                List.of("Resposta Groq"),
                cliente.executar("Olá", new BolaContext(List.of(), List.of())));
            assertEquals("Bearer chave-de-teste", autorizacao.get());
            assertTrue(corpo.get().contains("\"model\":\"modelo-de-teste\""));
        } finally {
            servidorHttp.stop(0);
        }
    }

    @Test
    void localizaEnvNaRaizMesmoQuandoExecucaoParteDeSubpasta() throws Exception {
        java.nio.file.Path raiz = java.nio.file.Files.createTempDirectory("projeto-assistentes");
        java.nio.file.Path subpasta = java.nio.file.Files.createDirectories(raiz.resolve("src/main/java"));
        java.nio.file.Path env = java.nio.file.Files.createFile(raiz.resolve(".env"));
        try {
            org.junit.jupiter.api.Assertions.assertEquals(
                env, ConfiguracaoAssistentes.localizarArquivoEnv(subpasta));
        } finally {
            java.nio.file.Files.deleteIfExists(env);
            java.nio.file.Files.deleteIfExists(subpasta);
            java.nio.file.Files.deleteIfExists(subpasta.getParent());
            java.nio.file.Files.deleteIfExists(subpasta.getParent().getParent());
            java.nio.file.Files.deleteIfExists(raiz);
        }
    }

    @Test
    void configuracaoCarregaTokensSeparadosEIgnoraVariaveisGlobaisHf() throws Exception {
        java.nio.file.Path arquivo = java.nio.file.Files.createTempFile("assistentes", ".env");
        try {
            java.nio.file.Files.writeString(arquivo,
                "GROQ_API_KEY=token-groq\n"
                    + "GROQ_MODEL=modelo-groq\n"
                    + "ASSISTENTE_IA_TOKEN=token-ia\n"
                    + "ASSISTENTE_IA_MODEL=modelo-ia:provider\n"
                    + "BOLA_TOKEN=token-bola\n"
                    + "BOLA_MODEL=modelo-bola:provider\n");

            ConfiguracaoAssistentes configuracao =
                ConfiguracaoAssistentes.ler(arquivo, Map.of("HF_TOKEN", "nao-usar"));

            org.junit.jupiter.api.Assertions.assertEquals("token-ia", configuracao.assistenteIaToken());
            org.junit.jupiter.api.Assertions.assertEquals("modelo-ia:provider", configuracao.assistenteIaModelo());
            org.junit.jupiter.api.Assertions.assertEquals("token-groq", configuracao.groqApiKey());
            org.junit.jupiter.api.Assertions.assertEquals("modelo-groq", configuracao.groqModel());
            org.junit.jupiter.api.Assertions.assertEquals("token-bola", configuracao.bolaToken());
            org.junit.jupiter.api.Assertions.assertEquals("modelo-bola:provider", configuracao.bolaModelo());
        } finally {
            java.nio.file.Files.deleteIfExists(arquivo);
        }
    }

    @Test
    void variaveisDeAmbienteEspecificasSobrescrevemArquivoEnv() throws Exception {
        java.nio.file.Path arquivo = java.nio.file.Files.createTempFile("assistentes", ".env");
        try {
            java.nio.file.Files.writeString(arquivo, "ASSISTENTE_IA_TOKEN=token-do-arquivo\n");

            ConfiguracaoAssistentes configuracao = ConfiguracaoAssistentes.ler(
                arquivo, Map.of(
                    "ASSISTENTE_IA_TOKEN", "token-do-ambiente",
                    "GROQ_API_KEY", "groq-do-ambiente",
                    "GROQ_MODEL", "modelo-groq"));

            org.junit.jupiter.api.Assertions.assertEquals(
                "token-do-ambiente", configuracao.assistenteIaToken());
            org.junit.jupiter.api.Assertions.assertEquals(
                "groq-do-ambiente", configuracao.groqApiKey());
            org.junit.jupiter.api.Assertions.assertEquals("modelo-groq", configuracao.groqModel());
            org.junit.jupiter.api.Assertions.assertEquals(
                ConfiguracaoAssistentes.MODELO_IA_PADRAO, configuracao.assistenteIaModelo());
        } finally {
            java.nio.file.Files.deleteIfExists(arquivo);
        }
    }

    @Test
    void assistenteIaPreservaSuasRespostasEIsolaOContextoDaBola() {
        BolaHuggingFaceSkill assistente = new BolaHuggingFaceSkill(
            "Assistente IA", "Assistente geral em português.", "Sem token.");
        BolaContext contexto = new BolaContext(List.of(
            Mensagem.nova("Ana", "Explique frações."),
            Mensagem.nova("Assistente IA", "Uma fração representa partes de um todo."),
            Mensagem.nova("Bola", "O Flamengo venceu a partida."),
            Mensagem.nova("Bia", "Pode dar outro exemplo?")
        ), List.of("Ana", "Bia"));

        String corpo = assistente.criarCorpo("modelo-teste", "Explique com pizza.", contexto);

        assertTrue(corpo.contains("\"role\":\"assistant\",\"content\":\""
            + "Uma fração representa partes de um todo.\""));
        assertTrue(corpo.contains("\"role\":\"user\",\"content\":\"Ana: Explique frações.\""));
        assertTrue(corpo.contains("\"role\":\"user\",\"content\":\"Bia: Pode dar outro exemplo?\""));
        assertTrue(corpo.contains("\"role\":\"user\",\"content\":\"Explique com pizza.\""));
        assertFalse(corpo.contains("O Flamengo venceu a partida."));
        assertTrue(corpo.contains("Assistente geral em português."));
    }

    @Test
    void limitesAmpliadosPodemSerConfiguradosSemAlterarOsPadroes() {
        BolaHuggingFaceSkill assistente = new BolaHuggingFaceSkill(
            "Assistente IA", "Ajude em vários assuntos.", "Sem token.",
            "token", "modelo", BolaHuggingFaceSkill.ENDPOINT_GROQ, "Groq",
            800, 2500);
        String corpo = assistente.criarCorpo(
            "modelo", "Explique fotossíntese.", new BolaContext(List.of(), List.of()));

        assertTrue(corpo.contains("\"max_tokens\":800"));

        BolaHuggingFaceSkill bola = new BolaHuggingFaceSkill(
            "Assistente IA", "Ajude em vários assuntos.", "Sem token.");
        String corpoPadrao = bola.criarCorpo(
            "modelo", "Pergunta", new BolaContext(List.of(), List.of()));
        assertTrue(corpoPadrao.contains("\"max_tokens\":220"));
    }

    @Test
    void groqComGptOssPedeRaciocinioBaixoParaNaoGastarOsTokens() {
        BolaHuggingFaceSkill groq = new BolaHuggingFaceSkill(
            "Assistente IA", "Ajude.", "Sem token.", "token", "openai/gpt-oss-20b",
            BolaHuggingFaceSkill.ENDPOINT_GROQ, "Groq", 2000, 2500);
        String corpo = groq.criarCorpo(
            "openai/gpt-oss-20b", "Boa tarde", new BolaContext(List.of(), List.of()));
        assertTrue(corpo.contains("\"reasoning_effort\":\"low\""));
        assertTrue(corpo.endsWith("}"));

        String outro = groq.criarCorpo(
            "llama-qualquer", "Boa tarde", new BolaContext(List.of(), List.of()));
        assertTrue(!outro.contains("reasoning_effort"));
    }

    @Test
    void bolaPreservaSeuContextoEIsolaAsRespostasDaAssistenteIa() {
        BolaHuggingFaceSkill bola = new BolaHuggingFaceSkill();
        BolaContext contexto = new BolaContext(List.of(
            Mensagem.nova("Ana", "Qual é a regra do impedimento?"),
            Mensagem.nova("Bola", "O impedimento é avaliado no momento do passe."),
            Mensagem.nova("Assistente IA", "Posso explicar cálculo também."),
            Mensagem.nova("Bia", "E qual a exceção?")
        ), List.of("Ana", "Bia"));

        String corpo = bola.criarCorpo("modelo-teste", "Explique a exceção.", contexto);

        assertTrue(corpo.contains("\"role\":\"assistant\",\"content\":\""
            + "O impedimento é avaliado no momento do passe.\""));
        assertTrue(corpo.contains("\"role\":\"user\",\"content\":\"Bia: E qual a exceção?\""));
        assertFalse(corpo.contains("Posso explicar cálculo também."));
        assertTrue(corpo.contains("exclusivamente em futebol"));
        assertTrue(corpo.contains("Não invente resultados"));
    }
}
