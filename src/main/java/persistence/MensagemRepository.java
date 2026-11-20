package persistence;

import model.Mensagem;
import model.Protocolo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Único ponto do sistema que conversa com o banco via JDBC. */
public class MensagemRepository {
    private final String url;

    /** @param url ex.: "jdbc:sqlite:chat.db" */
    public MensagemRepository(String url) {
        this.url = url;
        criarTabela();
    }

    private Connection conectar() throws SQLException {
        return DriverManager.getConnection(url);
    }

    private void criarTabela() {
        String sql = """
            CREATE TABLE IF NOT EXISTS mensagem (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                remetente TEXT NOT NULL,
                conteudo  TEXT NOT NULL,
                horario   TEXT NOT NULL,
                cor       TEXT NOT NULL DEFAULT '#1E88E5',
                sala      TEXT NOT NULL DEFAULT 'geral'
            )""";
        try (Connection c = conectar(); Statement st = c.createStatement()) {
            st.execute(sql);
            st.execute("""
                CREATE TABLE IF NOT EXISTS perfil_usuario (
                    nome TEXT PRIMARY KEY COLLATE NOCASE,
                    avatar BLOB NOT NULL
                )""");
            try {
                // Bancos criados na versão anterior não tinham a coluna "cor"
                st.execute("ALTER TABLE mensagem ADD COLUMN cor TEXT NOT NULL DEFAULT '#1E88E5'");
            } catch (SQLException jaExiste) {
                // coluna já existe: tudo certo
            }
            try {
                st.execute("ALTER TABLE mensagem ADD COLUMN sala TEXT NOT NULL DEFAULT 'geral'");
            } catch (SQLException jaExiste) {
                // coluna já existe: tudo certo
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao criar tabela", e);
        }
    }

    public synchronized void salvarAvatar(String nome, byte[] imagem) {
        if (nome == null || nome.isBlank() || imagem == null || imagem.length == 0) {
            throw new IllegalArgumentException("Nome e imagem de perfil são obrigatórios.");
        }
        String sql = "INSERT INTO perfil_usuario (nome, avatar) VALUES (?, ?) "
            + "ON CONFLICT(nome) DO UPDATE SET avatar = excluded.avatar";
        try (Connection c = conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nome.trim());
            ps.setBytes(2, imagem.clone());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar avatar", e);
        }
    }

    public synchronized void removerAvatar(String nome) {
        String sql = "DELETE FROM perfil_usuario WHERE nome = ?";
        try (Connection c = conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nome);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao remover avatar", e);
        }
    }

    public synchronized byte[] buscarAvatar(String nome) {
        String sql = "SELECT avatar FROM perfil_usuario WHERE nome = ?";
        try (Connection c = conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nome);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBytes("avatar") : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar avatar", e);
        }
    }

    // synchronized: várias threads de clientes podem salvar ao mesmo tempo
    public synchronized void salvar(Mensagem m) {
        salvar(Protocolo.SALA_PADRAO, m);
    }

    public synchronized void salvar(String sala, Mensagem m) {
        String sql = "INSERT INTO mensagem (remetente, conteudo, horario, cor, sala) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getRemetente());
            ps.setString(2, m.getConteudo());
            ps.setString(3, m.getHorario().toString());
            ps.setString(4, m.getCor());
            ps.setString(5, Protocolo.normalizarSala(sala));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar mensagem", e);
        }
    }

    /** Retorna as últimas n mensagens, da mais antiga para a mais recente. */
    public synchronized List<Mensagem> buscarUltimas(int n) {
        return buscarUltimas(Protocolo.SALA_PADRAO, n);
    }

    public synchronized List<Mensagem> buscarUltimas(String sala, int n) {
        String sql = "SELECT remetente, conteudo, horario, cor FROM mensagem WHERE sala = ? ORDER BY id DESC LIMIT ?";
        List<Mensagem> lista = new ArrayList<>();
        try (Connection c = conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, Protocolo.normalizarSala(sala));
            ps.setInt(2, n);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Mensagem(
                        rs.getString("remetente"),
                        rs.getString("conteudo"),
                        LocalDateTime.parse(rs.getString("horario")),
                        rs.getString("cor")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar mensagens", e);
        }
        Collections.reverse(lista);
        return lista;
    }

    /** Busca mensagens públicas do canal; resultados vêm em ordem cronológica. */
    public synchronized List<Mensagem> buscar(String sala, String termo, int limite) {
        if (termo == null || termo.isBlank()) {
            throw new IllegalArgumentException("O termo de busca não pode ser vazio.");
        }
        if (limite < 1) {
            throw new IllegalArgumentException("O limite da busca deve ser positivo.");
        }
        String sql = "SELECT remetente, conteudo, horario, cor FROM mensagem "
            + "WHERE sala = ? AND instr(lower(conteudo), lower(?)) > 0 "
            + "ORDER BY id DESC LIMIT ?";
        List<Mensagem> lista = new ArrayList<>();
        try (Connection c = conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, Protocolo.normalizarSala(sala));
            ps.setString(2, termo.trim());
            ps.setInt(3, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Mensagem(
                        rs.getString("remetente"),
                        rs.getString("conteudo"),
                        LocalDateTime.parse(rs.getString("horario")),
                        rs.getString("cor")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar mensagens", e);
        }
        Collections.reverse(lista);
        return lista;
    }
}
