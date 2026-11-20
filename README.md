# Chat Multiusuário com Histórico

## Rodar no VS Code
1. JDK 25+ e Maven 3.9+ (recomendado), além da extensão **Extension Pack for Java**.
2. `File > Open Folder` na pasta que contém o `pom.xml`.
3. Abra `server/Servidor.java` e clique em **Run**. O console mostra o **IP** e o **CÓDIGO DA SALA**.
4. Abra `client/ClienteGUI.java` e clique em **Run** (uma janela por pessoa).
5. Testes: ícone de frasco (Testing) ou `mvn test`.

O projeto configura automaticamente o acesso nativo exigido pelo SQLite no JDK 25 e o provedor de logging do driver.

## Convidar alguém
- **Mesma rede (Wi-Fi):** passe o CÓDIGO DA SALA (ou o IP). A pessoa cola na tela de login.
  Se o Windows perguntar sobre o Firewall ao ligar o servidor, clique em "Permitir".
- **Pela internet:** o servidor precisa ser alcançável de fora. Opções:
  - Tailscale (todos instalam e entram na mesma rede privada; use o IP 100.x.x.x);
  - ngrok: `ngrok tcp 5000`, e passe o endereço `0.tcp.ngrok.io:PORTA` que ele mostrar.
  Em ambos, a pessoa cola o IP ou endereço:porta no campo da sala.

## No chat
- Servidor e canal são campos separados. Quem escolher o mesmo servidor e canal conversa junto; por exemplo, `geral` e `estudos` têm mensagens, histórico e contagem online separados.
- Ao iniciar a interface, escolha entre carregar as últimas mensagens da sala ou começar sem exibir o histórico anterior. Essa escolha não apaga as mensagens salvas no servidor.
- O título mostra o canal e quantas pessoas estão nele. A indicação de digitação aparece abaixo da conversa; mensagens recebidas de outras pessoas emitem um aviso sonoro.
- Fundo: temas animados, cor sólida, tema escuro ou campo de futebol (a escolha é local).
- Nome, servidor, canal e cor do texto ficam preenchidos na próxima abertura.
- Emojis: botão "Emoji" com categorias ou atalhos como :) :D <3 (y) :fogo: :festa:

