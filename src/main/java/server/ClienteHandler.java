package server;

import model.Emojis;
import model.Mensagem;
import model.Protocolo;
import model.Usuario;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** Cuida de uma única conexão de cliente (roda em sua própria thread). */
public class ClienteHandler implements Runnable {
    private static final int TAMANHO_MAXIMO_TEXTO = 500;
    private static final int TAMANHO_MAXIMO_LINHA_ANEXO =
        ((Protocolo.TAMANHO_MAXIMO_ANEXO + 2) / 3) * 4 + 2048;

    private final BolaAssistant bola;
    private final ConfiguracaoAssistentes configuracaoAssistentes;
    private final Socket socket;
    private final Servidor servidor;
    private PrintWriter saida;
    private Usuario usuario;
    private String sala;
    private byte[] avatar;

    public ClienteHandler(Socket socket, Servidor servidor) {
        this.socket = socket;
        this.servidor = servidor;
        this.configuracaoAssistentes = servidor.configuracaoAssistentes();
        this.bola = new BolaAssistant(
            configuracaoAssistentes.groqApiKey(), configuracaoAssistentes.groqModel(),
            configuracaoAssistentes.bolaToken(), configuracaoAssistentes.bolaModelo());
    }

    @Override
    public void run() {
        try (BufferedReader entrada = new BufferedReader(
                 new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            saida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

            // 1ª linha enviada pelo cliente: ENTRAR|nome|#cor|sala[|carregarHistorico]
            Protocolo.Entrada login = Protocolo.lerEntrada(entrada.readLine());
            usuario = login.usuario();
            sala = login.sala();
            if (usuario.getNome().equalsIgnoreCase("Bola")) {
                enviar(Protocolo.sys("O nome Bola é reservado para o assistente de futebol."));
                usuario = null;
                sala = null;
                return;
            }

            // Histórico recente antes de entrar no broadcast
            if (login.carregarHistorico()) {
                enviar(Protocolo.sys("--- últimas mensagens ---"));
                for (Mensagem m : servidor.historico(sala)) {
                    enviar(Protocolo.msg(m));
                }
                enviar(Protocolo.sys("--- fim do histórico ---"));
            }

            servidor.adicionar(this);
            servidor.avisar(sala, usuario + " entrou no chat", this);
            servidor.atualizarOnline(sala);
            servidor.enviarAvatares(this);

            String linha;
            while ((linha = entrada.readLine()) != null) {
                Protocolo.Comando cmd = Protocolo.lerComando(linha);
                if (cmd.tipo().equals(Protocolo.SAIR)) {
                    break;
                } else if (cmd.tipo().equals(Protocolo.COR)) {
                    if (Usuario.corValida(cmd.valor())) {
                        usuario = usuario.comCor(cmd.valor());
                    }
                } else if (cmd.tipo().equals(Protocolo.DIGITANDO)) {
                    if (cmd.valor().equals("true") || cmd.valor().equals("false")) {
                        servidor.avisarDigitando(sala, usuario.getNome(), Boolean.parseBoolean(cmd.valor()), this);
                    }
                } else if (linha.startsWith(Protocolo.ANEXO + "|")) {
                    processarAnexo(linha);
                } else if (linha.startsWith(Protocolo.AVATAR + "|")) {
                    processarAvatar(linha);
                } else if (cmd.tipo().equals(Protocolo.REMOVER_AVATAR) && cmd.valor().isBlank()) {
                    servidor.removerAvatar(this);
                } else if (cmd.tipo().equals(Protocolo.IA_PERGUNTA)) {
                    String pergunta = Protocolo.lerPerguntaAssistenteIa(linha);
                    responderIa(pergunta, servidor.historico(sala));
                } else if (cmd.tipo().equals(Protocolo.TXT) && !cmd.valor().isBlank()) {
                    String texto = Emojis.converter(cmd.valor());
                    if (texto.length() > TAMANHO_MAXIMO_TEXTO) {
                        texto = texto.substring(0, TAMANHO_MAXIMO_TEXTO);
                    }
                    if (processarComando(texto)) {
                        continue;
                    }
                    boolean chamadaBola = texto.regionMatches(true, 0, "@bola", 0, 5)
                        && (texto.length() == 5 || Character.isWhitespace(texto.charAt(5)));
                    List<Mensagem> contextoBola = chamadaBola ? servidor.historico(sala) : List.of();
                    servidor.broadcast(sala, Mensagem.nova(usuario, texto));
                    if (chamadaBola) {
                        responderBola(texto.substring(5).trim(), contextoBola);
                    }
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Conexão encerrada: " + e.getMessage());
        } finally {
            servidor.remover(this);
            if (usuario != null) {
                servidor.avisar(sala, usuario + " saiu do chat", this);
            }
            try { socket.close(); } catch (IOException ignored) { }
        }
    }

    /** Pode ser chamado por outras threads (broadcast), por isso é synchronized. */
    public synchronized void enviar(String linha) {
        if (saida != null) {
            saida.println(linha);
        }
    }

    public String getSala() { return sala; }
    public Usuario getUsuario() { return usuario; }
    public synchronized byte[] getAvatar() {
        return avatar == null ? null : avatar.clone();
    }

    public synchronized void setAvatar(byte[] imagem) {
        avatar = imagem == null ? null : imagem.clone();
    }

    private void processarAvatar(String linha) {
        int tamanhoMaximoLinha = ((Protocolo.TAMANHO_MAXIMO_AVATAR + 2) / 3) * 4 + 256;
        if (linha.length() > tamanhoMaximoLinha) {
            enviar(Protocolo.sys("A foto de perfil excede o limite de 256 KB."));
            return;
        }
        try {
            Protocolo.Recebida recebida = Protocolo.ler(linha);
            if (recebida.tipo().equals(Protocolo.AVATAR)
                    && recebida.texto().equals(usuario.getNome())) {
                byte[] imagem = java.util.Base64.getUrlDecoder().decode(recebida.cor());
                validarAvatar(imagem);
                servidor.atualizarAvatar(this, imagem);
            }
        } catch (IOException | IllegalArgumentException e) {
            enviar(Protocolo.sys("Não foi possível atualizar a foto de perfil: " + e.getMessage()));
        }
    }

    private static void validarAvatar(byte[] bytes) throws IOException {
        try (ImageInputStream entrada = ImageIO.createImageInputStream(
                new java.io.ByteArrayInputStream(bytes))) {
            if (entrada == null) throw new IOException("O arquivo não é uma imagem compatível.");
            java.util.Iterator<ImageReader> leitores = ImageIO.getImageReaders(entrada);
            if (!leitores.hasNext()) throw new IOException("O formato da imagem não é compatível.");
            ImageReader leitor = leitores.next();
            try {
                leitor.setInput(entrada, true, true);
                int largura = leitor.getWidth(0);
                int altura = leitor.getHeight(0);
                if (largura < 1 || altura < 1 || largura > 512 || altura > 512) {
                    throw new IOException("A imagem precisa ter dimensões de até 512 × 512 pixels.");
                }
                if (leitor.read(0) == null) throw new IOException("Não foi possível decodificar a imagem.");
            } finally {
                leitor.dispose();
            }
        }
    }

    private void processarAnexo(String linha) {
        if (linha.length() > TAMANHO_MAXIMO_LINHA_ANEXO) {
            enviar(Protocolo.sys("O anexo excede o limite de 10 MB."));
            return;
        }
        try {
            Protocolo.Recebida recebida = Protocolo.ler(linha);
            Protocolo.Anexo anexo = recebida.anexo();
            if (anexo == null) {
                throw new IllegalArgumentException("Anexo inválido.");
            }
            servidor.retransmitirAnexo(sala, linha, this, anexo.nome(), anexo.conteudo().length);
        } catch (IllegalArgumentException e) {
            enviar(Protocolo.sys("Não foi possível enviar o anexo: " + e.getMessage()));
        }
    }

    private boolean processarComando(String texto) {
        if (texto.equalsIgnoreCase("/ajuda")) {
            enviar(Protocolo.sys("Comandos: /p nome mensagem | /buscar palavra | /quem | /ajuda | @bola pergunta"));
            enviar(Protocolo.sys("Bola: @bola notícias do time | classificação série A/B | resumo | clima | palpite time A x time B | sortear"));
            enviar(Protocolo.sys("Para conversar com a Assistente IA, abra a janela Assistente IA no menu."));
            return true;
        }
        if (texto.equalsIgnoreCase("/quem")) {
            List<String> nomes = servidor.nomesNaSala(sala);
            enviar(Protocolo.sys("Pessoas neste canal (" + nomes.size() + "): " + String.join(", ", nomes)));
            return true;
        }
        if (texto.regionMatches(true, 0, "/buscar", 0, 7)
                && (texto.length() == 7 || Character.isWhitespace(texto.charAt(7)))) {
            String termo = texto.substring(7).trim();
            if (termo.isEmpty()) {
                enviar(Protocolo.sys("Uso: /buscar palavra ou frase"));
                return true;
            }
            List<Mensagem> resultados = servidor.buscar(sala, termo);
            enviar(Protocolo.sys("Busca por \"" + termo + "\": " + resultados.size() + " resultado(s)."));
            for (Mensagem mensagem : resultados) {
                enviar(Protocolo.sys(mensagem.formatar()));
            }
            return true;
        }
        if (texto.regionMatches(true, 0, "/p ", 0, 3)) {
            String restante = texto.substring(3).trim();
            int separador = restante.indexOf(' ');
            if (separador <= 0 || restante.substring(separador + 1).isBlank()) {
                enviar(Protocolo.sys("Uso: /p nome mensagem"));
                return true;
            }
            servidor.enviarPrivada(this, restante.substring(0, separador),
                restante.substring(separador + 1).trim());
            return true;
        }
        return false;
    }

    private void responderBola(String pedido, List<Mensagem> contexto) {
        BolaContext bolaContexto = new BolaContext(contexto, servidor.nomesNaSala(sala));
        try {
            for (String resposta : bola.responder(pedido, bolaContexto)) {
                servidor.broadcast(sala, Mensagem.nova("Bola", resposta));
            }
        } catch (IOException e) {
            servidor.broadcast(sala, Mensagem.nova("Bola",
                "Não consegui consultar o Bola agora: " + e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            servidor.broadcast(sala, Mensagem.nova("Bola", "A consulta do Bola foi interrompida."));
        }
    }

    private void responderIa(String pedido, List<Mensagem> contexto) {
        if (pedido.isBlank()) {
            enviarResultadoIa(
                "Escreva sua pergunta na janela Assistente IA.",
                pedido);
            return;
        }
        BolaContext iaContexto = new BolaContext(contexto, servidor.nomesNaSala(sala));
        HuggingFaceService ia = new HuggingFaceService(
            configuracaoAssistentes.groqApiKey(),
            configuracaoAssistentes.groqModel(),
            configuracaoAssistentes.assistenteIaToken(),
            configuracaoAssistentes.assistenteIaModelo());
        try {
            String resposta = ia.enviarMensagem(pedido, iaContexto);
            enviarResultadoIa(resposta, pedido);
        } catch (IOException e) {
            String detalhe = e.getMessage();
            enviarResultadoIa("Não consegui responder agora."
                + (detalhe == null || detalhe.isBlank() ? "" : " " + detalhe), pedido);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            enviarResultadoIa("A consulta foi interrompida.", pedido);
        }
    }

    private void enviarResultadoIa(String resposta, String pedido) {
        enviar(Protocolo.resultadoAssistenteIa(pedido, resposta));
    }
}
