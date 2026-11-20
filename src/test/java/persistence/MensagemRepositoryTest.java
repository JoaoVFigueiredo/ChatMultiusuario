package persistence;

import model.Mensagem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Usa um banco SQLite temporário: não depende de rede nem do chat.db real. */
class MensagemRepositoryTest {
    private MensagemRepository repo;

    @BeforeEach
    void preparar(@TempDir Path pasta) {
        repo = new MensagemRepository("jdbc:sqlite:" + pasta.resolve("teste.db"));
    }

    @Test
    void salvaERecuperaMensagem() {
        repo.salvar(Mensagem.nova("Ana", "primeira"));
        List<Mensagem> lista = repo.buscarUltimas(10);
        assertEquals(1, lista.size());
        assertEquals("Ana", lista.get(0).getRemetente());
        assertEquals("primeira", lista.get(0).getConteudo());
    }

    @Test
    void buscarUltimasRespeitaLimiteEOrdem() {
        for (int i = 1; i <= 5; i++) {
            repo.salvar(Mensagem.nova("Ana", "msg " + i));
        }
        List<Mensagem> lista = repo.buscarUltimas(3);
        assertEquals(3, lista.size());
        assertEquals("msg 3", lista.get(0).getConteudo());
        assertEquals("msg 5", lista.get(2).getConteudo());
    }

    @Test
    void historicoEhSeparadoPorSala() {
        repo.salvar("geral", Mensagem.nova("Ana", "mensagem geral"));
        repo.salvar("estudos", Mensagem.nova("Bia", "mensagem de estudos"));

        assertEquals("mensagem geral", repo.buscarUltimas("geral", 10).get(0).getConteudo());
        assertEquals("mensagem de estudos", repo.buscarUltimas("estudos", 10).get(0).getConteudo());
        assertTrue(repo.buscarUltimas("outro", 10).isEmpty());
    }

    @Test
    void corDaMensagemEhPersistida() {
        repo.salvar(new Mensagem("Bia", "oi", LocalDateTime.now(), "#9C27B0"));
        assertEquals("#9C27B0", repo.buscarUltimas(1).get(0).getCor());
    }

    @Test
    void avatarEhPersistidoAtualizadoERemovidoNoBanco() {
        byte[] foto = {1, 2, 3, 4};
        repo.salvarAvatar("Ana", foto);
        assertArrayEquals(foto, repo.buscarAvatar("ana"));

        byte[] fotoAtualizada = {5, 6, 7};
        repo.salvarAvatar("ANA", fotoAtualizada);
        assertArrayEquals(fotoAtualizada, repo.buscarAvatar("Ana"));

        repo.removerAvatar("ana");
        assertNull(repo.buscarAvatar("Ana"));
    }

    @Test
    void avatarContinuaDisponivelAposReabrirRepositorio(@TempDir Path pasta) {
        String url = "jdbc:sqlite:" + pasta.resolve("perfis.db");
        MensagemRepository primeiro = new MensagemRepository(url);
        byte[] foto = {9, 8, 7, 6};
        primeiro.salvarAvatar("Bia", foto);

        MensagemRepository reaberto = new MensagemRepository(url);

        assertArrayEquals(foto, reaberto.buscarAvatar("Bia"));
    }

    @Test
    void avataresDeUsuariosDiferentesPermanecemSeparados() {
        byte[] fotoAna = {1, 2, 3};
        byte[] fotoJoao = {4, 5, 6};
        repo.salvarAvatar("Ana", fotoAna);
        repo.salvarAvatar("João", fotoJoao);

        assertArrayEquals(fotoAna, repo.buscarAvatar("ANA"));
        assertArrayEquals(fotoJoao, repo.buscarAvatar("joão"));
    }

    @Test
    void buscaPorTextoEhInsensivelAMaiusculasEDistintaPorSala() {
        repo.salvar("geral", Mensagem.nova("Ana", "Gol do Brasil"));
        repo.salvar("geral", Mensagem.nova("Bia", "Vamos conversar"));
        repo.salvar("estudos", Mensagem.nova("Caio", "gol do Brasil"));

        List<Mensagem> resultados = repo.buscar("geral", "GOL", 50);

        assertEquals(1, resultados.size());
        assertEquals("Gol do Brasil", resultados.get(0).getConteudo());
    }

    @Test
    void historicoDaAssistentePersisteESeparaUsuarioServidorESala(@TempDir Path pasta) {
        Path arquivo = pasta.resolve("historico.db");
        HistoricoAssistenteRepository primeiro =
            new HistoricoAssistenteRepository(arquivo);
        primeiro.salvar("Ana", "localhost:5000", "geral",
            "Pergunta inicial", "Resposta inicial");
        primeiro.salvar("Bia", "localhost:5000", "geral",
            "Outra pergunta", "Outra resposta");
        primeiro.salvar("Ana", "localhost:5000", "estudos",
            "Pergunta em outra sala", "Resposta em outra sala");

        HistoricoAssistenteRepository reaberto =
            new HistoricoAssistenteRepository(arquivo);
        List<HistoricoAssistenteRepository.Conversa> conversas =
            reaberto.buscar("ana", "localhost:5000", "geral");

        assertEquals(1, conversas.size());
        assertEquals("Pergunta inicial", conversas.get(0).pergunta());
        assertEquals("Resposta inicial", conversas.get(0).resposta());
        assertTrue(reaberto.buscar("Carlos", "localhost:5000", "geral").isEmpty());
    }
}
