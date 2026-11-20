package model;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Formato das linhas trocadas entre cliente e servidor (uma linha = um comando).
 *
 * Cliente -> Servidor: ENTRAR|nome|#cor|sala|carregarHistorico   TXT|texto   COR|#cor   AVATAR|dados   SAIR
 * Servidor -> Cliente: MSG|#cor|texto   SYS|aviso   ONLINE|quantidade   USERS|nomes   AVATAR|dados
 */
public final class Protocolo {
    public static final String ENTRAR = "ENTRAR";
    public static final String TXT = "TXT";
    public static final String COR = "COR";
    public static final String SAIR = "SAIR";
    public static final String MSG = "MSG";
    public static final String SYS = "SYS";
    public static final String ONLINE = "ONLINE";
    public static final String USERS = "USERS";
    public static final String DIGITANDO = "DIGITANDO";
    public static final String ANEXO = "ANEXO";
    public static final String AVATAR = "AVATAR";
    public static final String AVATARES_SINCRONIZADOS = "AVATARES_SINCRONIZADOS";
    public static final String REMOVER_AVATAR = "REMOVER_AVATAR";
    public static final String AVATAR_REMOVIDO = "AVATAR_REMOVIDO";
    public static final String IA_PERGUNTA = "IA_PERGUNTA";
    public static final String IA_RESULTADO = "IA_RESULTADO";
    public static final int TAMANHO_MAXIMO_PERGUNTA_IA = 496;
    public static final int TAMANHO_MAXIMO_AVATAR = 256 * 1024;
    public static final String SALA_PADRAO = "geral";
    public static final int TAMANHO_MAXIMO_ANEXO = 10 * 1024 * 1024;
    private static final int TAMANHO_MAXIMO_NOME_ANEXO = 120;
    private static final Pattern TIPO_MIME =
        Pattern.compile("[a-zA-Z0-9.+-]+/[a-zA-Z0-9.+-]+");

    /** Linha recebida pelo cliente. Para SYS, cor é null. */
    public record Recebida(String tipo, String cor, String texto, List<String> usuarios, Anexo anexo) {
        public Recebida(String tipo, String cor, String texto) {
            this(tipo, cor, texto, List.of(), null);
        }

        public Recebida {
            usuarios = List.copyOf(usuarios);
        }
    }

    public record Anexo(String nome, String mime, byte[] conteudo) {
        public Anexo {
            nome = validarNomeAnexo(nome);
            if (mime == null || !TIPO_MIME.matcher(mime).matches()) {
                throw new IllegalArgumentException("Tipo de anexo inválido.");
            }
            if (conteudo == null || conteudo.length == 0
                    || conteudo.length > TAMANHO_MAXIMO_ANEXO) {
                throw new IllegalArgumentException("O anexo deve ter entre 1 byte e 10 MB.");
            }
            conteudo = conteudo.clone();
        }

        @Override
        public byte[] conteudo() {
            return conteudo.clone();
        }
    }

    /** Comando recebido pelo servidor. */
    public record Comando(String tipo, String valor) { }

    /** Dados do login, incluindo o canal escolhido. */
    public record Entrada(Usuario usuario, String sala, boolean carregarHistorico) { }

    private Protocolo() { }

    // ----- Cliente -> Servidor -----
    public static String entrar(Usuario u) {
        return entrar(u, SALA_PADRAO);
    }

    public static String entrar(Usuario u, String sala) {
        return ENTRAR + "|" + u.getNome() + "|" + u.getCor() + "|" + normalizarSala(sala);
    }

    public static String entrar(Usuario u, String sala, boolean carregarHistorico) {
        String login = entrar(u, sala);
        return carregarHistorico ? login : login + "|false";
    }

    public static Usuario lerEntrar(String linha) {
        return lerEntrada(linha).usuario();
    }

