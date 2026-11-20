package model;

/**
 * Transforma "IP + porta" em um código curto (ex.: 1K9ZX-4F2QD) que o amigo
 * cola na tela de login, e faz o caminho inverso.
 */
public final class CodigoSala {
    public static final int PORTA_PADRAO = 5000;

    public record Endereco(String host, int porta) { }

    private CodigoSala() { }

    public static String gerar(String ipv4, int porta) {
        String[] partes = ipv4 == null ? new String[0] : ipv4.trim().split("\\.");
        if (partes.length != 4) {
            throw new IllegalArgumentException("IP inválido: " + ipv4);
        }
        if (porta < 1 || porta > 65535) {
            throw new IllegalArgumentException("Porta inválida: " + porta);
        }
        long v = 0;
        for (String s : partes) {
            int octeto;
            try {
                octeto = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("IP inválido: " + ipv4);
            }
            if (octeto < 0 || octeto > 255) {
                throw new IllegalArgumentException("IP inválido: " + ipv4);
            }
            v = (v << 8) | octeto;
        }
        v = (v << 16) | porta;
        String c = Long.toString(v, 36).toUpperCase();
        return c.length() > 5 ? c.substring(0, c.length() - 5) + "-" + c.substring(c.length() - 5) : c;
    }

    public static Endereco decodificar(String codigo) {
        String limpo = codigo == null ? "" : codigo.replaceAll("[\\s-]", "").toUpperCase();
        if (!limpo.matches("[0-9A-Z]{1,10}")) {
            throw new IllegalArgumentException("Código de sala inválido.");
        }
        long v = Long.parseLong(limpo, 36);
        if (v >= (1L << 48)) {
            throw new IllegalArgumentException("Código de sala inválido.");
        }
        int porta = (int) (v & 0xFFFF);
        long ip = v >> 16;
        if (porta == 0) {
            throw new IllegalArgumentException("Código de sala inválido.");
        }
        String host = ((ip >> 24) & 255) + "." + ((ip >> 16) & 255) + "."
                    + ((ip >> 8) & 255) + "." + (ip & 255);
        return new Endereco(host, porta);
    }

    /** Aceita: código da sala, "ip", "ip:porta", "nome.de.host:porta" ou "localhost". */
    public static Endereco interpretar(String entrada) {
        String s = entrada == null ? "" : entrada.trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Informe o código da sala ou o IP.");
        }
        if (s.contains(".") || s.contains(":") || s.equalsIgnoreCase("localhost")) {
            int i = s.lastIndexOf(':');
            if (i < 0) {
                return new Endereco(s, PORTA_PADRAO);
            }
            try {
                return new Endereco(s.substring(0, i), Integer.parseInt(s.substring(i + 1)));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Porta inválida em: " + s);
            }
        }
        return decodificar(s);
    }
}
