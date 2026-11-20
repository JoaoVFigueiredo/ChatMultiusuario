package server;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class BolaNoticiasSkill implements BolaSkill {
    private static final String FEED = "https://news.google.com/rss/search?q=";
    private static final int MAXIMO_FEED_BYTES = 1_000_000;
    private static final int QUANTIDADE_NOTICIAS = 3;
    private static final Set<String> TERMOS_FUTEBOL = Set.of(
        "futebol", "time", "times", "brasileirao", "brasileirão", "copa", "seleção",
        "selecao", "flamengo", "corinthians", "palmeiras", "sao paulo", "são paulo",
        "santos", "vasco", "botafogo", "fluminense", "gremio", "grêmio", "internacional",
        "cruzeiro", "atletico mineiro", "atlético mineiro", "real madrid", "barcelona",
        "chelsea", "arsenal", "liverpool", "manchester", "psg", "messi", "neymar", "vini jr"
    );
    private static final Set<String> TERMOS_NOTICIAS = Set.of(
        "noticia", "notícias", "noticias", "manchete", "manchetes", "novidade",
        "novidades", "mercado da bola", "últimas", "ultimas"
    );
    private static final Set<String> TERMOS_JOGOS = Set.of(
        "jogo", "jogos", "partida", "partidas", "resultado", "resultados", "placar",
        "rodada", "agenda"
    );
    private static final Set<String> TERMOS_ATUALIDADE = Set.of(
        "hoje", "ontem", "amanhã", "amanha", "agora", "próximo", "proximo",
        "próxima", "proxima", "ao vivo", "quem ganhou", "quem venceu", "placar",
        "resultado", "resultados"
    );
    private static final Set<String> TERMOS_CLIMA = Set.of(
        "clima", "tempo", "previsão", "previsao", "chuva", "temperatura"
    );
    private static final Set<String> PALAVRAS_DE_PERGUNTA = Set.of(
        "quem", "que", "qual", "quais", "quantos", "quantas", "quanto", "quanta",
        "como", "quando", "onde", "porque", "porquê", "explique", "explica", "história",
        "historia", "regra", "regras"
    );
    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    @Override
    public boolean atende(String consulta) {
        String normalizada = consulta.toLowerCase(Locale.ROOT);
        if (TERMOS_CLIMA.stream().anyMatch(normalizada::contains)) return false;
        if (TERMOS_NOTICIAS.stream().anyMatch(normalizada::contains)) return true;
        boolean consultaDeJogo = TERMOS_JOGOS.stream().anyMatch(normalizada::contains);
        boolean pedeAtualidade = TERMOS_ATUALIDADE.stream().anyMatch(normalizada::contains);
        boolean mencionaFutebol = TERMOS_FUTEBOL.stream().anyMatch(normalizada::contains);
        if (consultaDeJogo && (pedeAtualidade || mencionaFutebol)) return true;
        if (normalizada.contains("?")) return false;
        String[] palavras = normalizada.trim().split("\\s+");
        if (palavras.length > 4 || (palavras.length > 0
                && PALAVRAS_DE_PERGUNTA.contains(palavras[0]))) return false;
        return TERMOS_FUTEBOL.stream().anyMatch(normalizada::contains);
    }

    @Override
    public List<String> executar(String consulta, BolaContext contexto)
            throws IOException, InterruptedException {
        String assunto = consulta.replaceAll("(?i)\\b(tem|quero|mostre|mostra|buscar|busque|"
            + "notícia|notícias|noticia|noticias|manchetes?|novidades|últimas|ultimas)\\b", " ")
            .replaceAll("(?i)\\b(do|da|de|sobre|para|por|as|os|a|o)\\b", " ")
            .replaceAll("\\s+", " ").trim();
        if (assunto.isBlank() || assunto.equalsIgnoreCase("time")
                || assunto.equalsIgnoreCase("qual time")
                || assunto.equalsIgnoreCase("qual o time")) {
            assunto = "futebol brasileiro";
        }
        return buscar(assunto);
    }

    private List<String> buscar(String assunto) throws IOException, InterruptedException {
        String query = "futebol " + assunto;
        URI uri = URI.create(FEED + URLEncoder.encode(query, StandardCharsets.UTF_8)
            + "&hl=pt-BR&gl=BR&ceid=BR%3Apt-419");
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(15))
            .header("User-Agent", "ChatMultiusuario/1.0")
            .header("Accept", "application/rss+xml, application/xml, text/xml")
            .GET()
            .build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("A fonte de notícias respondeu HTTP " + response.statusCode() + ".");
        }
        byte[] bytes = response.body();
        if (bytes.length > MAXIMO_FEED_BYTES) {
            throw new IOException("O feed de notícias excedeu o limite permitido.");
        }
        List<Noticia> noticias = lerFeed(bytes);
        if (noticias.isEmpty()) {
            return List.of("Não encontrei notícias recentes sobre " + assunto + ".");
        }
        List<String> resposta = new ArrayList<>();
        resposta.add("Notícias recentes sobre " + assunto + " (Google Notícias; clique no link para abrir):");
        for (int i = 0; i < Math.min(QUANTIDADE_NOTICIAS, noticias.size()); i++) {
            Noticia noticia = noticias.get(i);
            String linha = (i + 1) + ". " + noticia.titulo();
            if (!noticia.fonte().isBlank()) linha += " — " + noticia.fonte();
            if (!noticia.data().isBlank()) linha += " (" + noticia.data() + ")";
            resposta.add(linha);
            resposta.add(noticia.url());
        }
        return List.copyOf(resposta);
    }

    static List<Noticia> lerFeed(byte[] bytes) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler() {
                @Override public void warning(SAXParseException e) throws SAXException { throw e; }
                @Override public void error(SAXParseException e) throws SAXException { throw e; }
                @Override public void fatalError(SAXParseException e) throws SAXException { throw e; }
            });
            Document document = builder.parse(new ByteArrayInputStream(bytes));
            NodeList items = document.getElementsByTagName("item");
            List<Noticia> noticias = new ArrayList<>();
            for (int i = 0; i < items.getLength(); i++) {
                if (!(items.item(i) instanceof Element item)) continue;
                String titulo = texto(item, "title");
                String link = texto(item, "link");
                String fonte = texto(item, "source");
                String data = formatarData(texto(item, "pubDate"));
                URI url;
                try {
                    url = URI.create(link);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                if (!titulo.isBlank() && link.length() <= 2048
                        && "https".equalsIgnoreCase(url.getScheme())
                        && "news.google.com".equalsIgnoreCase(url.getHost())) {
                    noticias.add(new Noticia(limitar(limparTitulo(titulo, fonte), 350),
                        limitar(limpar(fonte), 80), data, link));
                }
            }
            return noticias;
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Não consegui interpretar o feed de notícias.", e);
        }
    }

    private static String texto(Element pai, String nome) {
        NodeList elementos = pai.getElementsByTagName(nome);
        return elementos.getLength() == 0 ? "" : elementos.item(0).getTextContent().trim();
    }

    private static String formatarData(String data) {
        if (data.isBlank()) return "";
        try {
            return ZonedDateTime.parse(data, DateTimeFormatter.RFC_1123_DATE_TIME)
                .withZoneSameInstant(ZoneId.of("America/Sao_Paulo"))
                .format(DateTimeFormatter.ofPattern("dd/MM HH:mm", Locale.forLanguageTag("pt-BR")));
        } catch (DateTimeParseException e) {
            return "";
        }
    }

    private static String limpar(String texto) {
        return texto.replaceAll("\\s+", " ").trim();
    }

    private static String limparTitulo(String titulo, String fonte) {
        String limpo = limpar(titulo);
        if (fonte.isBlank()) return limpo;
        String nomeFonte = limpar(fonte);
        for (String separador : List.of(" — ", " – ", " - ")) {
            String sufixo = separador + nomeFonte;
            while (limpo.toLowerCase(Locale.ROOT).endsWith(sufixo.toLowerCase(Locale.ROOT))) {
                limpo = limpo.substring(0, limpo.length() - sufixo.length()).trim();
            }
        }
        return limpo;
    }

    private static String limitar(String texto, int limite) {
        return texto.length() <= limite ? texto : texto.substring(0, limite - 3) + "...";
    }

    record Noticia(String titulo, String fonte, String data, String url) { }
}
