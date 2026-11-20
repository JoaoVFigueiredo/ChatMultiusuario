package server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

record ConfiguracaoAssistentes(
        String groqApiKey,
        String groqModel,
        String assistenteIaToken,
        String assistenteIaModelo,
        String bolaToken,
        String bolaModelo) {
    static final String MODELO_GROQ_PADRAO = "openai/gpt-oss-20b";
    static final String MODELO_IA_PADRAO =
        "meta-llama/Llama-3.1-8B-Instruct:featherless-ai";
    static final String MODELO_BOLA_PADRAO =
        "Qwen/Qwen2.5-1.5B-Instruct:featherless-ai";

    static ConfiguracaoAssistentes carregar(Path arquivo) throws IOException {
        return ler(arquivo, System.getenv());
    }

    static Path localizarArquivoEnv(Path diretorioInicial) {
        Path diretorio = diretorioInicial.toAbsolutePath().normalize();
        while (diretorio != null) {
            Path arquivo = diretorio.resolve(".env");
            if (Files.isRegularFile(arquivo)) return arquivo;
            diretorio = diretorio.getParent();
        }
        return diretorioInicial.toAbsolutePath().normalize().resolve(".env");
    }

    static ConfiguracaoAssistentes ler(Path arquivo, Map<String, String> ambiente)
            throws IOException {
        Map<String, String> arquivoEnv = new HashMap<>();
        if (Files.isRegularFile(arquivo)) {
            for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
                String conteudo = linha.trim();
                if (conteudo.isEmpty() || conteudo.startsWith("#")) continue;
                if (conteudo.startsWith("export ")) conteudo = conteudo.substring(7).trim();
                int separador = conteudo.indexOf('=');
                if (separador <= 0) continue;
                String chave = conteudo.substring(0, separador).trim();
                if (!chave.matches("[A-Z][A-Z0-9_]*")) continue;
                arquivoEnv.put(chave, removerAspas(conteudo.substring(separador + 1).trim()));
            }
        }

        return new ConfiguracaoAssistentes(
            valor("GROQ_API_KEY", "", arquivoEnv, ambiente),
            valor("GROQ_MODEL", MODELO_GROQ_PADRAO, arquivoEnv, ambiente),
            valor("ASSISTENTE_IA_TOKEN", "", arquivoEnv, ambiente),
            valor("ASSISTENTE_IA_MODEL", MODELO_IA_PADRAO, arquivoEnv, ambiente),
            valor("BOLA_TOKEN", "", arquivoEnv, ambiente),
            valor("BOLA_MODEL", MODELO_BOLA_PADRAO, arquivoEnv, ambiente));
    }

    private static String valor(String chave, String padrao, Map<String, String> arquivo,
                                Map<String, String> ambiente) {
        String valorAmbiente = ambiente.get(chave);
        if (valorAmbiente != null && !valorAmbiente.isBlank()) return valorAmbiente.trim();
        String valorArquivo = arquivo.get(chave);
        return valorArquivo == null || valorArquivo.isBlank() ? padrao : valorArquivo.trim();
    }

    private static String removerAspas(String valor) {
        if (valor.length() >= 2) {
            char primeira = valor.charAt(0);
            char ultima = valor.charAt(valor.length() - 1);
            if ((primeira == '"' && ultima == '"') || (primeira == '\'' && ultima == '\'')) {
                return valor.substring(1, valor.length() - 1);
            }
        }
        return valor;
    }
}
