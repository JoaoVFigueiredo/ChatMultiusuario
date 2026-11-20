package persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Guarda localmente as conversas da Assistente IA por usuário, servidor e sala. */
public final class HistoricoAssistenteRepository {
    public record Conversa(String pergunta, String resposta, LocalDateTime horario) { }

    private final String url;

    public HistoricoAssistenteRepository(Path arquivo) {
        if (arquivo == null) {
            throw new IllegalArgumentException("O caminho do histórico é obrigatório.");
        }
        Path pasta = arquivo.toAbsolutePath().getParent();
        try {
            if (pasta != null) Files.createDirectories(pasta);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível criar a pasta do histórico da IA.", e);
        }
        url = "jdbc:sqlite:" + arquivo.toAbsolutePath();
        criarTabela();
    }

    private Connection conectar() throws SQLException {
        return DriverManager.getConnection(url);
    }

    private void criarTabela() {
        try (Connection conexao = conectar(); Statement comando = conexao.createStatement()) {
            comando.execute("""
                CREATE TABLE IF NOT EXISTS historico_assistente (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario TEXT NOT NULL COLLATE NOCASE,
                    servidor TEXT NOT NULL COLLATE NOCASE,
                    sala TEXT NOT NULL COLLATE NOCASE,
                    pergunta TEXT NOT NULL,
                    resposta TEXT NOT NULL,
                    horario TEXT NOT NULL
                )""");
            comando.execute("""
                CREATE INDEX IF NOT EXISTS idx_historico_assistente_usuario
                ON historico_assistente (usuario, servidor, sala, id)""");
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível preparar o histórico local da IA.", e);
        }
    }

    public synchronized void salvar(
            String usuario, String servidor, String sala, String pergunta, String resposta) {
        validar(usuario, "usuário");
        validar(servidor, "servidor");
        validar(sala, "sala");
        validar(pergunta, "pergunta");
        validar(resposta, "resposta");
        String sql = """
            INSERT INTO historico_assistente
                (usuario, servidor, sala, pergunta, resposta, horario)
            VALUES (?, ?, ?, ?, ?, ?)""";
        try (Connection conexao = conectar(); PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, usuario.trim());
            comando.setString(2, servidor.trim());
            comando.setString(3, sala.trim());
            comando.setString(4, pergunta.trim());
            comando.setString(5, resposta.trim());
            comando.setString(6, LocalDateTime.now().toString());
            comando.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível salvar o histórico local da IA.", e);
        }
    }

    public synchronized List<Conversa> buscar(String usuario, String servidor, String sala) {
        validar(usuario, "usuário");
        validar(servidor, "servidor");
        validar(sala, "sala");
        String sql = """
            SELECT pergunta, resposta, horario FROM historico_assistente
            WHERE usuario = ? AND servidor = ? AND sala = ?
            ORDER BY id""";
        List<Conversa> conversas = new ArrayList<>();
        try (Connection conexao = conectar(); PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, usuario.trim());
            comando.setString(2, servidor.trim());
            comando.setString(3, sala.trim());
            try (ResultSet resultados = comando.executeQuery()) {
                while (resultados.next()) {
                    conversas.add(new Conversa(
                        resultados.getString("pergunta"),
                        resultados.getString("resposta"),
                        LocalDateTime.parse(resultados.getString("horario"))));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível carregar o histórico local da IA.", e);
        }
        return List.copyOf(conversas);
    }

    private static void validar(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("O campo " + campo + " do histórico é obrigatório.");
        }
    }
}