    public static Entrada lerEntrada(String linha) {
        if (linha == null) {
            throw new IllegalArgumentException("Conexão fechada antes do login.");
        }
        String[] p = linha.split("\\|", 5);
        if ((p.length < 3 || p.length > 5) || !p[0].equals(ENTRAR)) {
            throw new IllegalArgumentException("Protocolo inválido no login.");
        }
        Usuario usuario = new Usuario(p[1], p[2]);
        String sala = p.length >= 4 ? p[3] : SALA_PADRAO;
        boolean carregarHistorico = true;
        if (p.length == 5) {
            if (!p[4].equals("true") && !p[4].equals("false")) {
                throw new IllegalArgumentException("Opção de histórico inválida no login.");
            }
            carregarHistorico = Boolean.parseBoolean(p[4]);
        }
        return new Entrada(usuario, normalizarSala(sala), carregarHistorico);
    }

    public static String normalizarSala(String sala) {
        String normalizada = sala == null ? "" : sala.trim().toLowerCase(java.util.Locale.ROOT);
        if (!normalizada.matches("[a-z0-9_-]{1,24}")) {
            throw new IllegalArgumentException("A sala deve ter de 1 a 24 caracteres (letras, números, _ ou -).");
        }
        return normalizada;
    }

    public static String texto(String conteudo) {
        return TXT + "|" + conteudo;
    }

