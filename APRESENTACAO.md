# Guia para apresentacao — Chat Multiusuario com IA

## Preparacao
1. Instale JDK 25 e Maven 3.9+ e abra a pasta do projeto no VS Code.
2. Copie `.env.example` para `.env` na raiz e configure `GROQ_API_KEY` com uma chave Groq valida; essa chave habilita a Assistente IA e as perguntas livres do Bola.
3. Como alternativa, configure `ASSISTENTE_IA_TOKEN` e `BOLA_TOKEN` com tokens Hugging Face que permitam **Make calls to Inference Providers**; confirme que os modelos aceitam **chat completion**.
4. No terminal, execute `mvn test` e depois `mvn compile exec:java -Dexec.mainClass=server.Servidor`.
5. Em outro terminal, execute `mvn compile exec:java -Dexec.mainClass=client.ClienteGUI` (repita para abrir outro usuario).
6. Entre com dois nomes no mesmo servidor e canal, envie mensagens entre eles e confirme que elas ficam apenas no canal. Abra a janela **Assistente IA** e pergunte: `Explique programação orientada a objetos em duas frases`; a resposta deve aparecer apenas nessa janela.

## Se a IA nao responder
- Mensagem sobre token: verifique `GROQ_API_KEY` ou os tokens Hugging Face em `.env`, permissao e reinicie o servidor.
- HTTP 400 / modelo sem provedor: selecione um modelo habilitado em Inference Providers na sua conta e atualize `ASSISTENTE_IA_MODEL`.
- HTTP 401/403: token incorreto ou sem permissao.
- HTTP 429: limite de uso; aguarde.
- Falha de rede: verifique internet e firewall.
- Nao existe modo IA offline: a demonstracao depende da disponibilidade do servico remoto.

## Escopo das verificacoes
A revisao foi feita sem token pessoal nem acesso ao endpoint autenticado; portanto, a chamada real precisa ser testada na maquina de apresentacao. A validacao completa Maven tambem requer JDK 25.
