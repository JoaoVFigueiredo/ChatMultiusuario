package server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BolaNoticiasSkillTest {
    private final BolaNoticiasSkill skill = new BolaNoticiasSkill();

    @Test
    void encaminhaNoticiasETimesParaBuscaEDeixaClimaParaOutraHabilidade() {
        assertTrue(skill.atende("notícias do Flamengo"));
        assertTrue(skill.atende("Palmeiras"));
        assertTrue(skill.atende("futebol"));
        assertTrue(skill.atende("time do Flamengo"));
        assertTrue(skill.atende("Quem ganhou o jogo do Flamengo ontem?"));
        assertTrue(skill.atende("Quais são os próximos jogos?"));
        assertFalse(skill.atende("Quantos jogadores tem o Flamengo?"));
        assertFalse(skill.atende("clima em São Paulo"));
    }

    @Test
    void interpretaMancheteFonteDataELinkDoFeed() throws Exception {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"><channel><item>
              <title>Time vence clássico - Jornal Esportivo — Jornal Esportivo</title>
              <link>https://news.google.com/noticia/123</link>
              <pubDate>Wed, 21 Oct 2026 19:13:58 GMT</pubDate>
              <source url="https://exemplo.com">Jornal Esportivo</source>
            </item></channel></rss>
            """;

        List<BolaNoticiasSkill.Noticia> noticias =
            BolaNoticiasSkill.lerFeed(xml.getBytes(StandardCharsets.UTF_8));

        assertEquals(1, noticias.size());
        assertEquals("Time vence clássico", noticias.get(0).titulo());
        assertEquals("Jornal Esportivo", noticias.get(0).fonte());
        assertEquals("https://news.google.com/noticia/123", noticias.get(0).url());
        assertEquals("21/10 16:13", noticias.get(0).data());
    }

    @Test
    void ignoraLinksQueNaoSaoDaFonteEsperada() throws Exception {
        String xml = """
            <rss><channel>
              <item><title>Link externo</title><link>https://exemplo.com/noticia</link></item>
              <item><title>Link seguro</title><link>https://news.google.com/noticia/123</link></item>
            </channel></rss>
            """;

        List<BolaNoticiasSkill.Noticia> noticias =
            BolaNoticiasSkill.lerFeed(xml.getBytes(StandardCharsets.UTF_8));

        assertEquals(1, noticias.size());
        assertEquals("Link seguro", noticias.get(0).titulo());
    }

    @Test
    void rejeitaFeedComDoctype() {
        String xml = "<!DOCTYPE rss [<!ENTITY xxe SYSTEM \"file:///windows/win.ini\">]>"
            + "<rss><channel><item><title>&xxe;</title></item></channel></rss>";

        assertThrows(IOException.class,
            () -> BolaNoticiasSkill.lerFeed(xml.getBytes(StandardCharsets.UTF_8)));
    }
}