    public static String perguntaAssistenteIa(String pergunta) {
        if (pergunta == null || pergunta.isBlank()
                || pergunta.length() > TAMANHO_MAXIMO_PERGUNTA_IA) {
            throw new IllegalArgumentException("A pergunta deve ter entre 1 e "
                + TAMANHO_MAXIMO_PERGUNTA_IA + " caracteres.");
        }
        return IA_PERGUNTA + "|" + Base64.getUrlEncoder().withoutPadding().encodeToString(
            pergunta.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static String lerPerguntaAssistenteIa(String linha) {
        String[] partes = linha.split("\\|", 2);
        if (partes.length != 2 || !partes[0].equals(IA_PERGUNTA)) {
            throw new IllegalArgumentException("Pergunta da Assistente IA inválida.");
        }
        try {
            String pergunta = new String(Base64.getUrlDecoder().decode(partes[1]),
                java.nio.charset.StandardCharsets.UTF_8);
            if (pergunta.isBlank() || pergunta.length() > TAMANHO_MAXIMO_PERGUNTA_IA) {
                throw new IllegalArgumentException("Pergunta da Assistente IA inválida.");
            }
            return pergunta;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Pergunta da Assistente IA inválida.", e);
        }
    }

    public static String resultadoAssistenteIa(String pergunta, String resposta) {
        if (pergunta == null || pergunta.isBlank() || resposta == null || resposta.isBlank()) {
            throw new IllegalArgumentException("Pergunta e resposta são obrigatórias.");
        }
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return IA_RESULTADO + "|"
            + encoder.encodeToString(pergunta.getBytes(java.nio.charset.StandardCharsets.UTF_8))
            + "|"
            + encoder.encodeToString(resposta.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static HistoricoAssistente lerResultadoAssistenteIa(String payload) {
        String[] partes = payload.split("\\|", 2);
        if (partes.length != 2) {
            throw new IllegalArgumentException("Resultado da Assistente IA inválido.");
        }
        try {
            Base64.Decoder decoder = Base64.getUrlDecoder();
            String pergunta = new String(decoder.decode(partes[0]),
                java.nio.charset.StandardCharsets.UTF_8);
            String resposta = new String(decoder.decode(partes[1]),
                java.nio.charset.StandardCharsets.UTF_8);
            if (pergunta.isBlank() || resposta.isBlank()) {
                throw new IllegalArgumentException("Resultado da Assistente IA inválido.");
            }
            return new HistoricoAssistente(pergunta, resposta);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Resultado da Assistente IA inválido.", e);
        }
    }

    public record HistoricoAssistente(String pergunta, String resposta) { }

    public static String anexo(String nome, String mime, byte[] conteudo) {
        Anexo anexo = new Anexo(nome, mime, conteudo);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return ANEXO + "|" + encoder.encodeToString(
            anexo.nome().getBytes(java.nio.charset.StandardCharsets.UTF_8))
            + "|" + encoder.encodeToString(
            anexo.mime().getBytes(java.nio.charset.StandardCharsets.UTF_8))
            + "|" + encoder.encodeToString(anexo.conteudo());
    }

    public static String avatar(String nome, byte[] conteudo) {
        if (nome == null || nome.isBlank() || nome.length() > Usuario.TAMANHO_MAXIMO_NOME
                || nome.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Nome de usuário inválido para a foto.");
        }
        if (conteudo == null || conteudo.length == 0 || conteudo.length > TAMANHO_MAXIMO_AVATAR) {
            throw new IllegalArgumentException("A foto de perfil deve ter até 256 KB.");
        }
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return AVATAR + "|" + encoder.encodeToString(
            nome.getBytes(java.nio.charset.StandardCharsets.UTF_8))
            + "|" + encoder.encodeToString(conteudo);
    }

    public static String avatarRemovido(String nome) {
        if (nome == null || nome.isBlank() || nome.length() > Usuario.TAMANHO_MAXIMO_NOME
                || nome.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Nome de usuário inválido para remover a foto.");
        }
        return AVATAR_REMOVIDO + "|" + Base64.getUrlEncoder().withoutPadding().encodeToString(
            nome.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static byte[] lerAvatar(String linha) {
        if (linha == null || !linha.startsWith(AVATAR + "|")) {
            throw new IllegalArgumentException("Foto de perfil inválida.");
        }
        String[] partes = linha.split("\\|", 3);
        if (partes.length != 3) throw new IllegalArgumentException("Foto de perfil inválida.");
        try {
            Base64.Decoder decoder = Base64.getUrlDecoder();
            String nome = new String(decoder.decode(partes[1]),
                java.nio.charset.StandardCharsets.UTF_8);
            byte[] imagem = decoder.decode(partes[2]);
            avatar(nome, imagem);
            return imagem;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Foto de perfil inválida ou maior que 256 KB.", e);
        }
    }

    private static String validarNomeAnexo(String nome) {
        if (nome == null || nome.isBlank() || nome.length() > TAMANHO_MAXIMO_NOME_ANEXO
                || nome.equals(".") || nome.equals("..")
                || nome.contains("/") || nome.contains("\\")
                || nome.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Nome do anexo inválido.");
        }
        return nome;
    }

    public static String respostaPrivada(String destinatario, String texto) {
        return "[" + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            + "] [privado para " + destinatario + "]: " + texto;
    }

    public static String recebidaPrivada(String remetente, String texto) {
        return "[" + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            + "] [privado de " + remetente + "]: " + texto;
    }

    public static String comandoCor(String cor) {
        return COR + "|" + cor;
    }

    public static String comandoDigitando(boolean digitando) {
        return DIGITANDO + "|" + digitando;
    }

    public static Comando lerComando(String linha) {
        String[] p = linha.split("\\|", 2);
        return new Comando(p[0], p.length > 1 ? p[1] : "");
    }

    // ----- Servidor -> Cliente -----
    public static String msg(Mensagem m) {
        String texto = m.formatar();
        if (texto.indexOf('\n') < 0 && texto.indexOf('\r') < 0) {
            return MSG + "|" + m.getCor() + "|" + texto;
        }
        return "MSG64|" + m.getCor() + "|"
            + Base64.getUrlEncoder().withoutPadding().encodeToString(
                texto.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static String sys(String aviso) {
        return SYS + "|" + aviso;
    }

    public static String online(int quantidade) {
        return ONLINE + "|" + quantidade;
    }

    public static String usuariosOnline(List<String> nomes) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        List<String> codificados = new ArrayList<>();
        for (String nome : nomes) {
            codificados.add(encoder.encodeToString(
                nome.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }
        return USERS + "|" + String.join(",", codificados);
    }

    public static String digitando(String nome, boolean digitando) {
        return DIGITANDO + "|" + nome + "|" + digitando;
    }

    public static Recebida ler(String linha) {
        if (linha.startsWith("MSG64|")) {
            String[] partes = linha.split("\\|", 3);
            if (partes.length != 3) {
                throw new IllegalArgumentException("Mensagem multilinha inválida.");
            }
            try {
                String texto = new String(Base64.getUrlDecoder().decode(partes[2]),
                    java.nio.charset.StandardCharsets.UTF_8);
                return new Recebida(MSG, partes[1], texto);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Mensagem multilinha inválida.", e);
            }
        }
        if (linha.startsWith(IA_RESULTADO + "|")) {
            return new Recebida(IA_RESULTADO, null, linha.substring(IA_RESULTADO.length() + 1));
        }
        if (linha.equals(AVATARES_SINCRONIZADOS)) {
            return new Recebida(AVATARES_SINCRONIZADOS, null, "");
        }
        if (linha.startsWith(AVATAR_REMOVIDO + "|")) {
            try {
                String nome = new String(Base64.getUrlDecoder().decode(
                    linha.substring(AVATAR_REMOVIDO.length() + 1)),
                    java.nio.charset.StandardCharsets.UTF_8);
                avatarRemovido(nome);
                return new Recebida(AVATAR_REMOVIDO, null, nome);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Evento de remoção de avatar inválido.", e);
            }
        }
        if (linha.startsWith(AVATAR + "|")) {
            String[] partes = linha.split("\\|", 3);
            if (partes.length != 3) throw new IllegalArgumentException("Foto de perfil inválida.");
            try {
                Base64.Decoder decoder = Base64.getUrlDecoder();
                String nome = new String(decoder.decode(partes[1]),
                    java.nio.charset.StandardCharsets.UTF_8);
                byte[] imagem = decoder.decode(partes[2]);
                avatar(nome, imagem);
                return new Recebida(AVATAR,
                    Base64.getUrlEncoder().withoutPadding().encodeToString(imagem), nome);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Foto de perfil inválida ou maior que 256 KB.", e);
            }
        }
        if (linha.startsWith(ANEXO + "|")) {
            String[] partes = linha.split("\\|", 4);
            if (partes.length != 4) {
                throw new IllegalArgumentException("Anexo recebido em formato inválido.");
            }
            try {
                Base64.Decoder decoder = Base64.getUrlDecoder();
                String nome = new String(decoder.decode(partes[1]),
                    java.nio.charset.StandardCharsets.UTF_8);
                String mime = new String(decoder.decode(partes[2]),
                    java.nio.charset.StandardCharsets.UTF_8);
                byte[] conteudo = decoder.decode(partes[3]);
                Anexo anexo = new Anexo(nome, mime, conteudo);
                return new Recebida(ANEXO, null, anexo.nome(), List.of(), anexo);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Anexo recebido inválido ou maior que 10 MB.", e);
            }
        }
        String[] p = linha.split("\\|", 3);
        if (p.length == 3 && p[0].equals(MSG)) {
            return new Recebida(MSG, p[1], p[2]);
        }
        if (p.length == 2 && p[0].equals(ONLINE)) {
            return new Recebida(ONLINE, null, p[1]);
        }
        if (p.length == 2 && p[0].equals(USERS)) {
            Base64.Decoder decoder = Base64.getUrlDecoder();
            List<String> nomes = new ArrayList<>();
            if (!p[1].isEmpty()) {
                for (String nome : p[1].split(",", -1)) {
                    nomes.add(new String(decoder.decode(nome),
                        java.nio.charset.StandardCharsets.UTF_8));
                }
            }
            return new Recebida(USERS, null, "", nomes, null);
        }
        if (p.length == 3 && p[0].equals(DIGITANDO)) {
            return new Recebida(DIGITANDO, null, p[1] + "|" + p[2]);
        }
        if (linha.startsWith(SYS + "|")) {
            return new Recebida(SYS, null, linha.substring(SYS.length() + 1));
        }
        return new Recebida(SYS, null, linha);
    }
}
