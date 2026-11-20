package server;

import model.Mensagem;

import java.util.List;

record BolaContext(List<Mensagem> historico, List<String> usuariosOnline) {
    BolaContext {
        historico = List.copyOf(historico);
        usuariosOnline = List.copyOf(usuariosOnline);
    }
}
