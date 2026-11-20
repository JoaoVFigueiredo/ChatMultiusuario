package server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BolaClassificacaoSkillTest {
    private final BolaClassificacaoSkill skill = new BolaClassificacaoSkill(2026);

    @Test
    void reconhecePedidosDaSerieAeDaSerieB() {
        assertTrue(skill.atende("classificação Série A"));
        assertTrue(skill.atende("todos os times da serie b"));
        assertTrue(skill.atende("classificação do brasileirão"));
        assertTrue(skill.atende("quais são todos os times do Brasileirão?"));
        assertTrue(skill.atende("classificação brasileira"));
    }

    @Test
    void usaSerieAComoPadraoParaClassificacaoOuTabelaSemSerie() {
        assertTrue(skill.atende("@bola classificação"));
        assertTrue(skill.atende("@bola tabela"));
        assertEquals(List.of("A"), BolaClassificacaoSkill.seriesSolicitadas("@bola classificação"));
        assertEquals(List.of("A"), BolaClassificacaoSkill.seriesSolicitadas("@bola tabela"));
    }

    @Test
    void preservaSelecaoDaSerieBDeAmbasOuDoBrasileirao() {
        assertEquals(List.of("B"),
            BolaClassificacaoSkill.seriesSolicitadas("@bola classificação Série B"));
        assertEquals(List.of("A", "B"),
            BolaClassificacaoSkill.seriesSolicitadas("@bola classificação Série A e Série B"));
        assertEquals(List.of("A", "B"),
            BolaClassificacaoSkill.seriesSolicitadas("@bola classificação do Brasileirão"));
        assertFalse(skill.atende("@bola Brasileirão"));
    }

    @Test
    void leNomesEscapadosEEstatisticasDaRespostaDaFonte() throws Exception {
        String json = """
            {"children":[{"standings":{"entries":[
              {"team":{"displayName":"Atlético \\"MG\\""},"stats":[
                {"name":"rank","displayValue":"1"},
                {"name":"points","displayValue":"74"}]},
              {"team":{"displayName":"São Paulo"},"stats":[
                {"name":"rank","displayValue":"2"},
                {"name":"points","displayValue":"70"}]}
            ]}}]}
            """;

        List<BolaClassificacaoSkill.Time> times = BolaClassificacaoSkill.lerTabela(json);

        assertEquals(2, times.size());
        assertEquals("Atlético \"MG\"", times.get(0).nome());
        assertEquals("1", times.get(0).estatisticas().get("rank"));
        assertEquals("70", times.get(1).estatisticas().get("points"));
    }

    @Test
    void exibeNomeCompletoDoRemo() throws Exception {
        List<BolaClassificacaoSkill.Time> times = BolaClassificacaoSkill.lerTabela("""
            {"entries":[
              {"team":{"displayName":"Remo"},"stats":[]},
              {"team":{"displayName":"Athletico Paranaense"},"stats":[]},
              {"team":{"displayName":"Coritiba"},"stats":[]}
            ]}
            """);

        assertEquals("Clube do Remo", times.get(0).nome());
        assertEquals("Athletico Paranaense", times.get(1).nome());
        assertEquals("Coritiba", times.get(2).nome());
    }

    @Test
    void rejeitaRespostaSemTabelaValida() {
        assertThrows(IOException.class, () -> BolaClassificacaoSkill.lerTabela("{}"));
    }
}
