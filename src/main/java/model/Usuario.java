package model;

import java.util.regex.Pattern;

/** Entidade de domínio: quem participa do chat (nome + cor do texto). */
public class Usuario {
    public static final int TAMANHO_MAXIMO_NOME = 20;
    public static final String COR_PADRAO = "#1E88E5";
    private static final Pattern COR_HEX = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private final String nome;
    private final String cor;

    public Usuario(String nome) {
        this(nome, COR_PADRAO);
    }

    public Usuario(String nome, String cor) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome não pode ser vazio.");
        }
        String limpo = nome.trim();
        if (limpo.length() > TAMANHO_MAXIMO_NOME) {
            throw new IllegalArgumentException(
                "O nome deve ter no máximo " + TAMANHO_MAXIMO_NOME + " caracteres.");
        }
        if (limpo.contains("|")) {
            throw new IllegalArgumentException("O nome não pode conter o caractere '|'.");
        }
        if (!corValida(cor)) {
            throw new IllegalArgumentException("Cor inválida. Use o formato #RRGGBB.");
        }
        this.nome = limpo;
        this.cor = cor.toUpperCase();
    }

    public static boolean corValida(String cor) {
        return cor != null && COR_HEX.matcher(cor).matches();
    }

    /** Usuario é imutável: trocar a cor gera um novo objeto. */
    public Usuario comCor(String novaCor) {
        return new Usuario(nome, novaCor);
    }

    public String getNome() { return nome; }
    public String getCor()  { return cor; }

    @Override
    public String toString() {
        return nome;
    }
}
