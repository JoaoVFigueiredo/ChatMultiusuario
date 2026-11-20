package server;

import model.Mensagem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

final class BolaComandosSkill implements BolaSkill {
    private static final int LIMITE_RESPOSTA = 480;

    @Override
    public boolean atende(String consulta) {
        String normalizada = consulta.toLowerCase(Locale.ROOT).trim();
        return normalizada.equals("resumo")
            || normalizada.equals("clima")
            || normalizada.startsWith("clima ")
            || normalizada.equals("sortear")
            || normalizada.startsWith("palpite ")
            || normalizada.isEmpty();
    }

    @Override
    public List<String> executar(String consulta, BolaContext contexto) {
        String normalizada = consulta.toLowerCase(Locale.ROOT).trim();
        if (normalizada.equals("resumo")) {
            return List.of(resumir(contexto.historico()));
        }
        if (normalizada.equals("sortear")) {
            return List.of(sortear(contexto.usuariosOnline()));
        }
        if (normalizada.equals("clima") || normalizada.startsWith("clima ")) {
            return List.of("Ainda não consulto previsão do tempo em tempo real. "
                + "Para notícias, escreva, por exemplo: @bola notícias do Flamengo.");
        }
        if (normalizada.isEmpty()) {
            return List.of("Oi! Posso buscar notícias de futebol, resumir o canal, sortear times "
                + "ou dar um palpite. Experimente: @bola notícias do Flamengo.");
        }
        return List.of(palpite(consulta));
    }

    private static String resumir(List<Mensagem> historico) {
        if (historico.isEmpty()) {
            return "Ainda não há mensagens públicas para resumir neste canal.";
        }
        StringBuilder resumo = new StringBuilder("Resumo das últimas mensagens: ");
        int inicio = Math.max(0, historico.size() - 5);
        int quantidade = 0;
        for (int i = inicio; i < historico.size(); i++) {
            Mensagem mensagem = historico.get(i);
            if (mensagem.getRemetente().equalsIgnoreCase("Bola")) continue;
            if (quantidade++ > 0) resumo.append(" | ");
            resumo.append(mensagem.getRemetente()).append(": ").append(mensagem.getConteudo());
            if (resumo.length() >= LIMITE_RESPOSTA) {
                resumo.setLength(LIMITE_RESPOSTA - 3);
                resumo.append("...");
                break;
            }
        }
        return quantidade == 0
            ? "Não encontrei mensagens recentes de usuários para resumir."
            : resumo.toString();
    }

    private static String sortear(List<String> usuariosOnline) {
        List<String> nomes = new ArrayList<>(usuariosOnline);
        Collections.shuffle(nomes);
        StringBuilder time1 = new StringBuilder();
        StringBuilder time2 = new StringBuilder();
        for (int i = 0; i < nomes.size(); i++) {
            StringBuilder time = i % 2 == 0 ? time1 : time2;
            if (time.length() > 0) time.append(", ");
            time.append(nomes.get(i));
        }
        return "Times sorteados: Time 1: " + time1 + " | Time 2: " + time2;
    }

    private static String palpite(String consulta) {
        String confronto = consulta.substring("palpite".length()).trim();
        int separador = confronto.toLowerCase(Locale.ROOT).indexOf(" x ");
        if (separador < 1 || separador + 3 >= confronto.length()) {
            return "Use: @bola palpite time A x time B. É apenas um palpite, não uma previsão garantida.";
        }
        String timeA = confronto.substring(0, separador).trim();
        String timeB = confronto.substring(separador + 3).trim();
        String escolhido = ThreadLocalRandom.current().nextBoolean() ? timeA : timeB;
        return "Palpite aleatório: " + escolhido
            + " pode levar vantagem, mas não tenho estatísticas ao vivo e o resultado é imprevisível.";
    }
}
