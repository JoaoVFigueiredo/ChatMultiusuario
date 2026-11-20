package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Entidade de domínio: uma mensagem com remetente, conteúdo, horário e cor do texto. */
public class Mensagem {
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final String remetente;
    private final String conteudo;
    private final LocalDateTime horario;
    private final String cor;

    public Mensagem(String remetente, String conteudo, LocalDateTime horario) {
        this(remetente, conteudo, horario, Usuario.COR_PADRAO);
    }

    public Mensagem(String remetente, String conteudo, LocalDateTime horario, String cor) {
        if (remetente == null || remetente.isBlank()) {
            throw new IllegalArgumentException("Remetente obrigatório.");
        }
        if (conteudo == null || conteudo.isBlank()) {
            throw new IllegalArgumentException("Conteúdo não pode ser vazio.");
        }
        if (horario == null) {
            throw new IllegalArgumentException("Horário obrigatório.");
        }
        if (!Usuario.corValida(cor)) {
            throw new IllegalArgumentException("Cor inválida.");
        }
        this.remetente = remetente.trim();
        this.conteudo = conteudo.trim();
        this.horario = horario;
        this.cor = cor.toUpperCase();
    }

    /** Cria uma mensagem com o horário atual e a cor padrão. */
    public static Mensagem nova(String remetente, String conteudo) {
        return new Mensagem(remetente, conteudo, LocalDateTime.now());
    }

    /** Cria uma mensagem com o horário atual, usando nome e cor do usuário. */
    public static Mensagem nova(Usuario usuario, String conteudo) {
        return new Mensagem(usuario.getNome(), conteudo, LocalDateTime.now(), usuario.getCor());
    }

    /** Texto que é enviado aos clientes, ex.: "[14:30] Ana: oi!" */
    public String formatar() {
        return "[" + horario.format(FORMATO_HORA) + "] " + remetente + ": " + conteudo;
    }

    public String getRemetente() { return remetente; }
    public String getConteudo()  { return conteudo; }
    public LocalDateTime getHorario() { return horario; }
    public String getCor() { return cor; }
}
