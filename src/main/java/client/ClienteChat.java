package client;

import model.CodigoSala;
import model.Protocolo;
import model.Usuario;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.function.Consumer;

/**
 * Parte de REDE do cliente: conecta, envia e recebe. Não sabe nada de janelas.
 * A janela (ClienteGUI) usa esta classe; o main abaixo é uma versão só de terminal.
 */
public class ClienteChat {
    private final String host;
    private final int porta;
    private final String sala;
    private Usuario usuario;
    private Socket socket;
    private PrintWriter saida;

    public ClienteChat(String host, int porta, Usuario usuario) {
        this(host, porta, usuario, Protocolo.SALA_PADRAO);
    }

    public ClienteChat(String host, int porta, Usuario usuario, String sala) {
        this.host = host;
        this.porta = porta;
        this.usuario = usuario;
        this.sala = Protocolo.normalizarSala(sala);
    }

    /** Conecta e começa a escutar em uma thread separada. */
    public void conectar(Consumer<Protocolo.Recebida> aoReceber, Runnable aoDesconectar)
            throws IOException {
        conectar(aoReceber, aoDesconectar, true);
    }

    /** Conecta e escolhe se o servidor deve enviar o histórico da sala. */
    public void conectar(Consumer<Protocolo.Recebida> aoReceber, Runnable aoDesconectar,
                         boolean carregarHistorico) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, porta), 5000);
        saida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
        BufferedReader entrada = new BufferedReader(
            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

        saida.println(Protocolo.entrar(usuario, sala, carregarHistorico));

        Thread receptor = new Thread(() -> {
            try {
                String linha;
                while ((linha = entrada.readLine()) != null) {
                    aoReceber.accept(Protocolo.ler(linha));
                }
            } catch (IOException ignored) { }
            aoDesconectar.run();
        });
        receptor.setDaemon(true);
        receptor.start();
    }

    public void enviar(String texto) {
        saida.println(Protocolo.texto(texto));
    }

    public void enviarPerguntaAssistente(String pergunta) throws IOException {
        String linha = Protocolo.perguntaAssistenteIa(pergunta);
        synchronized (this) {
            if (saida == null || socket == null || socket.isClosed()) {
                throw new IOException("O chat está desconectado.");
            }
            saida.println(linha);
            if (saida.checkError()) {
                throw new IOException("Falha ao enviar a pergunta para a Assistente IA.");
            }
        }
    }

    public void enviarAvatar(byte[] imagem) throws IOException {
        synchronized (this) {
            if (saida == null || socket == null || socket.isClosed()) {
                throw new IOException("O chat está desconectado.");
            }
            saida.println(Protocolo.avatar(usuario.getNome(), imagem));
            if (saida.checkError()) {
                throw new IOException("Falha ao enviar a foto de perfil ao servidor.");
            }
        }
    }

    public void removerAvatar() throws IOException {
        synchronized (this) {
            if (saida == null || socket == null || socket.isClosed()) {
                throw new IOException("O chat está desconectado.");
            }
            saida.println(Protocolo.REMOVER_AVATAR);
            if (saida.checkError()) {
                throw new IOException("Falha ao remover a foto de perfil no servidor.");
            }
        }
    }

    public void enviarAnexo(Path caminho) throws IOException {
        long tamanho = Files.size(caminho);
        if (tamanho < 1 || tamanho > Protocolo.TAMANHO_MAXIMO_ANEXO) {
            throw new IllegalArgumentException("Escolha um arquivo com até 10 MB.");
        }
        byte[] conteudo = Files.readAllBytes(caminho);
        String mime = Files.probeContentType(caminho);
        if (mime == null || !mime.matches("[a-zA-Z0-9.+-]+/[a-zA-Z0-9.+-]+")) {
            mime = "application/octet-stream";
        }
        String linha = Protocolo.anexo(caminho.getFileName().toString(), mime, conteudo);
        synchronized (this) {
            if (saida == null || socket == null || socket.isClosed()) {
                throw new IOException("O chat está desconectado.");
            }
            saida.println(linha);
            if (saida.checkError()) {
                throw new IOException("Falha ao enviar o anexo ao servidor.");
            }
        }
    }

    public void mudarCor(String cor) {
        usuario = usuario.comCor(cor);
        saida.println(Protocolo.comandoCor(cor));
    }

    public void digitando(boolean ativo) {
        saida.println(Protocolo.comandoDigitando(ativo));
    }

    public void sair() {
        if (saida != null) {
            saida.println(Protocolo.SAIR);
        }
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) { }
    }

    /** Versão de terminal (sem janela). A versão colorida é a ClienteGUI. */
    public static void main(String[] args) throws IOException {
        try (Scanner teclado = new Scanner(System.in, StandardCharsets.UTF_8)) {
            System.out.print("Seu nome: ");
            String nome = teclado.nextLine();
            System.out.print("Código da sala ou IP (Enter = localhost): ");
            String sala = teclado.nextLine();
            CodigoSala.Endereco e = sala.isBlank()
                ? new CodigoSala.Endereco("localhost", CodigoSala.PORTA_PADRAO)
                : CodigoSala.interpretar(sala);

            ClienteChat chat = new ClienteChat(e.host(), e.porta(), new Usuario(nome));
            chat.conectar(
                r -> System.out.println(r.texto()),
                () -> { System.out.println("Desconectado do servidor."); System.exit(0); });

            System.out.println("Digite suas mensagens (/sair para encerrar):");
            while (teclado.hasNextLine()) {
                String texto = teclado.nextLine();
                if (texto.equalsIgnoreCase("/sair")) {
                    chat.sair();
                    break;
                }
                chat.enviar(texto);
            }
        }
    }
}
