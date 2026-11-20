package server;

import model.CodigoSala;
import model.Mensagem;
import model.Protocolo;
import persistence.MensagemRepository;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Aceita conexões e mantém a lista sincronizada de clientes ativos. */
public class Servidor {
    public static final int PORTA_PADRAO = 5000;
    public static final int TAMANHO_HISTORICO = 20;

    private final int porta;
    private final MensagemRepository repositorio;
    private final ConfiguracaoAssistentes configuracaoAssistentes;
    // Recurso compartilhado entre todas as threads -> lista sincronizada
    private final List<ClienteHandler> clientes =
        Collections.synchronizedList(new ArrayList<>());

    public Servidor(int porta, MensagemRepository repositorio) {
        this(porta, repositorio, configuracaoDoAmbiente());
    }

    public Servidor(int porta, MensagemRepository repositorio,
                    ConfiguracaoAssistentes configuracaoAssistentes) {
        this.porta = porta;
        this.repositorio = repositorio;
        this.configuracaoAssistentes = configuracaoAssistentes;
    }

    ConfiguracaoAssistentes configuracaoAssistentes() {
        return configuracaoAssistentes;
    }

    private static ConfiguracaoAssistentes configuracaoDoAmbiente() {
        try {
            return ConfiguracaoAssistentes.ler(
                ConfiguracaoAssistentes.localizarArquivoEnv(Path.of(".")), System.getenv());
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível carregar o arquivo .env.", e);
        }
    }