## Comandos e recursos
- **Bola:** escreva `@bola pergunta` ou clique em **Chamar o Bola**. Perguntas livres sobre futebol usam `GROQ_API_KEY`/`GROQ_MODEL` quando configurados; sem Groq, configure `BOLA_TOKEN`/`BOLA_MODEL` da Hugging Face. Pedidos de notícias e jogos recentes consultam manchetes com fonte e link. Consulte as tabelas completas e atuais com `@bola classificação série A`, `@bola classificação série B`, `@bola classificação do Brasileirão` ou `@bola todos os times do Brasileirão` (dados da ESPN; exige internet). Também aceita `@bola resumo`, `@bola clima`, `@bola palpite Brasil x Argentina` e `@bola sortear`.
- **Assistente IA:** abra a janela **Assistente IA** no menu para conversar sobre estudos, escrita, tecnologia e dúvidas gerais. As perguntas e respostas dessa janela são privadas e não aparecem no painel público; mensagens normais no canal nunca acionam a IA. A Assistente IA usa `GROQ_API_KEY` e `GROQ_MODEL` do `.env` quando a chave Groq estiver configurada; caso contrário, usa `ASSISTENTE_IA_TOKEN` e `ASSISTENTE_IA_MODEL` da Hugging Face.
- **Foto de perfil:** clique no menu ao lado do seu nome para escolher JPG, JPEG ou PNG (WEBP quando houver suporte no Java). O servidor armazena a imagem como BLOB no SQLite (até 256 KB), evitando caminhos locais que não funcionariam nos outros dispositivos. A foto é sincronizada com os usuários conectados; clique em qualquer avatar para ampliar, use a roda do mouse ou os botões para ajustar o zoom. O menu permite alterar ou remover a foto. Sem imagem, é exibido um avatar padrão.
- **Personalizar conversa:** em **Configurações** ou pelo ícone de imagem no cabeçalho, selecione uma imagem local ou uma opção da galeria, confira a prévia e ajuste a camada escura antes de aplicar. O papel de parede mantém a proporção e preenche somente a área das mensagens. Cada usuário e combinação de servidor/canal tem sua própria escolha, salva localmente para continuar após reiniciar. **Remover papel de parede** mantém o tema atual; **Restaurar fundo original** também volta ao tema Messenger padrão.
- **Busca:** `/buscar palavra` procura mensagens públicas antigas do canal atual (até 50 resultados). O botão **Buscar** também pode ser usado. Mensagens privadas não são pesquisadas.
- **Privado:** `/p nome mensagem` ou o botão **Privado** envia uma mensagem somente para a pessoa indicada no mesmo canal. Mensagens privadas não são salvas no histórico.
- **Pessoas online:** a lista lateral mostra quem está conectado ao canal e atualiza quando alguém entra ou sai.
- **Vários usuários no mesmo computador:** use **Abrir outro usuário** no menu lateral para abrir uma instância separada do cliente sem substituir a janela atual. Entre com um nome diferente e o mesmo servidor/canal.
- **Notificações:** mensagens públicas recebidas e mensagens privadas destinadas a você avisam quando a janela está em segundo plano. O aviso do sistema identifica o remetente e o canal, sem mostrar o conteúdo privado; ao voltar para a janela, o contador de novas mensagens é limpo.
- **Sair do canal:** use **Sair do canal** para desconectar e voltar à tela de entrada.
- **Anexos:** **Anexar foto/arquivo** envia um arquivo de até 10 MB às outras pessoas conectadas no mesmo canal. Fotos e arquivos aparecem como links para salvar; o receptor escolhe onde gravá-los. Os anexos não são salvos no banco nem no disco do servidor e ficam temporariamente na memória dos clientes.
- `/quem` mostra quem está no canal; `/ajuda` lista os comandos. O nome **Bola** é reservado.

### Configurar as integrações de IA (opcional)
O servidor carrega `.env` da raiz do projeto ao iniciar; variáveis de ambiente com os mesmos nomes podem sobrescrever os valores do arquivo. Para usar Groq na Assistente IA e na Bola, configure `GROQ_API_KEY` e `GROQ_MODEL`; o Groq é priorizado se a chave estiver preenchida. Use um ID de modelo ativo listado no [catálogo da Groq](https://console.groq.com/docs/models). O modelo padrão se `GROQ_MODEL` estiver vazio é `openai/gpt-oss-20b`. Se preferir Hugging Face, configure `ASSISTENTE_IA_TOKEN` e, para perguntas livres da Bola, `BOLA_TOKEN`, com tokens fine-grained que permitam **Make calls to Inference Providers**. Reinicie o servidor depois de alterar `.env`.

Exemplo de configuração na raiz (substitua o placeholder localmente e não compartilhe o token):

```dotenv
GROQ_API_KEY=sua_chave_groq
GROQ_MODEL=openai/gpt-oss-20b
# Alternativa: Hugging Face, usada quando GROQ_API_KEY estiver vazia
ASSISTENTE_IA_TOKEN=seu_token_novo
ASSISTENTE_IA_MODEL=meta-llama/Llama-3.1-8B-Instruct:featherless-ai
```

Sem credenciais, a Bola mantém classificação online, notícias RSS e comandos locais de resumo, clima, palpite e sorteio; respostas livres precisam de `GROQ_API_KEY` ou `BOLA_TOKEN`. As manchetes vêm do Google Notícias e exigem internet. `@bola clima` informa que não há previsão do tempo em tempo real. Não coloque tokens no código, não os envie pelo chat e não versione `.env`; se um token foi exposto, revogue-o e gere outro. As chamadas a provedores podem depender de disponibilidade e cobrança.

Não registre `.env` no Git. O projeto já o ignora no `.gitignore`.
