package client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FormatadorRespostaIA {
    private static final Pattern TITULO = Pattern.compile("^\\s{0,3}#{1,6}\\s+(.+)$");
    private static final Pattern MARCADOR_LISTA =
        Pattern.compile("^\\s*((?:[-*+•]\\s+|\\d+[.)]\\s+))(.+)$");
    private static final Pattern SEPARADOR_TABELA = Pattern.compile(":?-{3,}:?");

    record Trecho(int inicio, int comprimento) { }

    record Resultado(String texto, List<Trecho> negrito, List<Trecho> titulos) { }

    private FormatadorRespostaIA() { }

    static Resultado formatar(String resposta) {
        List<String> linhas = converterTabelas(resposta);
        StringBuilder texto = new StringBuilder();
        List<Trecho> negrito = new ArrayList<>();
        List<Trecho> titulos = new ArrayList<>();
        boolean proximoEmFraseSeparada = false;
        boolean linhaEmBranco = false;
        boolean linhaAnteriorLista = false;
        boolean linhaAnteriorTitulo = false;

        for (String linha : linhas) {
            String aparada = linha.trim();
            if (aparada.isEmpty()) {
                if (!texto.isEmpty()) linhaEmBranco = true;
                continue;
            }
            if (aparada.matches("[-*_]{3,}")) continue;
            Matcher titulo = TITULO.matcher(aparada);
            boolean ehTitulo = titulo.matches();
            String conteudo = ehTitulo ? titulo.group(1).trim() : aparada;
            Matcher marcador = MARCADOR_LISTA.matcher(conteudo);
            boolean itemDeLista = marcador.matches();
            String marcadorLista = "";
            if (itemDeLista) {
                String original = marcador.group(1).trim();
                marcadorLista = original.matches("\\d+[.)]") ? original + " " : "• ";
                conteudo = marcador.group(2).trim();
            }
            if (conteudo.startsWith("```")) continue;
            conteudo = conteudo.replaceAll("\\s+", " ").trim();

            if (texto.length() > 0) {
                if (proximoEmFraseSeparada && !terminaFrase(texto)) texto.append('.');
                if (linhaEmBranco) texto.append("\n\n");
                else if (itemDeLista || linhaAnteriorLista || ehTitulo || linhaAnteriorTitulo) {
                    texto.append('\n');
                } else texto.append(' ');
            }
            if (itemDeLista) texto.append(marcadorLista);
            int inicio = texto.length();
            adicionarTextoFormatado(texto, conteudo, negrito);
            int comprimento = texto.length() - inicio;
            if (ehTitulo && comprimento > 0) titulos.add(new Trecho(inicio, comprimento));
            proximoEmFraseSeparada = itemDeLista || ehTitulo;
            linhaAnteriorLista = itemDeLista;
            linhaAnteriorTitulo = ehTitulo;
            linhaEmBranco = false;
        }

        if (texto.length() > 0 && !terminaFrase(texto)) texto.append('.');
        return new Resultado(texto.toString(), List.copyOf(negrito), List.copyOf(titulos));
    }

    private static boolean terminaFrase(StringBuilder texto) {
        if (texto.isEmpty()) return true;
        char ultimo = texto.charAt(texto.length() - 1);
        return ultimo == '.' || ultimo == '!' || ultimo == '?' || ultimo == ':'
            || ultimo == '…' || ultimo == ';';
    }

    private static List<String> converterTabelas(String resposta) {
        String normalizada = resposta == null ? "" : resposta.replace("\r\n", "\n").replace('\r', '\n');
        String[] linhas = normalizada.split("\n", -1);
        List<String> resultado = new ArrayList<>();
        for (int i = 0; i < linhas.length;) {
            if (!linhas[i].contains("|")) {
                resultado.add(linhas[i++]);
                continue;
            }

            List<String> bloco = new ArrayList<>();
            while (i < linhas.length && linhas[i].contains("|")) {
                bloco.add(linhas[i++]);
            }
            adicionarTabelaOuLinha(resultado, bloco);
        }
        return resultado;
    }

    private static void adicionarTabelaOuLinha(List<String> destino, List<String> linhas) {
        List<List<String>> celulas = linhas.stream().map(FormatadorRespostaIA::separarCelulas).toList();
        int separador = -1;
        for (int i = 0; i < celulas.size(); i++) {
            if (!celulas.get(i).isEmpty()
                    && celulas.get(i).stream().allMatch(c -> SEPARADOR_TABELA.matcher(c).matches())) {
                separador = i;
                break;
            }
        }

        if (separador > 0) {
            List<String> cabecalhos = celulas.get(separador - 1);
            for (int i = separador + 1; i < celulas.size(); i++) {
                List<String> linha = celulas.get(i);
                if (!linha.isEmpty() && linha.stream().noneMatch(c -> SEPARADOR_TABELA.matcher(c).matches())) {
                    adicionarLinhaDaTabela(destino, cabecalhos, linha);
                }
            }
            return;
        }

        for (List<String> linha : celulas) {
            List<String> valores = linha.stream().filter(c -> !c.isBlank()).toList();
            if (!valores.isEmpty() && valores.stream().noneMatch(c -> SEPARADOR_TABELA.matcher(c).matches())) {
                destino.add(String.join(", ", valores));
            }
        }
    }

    private static void adicionarLinhaDaTabela(
            List<String> destino, List<String> cabecalhos, List<String> valores) {
        int titulo = indiceTitulo(cabecalhos);
        if (titulo >= valores.size()) titulo = -1;
        List<String> campos = new ArrayList<>();
        if (titulo >= 0 && !valores.get(titulo).isBlank()) {
            campos.add(limparCelula(valores.get(titulo)));
        }
        for (int i = 0; i < valores.size(); i++) {
            if (i == titulo || valores.get(i).isBlank() || ehIndice(cabecalhos, i)) continue;
            String rotulo = i < cabecalhos.size() && !cabecalhos.get(i).isBlank()
                ? cabecalhos.get(i) : "";
            String valor = limparCelula(valores.get(i));
            campos.add(rotulo.isBlank() ? valor : rotulo.trim() + ": " + valor);
        }
        if (!campos.isEmpty()) destino.add("• " + String.join(". ", campos));
    }

    private static int indiceTitulo(List<String> cabecalhos) {
        for (int i = 0; i < cabecalhos.size(); i++) {
            String rotulo = cabecalhos.get(i).toLowerCase(Locale.ROOT);
            if (rotulo.contains("ideia") || rotulo.contains("grupo") || rotulo.contains("tema")
                    || rotulo.contains("ação") || rotulo.contains("acao")
                    || rotulo.contains("item") || rotulo.contains("nome")) {
                return i;
            }
        }
        for (int i = 0; i < cabecalhos.size(); i++) {
            if (!ehIndice(cabecalhos, i)) return i;
        }
        return -1;
    }

    private static boolean ehIndice(List<String> cabecalhos, int indice) {
        if (indice >= cabecalhos.size()) return false;
        String rotulo = cabecalhos.get(indice).trim().toLowerCase(Locale.ROOT);
        return rotulo.equals("#") || rotulo.equals("n") || rotulo.equals("nº")
            || rotulo.equals("no.") || rotulo.equals("número") || rotulo.equals("numero");
    }

    private static List<String> separarCelulas(String linha) {
        String[] partes = linha.trim().split("\\|", -1);
        int inicio = partes.length > 0 && partes[0].isBlank() ? 1 : 0;
        int fim = partes.length > inicio && partes[partes.length - 1].isBlank()
            ? partes.length - 1 : partes.length;
        List<String> celulas = new ArrayList<>();
        for (int i = inicio; i < fim; i++) celulas.add(limparCelula(partes[i]));
        return celulas;
    }

    private static String limparCelula(String celula) {
        return celula.trim().replace("\\|", "—");
    }

    private static void adicionarTextoFormatado(
            StringBuilder destino, String linha, List<Trecho> negrito) {
        for (int i = 0; i < linha.length();) {
            String marcador = linha.startsWith("**", i) ? "**"
                : linha.startsWith("__", i) ? "__"
                : linha.charAt(i) == '*' || linha.charAt(i) == '_' ? linha.substring(i, i + 1) : null;
            if (marcador != null) {
                int fim = linha.indexOf(marcador, i + marcador.length());
                boolean sublinhado = marcador.charAt(0) == '_';
                boolean limiteEsquerdo = i == 0
                    || !Character.isLetterOrDigit(linha.charAt(i - 1));
                boolean limiteDireito = fim + marcador.length() >= linha.length()
                    || !Character.isLetterOrDigit(linha.charAt(fim + marcador.length()));
                if (fim > i + marcador.length()
                        && (!sublinhado || limiteEsquerdo && limiteDireito)
                        && (marcador.length() > 1 || !Character.isWhitespace(linha.charAt(i + 1)))) {
                    int inicioTexto = destino.length();
                    destino.append(linha, i + marcador.length(), fim);
                    if (destino.length() > inicioTexto) {
                        negrito.add(new Trecho(inicioTexto, destino.length() - inicioTexto));
                    }
                    i = fim + marcador.length();
                    continue;
                }
                if (sublinhado) destino.append(marcador);
                i += marcador.length();
                continue;
            }
            char caractere = linha.charAt(i++);
            if (caractere != '`' && caractere != '#') destino.append(caractere);
        }
    }
}
