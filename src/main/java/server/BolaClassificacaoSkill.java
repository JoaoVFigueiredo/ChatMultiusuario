package server;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class BolaClassificacaoSkill implements BolaSkill {
    private static final String ENDPOINT =
        "https://site.api.espn.com/apis/v2/sports/soccer/bra.";
    private static final int QUANTIDADE_TIMES = 20;
    private static final int LIMITE_RESPOSTA_BYTES = 1_000_000;
    private static final ZoneId FUSO_BRASIL = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATO_DATA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"));
    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
    private final int temporada;

    BolaClassificacaoSkill() {
        this(Year.now(FUSO_BRASIL).getValue());
    }

    BolaClassificacaoSkill(int temporada) {
        this.temporada = temporada;
    }

    @Override
    public boolean atende(String consulta) {
        String normalizada = normalizar(consulta);
        boolean serieA = normalizada.matches("(?s).*\\bserie\\s*a\\b.*");
        boolean serieB = normalizada.matches("(?s).*\\bserie\\s*b\\b.*");
        boolean pedeTabela = normalizada.contains("classificacao")
            || normalizada.contains("tabela")
            || normalizada.contains("classific");
        boolean pedeListaDeTimes = normalizada.contains("time")
            || normalizada.contains("clube")
            || normalizada.contains("equipe")
            || normalizada.contains("participante");
        boolean mencionaCompeticaoBrasileira = mencionaCompeticaoBrasileira(normalizada);
        return (serieA || serieB) && (pedeTabela || pedeListaDeTimes)
            || mencionaCompeticaoBrasileira && (pedeTabela || pedeListaDeTimes)
            || pedeTabela && !normalizada.matches("(?s).*\\bserie\\s+[a-z]\\b.*");
    }

    @Override
    public List<String> executar(String consulta, BolaContext contexto)
            throws IOException, InterruptedException {
        List<String> series = seriesSolicitadas(consulta);
        if (series.isEmpty()) {
            return List.of("Informe se você quer a classificação da Série A ou da Série B.");
        }

        StringBuilder resposta = new StringBuilder();
        for (String serie : series) anexarTabela(resposta, serie);
        return List.of(resposta.toString().trim());
    }

    static List<String> seriesSolicitadas(String consulta) {
        String normalizada = normalizar(consulta);
        boolean serieA = normalizada.matches("(?s).*\\bserie\\s*a\\b.*");
        boolean serieB = normalizada.matches("(?s).*\\bserie\\s*b\\b.*");
        if (serieA && serieB) return List.of("A", "B");
        if (serieA) return List.of("A");
        if (serieB) return List.of("B");
        if (mencionaCompeticaoBrasileira(normalizada)) return List.of("A", "B");
        boolean pedeTabela = normalizada.contains("classific") || normalizada.contains("tabela");
        if (pedeTabela && !normalizada.matches("(?s).*\\bserie\\s+[a-z]\\b.*")) {
            return List.of("A");
        }
        return List.of();
    }

    private void anexarTabela(StringBuilder resposta, String serie)
            throws IOException, InterruptedException {
        List<Time> times = buscarTabela(serie);
        if (times.size() != QUANTIDADE_TIMES) {
            throw new IOException("A fonte da Série " + serie + " retornou "
                + times.size() + " times; a classificação completa deve ter "
                + QUANTIDADE_TIMES + ".");
        }

        if (!resposta.isEmpty()) resposta.append("\n\n");
        resposta.append("Classificação da Série ").append(serie)
            .append(" — ").append(temporada).append('\n');
        for (Time time : times) {
            String posicao = time.estatisticas().get("rank");
            if (posicao == null || posicao.isBlank()) {
                throw new IOException("A classificação recebida não informa a posição de todos os times.");
            }
            resposta.append(posicao).append(". ").append(time.nome())
                .append(" — ").append(estatistica(time, "points")).append(" pts | ")
                .append(estatistica(time, "gamesPlayed")).append(" J | ")
                .append(estatistica(time, "wins")).append(" V | ")
                .append(estatistica(time, "ties")).append(" E | ")
                .append(estatistica(time, "losses")).append(" D | SG ")
                .append(estatistica(time, "pointDifferential")).append('\n');
        }
        resposta.append("Fonte: ESPN (consulta em ")
            .append(ZonedDateTime.now(FUSO_BRASIL).format(FORMATO_DATA))
            .append(")\nhttps://www.espn.com/soccer/table/_/league/bra.")
            .append(serie.equals("A") ? "1" : "2");
    }

    private List<Time> buscarTabela(String serie) throws IOException, InterruptedException {
        String identificador = serie.equals("A") ? "1" : "2";
        URI uri = URI.create(ENDPOINT + identificador + "/standings?season=" + temporada);
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(15))
            .header("Accept", "application/json")
            .header("User-Agent", "ChatMultiusuario/1.0")
            .GET()
            .build();
        HttpResponse<byte[]> response =
            http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("A fonte da classificação respondeu HTTP "
                + response.statusCode() + ".");
        }
        byte[] bytes = response.body();
        if (bytes.length > LIMITE_RESPOSTA_BYTES) {
            throw new IOException("A resposta da classificação excedeu o limite permitido.");
        }
        return lerTabela(new String(bytes, StandardCharsets.UTF_8));
    }

    static List<Time> lerTabela(String json) throws IOException {
        List<String> entradas = objetosDoArray(campo(json, "entries", '['));
        if (entradas.isEmpty()) {
            throw new IOException("A resposta da fonte não contém times na classificação.");
        }

        List<Time> times = new ArrayList<>();
        for (String entrada : entradas) {
            String equipe = campo(entrada, "team", '{');
            String nome = texto(equipe, "displayName");
            if (nome.isBlank()) nome = texto(equipe, "name");
            if (nome.isBlank()) {
                throw new IOException("A fonte retornou um time sem nome.");
            }
            nome = nomeExibicao(nome);

            Map<String, String> estatisticas = new LinkedHashMap<>();
            for (String estatistica : objetosDoArray(campo(entrada, "stats", '['))) {
                String chave = texto(estatistica, "name");
                String valor = texto(estatistica, "displayValue");
                if (!chave.isBlank()) estatisticas.put(chave, valor);
            }
            times.add(new Time(nome, Map.copyOf(estatisticas)));
        }
        return List.copyOf(times);
    }

    private static String estatistica(Time time, String nome) throws IOException {
        String valor = time.estatisticas().get(nome);
        if (valor == null || valor.isBlank()) {
            throw new IOException("A classificação não trouxe o campo " + nome
                + " para " + time.nome() + ".");
        }
        return valor;
    }

    private static String campo(String json, String nome, char tipoEsperado)
            throws IOException {
        int chave = json.indexOf("\"" + nome + "\"");
        if (chave < 0) throw new IOException("Campo " + nome + " ausente na classificação.");
        int separador = json.indexOf(':', chave + nome.length() + 2);
        if (separador < 0) throw new IOException("Campo " + nome + " inválido na classificação.");
        int inicio = ignorarEspacos(json, separador + 1);
        if (inicio >= json.length() || json.charAt(inicio) != tipoEsperado) {
            throw new IOException("Campo " + nome + " tem formato inválido na classificação.");
        }
        int fim = tipoEsperado == '{' ? fimObjeto(json, inicio) : fimArray(json, inicio);
        return json.substring(inicio, fim + 1);
    }

    private static List<String> objetosDoArray(String array) throws IOException {
        List<String> objetos = new ArrayList<>();
        int posicao = 1;
        while (posicao < array.length()) {
            posicao = ignorarEspacos(array, posicao);
            if (posicao >= array.length() || array.charAt(posicao) == ']') break;
            if (array.charAt(posicao) == ',') {
                posicao++;
                continue;
            }
            if (array.charAt(posicao) != '{') {
                throw new IOException("A lista da classificação contém um item inválido.");
            }
            int fim = fimObjeto(array, posicao);
            objetos.add(array.substring(posicao, fim + 1));
            posicao = fim + 1;
        }
        return objetos;
    }

    private static int fimObjeto(String json, int inicio) throws IOException {
        int profundidade = 0;
        boolean emTexto = false;
        boolean escape = false;
        for (int i = inicio; i < json.length(); i++) {
            char atual = json.charAt(i);
            if (emTexto) {
                if (escape) escape = false;
                else if (atual == '\\') escape = true;
                else if (atual == '"') emTexto = false;
                continue;
            }
            if (atual == '"') emTexto = true;
            else if (atual == '{') profundidade++;
            else if (atual == '}' && --profundidade == 0) return i;
        }
        throw new IOException("Objeto JSON incompleto na classificação.");
    }

    private static int fimArray(String json, int inicio) throws IOException {
        int profundidade = 0;
        boolean emTexto = false;
        boolean escape = false;
        for (int i = inicio; i < json.length(); i++) {
            char atual = json.charAt(i);
            if (emTexto) {
                if (escape) escape = false;
                else if (atual == '\\') escape = true;
                else if (atual == '"') emTexto = false;
                continue;
            }
            if (atual == '"') emTexto = true;
            else if (atual == '[') profundidade++;
            else if (atual == ']' && --profundidade == 0) return i;
        }
        throw new IOException("Lista JSON incompleta na classificação.");
    }

    private static String texto(String json, String nome) throws IOException {
        int chave = json.indexOf("\"" + nome + "\"");
        if (chave < 0) return "";
        int separador = json.indexOf(':', chave + nome.length() + 2);
        if (separador < 0) throw new IOException("Campo " + nome + " inválido na classificação.");
        int inicio = ignorarEspacos(json, separador + 1);
        if (inicio >= json.length() || json.charAt(inicio) != '"') return "";

        StringBuilder valor = new StringBuilder();
        for (int i = inicio + 1; i < json.length(); i++) {
            char atual = json.charAt(i);
            if (atual == '"') return valor.toString();
            if (atual != '\\') {
                valor.append(atual);
                continue;
            }
            if (++i >= json.length()) break;
            char escape = json.charAt(i);
            switch (escape) {
                case '"', '\\', '/' -> valor.append(escape);
                case 'b' -> valor.append('\b');
                case 'f' -> valor.append('\f');
                case 'n' -> valor.append('\n');
                case 'r' -> valor.append('\r');
                case 't' -> valor.append('\t');
                case 'u' -> {
                    if (i + 4 >= json.length()) throw new IOException("Escape Unicode incompleto.");
                    try {
                        valor.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
                    } catch (NumberFormatException e) {
                        throw new IOException("Escape Unicode inválido.", e);
                    }
                    i += 4;
                }
                default -> throw new IOException("Escape JSON inválido na classificação.");
            }
        }
        throw new IOException("Texto JSON incompleto na classificação.");
    }

    private static int ignorarEspacos(String texto, int inicio) {
        int posicao = inicio;
        while (posicao < texto.length() && Character.isWhitespace(texto.charAt(posicao))) {
            posicao++;
        }
        return posicao;
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT);
    }

    private static boolean mencionaCompeticaoBrasileira(String normalizada) {
        return normalizada.contains("brasileir");
    }

    private static String nomeExibicao(String nome) {
        return normalizar(nome).equals("remo") ? "Clube do Remo" : nome;
    }

    record Time(String nome, Map<String, String> estatisticas) { }
}