    /** @param ipAnunciado IP que será embutido no código da sala mostrado no console */
    public void iniciar(String ipAnunciado) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            System.out.println("Servidor ouvindo na porta " + porta);
            System.out.println("IP: " + ipAnunciado);
            System.out.println("CÓDIGO DA SALA: " + CodigoSala.gerar(ipAnunciado, porta));
            System.out.println("(passe o código ou o IP para quem vai entrar)");
            while (true) {
                Socket socket = serverSocket.accept();
                ClienteHandler handler = new ClienteHandler(socket, this);
                new Thread(handler).start();   // uma thread por cliente
            }
        }
    }

    public void adicionar(ClienteHandler c) {
        clientes.add(c);
    }

    public void remover(ClienteHandler c) {
        if (clientes.remove(c) && c.getSala() != null) {
            atualizarOnline(c.getSala());
        }
    }

    public List<Mensagem> historico() {
        return historico(Protocolo.SALA_PADRAO);
    }

    public List<Mensagem> historico(String sala) {
        return repositorio.buscarUltimas(sala, TAMANHO_HISTORICO);
    }

    public List<Mensagem> buscar(String sala, String termo) {
        return repositorio.buscar(sala, termo, 50);
    }

    /** Persiste a mensagem e a retransmite a TODOS (inclusive quem enviou, para ver a própria). */
    public void broadcast(Mensagem m) {
        broadcast(Protocolo.SALA_PADRAO, m);
    }

    public void broadcast(String sala, Mensagem m) {
        repositorio.salvar(sala, m);
        enviarNaSala(sala, Protocolo.msg(m), null);
    }

    /** Aviso do sistema (entrou/saiu). Não é persistido. */
    public void avisar(String texto, ClienteHandler origem) {
        avisar(Protocolo.SALA_PADRAO, texto, origem);
    }

    public void avisar(String sala, String texto, ClienteHandler origem) {
        enviarNaSala(sala, Protocolo.sys("*** " + texto + " ***"), origem);
    }

    public void atualizarOnline(String sala) {
        enviarNaSala(sala, Protocolo.online(contarNaSala(sala)), null);
        enviarNaSala(sala, Protocolo.usuariosOnline(nomesNaSala(sala)), null);
    }

    public void atualizarAvatar(ClienteHandler remetente, byte[] imagem) {
        try {
            repositorio.salvarAvatar(remetente.getUsuario().getNome(), imagem);
        } catch (IllegalStateException e) {
            remetente.enviar(Protocolo.sys("Não foi possível salvar a foto de perfil no servidor."));
            return;
        }
        remetente.setAvatar(imagem);
        synchronized (clientes) {
            String linha = Protocolo.avatar(remetente.getUsuario().getNome(), imagem);
            for (ClienteHandler cliente : clientes) {
                if (remetente.getSala().equals(cliente.getSala())) {
                    cliente.enviar(linha);
                }
            }
        }
    }

    public void removerAvatar(ClienteHandler remetente) {
        try {
            repositorio.removerAvatar(remetente.getUsuario().getNome());
        } catch (IllegalStateException e) {
            remetente.enviar(Protocolo.sys("Não foi possível remover a foto de perfil no servidor."));
            return;
        }
        remetente.setAvatar(null);
        synchronized (clientes) {
            String linha = Protocolo.avatarRemovido(remetente.getUsuario().getNome());
            for (ClienteHandler cliente : clientes) {
                if (remetente.getSala().equals(cliente.getSala())) {
                    cliente.enviar(linha);
                }
            }
        }
    }

    public void enviarAvatares(ClienteHandler destinatario) {
        Map<String, String> nomes = new LinkedHashMap<>();
        for (Mensagem mensagem : historico(destinatario.getSala())) {
            nomes.putIfAbsent(
                mensagem.getRemetente().toLowerCase(java.util.Locale.ROOT),
                mensagem.getRemetente());
        }
        synchronized (clientes) {
            for (ClienteHandler cliente : clientes) {
                if (destinatario.getSala().equals(cliente.getSala())) {
                    String nome = cliente.getUsuario().getNome();
                    nomes.putIfAbsent(nome.toLowerCase(java.util.Locale.ROOT), nome);
                }
            }
        }
        for (String nome : nomes.values()) {
            byte[] imagem = repositorio.buscarAvatar(nome);
            if (imagem != null) {
                String linha = Protocolo.avatar(nome, imagem);
                synchronized (clientes) {
                    for (ClienteHandler cliente : clientes) {
                        if (destinatario.getSala().equals(cliente.getSala())
                                && (cliente == destinatario
                                    || nome.equalsIgnoreCase(
                                        destinatario.getUsuario().getNome()))) {
                            cliente.setAvatar(imagem);
                            cliente.enviar(linha);
                        }
                    }
                }
            }
        }
        destinatario.enviar(Protocolo.AVATARES_SINCRONIZADOS);
    }

    public void avisarDigitando(String sala, String nome, boolean digitando, ClienteHandler origem) {
        enviarNaSala(sala, Protocolo.digitando(nome, digitando), origem);
    }

    public void retransmitirAnexo(String sala, String linha, ClienteHandler remetente,
                                  String nomeArquivo, long tamanho) {
        synchronized (clientes) {
            boolean destinatarioEncontrado = false;
            for (ClienteHandler cliente : clientes) {
                if (cliente != remetente && sala.equals(cliente.getSala())) {
                    cliente.enviar(linha);
                    destinatarioEncontrado = true;
                }
            }
            if (!destinatarioEncontrado) {
                remetente.enviar(Protocolo.sys("Anexo não enviado: não há outras pessoas online neste canal."));
            } else {
                remetente.enviar(Protocolo.sys("Anexo \"" + nomeArquivo + "\" enviado ("
                    + tamanhoLegivel(tamanho) + ")."));
            }
        }
    }

    public void enviarPrivada(ClienteHandler remetente, String nomeDestinatario, String texto) {
        ClienteHandler destinatario = null;
        synchronized (clientes) {
            for (ClienteHandler cliente : clientes) {
                if (cliente != remetente && remetente.getSala().equals(cliente.getSala())
                        && cliente.getUsuario().getNome().equalsIgnoreCase(nomeDestinatario)) {
                    destinatario = cliente;
                    break;
                }
            }
        }
        if (destinatario == null) {
            remetente.enviar(Protocolo.sys("Não encontrei \"" + nomeDestinatario + "\" neste canal."));
            return;
        }
        destinatario.enviar(Protocolo.sys(Protocolo.recebidaPrivada(
            remetente.getUsuario().getNome(), texto)));
        remetente.enviar(Protocolo.sys(Protocolo.respostaPrivada(
            destinatario.getUsuario().getNome(), texto)));
    }

    public List<String> nomesNaSala(String sala) {
        List<String> nomes = new ArrayList<>();
        synchronized (clientes) {
            for (ClienteHandler cliente : clientes) {
                if (sala.equals(cliente.getSala())) {
                    nomes.add(cliente.getUsuario().getNome());
                }
            }
        }
        return nomes;
    }

    private int contarNaSala(String sala) {
        int quantidade = 0;
        synchronized (clientes) {
            for (ClienteHandler c : clientes) {
                if (sala.equals(c.getSala())) quantidade++;
            }
        }
        return quantidade;
    }

    private static String tamanhoLegivel(long tamanho) {
        return tamanho >= 1024 * 1024
            ? String.format(java.util.Locale.ROOT, "%.1f MB", tamanho / (1024.0 * 1024.0))
            : Math.max(1, tamanho / 1024) + " KB";
    }

    private void enviarNaSala(String sala, String linha, ClienteHandler excluir) {
        // Iterar sobre lista sincronizada exige bloco synchronized manual
        synchronized (clientes) {
            for (ClienteHandler c : clientes) {
                if (c != excluir && sala.equals(c.getSala())) {
                    c.enviar(linha);
                }
            }
        }
    }

    private static String ipLocal() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address && a.isSiteLocalAddress()) {
                        return a.getHostAddress();
                    }
                }
            }
        } catch (SocketException ignored) { }
        return "127.0.0.1";
    }

    /** Uso: Servidor [ip-para-o-codigo] */
    public static void main(String[] args) throws IOException {
        MensagemRepository repo = new MensagemRepository("jdbc:sqlite:chat.db");
        String ip = args.length > 0 ? args[0] : ipLocal();
        Path arquivoEnv = ConfiguracaoAssistentes.localizarArquivoEnv(Path.of("."));
        ConfiguracaoAssistentes configuracao = ConfiguracaoAssistentes.carregar(arquivoEnv);
        boolean groqConfigurado = !configuracao.groqApiKey().isBlank();
        boolean iaHuggingFaceConfigurada = !configuracao.assistenteIaToken().isBlank();
        boolean bolaHuggingFaceConfigurada = !configuracao.bolaToken().isBlank();
        System.out.println("Configuração dos assistentes carregada de: "
            + arquivoEnv.toAbsolutePath().normalize());
        System.out.println("Assistente IA: " + (groqConfigurado
            ? "Groq configurado." : iaHuggingFaceConfigurada
                ? "Hugging Face configurado."
                : "configure GROQ_API_KEY ou ASSISTENTE_IA_TOKEN no .env."));
        System.out.println("Bola: " + (groqConfigurado
            ? "Groq configurado; " : bolaHuggingFaceConfigurada
                ? "Hugging Face configurado; "
                : "configure GROQ_API_KEY ou BOLA_TOKEN para respostas livres; ")
            + "notícias e classificação disponíveis.");
        new Servidor(PORTA_PADRAO, repo, configuracao).iniciar(ip);
    }
}
