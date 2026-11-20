package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CodigoSalaTest {
    @Test
    void gerarEDecodificarFazemIdaEVolta() {
        String codigo = CodigoSala.gerar("192.168.0.15", 5000);
        assertEquals(new CodigoSala.Endereco("192.168.0.15", 5000), CodigoSala.decodificar(codigo));
    }

    @Test
    void decodificarAceitaMinusculasEEspacos() {
        String codigo = CodigoSala.gerar("10.0.0.2", 5000);
        assertEquals("10.0.0.2", CodigoSala.decodificar(" " + codigo.toLowerCase() + " ").host());
    }

    @Test
    void ipInvalidoLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> CodigoSala.gerar("999.1.1.1", 5000));
        assertThrows(IllegalArgumentException.class, () -> CodigoSala.gerar("1.2.3", 5000));
    }

    @Test
    void interpretarAceitaIpComEPortaSemPorta() {
        assertEquals(new CodigoSala.Endereco("10.0.0.2", 6000), CodigoSala.interpretar("10.0.0.2:6000"));
        assertEquals(new CodigoSala.Endereco("10.0.0.2", 5000), CodigoSala.interpretar("10.0.0.2"));
        assertEquals(new CodigoSala.Endereco("localhost", 5000), CodigoSala.interpretar("localhost"));
    }

    @Test
    void interpretarAceitaOCodigoDaSala() {
        String codigo = CodigoSala.gerar("172.16.4.9", 5000);
        assertEquals("172.16.4.9", CodigoSala.interpretar(codigo).host());
    }
}
