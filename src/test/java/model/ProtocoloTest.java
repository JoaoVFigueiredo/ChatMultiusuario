package model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ProtocoloTest {
    @Test
    void entrarEhLidoDeVolta() {
        Usuario u = Protocolo.lerEntrar(Protocolo.entrar(new Usuario("Ana", "#E91E63")));
        assertEquals("Ana", u.getNome());
        assertEquals("#E91E63", u.getCor());
    }

    @Test
    void loginPreservaSalaECompatibilidadeAntiga() {
        Protocolo.Entrada entrada = Protocolo.lerEntrada(
            Protocolo.entrar(new Usuario("Ana"), " Estudos "));
        assertEquals("estudos", entrada.sala());
        assertTrue(entrada.carregarHistorico());
        assertEquals("geral", Protocolo.lerEntrada("ENTRAR|Bia|#1E88E5").sala());
        assertTrue(Protocolo.lerEntrada("ENTRAR|Bia|#1E88E5").carregarHistorico());
        assertThrows(IllegalArgumentException.class,
            () -> Protocolo.normalizarSala("sala com espaços"));
    }

    @Test
    void loginPodeIgnorarHistoricoSemQuebrarCompatibilidade() {
        Protocolo.Entrada entrada = Protocolo.lerEntrada(
            Protocolo.entrar(new Usuario("Ana"), "estudos", false));
        assertEquals("estudos", entrada.sala());
        assertFalse(entrada.carregarHistorico());
        assertThrows(IllegalArgumentException.class,
            () -> Protocolo.lerEntrada("ENTRAR|Ana|#1E88E5|geral|talvez"));
    }

    @Test
    void eventosDePresencaEDigitacaoSaoInterpretados() {
        assertEquals("3", Protocolo.ler(Protocolo.online(3)).texto());
        Protocolo.Recebida digitando = Protocolo.ler(Protocolo.digitando("Ana", true));
        assertEquals(Protocolo.DIGITANDO, digitando.tipo());
        assertEquals("Ana|true", digitando.texto());
    }

    @Test
    void loginInvalidoLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> Protocolo.lerEntrar("oi"));
        assertThrows(IllegalArgumentException.class, () -> Protocolo.lerEntrar(null));
    }

    @Test
    void mensagemPreservaCorETextoComBarras() {
        Mensagem m = new Mensagem("Ana", "a|b|c", LocalDateTime.of(2026, 1, 1, 10, 5), "#9C27B0");
        Protocolo.Recebida r = Protocolo.ler(Protocolo.msg(m));
        assertEquals(Protocolo.MSG, r.tipo());
        assertEquals("#9C27B0", r.cor());
        assertEquals("[10:05] Ana: a|b|c", r.texto());
    }

    @Test
    void mensagemMultilinhaPreservaQuebrasBarrasEAcentos() {
        Mensagem mensagem = new Mensagem("Assistente IA", "Primeira linha\nSegunda | linha\nSão Paulo",
            LocalDateTime.of(2026, 1, 1, 10, 5), "#9C27B0");

        Protocolo.Recebida recebida = Protocolo.ler(Protocolo.msg(mensagem));

        assertEquals(Protocolo.MSG, recebida.tipo());
        assertEquals("#9C27B0", recebida.cor());
        assertEquals("[10:05] Assistente IA: Primeira linha\nSegunda | linha\nSão Paulo",
            recebida.texto());
    }

    @Test
    void avisoDoSistemaNaoTemCor() {
        Protocolo.Recebida r = Protocolo.ler(Protocolo.sys("*** Bia entrou ***"));
        assertEquals(Protocolo.SYS, r.tipo());
        assertNull(r.cor());
        assertEquals("*** Bia entrou ***", r.texto());
    }

    @Test
    void comandoDeTextoMantemBarrasNoValor() {
        Protocolo.Comando c = Protocolo.lerComando(Protocolo.texto("oi|tudo"));
        assertEquals(Protocolo.TXT, c.tipo());
        assertEquals("oi|tudo", c.valor());
    }

    @Test
    void resultadoDaAssistentePreservaTextoComBarrasENovasLinhas() {
        String pergunta = "Como faço isso? | com detalhes";
        String resposta = "Primeira linha\nSegunda linha";

        Protocolo.Recebida recebida = Protocolo.ler(
            Protocolo.resultadoAssistenteIa(pergunta, resposta));
        Protocolo.HistoricoAssistente historico =
            Protocolo.lerResultadoAssistenteIa(recebida.texto());
        assertEquals(Protocolo.IA_RESULTADO, recebida.tipo());
        assertEquals(pergunta, historico.pergunta());
        assertEquals(resposta, historico.resposta());
    }

    @Test
    void perguntaDaAssistenteUsaComandoSeparadoEPreservaNovasLinhas() {
        String pergunta = "Explique\ncom exemplos | por favor";
        String linha = Protocolo.perguntaAssistenteIa(pergunta);

        assertEquals(Protocolo.IA_PERGUNTA, Protocolo.lerComando(linha).tipo());
        assertEquals(pergunta, Protocolo.lerPerguntaAssistenteIa(linha));
    }

    @Test
    void rejeitaPerguntaDaAssistenteVaziaOuMaiorQueOLimite() {
        assertThrows(IllegalArgumentException.class,
            () -> Protocolo.perguntaAssistenteIa(" "));
        assertThrows(IllegalArgumentException.class,
            () -> Protocolo.perguntaAssistenteIa("x".repeat(
                Protocolo.TAMANHO_MAXIMO_PERGUNTA_IA + 1)));
    }

    @Test
    void mensagensPrivadasIdentificamRemetenteEDestinatario() {
        assertTrue(Protocolo.recebidaPrivada("Ana", "oi").contains("privado de Ana"));
        assertTrue(Protocolo.respostaPrivada("Bia", "oi").contains("privado para Bia"));
    }

    @Test
    void listaDeUsuariosOnlinePreservaNomesComEspacosEAcentos() {
        Protocolo.Recebida recebida =
            Protocolo.ler(Protocolo.usuariosOnline(List.of("Ana Maria", "João, Jr.")));

        assertEquals(Protocolo.USERS, recebida.tipo());
        assertEquals(List.of("Ana Maria", "João, Jr."), recebida.usuarios());
    }

    @Test
    void anexoValidoFazRoundTripSemAlterarBytes() {
        byte[] bytes = {0, 1, 2, 10, 13, 127, (byte) 255};
        Protocolo.Recebida recebida =
            Protocolo.ler(Protocolo.anexo("foto da Ana.png", "image/png", bytes));

        assertEquals(Protocolo.ANEXO, recebida.tipo());
        assertEquals("foto da Ana.png", recebida.anexo().nome());
        assertEquals("image/png", recebida.anexo().mime());
        assertArrayEquals(bytes, recebida.anexo().conteudo());
    }

    @Test
    void anexoRejeitaTamanhoAcimaDoLimiteENomesDeCaminho() {
        assertThrows(IllegalArgumentException.class,
            () -> Protocolo.anexo("grande.zip", "application/zip",
                new byte[Protocolo.TAMANHO_MAXIMO_ANEXO + 1]));
        assertThrows(IllegalArgumentException.class,
            () -> Protocolo.anexo("..\\segredo.txt", "text/plain", new byte[] {1}));
    }

    @Test
    void fotoDePerfilPreservaNomeEAte256KbDeBytes() {
        byte[] bytes = {0, 1, 2, 10, 13, 127, (byte) 255};
        String linha = Protocolo.avatar("João Figueiredo", bytes);
        Protocolo.Recebida recebida = Protocolo.ler(linha);

        assertEquals(Protocolo.AVATAR, recebida.tipo());
        assertEquals("João Figueiredo", recebida.texto());
        assertArrayEquals(bytes, Protocolo.lerAvatar(linha));
        assertThrows(IllegalArgumentException.class,
            () -> Protocolo.avatar("João", new byte[Protocolo.TAMANHO_MAXIMO_AVATAR + 1]));
    }

    @Test
    void remocaoDeAvatarPreservaNomeComAcentos() {
        Protocolo.Recebida recebida = Protocolo.ler(Protocolo.avatarRemovido("João Figueiredo"));

        assertEquals(Protocolo.AVATAR_REMOVIDO, recebida.tipo());
        assertEquals("João Figueiredo", recebida.texto());
    }
}
