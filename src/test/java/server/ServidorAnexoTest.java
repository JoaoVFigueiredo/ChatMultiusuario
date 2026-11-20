package server;

import model.Protocolo;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class ServidorAnexoTest {
    @Test
    void mensagensPublicasNaoChamamIaERespostaDaJanelaFicaPrivada() throws Exception {
        int porta;
        try (ServerSocket socket = new ServerSocket(0)) {
            porta = socket.getLocalPort();
        }
        var arquivoBanco = Files.createTempDirectory("chat-ia-privada-test").resolve("chat.db");
        var configuracao = new ConfiguracaoAssistentes(
            "", ConfiguracaoAssistentes.MODELO_GROQ_PADRAO,
            "", ConfiguracaoAssistentes.MODELO_IA_PADRAO,
            "", ConfiguracaoAssistentes.MODELO_BOLA_PADRAO);
        var servidor = new Servidor(porta,
            new persistence.MensagemRepository("jdbc:sqlite:" + arquivoBanco), configuracao);
        Thread threadServidor = new Thread(() -> {
            try {
                servidor.iniciar("127.0.0.1");
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        });
        threadServidor.setDaemon(true);
        threadServidor.start();

        try (Peer ana = new Peer(porta, "Ana"); Peer bia = new Peer(porta, "Bia")) {
            ana.lerAte(s -> s.startsWith(Protocolo.USERS + "|"));
            ana.lerAte(s -> s.startsWith(Protocolo.USERS + "|"));
            bia.lerAte(s -> s.startsWith(Protocolo.USERS + "|"));
            bia.lerAte(s -> s.equals(Protocolo.AVATARES_SINCRONIZADOS));

            ana.enviar(Protocolo.texto("oi Ana"));
            Protocolo.Recebida mensagemPublica =
                Protocolo.ler(bia.lerAte(s -> s.startsWith(Protocolo.MSG + "|")));
            assertTrue(mensagemPublica.texto().contains("Ana: oi Ana"));
            ana.lerAte(s -> s.startsWith(Protocolo.MSG + "|"));

            String pergunta = "Me ajude com meus estudos";
            ana.enviar(Protocolo.perguntaAssistenteIa(pergunta));
            Protocolo.Recebida recebida = Protocolo.ler(
                ana.lerAte(s -> s.startsWith(Protocolo.IA_RESULTADO + "|")));
            Protocolo.HistoricoAssistente resposta =
                Protocolo.lerResultadoAssistenteIa(recebida.texto());
            assertEquals(pergunta, resposta.pergunta());
            assertFalse(resposta.resposta().isBlank());
            assertThrows(java.net.SocketTimeoutException.class, bia::lerLinha);
        }
    }

    @Test
    void anexoEhEncaminhadoSomenteAosOutrosUsuariosOnlineSemPersistir() throws Exception {
        int porta;
        try (ServerSocket socket = new ServerSocket(0)) {
            porta = socket.getLocalPort();
        }
        var banco = Files.createTempDirectory("chat-anexo-test").resolve("chat.db");
        var servidor = new Servidor(porta,
            new persistence.MensagemRepository("jdbc:sqlite:" + banco));
        Thread threadServidor = new Thread(() -> {
            try {
                servidor.iniciar("127.0.0.1");
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        });
        threadServidor.setDaemon(true);
        threadServidor.start();

        try (Peer ana = new Peer(porta, "Ana"); Peer bia = new Peer(porta, "Bia")) {
            ana.lerAte(s -> s.startsWith(Protocolo.USERS + "|"));
            Protocolo.Recebida usuariosBia = Protocolo.ler(
                bia.lerAte(s -> s.startsWith(Protocolo.USERS + "|")));
            assertTrue(usuariosBia.usuarios().containsAll(java.util.List.of("Ana", "Bia")));
            Protocolo.Recebida usuariosAna = Protocolo.ler(
                ana.lerAte(s -> s.startsWith(Protocolo.USERS + "|")));
            assertTrue(usuariosAna.usuarios().containsAll(java.util.List.of("Ana", "Bia")));
            byte[] foto = fotoValida();
            ana.enviar(Protocolo.avatar("Ana", foto));
            Protocolo.Recebida fotoBia = Protocolo.ler(
                bia.lerAte(s -> s.startsWith(Protocolo.AVATAR + "|")));
            assertEquals("Ana", fotoBia.texto());
            assertArrayEquals(foto, java.util.Base64.getUrlDecoder().decode(fotoBia.cor()));

            byte[] conteudo = {1, 2, 3, 4};
            ana.enviar(Protocolo.anexo("foto.png", "image/png", conteudo));

            Protocolo.Recebida recebida = Protocolo.ler(
                bia.lerAte(s -> s.startsWith(Protocolo.ANEXO + "|")));
            assertEquals("foto.png", recebida.anexo().nome());
            assertArrayEquals(conteudo, recebida.anexo().conteudo());
            assertTrue(ana.lerAte(s -> s.startsWith("SYS|") && s.contains("Anexo \"foto.png\" enviado"))
                .contains("Anexo \"foto.png\" enviado"));
            assertTrue(servidor.historico("geral").isEmpty());
            bia.enviar("SAIR");
            Protocolo.Recebida usuariosDepoisDaSaida = Protocolo.ler(
                ana.lerAte(s -> s.startsWith(Protocolo.USERS + "|")));
            assertEquals(java.util.List.of("Ana"), usuariosDepoisDaSaida.usuarios());
        }
    }

    @Test
    void avatarPersistidoEhEnviadoAoReconectarERemocaoEhSincronizada() throws Exception {
        int porta;
        try (ServerSocket socket = new ServerSocket(0)) {
            porta = socket.getLocalPort();
        }
        var arquivoBanco = Files.createTempDirectory("chat-avatar-test").resolve("chat.db");
        String urlBanco = "jdbc:sqlite:" + arquivoBanco;
        var repositorio = new persistence.MensagemRepository(urlBanco);
        var servidor = new Servidor(porta, repositorio);
        Thread threadServidor = new Thread(() -> {
            try {
                servidor.iniciar("127.0.0.1");
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        });
        threadServidor.setDaemon(true);
        threadServidor.start();

        byte[] foto = fotoValida();
        try (Peer ana = new Peer(porta, "Ana")) {
            ana.lerAte(s -> s.startsWith(Protocolo.USERS + "|"));
            ana.enviar(Protocolo.avatar("Ana", foto));
            ana.lerAte(s -> s.startsWith(Protocolo.AVATAR + "|"));
        }

        assertArrayEquals(foto, new persistence.MensagemRepository(urlBanco).buscarAvatar("ANA"));

        try (Peer bia = new Peer(porta, "Bia")) {
            bia.lerAte(s -> s.startsWith(Protocolo.USERS + "|"));
            try (Peer anaReconectada = new Peer(porta, "Ana")) {
                anaReconectada.lerAte(s -> s.startsWith(Protocolo.USERS + "|"));
                Protocolo.Recebida avatarSincronizado = Protocolo.ler(
                    bia.lerAte(s -> s.startsWith(Protocolo.AVATAR + "|")));
                assertEquals("Ana", avatarSincronizado.texto());
                assertArrayEquals(foto,
                    java.util.Base64.getUrlDecoder().decode(avatarSincronizado.cor()));

            Protocolo.Recebida avatarPersistido = Protocolo.ler(
                anaReconectada.lerAte(s -> s.startsWith(Protocolo.AVATAR + "|")));
            assertArrayEquals(foto,
                java.util.Base64.getUrlDecoder().decode(avatarPersistido.cor()));

            anaReconectada.enviar(Protocolo.REMOVER_AVATAR);
            Protocolo.Recebida remocao = Protocolo.ler(
                bia.lerAte(s -> s.startsWith(Protocolo.AVATAR_REMOVIDO + "|")));
            assertEquals("Ana", remocao.texto());
            }
        }

        assertNull(new persistence.MensagemRepository(urlBanco).buscarAvatar("Ana"));
    }

    private static byte[] fotoValida() throws Exception {
        BufferedImage imagem = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        imagem.setRGB(0, 0, 0x336699);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(imagem, "jpg", bytes));
        return bytes.toByteArray();
    }

    private static final class Peer implements AutoCloseable {
        private final Socket socket;
        private final BufferedReader entrada;
        private final PrintWriter saida;

        private Peer(int porta, String nome) throws Exception {
            Socket conectado = null;
            java.net.ConnectException ultimoErro = null;
            long limite = System.currentTimeMillis() + 2000;
            while (conectado == null && System.currentTimeMillis() < limite) {
                Socket tentativa = new Socket();
                try {
                    tentativa.connect(new java.net.InetSocketAddress("127.0.0.1", porta), 200);
                    conectado = tentativa;
                } catch (java.net.ConnectException e) {
                    ultimoErro = e;
                    tentativa.close();
                    Thread.sleep(20);
                }
            }
            if (conectado == null) throw ultimoErro;
            socket = conectado;
            socket.setSoTimeout(3000);
            entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            saida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            saida.println("ENTRAR|" + nome + "|#1E88E5|geral|false");
        }

        private void enviar(String texto) {
            saida.println(texto);
        }

        private String lerAte(java.util.function.Predicate<String> predicate) throws Exception {
            long limite = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < limite) {
                String linha = entrada.readLine();
                if (linha == null) throw new AssertionError("Conexão encerrada antes do evento.");
                if (predicate.test(linha)) return linha;
            }
            throw new AssertionError("Evento esperado não recebido.");
        }

        private String lerLinha() throws Exception {
            return entrada.readLine();
        }

        @Override
        public void close() throws Exception {
            socket.close();
        }
    }
}
