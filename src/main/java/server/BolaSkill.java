package server;

import java.io.IOException;
import java.util.List;

interface BolaSkill {
    boolean atende(String consulta);

    List<String> executar(String consulta, BolaContext contexto)
            throws IOException, InterruptedException;
}
