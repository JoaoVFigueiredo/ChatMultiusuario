package client;

import model.CodigoSala;
import model.Protocolo;
import model.Usuario;
import persistence.HistoricoAssistenteRepository;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JCheckBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.Scrollable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.ParagraphView;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.PopupMenu;
import java.awt.MenuItem;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.AWTException;
import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Path2D;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import javax.imageio.ImageIO;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriter;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriter;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Deque;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.prefs.Preferences;

/** Cliente com janela: fundo colorido/animado, cor de texto e painel de emojis. */
public class ClienteGUI {
    private static final String ATRIBUTO_LINK = "chatLink";
    private static final Pattern PADRAO_LINK = Pattern.compile("https?://\\S+");
    private static final Pattern PADRAO_MENSAGEM =
        Pattern.compile("^\\[(\\d{2}:\\d{2})\\]\\s+(.+?):\\s(.*)$", Pattern.DOTALL);

    private static final String[] PALETA = {
        "#E91E63", "#9C27B0", "#3F51B5", "#1E88E5",
        "#00897B", "#2E7D32", "#EF6C00", "#D32F2F"
    };
    private static final String FONTE_EMOJI = descobrirFonteEmoji();

    private final Preferences preferencias = Preferences.userNodeForPackage(ClienteGUI.class);
    private HistoricoAssistenteRepository historicoAssistente;
    private String nome = preferencias.get("nome", "");
    private String sala = preferencias.get("servidor", "localhost");
    private String salaChat = preferencias.get("salaChat", Protocolo.SALA_PADRAO);
    private String corTexto = preferencias.get("corTexto", Usuario.COR_PADRAO);
    private boolean lembrarDados = preferencias.getBoolean("lembrarDados", true);

    private ClienteChat cliente;
    private JFrame janela;
    private FundoPanel fundo;
    private ChatFeed area;
    private JPanel cartaoBoasVindas;
    private JDialog janelaBola;
    private JPanel mensagensBola;
    private JScrollPane rolagemBola;
    private JLabel estadoBola;
    private JPanel saudacaoBola;
    private JDialog janelaAssistente;
    private JPanel mensagensAssistente;
    private JScrollPane rolagemAssistente;
    private JTextField campoAssistente;
    private JScrollPane rolagemMensagens;
    private JPanel painelConversaTema;
    private JPanel painelCabecalhoTema;
    private JPanel painelOnlineTema;
    private JPanel painelCentroTema;
    private PapelParedeViewport viewportConversa;
    private BufferedImage papelParedeAtual;
    private int escurecimentoPapelParede;
    private String chavePapelParede;
    private JComboBox<Tema> comboTemaFundo;
    private JTextField campo;
    private JLabel estadoDigitacao;
    private JLabel rotuloOnline;
    private JLabel rotuloCanalOnline;
    private JPopupMenu popupEmoji;
    private JButton botaoSino;
    private final Deque<String> notificacoes = new ArrayDeque<>();
    private JWindow janelaNotificacao;
    private javax.swing.Timer timerNotificacao;
    private volatile boolean saindo = false;
    private boolean avisandoDigitacao;
    private final Set<String> usuariosDigitando = new HashSet<>();
    private final DefaultListModel<String> modeloUsuariosOnline = new DefaultListModel<>();
    private final Map<String, Protocolo.Anexo> anexosRecebidos = new LinkedHashMap<>();
    private final Map<String, BufferedImage> fotosUsuarios = new LinkedHashMap<>();
    private final Map<String, java.util.List<AvatarLabel>> avataresRenderizados = new LinkedHashMap<>();
    private int pessoasOnline = 1;
    private JList<String> listaUsuariosOnline;
    private AvatarLabel avatarPerfil;
    private BufferedImage fotoPerfilLegada;
    private boolean avatarProprioRecebido;
    private javax.swing.Timer timerDigitando;
    private boolean carregarHistorico = true;
    private int mensagensNaoLidas;
    private TrayIcon iconeBandeja;
    private String anexoProximaMensagem;

    public static void main(String[] args) {
        boolean novoUsuario = args.length > 0 && args[0].equals("--novo-usuario");
        SwingUtilities.invokeLater(() -> {
            ClienteGUI cliente = new ClienteGUI();
            if (novoUsuario) cliente.nome = "";
            cliente.iniciar();
        });
    }

    // ---------------------------------------------------------------- login
    private void iniciar() {
        int escolhaHistorico = escolherInicio();
        if (escolhaHistorico < 0) {
            System.exit(0);
        }
        carregarHistorico = escolhaHistorico == 0;
        try {
            historicoAssistente = new HistoricoAssistenteRepository(
                Path.of(System.getProperty("user.home"), ".chat-multiusuario",
                    "historico-assistente.db"));
        } catch (IllegalStateException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(),
                "Erro ao preparar o histórico da Assistente IA", JOptionPane.ERROR_MESSAGE);
            return;
        }

        while (true) {
            if (!pedirLogin()) {
                System.exit(0);
            }
            if (nome.trim().equalsIgnoreCase("Bola")) {
                JOptionPane.showMessageDialog(null, "O nome Bola é reservado para o assistente de futebol.",
                    "Nome reservado", JOptionPane.WARNING_MESSAGE);
                continue;
            }
            try {
                Usuario usuario = new Usuario(nome, corTexto);
                CodigoSala.Endereco end = CodigoSala.interpretar(sala);
                salaChat = Protocolo.normalizarSala(salaChat);
                chavePapelParede = criarChavePapelParede(
                    end.host() + ":" + end.porta(), usuario.getNome(), salaChat);
                carregarPapelParedeSalvo();
                cliente = new ClienteChat(end.host(), end.porta(), usuario, salaChat);
                fotoPerfilLegada = carregarFotoPerfilSalva(usuario.getNome());
                if (fotoPerfilLegada != null) {
                    fotosUsuarios.put(usuario.getNome().toLowerCase(java.util.Locale.ROOT),
                        fotoPerfilLegada);
                }
                cliente.conectar(this::aoReceber, this::aoDesconectar, carregarHistorico);
                preferencias.putBoolean("lembrarDados", lembrarDados);
                if (lembrarDados) {
                    preferencias.put("nome", nome.trim());
                    preferencias.put("servidor", sala.trim());
                    preferencias.put("salaChat", salaChat);
                } else {
                    preferencias.remove("nome");
                    preferencias.remove("servidor");
                    preferencias.remove("salaChat");
                }
                preferencias.put("corTexto", corTexto);
                montarJanela(usuario);
                return;
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "Dados inválidos",
                    JOptionPane.WARNING_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null,
                    "Não consegui conectar ao servidor.\nConfira o código/IP e se o servidor está ligado.",
                    "Erro de conexão", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private int escolherInicio() {
        int[] escolha = {-1};
        JDialog dialogo = new JDialog((Frame) null, "Iniciar conversa", true);
        JPanel painel = painelEntrada();
        painel.setLayout(new BorderLayout(0, 18));
        painel.add(cabecalhoEntrada("Como iniciar o chat",
            "Escolha como quer abrir sua conversa."), BorderLayout.NORTH);

        JPanel opcoes = new JPanel(new GridLayout(2, 1, 0, 10));
        opcoes.setOpaque(false);
        JButton voltar = botaoOpcaoEntrada("Voltar à conversa antiga",
            "Carregar as mensagens recentes de #" + escaparHtml(salaChat));
        voltar.addActionListener(e -> {
            escolha[0] = 0;
            dialogo.dispose();
        });
        JButton nova = botaoOpcaoEntrada("Iniciar uma nova conversa",
            "Entrar sem exibir mensagens anteriores. O histórico salvo não será apagado.");
        nova.addActionListener(e -> {
            escolha[0] = 1;
            dialogo.dispose();
        });
        opcoes.add(voltar);
        opcoes.add(nova);
        painel.add(opcoes, BorderLayout.CENTER);

        dialogo.setContentPane(painel);
        dialogo.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialogo.pack();
        dialogo.setMinimumSize(new Dimension(490, 310));
        dialogo.setLocationRelativeTo(null);
        dialogo.setVisible(true);
        return escolha[0];
    }

    private void perguntarBola() {
        if (janelaBola == null) criarJanelaBola();
        janelaBola.setVisible(true);
        janelaBola.toFront();
        campoBola.requestFocusInWindow();
    }

    private JTextField campoBola;

    private void criarJanelaBola() {
        janelaBola = new JDialog(janela, "Bola IA", false);
        janelaBola.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(new Color(0x0B1020));

        JPanel lateral = new JPanel(new BorderLayout(0, 18));
        lateral.setPreferredSize(new Dimension(218, 0));
        lateral.setBackground(new Color(0x0E1428));
        lateral.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0x202B48)),
            BorderFactory.createEmptyBorder(18, 14, 16, 14)));

        JPanel marca = new JPanel(new BorderLayout(10, 0));
        marca.setOpaque(false);
        MascoteBola mascoteMarca = new MascoteBola(42);
        marca.add(mascoteMarca, BorderLayout.WEST);
        JPanel nomesMarca = new JPanel(new GridLayout(2, 1, 0, 3));
        nomesMarca.setOpaque(false);
        JLabel nomeMarca = new JLabel("Bola IA");
        nomeMarca.setForeground(new Color(0xF3F4FF));
        nomeMarca.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        JLabel descricaoMarca = new JLabel("ASSISTENTE DE FUTEBOL");
        descricaoMarca.setForeground(new Color(0x8996B8));
        descricaoMarca.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        nomesMarca.add(nomeMarca);
        nomesMarca.add(descricaoMarca);
        marca.add(nomesMarca, BorderLayout.CENTER);

        JPanel menu = new JPanel();
        menu.setOpaque(false);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        JLabel tituloMenu = new JLabel("  ESPAÇO BOLA");
        tituloMenu.setForeground(new Color(0x8792B0));
        tituloMenu.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        tituloMenu.setBorder(BorderFactory.createEmptyBorder(8, 0, 10, 0));
        tituloMenu.setAlignmentX(Component.LEFT_ALIGNMENT);
        menu.add(tituloMenu);

        CardLayout layoutPaginas = new CardLayout();
        JPanel paginas = new JPanel(layoutPaginas);
        paginas.setOpaque(false);
        JPanel conversa = criarPainelConversaBola();
        paginas.add(conversa, "conversa");
        paginas.add(criarPaginaBola("Funcionalidades",
            "Pergunte sobre notícias e assuntos de futebol, peça um resumo ou explore "
                + "os comandos disponíveis no canal. As respostas continuam aparecendo "
                + "também na conversa principal do Messenger."), "funcionalidades");
        paginas.add(criarPaginaBola("Configurações",
            "A conexão, as respostas e as configurações atuais do Messenger foram preservadas. "
                + "Esta janela é apenas uma nova apresentação visual do Bola IA."), "configuracoes");
        paginas.add(criarPaginaBola("Sobre o Bola IA",
            "Seu assistente de futebol, com visual próprio e integrado ao chat atual."), "sobre");

        JButton conversaBotao = botaoMenuBola("◉   Conversa", true);
        conversaBotao.addActionListener(e -> {
            layoutPaginas.show(paginas, "conversa");
            campoBola.requestFocusInWindow();
        });
        JButton historicoBotao = botaoMenuBola("◷   Histórico", false);
        historicoBotao.addActionListener(e -> {
            layoutPaginas.show(paginas, "conversa");
            if (rolagemBola != null) {
                rolagemBola.getVerticalScrollBar().setValue(0);
            }
            estadoBola.setText("Histórico desta janela; as mensagens também estão no canal.");
        });
        JButton funcionalidadesBotao = botaoMenuBola("✦   Funcionalidades", false);
        funcionalidadesBotao.addActionListener(e -> layoutPaginas.show(paginas, "funcionalidades"));
        JButton configuracoesBotao = botaoMenuBola("⚙   Configurações", false);
        configuracoesBotao.addActionListener(e -> layoutPaginas.show(paginas, "configuracoes"));
        JButton sobreBotao = botaoMenuBola("ⓘ   Sobre", false);
        sobreBotao.addActionListener(e -> layoutPaginas.show(paginas, "sobre"));
        for (JButton item : new JButton[] {
                conversaBotao, historicoBotao, funcionalidadesBotao, configuracoesBotao, sobreBotao}) {
            item.setAlignmentX(Component.LEFT_ALIGNMENT);
            menu.add(item);
            menu.add(Box.createVerticalStrut(5));
        }

        JPanel lateralConteudo = new JPanel(new BorderLayout(0, 20));
        lateralConteudo.setOpaque(false);
        lateralConteudo.add(marca, BorderLayout.NORTH);
        lateralConteudo.add(menu, BorderLayout.CENTER);
        JPanel identificacao = new JPanel(new BorderLayout(9, 0));
        identificacao.setBackground(new Color(0x171F36));
        identificacao.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x283657)),
            BorderFactory.createEmptyBorder(9, 9, 9, 9)));
        identificacao.add(new MascoteBola(32), BorderLayout.WEST);
        JLabel identidadeTexto = new JLabel("<html><b>Bola IA</b><br><font color='#8F9BB8'>"
            + "Sempre no seu time</font></html>");
        identidadeTexto.setForeground(new Color(0xE7EAF7));
        identidadeTexto.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        identificacao.add(identidadeTexto, BorderLayout.CENTER);
        lateral.add(lateralConteudo, BorderLayout.CENTER);
        lateral.add(identificacao, BorderLayout.SOUTH);

        raiz.add(lateral, BorderLayout.WEST);
        raiz.add(paginas, BorderLayout.CENTER);
        janelaBola.setContentPane(raiz);
        janelaBola.setMinimumSize(new Dimension(820, 560));
        janelaBola.setSize(1000, 680);
        janelaBola.setLocationRelativeTo(janela);
    }

    private JPanel criarPainelConversaBola() {
        JPanel painel = new JPanel(new BorderLayout(0, 12));
        painel.setOpaque(false);
        painel.setBorder(BorderFactory.createEmptyBorder(18, 22, 16, 22));

        JPanel cabecalho = new JPanel(new BorderLayout(12, 0));
        cabecalho.setOpaque(false);
        JPanel identidade = new JPanel(new BorderLayout(10, 0));
        identidade.setOpaque(false);
        identidade.add(new MascoteBola(48), BorderLayout.WEST);
        JPanel textosCabecalho = new JPanel(new GridLayout(2, 1, 0, 3));
        textosCabecalho.setOpaque(false);
        JLabel titulo = new JLabel("Bola IA");
        titulo.setForeground(new Color(0xF3F4FF));
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        JLabel subtitulo = new JLabel("Seu assistente de futebol");
        subtitulo.setForeground(new Color(0x96A2C0));
        subtitulo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        textosCabecalho.add(titulo);
        textosCabecalho.add(subtitulo);
        identidade.add(textosCabecalho, BorderLayout.CENTER);
        cabecalho.add(identidade, BorderLayout.WEST);

        JButton novaConversa = new JButton("＋  Nova conversa");
        novaConversa.setForeground(new Color(0xE8E7FF));
        novaConversa.setBackground(new Color(0x201D3C));
        novaConversa.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        novaConversa.setFocusPainted(false);
        novaConversa.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        novaConversa.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x5541A5)),
            BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        novaConversa.addActionListener(e -> limparConversaBola());
        cabecalho.add(novaConversa, BorderLayout.EAST);

        mensagensBola = new JPanel();
        mensagensBola.setOpaque(false);
        mensagensBola.setLayout(new BoxLayout(mensagensBola, BoxLayout.Y_AXIS));
        saudacaoBola = criarSaudacaoBola();
        mensagensBola.add(saudacaoBola);
        mensagensBola.add(Box.createVerticalGlue());
        rolagemBola = new JScrollPane(mensagensBola);
        rolagemBola.setBorder(BorderFactory.createEmptyBorder());
        rolagemBola.setOpaque(false);
        rolagemBola.getViewport().setOpaque(false);
        rolagemBola.getVerticalScrollBar().setUnitIncrement(18);

        JPanel entrada = new JPanel(new BorderLayout(10, 0));
        entrada.setOpaque(false);
        entrada.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x2B3654)),
            BorderFactory.createEmptyBorder(9, 11, 9, 9)));
        campoBola = new JTextField();
        campoBola.setForeground(new Color(0xF2F3FF));
        campoBola.setCaretColor(new Color(0xA78BFA));
        campoBola.setBackground(new Color(0x11182B));
        campoBola.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        campoBola.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x323E5E)),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        campoBola.setToolTipText("Digite sua pergunta para o Bola");
        campoBola.addActionListener(e -> enviarPerguntaBola());
        JButton enviar = new JButton("Enviar  ➤");
        enviar.setForeground(Color.WHITE);
        enviar.setBackground(new Color(0x7045F5));
        enviar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        enviar.setFocusPainted(false);
        enviar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        enviar.setBorder(BorderFactory.createEmptyBorder(11, 16, 11, 16));
        enviar.addActionListener(e -> enviarPerguntaBola());
        entrada.add(campoBola, BorderLayout.CENTER);
        entrada.add(enviar, BorderLayout.EAST);

        estadoBola = new JLabel("Suas perguntas continuam sendo enviadas pelo canal atual.");
        estadoBola.setForeground(new Color(0x8995B4));
        estadoBola.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        JPanel rodape = new JPanel(new BorderLayout(0, 7));
        rodape.setOpaque(false);
        rodape.add(entrada, BorderLayout.CENTER);
        rodape.add(estadoBola, BorderLayout.SOUTH);

        painel.add(cabecalho, BorderLayout.NORTH);
        painel.add(rolagemBola, BorderLayout.CENTER);
        painel.add(rodape, BorderLayout.SOUTH);
        return painel;
    }

    private JPanel criarSaudacaoBola() {
        RoundedPanel boasVindas = new RoundedPanel(new BorderLayout(0, 10));
        boasVindas.setBackground(new Color(0x111A31));
        boasVindas.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x343B69)),
            BorderFactory.createEmptyBorder(22, 24, 22, 24)));
        boasVindas.setAlignmentX(Component.CENTER_ALIGNMENT);
        boasVindas.setMaximumSize(new Dimension(560, 250));
        JPanel conteudo = new JPanel(new BorderLayout(16, 0));
        conteudo.setOpaque(false);
        conteudo.add(new MascoteBola(88), BorderLayout.WEST);
        JPanel textos = new JPanel(new GridLayout(3, 1, 0, 7));
        textos.setOpaque(false);
        JLabel saudacao = new JLabel("Oi! Eu sou o Bola.");
        saudacao.setForeground(new Color(0xF4F2FF));
        saudacao.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        JLabel descricao = new JLabel("<html>Estou pronto para conversar sobre futebol,<br>"
            + "notícias, times e muito mais.</html>");
        descricao.setForeground(new Color(0xB7C0D8));
        descricao.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        JLabel dica = new JLabel("Faça uma pergunta para começar  ✦");
        dica.setForeground(new Color(0xA78BFA));
        dica.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        textos.add(saudacao);
        textos.add(descricao);
        textos.add(dica);
        conteudo.add(textos, BorderLayout.CENTER);
        boasVindas.add(conteudo, BorderLayout.CENTER);
        JPanel centralizado = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 28));
        centralizado.setOpaque(false);
        centralizado.add(boasVindas);
        centralizado.setAlignmentX(Component.LEFT_ALIGNMENT);
        return centralizado;
    }

    private JPanel criarPaginaBola(String titulo, String descricao) {
        JPanel pagina = new JPanel(new BorderLayout(0, 18));
        pagina.setOpaque(false);
        pagina.setBorder(BorderFactory.createEmptyBorder(34, 36, 34, 36));
        JLabel cabecalho = new JLabel(titulo);
        cabecalho.setForeground(new Color(0xF3F4FF));
        cabecalho.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        JTextArea texto = new JTextArea(descricao);
        texto.setEditable(false);
        texto.setLineWrap(true);
        texto.setWrapStyleWord(true);
        texto.setOpaque(false);
        texto.setForeground(new Color(0xB6C0D8));
        texto.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        texto.setBorder(BorderFactory.createEmptyBorder());
        RoundedPanel cartao = new RoundedPanel(new BorderLayout(0, 14));
        cartao.setBackground(new Color(0x111A31));
        cartao.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x343B69)),
            BorderFactory.createEmptyBorder(22, 24, 22, 24)));
        cartao.add(new MascoteBola(64), BorderLayout.WEST);
        cartao.add(texto, BorderLayout.CENTER);
        pagina.add(cabecalho, BorderLayout.NORTH);
        pagina.add(cartao, BorderLayout.CENTER);
        return pagina;
    }

    private static JButton botaoMenuBola(String texto, boolean ativo) {
        JButton botao = new JButton(texto);
        botao.setHorizontalAlignment(JButton.LEFT);
        botao.setForeground(ativo ? new Color(0xF4F1FF) : new Color(0xA8B1CA));
        botao.setBackground(ativo ? new Color(0x29234B) : new Color(0x0E1428));
        botao.setFont(new Font(Font.SANS_SERIF, ativo ? Font.BOLD : Font.PLAIN, 12));
        botao.setFocusPainted(false);
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        botao.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ativo ? new Color(0x51428B) : new Color(0x0E1428)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        botao.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        return botao;
    }

    private void limparConversaBola() {
        if (mensagensBola == null) return;
        mensagensBola.removeAll();
        saudacaoBola = criarSaudacaoBola();
        mensagensBola.add(saudacaoBola);
        mensagensBola.add(Box.createVerticalGlue());
        mensagensBola.revalidate();
        mensagensBola.repaint();
        if (estadoBola != null) {
            estadoBola.setText("Tela reiniciada; o histórico compartilhado do canal foi preservado.");
        }
    }

    private void enviarPerguntaBola() {
        String pergunta = campoBola.getText().trim();
        if (pergunta.isEmpty()) return;
        adicionarMensagemBola(nome, pergunta);
        campoBola.setText("");
        if (estadoBola != null) estadoBola.setText("Pergunta enviada pelo canal atual.");
        cliente.enviar("@bola " + pergunta);
    }

    private void mostrarRespostaBola(String texto) {
        if (mensagensBola == null) return;
        Matcher mensagem = PADRAO_MENSAGEM.matcher(texto);
        if (mensagem.matches() && mensagem.group(2).equalsIgnoreCase("Bola")) {
            adicionarMensagemBola("Bola IA", mensagem.group(3));
        }
    }

    private void adicionarMensagemBola(String autor, String texto) {
        if (mensagensBola == null) return;
        if (saudacaoBola != null && saudacaoBola.getParent() == mensagensBola) {
            mensagensBola.remove(saudacaoBola);
        }
        Component ultimo = mensagensBola.getComponentCount() == 0 ? null
            : mensagensBola.getComponent(mensagensBola.getComponentCount() - 1);
        if (ultimo instanceof javax.swing.Box.Filler) mensagensBola.remove(ultimo);

        boolean assistente = autor.equalsIgnoreCase("Bola") || autor.equalsIgnoreCase("Bola IA");
        JPanel linha = new JPanel(new BorderLayout(10, 0));
        linha.setOpaque(false);
        linha.setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));
        MascoteBola avatar = assistente ? new MascoteBola(34) : null;
        if (assistente) linha.add(avatar, BorderLayout.WEST);

        JPanel balao = new RoundedPanel(new BorderLayout(0, 6));
        balao.setBackground(assistente ? new Color(0x151F38) : new Color(0x2B2253));
        balao.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(assistente ? new Color(0x34446B) : new Color(0x6049AA)),
            BorderFactory.createEmptyBorder(10, 13, 10, 13)));
        JLabel nomeAutor = new JLabel(assistente ? "Bola IA" : "Você");
        nomeAutor.setForeground(assistente ? new Color(0x78C8FF) : new Color(0xC4B5FD));
        nomeAutor.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        JTextArea conteudo = new JTextArea(texto);
        conteudo.setEditable(false);
        conteudo.setLineWrap(true);
        conteudo.setWrapStyleWord(true);
        conteudo.setOpaque(false);
        conteudo.setForeground(new Color(0xE7EAF6));
        conteudo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        int larguraTexto = rolagemBola == null ? 520
            : Math.max(220, Math.min(560, rolagemBola.getViewport().getWidth() - 110));
        int colunas = Math.max(25, larguraTexto / 8);
        int linhas = 0;
        for (String linhaTexto : texto.split("\\R", -1)) {
            linhas += Math.max(1, (linhaTexto.length() + colunas - 1) / colunas);
        }
        conteudo.setPreferredSize(new Dimension(larguraTexto, Math.max(22, linhas * 19)));
        balao.add(nomeAutor, BorderLayout.NORTH);
        balao.add(conteudo, BorderLayout.CENTER);
        balao.setMaximumSize(new Dimension(650, Integer.MAX_VALUE));
        if (assistente) linha.add(balao, BorderLayout.CENTER);
        else linha.add(balao, BorderLayout.EAST);
        linha.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        linha.setAlignmentX(Component.LEFT_ALIGNMENT);
        mensagensBola.add(linha);
        mensagensBola.add(Box.createVerticalStrut(8));
        mensagensBola.revalidate();
        mensagensBola.repaint();
        SwingUtilities.invokeLater(() -> {
            if (rolagemBola != null) {
                rolagemBola.getVerticalScrollBar().setValue(
                    rolagemBola.getVerticalScrollBar().getMaximum());
            }
        });
    }

    private void perguntarAssistente() {
        if (janelaAssistente == null) criarJanelaAssistente();
        janelaAssistente.setVisible(true);
        janelaAssistente.toFront();
        campoAssistente.requestFocusInWindow();
    }

    private void criarJanelaAssistente() {
        janelaAssistente = new JDialog(janela, "Assistente IA", false);
        janelaAssistente.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout(0, 12));
        raiz.setBackground(new Color(0x101321));
        raiz.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JPanel cabecalho = new JPanel(new BorderLayout(10, 0));
        cabecalho.setBackground(new Color(0x191E32));
        cabecalho.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x34304F)),
            BorderFactory.createEmptyBorder(12, 14, 12, 10)));
        JPanel identificacao = new JPanel(new GridLayout(2, 1, 0, 4));
        identificacao.setOpaque(false);
        JLabel titulo = new JLabel("✧  Assistente IA");
        titulo.setForeground(new Color(0xE5D9FF));
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        JLabel descricao = new JLabel("Converse sobre estudos, escrita, ideias e dúvidas gerais.");
        descricao.setForeground(new Color(0xA8ADC2));
        descricao.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        identificacao.add(titulo);
        identificacao.add(descricao);
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        acoes.setOpaque(false);
        JButton historico = botaoEstilizado("Histórico");
        historico.setToolTipText("Ver conversas anteriores salvas");
        historico.addActionListener(e -> mostrarHistoricoAssistente(janelaAssistente));
        JButton fechar = botaoEstilizado("Fechar");
        fechar.setToolTipText("Fechar esta janela");
        fechar.addActionListener(e -> janelaAssistente.setVisible(false));
        acoes.add(historico);
        acoes.add(fechar);
        cabecalho.add(identificacao, BorderLayout.CENTER);
        cabecalho.add(acoes, BorderLayout.EAST);

        mensagensAssistente = new JPanel();
        mensagensAssistente.setLayout(new BoxLayout(mensagensAssistente, BoxLayout.Y_AXIS));
        mensagensAssistente.setBackground(new Color(0x101321));
        mensagensAssistente.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
        rolagemAssistente = new JScrollPane(mensagensAssistente);
        rolagemAssistente.setBorder(BorderFactory.createLineBorder(new Color(0x272B40)));
        rolagemAssistente.getViewport().setBackground(new Color(0x101321));
        rolagemAssistente.getVerticalScrollBar().setUnitIncrement(18);

        campoAssistente = new JTextField();
        campoAssistente.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        campoAssistente.setBackground(new Color(0x191E32));
        campoAssistente.setForeground(new Color(0xE5E7EB));
        campoAssistente.setCaretColor(Color.WHITE);
        campoAssistente.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x343B5B)),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        campoAssistente.setToolTipText("Digite sua pergunta e pressione Enter");
        JButton enviar = botaoEstilizado("➤");
        enviar.setBackground(new Color(0x7C4DFF));
        enviar.setForeground(Color.WHITE);
        enviar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        enviar.setToolTipText("Enviar pergunta");
        enviar.addActionListener(e -> enviarPerguntaAssistente());
        campoAssistente.addActionListener(e -> enviarPerguntaAssistente());
        JPanel entrada = new JPanel(new BorderLayout(8, 0));
        entrada.setOpaque(false);
        entrada.add(campoAssistente, BorderLayout.CENTER);
        entrada.add(enviar, BorderLayout.EAST);

        raiz.add(cabecalho, BorderLayout.NORTH);
        raiz.add(rolagemAssistente, BorderLayout.CENTER);
        raiz.add(entrada, BorderLayout.SOUTH);
        janelaAssistente.setContentPane(raiz);
        janelaAssistente.setMinimumSize(new Dimension(620, 480));
        janelaAssistente.setSize(800, 620);
        janelaAssistente.setLocationRelativeTo(janela);
        carregarHistoricoAssistenteNaJanela();
    }

    private void enviarPerguntaAssistente() {
        String pergunta = campoAssistente.getText().trim();
        if (pergunta.isEmpty()) return;
        try {
            cliente.enviarPerguntaAssistente(pergunta);
            adicionarMensagemAssistente("Você", pergunta);
            campoAssistente.setText("");
            campoAssistente.requestFocusInWindow();
        } catch (IllegalArgumentException | IOException e) {
            JOptionPane.showMessageDialog(janelaAssistente, e.getMessage(),
                "Não foi possível enviar a pergunta", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void carregarHistoricoAssistenteNaJanela() {
        try {
            List<HistoricoAssistenteRepository.Conversa> conversas =
                historicoAssistente.buscar(nome, sala, salaChat);
            for (HistoricoAssistenteRepository.Conversa conversa : conversas) {
                adicionarMensagemAssistente("Você", conversa.pergunta());
                adicionarMensagemAssistente("Assistente IA", conversa.resposta());
            }
            if (conversas.isEmpty()) {
                adicionarMensagemAssistente("Assistente IA",
                    "Olá! Sou a Assistente IA. Pode me perguntar sobre estudos, escrita, "
                        + "tecnologia, ideias ou qualquer dúvida geral. O que gostaria de saber?");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            JOptionPane.showMessageDialog(janelaAssistente, e.getMessage(),
                "Não foi possível carregar o histórico", JOptionPane.ERROR_MESSAGE);
            adicionarMensagemAssistente("Assistente IA",
                "Olá! Pode escrever sua pergunta no campo abaixo e enviá-la para começarmos.");
        }
    }

    private void adicionarMensagemAssistente(String autor, String conteudo) {
        if (mensagensAssistente == null) return;
        boolean assistente = autor.equalsIgnoreCase("Assistente IA");
        JPanel linha = new JPanel(new BorderLayout());
        linha.setOpaque(false);
        linha.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        JPanel balao = new RoundedPanel(new BorderLayout());
        balao.setBackground(assistente ? new Color(0x191E32) : new Color(0x29234F));
        balao.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(
                assistente ? new Color(0x34304F) : new Color(0x6049AA)),
            BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        JLabel rotulo = new JLabel(assistente ? "✧  Assistente IA" : "Você");
        rotulo.setForeground(assistente ? new Color(0xC4B5FD) : new Color(0xAFCBFF));
        rotulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        JTextArea texto = new JTextArea(conteudo);
        texto.setEditable(false);
        texto.setLineWrap(true);
        texto.setWrapStyleWord(true);
        texto.setColumns(58);
        texto.setRows(Math.max(1, (conteudo.length() + 69) / 70));
        texto.setOpaque(false);
        texto.setForeground(new Color(0xE7E9F2));
        texto.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        texto.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        balao.add(rotulo, BorderLayout.NORTH);
        balao.add(texto, BorderLayout.CENTER);
        balao.setMaximumSize(new Dimension(620, Integer.MAX_VALUE));
        if (assistente) linha.add(balao, BorderLayout.WEST);
        else linha.add(balao, BorderLayout.EAST);
        mensagensAssistente.add(linha);
        mensagensAssistente.revalidate();
        mensagensAssistente.repaint();
        SwingUtilities.invokeLater(() -> {
            if (rolagemAssistente != null) {
                rolagemAssistente.getVerticalScrollBar().setValue(
                    rolagemAssistente.getVerticalScrollBar().getMaximum());
            }
        });
    }

    private void mostrarHistoricoAssistente(JDialog dono) {
        List<HistoricoAssistenteRepository.Conversa> conversas;
        try {
            conversas = historicoAssistente.buscar(nome, sala, salaChat);
        } catch (IllegalArgumentException | IllegalStateException e) {
            JOptionPane.showMessageDialog(janela, e.getMessage(),
                "Não foi possível carregar o histórico", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JTextArea texto = new JTextArea();
        texto.setEditable(false);
        texto.setLineWrap(true);
        texto.setWrapStyleWord(true);
        texto.setBackground(new Color(0x171827));
        texto.setForeground(new Color(0xE7E9F2));
        texto.setCaretColor(Color.WHITE);
        texto.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        texto.setMargin(new Insets(12, 14, 12, 14));
        if (conversas.isEmpty()) {
            texto.setText("Ainda não há conversas salvas com a Assistente IA.");
        } else {
            java.time.format.DateTimeFormatter formato =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            StringBuilder conteudo = new StringBuilder();
            for (HistoricoAssistenteRepository.Conversa conversa : conversas) {
                conteudo.append("Você · ")
                    .append(conversa.horario().format(formato)).append('\n')
                    .append(conversa.pergunta()).append("\n\n")
                    .append("Assistente IA\n")
                    .append(conversa.resposta()).append("\n\n")
                    .append("────────────────────────\n\n");
            }
            texto.setText(conteudo.toString());
            texto.setCaretPosition(0);
        }

        JScrollPane rolagem = new JScrollPane(texto);
        rolagem.setPreferredSize(new Dimension(560, 390));
        rolagem.getViewport().setBackground(new Color(0x171827));
        JDialog dialogo = new JDialog(dono, "Histórico da Assistente IA", true);
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBackground(new Color(0x1E1F30));
        painel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        painel.add(rolagem, BorderLayout.CENTER);
        dialogo.setContentPane(painel);
        dialogo.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialogo.pack();
        dialogo.setLocationRelativeTo(janela);
        dialogo.setVisible(true);
    }

    private boolean pedirLogin() {
        JTextField fNome = new JTextField(nome, 18);
        JTextField fSala = new JTextField(sala, 18);
        JTextField fSalaChat = new JTextField(salaChat, 18);
        estilizarCampoEntrada(fNome);
        estilizarCampoEntrada(fSala);
        estilizarCampoEntrada(fSalaChat);
        JCheckBox lembrar = new JCheckBox("Lembrar meus dados neste dispositivo", lembrarDados);
        lembrar.setOpaque(false);
        lembrar.setForeground(new Color(0xD7D8F4));
        lembrar.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        lembrar.setFocusPainted(false);
        JLabel previa = new JLabel("Assim vai ficar o seu texto!");
        previa.setFont(previa.getFont().deriveFont(Font.BOLD, 14f));
        previa.setForeground(Color.decode(corTexto));

        JPanel paleta = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        paleta.setOpaque(false);
        for (String hex : PALETA) {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(26, 26));
            b.setBackground(Color.decode(hex));
            b.setBorder(BorderFactory.createLineBorder(new Color(0x555861)));
            b.setFocusPainted(false);
            b.addActionListener(e -> {
                corTexto = hex;
                previa.setForeground(Color.decode(hex));
            });
            paleta.add(b);
        }
        JButton outra = new JButton("Outra...");
        outra.addActionListener(e -> {
            Color c = escolherCor(null, Color.decode(corTexto), true);
            if (c != null) {
                corTexto = hex(c);
                previa.setForeground(c);
            }
        });
        paleta.add(outra);

        JPanel formulario = new JPanel(new GridLayout(0, 1, 0, 5));
        formulario.setOpaque(false);
        adicionarCampoEntrada(formulario, "Seu nome", fNome);
        adicionarCampoEntrada(formulario, "Servidor (código ou IP)", fSala);
        adicionarCampoEntrada(formulario, "Canal (ex.: geral, estudos)", fSalaChat);
        JLabel dicaServidor = rotuloEntrada(
            "No mesmo computador, use 127.0.0.1. Em outra rede, informe o IP do servidor.");
        dicaServidor.setFont(dicaServidor.getFont().deriveFont(Font.PLAIN, 11f));
        formulario.add(dicaServidor);
        formulario.add(lembrar);
        formulario.add(rotuloEntrada("Cor das suas mensagens"));
        formulario.add(paleta);
        formulario.add(previa);

        boolean[] entrar = {false};
        JDialog dialogo = new JDialog((Frame) null, "Entrar no chat", true);
        JPanel painel = painelEntrada();
        painel.setLayout(new BorderLayout(0, 14));
        painel.add(cabecalhoEntrada("Entrar no chat",
            "Conecte-se a um servidor e escolha seu canal."), BorderLayout.NORTH);
        painel.add(formulario, BorderLayout.CENTER);

        JButton cancelar = botaoAcaoEntrada("Cancelar", false);
        cancelar.addActionListener(e -> dialogo.dispose());
        JButton confirmar = botaoAcaoEntrada("Entrar no chat", true);
        confirmar.addActionListener(e -> {
            entrar[0] = true;
            dialogo.dispose();
        });
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acoes.setOpaque(false);
        acoes.add(cancelar);
        acoes.add(confirmar);
        painel.add(acoes, BorderLayout.SOUTH);

        dialogo.setContentPane(painel);
        dialogo.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialogo.getRootPane().setDefaultButton(confirmar);
        dialogo.pack();
        dialogo.setMinimumSize(new Dimension(490, 500));
        dialogo.setLocationRelativeTo(null);
        dialogo.setVisible(true);
        nome = fNome.getText();
        sala = fSala.getText();
        salaChat = fSalaChat.getText();
        lembrarDados = lembrar.isSelected();
        return entrar[0];
    }

    private JPanel painelEntrada() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBackground(new Color(0x313338));
        painel.setBorder(BorderFactory.createEmptyBorder(24, 28, 22, 28));
        return painel;
    }

    private JPanel cabecalhoEntrada(String titulo, String descricao) {
        JPanel cabecalho = new JPanel(new BorderLayout(12, 0));
        cabecalho.setOpaque(false);
        JLabel marca = new JLabel("M", JLabel.CENTER);
        marca.setOpaque(true);
        marca.setBackground(new Color(0x6738F5));
        marca.setForeground(Color.WHITE);
        marca.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        marca.setPreferredSize(new Dimension(48, 48));
        cabecalho.add(marca, BorderLayout.WEST);

        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 3));
        textos.setOpaque(false);
        JLabel tituloLabel = rotuloEntrada(titulo);
        tituloLabel.setFont(tituloLabel.getFont().deriveFont(Font.BOLD, 20f));
        JLabel descricaoLabel = rotuloEntrada(descricao);
        descricaoLabel.setFont(descricaoLabel.getFont().deriveFont(Font.PLAIN, 12f));
        textos.add(tituloLabel);
        textos.add(descricaoLabel);
        cabecalho.add(textos, BorderLayout.CENTER);
        return cabecalho;
    }

    private JButton botaoOpcaoEntrada(String titulo, String descricao) {
        JButton botao = new JButton("<html><div style='text-align:left;padding:4px'>"
            + "<b>" + titulo + "</b><br><span style='color:#b5bac1'>" + descricao
            + "</span></div></html>");
        botao.setHorizontalAlignment(JButton.LEFT);
        botao.setBackground(new Color(0x1C2038));
        botao.setForeground(new Color(0xF2F3F5));
        botao.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        botao.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x45436A)),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        botao.setFocusPainted(false);
        botao.setPreferredSize(new Dimension(420, 72));
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return botao;
    }

    private JButton botaoAcaoEntrada(String texto, boolean primario) {
        JButton botao = new JButton(texto);
        botao.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        botao.setForeground(Color.WHITE);
        botao.setBackground(primario ? new Color(0x6738F5) : new Color(0x35384D));
        botao.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        botao.setFocusPainted(false);
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return botao;
    }

    private JLabel rotuloEntrada(String texto) {
        JLabel label = new JLabel(texto);
        label.setForeground(new Color(0xDBDEE1));
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        return label;
    }

    private void adicionarCampoEntrada(JPanel formulario, String rotulo, JTextField campo) {
        formulario.add(rotuloEntrada(rotulo));
        formulario.add(campo);
    }

    private void estilizarCampoEntrada(JTextField campo) {
        campo.setBackground(new Color(0x1E1F22));
        campo.setForeground(new Color(0xF2F3F5));
        campo.setCaretColor(Color.WHITE);
        campo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        campo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x1E1F22)),
            BorderFactory.createEmptyBorder(9, 10, 9, 10)));
    }

    private String escaparHtml(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // --------------------------------------------------------------- janela
    private void montarJanela(Usuario usuario) {
        janela = new JFrame("Chat - " + usuario.getNome() + " | #" + salaChat
            + " (" + pessoasOnline + " online)");
        janela.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        janela.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saindo = true;
                cliente.sair();
                if (timerNotificacao != null) timerNotificacao.stop();
                if (janelaNotificacao != null) janelaNotificacao.dispose();
                removerIconeBandeja();
                janela.dispose();
                System.exit(0);
            }
        });
        janela.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                limparNotificacoes();
            }

            @Override
            public void windowLostFocus(WindowEvent e) { }
        });

        fundo = new FundoPanel();
        janela.setContentPane(fundo);

        // --- cabeçalho
        JPanel cabecalho = new PainelOndas.PainelCabecalho();
        JLabel canalAtivo = new JLabel("# " + salaChat);
        canalAtivo.setForeground(Color.WHITE);
        canalAtivo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        JLabel statusCanal = new JLabel("Mensagens e arquivos do canal");
        statusCanal.setForeground(new Color(0xB5BAC1));
        statusCanal.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        JPanel tituloCanal = new JPanel(new GridLayout(2, 1, 0, 3));
        tituloCanal.setOpaque(false);
        tituloCanal.add(canalAtivo);
        tituloCanal.add(statusCanal);
        JLabel iconeCanal = new JLabel("#", JLabel.CENTER);
        iconeCanal.setOpaque(true);
        iconeCanal.setBackground(new Color(0x6738F5));
        iconeCanal.setForeground(Color.WHITE);
        iconeCanal.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        iconeCanal.setPreferredSize(new Dimension(38, 38));
        JPanel identidadeCanal = new JPanel(new BorderLayout(12, 0));
        identidadeCanal.setOpaque(false);
        identidadeCanal.add(iconeCanal, BorderLayout.WEST);
        identidadeCanal.add(tituloCanal, BorderLayout.CENTER);
        cabecalho.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x1E1F22)),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        cabecalho.add(identidadeCanal, BorderLayout.WEST);
        rotuloCanalOnline = new JLabel("●  " + pessoasOnline + " online");
        rotuloCanalOnline.setForeground(new Color(0x22C55E));
        rotuloCanalOnline.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        JPanel acoesCanal = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        acoesCanal.setOpaque(false);
        JButton adicionarContato = botaoIcone("＋", "Adicionar contato ao canal");
        adicionarContato.addActionListener(e -> adicionarContato());
        JButton infoCanal = botaoIcone("ⓘ", "Informações da sala");
        infoCanal.addActionListener(e -> JOptionPane.showMessageDialog(janela,
            "Canal: #" + salaChat + "\nParticipantes online: " + pessoasOnline
                + "\nServidor: " + sala,
            "Informações da sala", JOptionPane.INFORMATION_MESSAGE));
        botaoSino = botaoIcone("♢", "Notificações recentes");
        botaoSino.addActionListener(e -> mostrarNotificacoes());
        JButton personalizarConversa = botaoIcone("▧", "Personalizar conversa");
        personalizarConversa.addActionListener(e -> personalizarConversa());
        acoesCanal.add(rotuloCanalOnline);
        acoesCanal.add(personalizarConversa);
        acoesCanal.add(adicionarContato);
        acoesCanal.add(infoCanal);
        acoesCanal.add(botaoSino);
        cabecalho.add(acoesCanal, BorderLayout.EAST);

        // --- barra de ferramentas e aparência
        JComboBox<Tema> combo = new JComboBox<>(Tema.values());
        comboTemaFundo = combo;
        combo.addActionListener(e -> {
            Tema t = (Tema) combo.getSelectedItem();
            if (t == Tema.SOLIDA) {
                Color c = escolherCor(janela, Color.decode("#FFE0F0"), false);
                if (c == null) {
                    combo.setSelectedItem(fundo.getTema());
                    return;
                }
                fundo.setSolida(c);
            } else {
                fundo.setTema(t);
            }
            aplicarTemaConversa();
            estadoDigitacao.setForeground(new Color(0xB5BAC1));
        });
        combo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        combo.setBackground(new Color(0x2B2D31));
        combo.setForeground(new Color(0xF2F3F5));
        combo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x45474E)),
            BorderFactory.createEmptyBorder(3, 7, 3, 7)));
        combo.setPreferredSize(new Dimension(190, 36));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, selected, focused);
                label.setOpaque(true);
                label.setBackground(selected ? new Color(0x4752C4) : new Color(0x2B2D31));
                label.setForeground(new Color(0xF2F3F5));
                label.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
                return label;
            }
        });
        combo.setToolTipText("Mudar o tema do fundo");

        Color[] corIndicadora = {Color.decode(corTexto)};
        JButton btnCor = botaoEstilizado("Cor do texto");
        btnCor.setIcon(new javax.swing.Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(corIndicadora[0]);
                g.fillOval(x, y, 12, 12);
                g.setColor(new Color(255, 255, 255, 150));
                g.drawOval(x, y, 12, 12);
            }

            @Override
            public int getIconWidth() { return 12; }

            @Override
            public int getIconHeight() { return 12; }
        });
        btnCor.setIconTextGap(8);
        configurarBotaoTopo(btnCor, new Color(0x3B3D45), new Color(0xF2F3F5));
        btnCor.addActionListener(e -> {
            Color c = escolherCor(janela, Color.decode(corTexto), true);
            if (c != null) {
                corTexto = hex(c);
                cliente.mudarCor(corTexto);
                preferencias.put("corTexto", corTexto);
                corIndicadora[0] = c;
                btnCor.repaint();
                btnCor.setToolTipText("Cor atual das mensagens: " + corTexto);
            }
        });

        JButton btnSair = botaoEstilizado("Sair do canal");
        btnSair.addActionListener(e -> sairDoCanal());
        configurarBotaoTopo(btnSair, new Color(0x7F3038), Color.WHITE);
        btnSair.setToolTipText("Sair desta conversa e voltar à entrada");

        // --- conversa
        area = new ChatFeed();
        area.setLayout(new BoxLayout(area, BoxLayout.Y_AXIS));
        area.setBackground(new Color(0x101321));
        area.setOpaque(false);
        area.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        rolagemMensagens = new JScrollPane(area);
        rolagemMensagens.setOpaque(true);
        rolagemMensagens.setBackground(new Color(0x101321));
        viewportConversa = new PapelParedeViewport();
        viewportConversa.setBackground(new Color(0x101321));
        viewportConversa.setOpaque(true);
        viewportConversa.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        rolagemMensagens.setViewport(viewportConversa);
        viewportConversa.setView(area);
        rolagemMensagens.setBorder(BorderFactory.createEmptyBorder());
        rolagemMensagens.getVerticalScrollBar().setUnitIncrement(18);

        JLabel nomeConversa = new JLabel("# " + salaChat);
        nomeConversa.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        nomeConversa.setForeground(new Color(0xF2F3F5));
        JLabel descricaoConversa = new JLabel("Bem-vindo ao canal. Esta é a conversa de #" + salaChat + ".");
        descricaoConversa.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        descricaoConversa.setForeground(new Color(0xB5BAC1));
        JPanel cabecalhoConversa = new JPanel(new GridLayout(2, 1, 0, 4));
        cabecalhoConversa.setBackground(new Color(0x191E32));
        cabecalhoConversa.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x1E1F22)),
            BorderFactory.createEmptyBorder(14, 18, 12, 18)));
        cabecalhoConversa.add(nomeConversa);
        cabecalhoConversa.add(descricaoConversa);

        estadoDigitacao = new JLabel(" ");
        estadoDigitacao.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        estadoDigitacao.setForeground(new Color(0xB5BAC1));
        estadoDigitacao.setOpaque(true);
        estadoDigitacao.setBackground(new Color(0x101321));
        estadoDigitacao.setBorder(BorderFactory.createEmptyBorder(6, 18, 10, 18));
        JPanel painelConversa = new JPanel(new BorderLayout());
        painelConversa.setBackground(new Color(0x101321));
        painelConversa.setBorder(BorderFactory.createLineBorder(new Color(0x1E1F22)));
        painelConversa.add(cabecalhoConversa, BorderLayout.NORTH);
        painelConversa.add(rolagemMensagens, BorderLayout.CENTER);
        painelConversa.add(estadoDigitacao, BorderLayout.SOUTH);
        painelConversaTema = painelConversa;
        painelCabecalhoTema = cabecalhoConversa;

        // --- contatos online
        JList<String> listaUsuarios = new JList<>(modeloUsuariosOnline);
        listaUsuariosOnline = listaUsuarios;
        listaUsuarios.setFixedCellHeight(40);
        listaUsuarios.setBackground(new Color(0x191E32));
        listaUsuarios.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        listaUsuarios.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, selected, focused);
                label.setText(value.toString());
                label.setIcon(new AvatarIcon(value.toString(), corAvatar(value.toString()), 25,
                    fotosUsuarios.get(value.toString().toLowerCase(java.util.Locale.ROOT))));
                label.setIconTextGap(10);
                label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
                label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                label.setOpaque(true);
                label.setBackground(selected ? new Color(0x282E49) : new Color(0x191E32));
                label.setForeground(new Color(0xE5E7EB));
                return label;
            }
        });
        listaUsuarios.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 1) return;
                int indice = listaUsuarios.locationToIndex(e.getPoint());
                if (indice < 0) return;
                Rectangle limites = listaUsuarios.getCellBounds(indice, indice);
                String contato = listaUsuarios.getModel().getElementAt(indice);
                if (limites != null && e.getX() - limites.x <= 50) {
                    mostrarFotoUsuario(contato);
                } else {
                    enviarPrivada(contato);
                }
            }
        });
        rotuloOnline = new JLabel(pessoasOnline + " conectados");
        rotuloOnline.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        rotuloOnline.setForeground(new Color(0xA78BFA));
        JLabel tituloContatos = new JLabel("Contatos online");
        tituloContatos.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        tituloContatos.setForeground(new Color(0xE5E7EB));
        JPanel cabecalhoContatos = new JPanel(new BorderLayout());
        cabecalhoContatos.setBackground(new Color(0x191E32));
        cabecalhoContatos.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x1E1F22)),
            BorderFactory.createEmptyBorder(14, 14, 10, 14)));
        cabecalhoContatos.add(tituloContatos, BorderLayout.NORTH);
        cabecalhoContatos.add(rotuloOnline, BorderLayout.SOUTH);
        JScrollPane rolagemContatos = new JScrollPane(listaUsuarios);
        rolagemContatos.setBorder(BorderFactory.createEmptyBorder());
        rolagemContatos.getViewport().setBackground(new Color(0x191E32));
        JPanel painelOnline = new JPanel(new BorderLayout());
        painelOnline.setBackground(new Color(0x191E32));
        painelOnline.setBorder(BorderFactory.createLineBorder(new Color(0x1E1F22)));
        painelOnline.add(cabecalhoContatos, BorderLayout.NORTH);
        painelOnline.add(rolagemContatos, BorderLayout.CENTER);
        painelOnline.setPreferredSize(new Dimension(205, 100));
        painelOnline.setMinimumSize(new Dimension(165, 100));
        painelOnlineTema = painelOnline;

        JPanel colunaConversa = new JPanel(new BorderLayout(0, 10));
        colunaConversa.setOpaque(false);
        colunaConversa.add(painelConversa, BorderLayout.CENTER);

        // --- ferramentas e composição
        campo = new JTextField();
        campo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        campo.setBackground(new Color(0x191E32));
        campo.setForeground(new Color(0xE5E7EB));
        campo.setCaretColor(Color.WHITE);
        campo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x1E1F22)),
            BorderFactory.createEmptyBorder(11, 14, 11, 14)));
        campo.setToolTipText("Escreva uma mensagem e pressione Enter para enviar");
        timerDigitando = new javax.swing.Timer(1200, e -> pararDeDigitar());
        timerDigitando.setRepeats(false);
        campo.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { atualizarDigitacao(); }
            @Override public void removeUpdate(DocumentEvent e) { atualizarDigitacao(); }
            @Override public void changedUpdate(DocumentEvent e) { atualizarDigitacao(); }
        });
        JButton btnEmoji = botaoEstilizado("☺");
        btnEmoji.setToolTipText("Inserir emoji");
        JButton btnAnexo = botaoEstilizado("＋");
        btnAnexo.setToolTipText("Anexar arquivo");
        btnAnexo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        btnAnexo.addActionListener(e -> escolherAnexo());
        JButton btnEnviar = botaoEstilizado("Enviar");
        btnEnviar.setText("➤");
        btnEnviar.setToolTipText("Enviar mensagem");
        btnEnviar.setBackground(new Color(0x6738F5));
        btnEnviar.setForeground(Color.WHITE);
        btnEnviar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        ActionListener enviar = e -> {
            String t = campo.getText().trim();
            if (!t.isEmpty()) {
                cliente.enviar(t);
                campo.setText("");
            }
        };
        campo.addActionListener(enviar);
        btnEnviar.addActionListener(enviar);
        btnEmoji.addActionListener(e -> mostrarEmojis(btnEmoji));

        JButton btnPrivado = botaoEstilizado("Privado");
        btnPrivado.addActionListener(e -> enviarPrivada());
        JButton btnBuscar = botaoEstilizado("Buscar");
        btnBuscar.addActionListener(e -> buscarMensagens());
        JButton btnBola = botaoEstilizado("✦  Bola IA");
        btnBola.setBackground(new Color(0x6738F5));
        btnBola.setForeground(Color.WHITE);
        btnBola.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnBola.setToolTipText("Abre a conversa visual do assistente de futebol.");
        btnBola.addActionListener(e -> perguntarBola());
        JPanel ferramentas = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        ferramentas.setOpaque(false);
        ferramentas.add(btnPrivado);
        ferramentas.add(btnBuscar);
        ferramentas.add(btnBola);

        JPanel entrada = new RoundedPanel(new BorderLayout(8, 0));
        entrada.setBackground(new Color(0x191E32));
        entrada.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x343B5B)),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        entrada.add(btnEmoji, BorderLayout.WEST);
        entrada.add(campo, BorderLayout.CENTER);
        JPanel acoesEntrada = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        acoesEntrada.setOpaque(false);
        acoesEntrada.add(btnAnexo);
        acoesEntrada.add(btnEnviar);
        entrada.add(acoesEntrada, BorderLayout.EAST);
        JPanel rodape = new JPanel(new BorderLayout(0, 6));
        rodape.setOpaque(false);
        rodape.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        rodape.add(entrada, BorderLayout.SOUTH);

        JPanel principal = new JPanel(new BorderLayout(12, 12));
        principal.setOpaque(false);
        principal.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
        principal.add(colunaConversa, BorderLayout.CENTER);
        principal.add(painelOnline, BorderLayout.EAST);
        JPanel conteudo = new JPanel(new BorderLayout(0, 0));
        conteudo.setOpaque(false);
        conteudo.add(principal, BorderLayout.CENTER);
        JPanel composer = new JPanel(new BorderLayout(0, 8));
        composer.setOpaque(false);
        composer.setBorder(BorderFactory.createEmptyBorder(4, 16, 16, 16));
        composer.add(ferramentas, BorderLayout.NORTH);
        composer.add(rodape, BorderLayout.CENTER);
        conteudo.add(composer, BorderLayout.SOUTH);
        JPanel corpo = new JPanel(new BorderLayout());
        corpo.setOpaque(false);
        corpo.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        corpo.add(conteudo, BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(new Color(0x101321));
        centro.add(cabecalho, BorderLayout.NORTH);
        centro.add(corpo, BorderLayout.CENTER);
        painelCentroTema = centro;
        JPanel navegacao = criarNavegacao(usuario, campo, painelOnline, listaUsuarios, rolagemMensagens,
            combo, btnCor, btnSair, btnAnexo);
        fundo.add(navegacao, BorderLayout.WEST);
        fundo.add(centro, BorderLayout.CENTER);

        mostrarBoasVindas(usuario);
        janela.setMinimumSize(new Dimension(900, 600));
        janela.setSize(1180, 760);
        janela.setLocationRelativeTo(null);
        aplicarTemaConversa();
        janela.setVisible(true);
        configurarIconeBandeja();
        campo.requestFocusInWindow();
    }

    private void mostrarBoasVindas(Usuario usuario) {
        cartaoBoasVindas = new RoundedPanel(new BorderLayout(0, 14));
        cartaoBoasVindas.setBackground(new Color(0x191E32));
        cartaoBoasVindas.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x453A79)),
            BorderFactory.createEmptyBorder(24, 28, 24, 28)));
        cartaoBoasVindas.setMaximumSize(new Dimension(560, 250));
        cartaoBoasVindas.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 7));
        textos.setOpaque(false);
        JLabel saudacao = new JLabel("Olá, " + usuario.getNome() + "!", JLabel.CENTER);
        saudacao.setForeground(new Color(0xF5F3FF));
        saudacao.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 23));
        JLabel descricao = new JLabel("Pronto para continuar sua conversa?", JLabel.CENTER);
        descricao.setForeground(new Color(0xC4C5D4));
        descricao.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        textos.add(saudacao);
        textos.add(descricao);

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        acoes.setOpaque(false);
        JButton resumo = botaoEstilizado("▤  Resumir conversa");
        configurarBotaoTopo(resumo, new Color(0x292644), new Color(0xE5D9FF));
        resumo.addActionListener(e -> cliente.enviar("@bola resumo"));
        JButton bola = botaoEstilizado("✦  Perguntar à Bola");
        configurarBotaoTopo(bola, new Color(0x6738F5), Color.WHITE);
        bola.addActionListener(e -> perguntarBola());
        acoes.add(resumo);
        acoes.add(bola);

        JPanel conteudo = new JPanel(new BorderLayout(0, 14));
        conteudo.setOpaque(false);
        JPanel mascoteDestaque = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        mascoteDestaque.setOpaque(false);
        mascoteDestaque.add(new MascoteBola(76));
        conteudo.add(mascoteDestaque, BorderLayout.NORTH);
        conteudo.add(textos, BorderLayout.CENTER);
        conteudo.add(acoes, BorderLayout.SOUTH);
        cartaoBoasVindas.add(conteudo, BorderLayout.CENTER);
        area.add(cartaoBoasVindas);
        area.revalidate();
    }

    private JPanel criarNavegacao(Usuario usuario, JTextField campoMensagem, JPanel painelOnline,
            JList<String> listaUsuarios, JScrollPane rolagemConversa,
            JComboBox<Tema> seletorTema, JButton btnCor,
            JButton btnSair, JButton btnAnexo) {
        JPanel navegacao = new PainelOndas();
        navegacao.setLayout(new BorderLayout());
        navegacao.setPreferredSize(new Dimension(218, 100));
        navegacao.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0x141519)));

        JPanel marca = new JPanel(new BorderLayout(10, 0));
        marca.setBackground(new Color(0x101321));
        marca.setBorder(BorderFactory.createEmptyBorder(18, 16, 18, 12));
        JLabel icone = new JLabel("M", JLabel.CENTER);
        icone.setOpaque(true);
        icone.setBackground(new Color(0x6738F5));
        icone.setForeground(Color.WHITE);
        icone.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        icone.setPreferredSize(new Dimension(40, 40));
        JPanel marcaTexto = new JPanel(new GridLayout(2, 1, 0, 3));
        marcaTexto.setOpaque(false);
        JLabel nomeApp = new JLabel("Messenger");
        nomeApp.setForeground(new Color(0xE5E7EB));
        nomeApp.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        JLabel descricaoApp = new JLabel("CHAT DA COMUNIDADE");
        descricaoApp.setForeground(new Color(0x949BA4));
        descricaoApp.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        marcaTexto.add(nomeApp);
        marcaTexto.add(descricaoApp);
        marca.add(icone, BorderLayout.WEST);
        marca.add(marcaTexto, BorderLayout.CENTER);

        JPanel menu = new JPanel();
        menu.setOpaque(false);
        menu.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        menu.setLayout(new javax.swing.BoxLayout(menu, javax.swing.BoxLayout.Y_AXIS));
        JLabel secao = new JLabel("  ESPAÇO");
        secao.setForeground(new Color(0x949BA4));
        secao.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        secao.setBorder(BorderFactory.createEmptyBorder(8, 2, 8, 0));
        secao.setAlignmentX(Component.LEFT_ALIGNMENT);
        menu.add(secao);

        JButton conversas = botaoNavegacao("▤   Conversas", true);
        conversas.addActionListener(e -> {
            rolagemConversa.getVerticalScrollBar().setValue(
                rolagemConversa.getVerticalScrollBar().getMaximum());
            campoMensagem.requestFocusInWindow();
        });
        JButton contatos = botaoNavegacao("♧   Contatos", false);
        contatos.addActionListener(e -> {
            listaUsuarios.requestFocusInWindow();
            listaUsuarios.scrollRectToVisible(listaUsuarios.getVisibleRect());
        });
        JButton outroUsuario = botaoNavegacao("＋   Abrir outro usuário", false);
        outroUsuario.setToolTipText(
            "Abre outro cliente sem fechar esta janela. Entre com um nome diferente.");
        outroUsuario.addActionListener(e -> abrirOutroUsuario());
        JButton arquivos = botaoNavegacao("▱   Arquivos", false);
        arquivos.addActionListener(e -> btnAnexo.doClick());
        JButton configuracoes = botaoNavegacao("⚙   Configurações", false);
        configuracoes.addActionListener(e -> mostrarConfiguracoes(seletorTema, btnCor));
        JButton sobre = botaoNavegacao("ⓘ   Sobre", false);
        sobre.addActionListener(e -> JOptionPane.showMessageDialog(janela,
            "Messenger\nConversa #" + salaChat + "\nConectado como " + usuario.getNome(),
            "Sobre o chat", JOptionPane.INFORMATION_MESSAGE));
        JButton assistente = botaoNavegacao("✧   Assistente IA", false);
        assistente.setForeground(new Color(0xC4B5FD));
        assistente.setToolTipText("Converse com a IA e acesse seu histórico.");
        assistente.addActionListener(e -> perguntarAssistente());
        menu.add(conversas);
        menu.add(Box.createVerticalStrut(5));
        menu.add(contatos);
        menu.add(Box.createVerticalStrut(5));
        menu.add(outroUsuario);
        menu.add(Box.createVerticalStrut(5));
        menu.add(arquivos);
        menu.add(Box.createVerticalStrut(5));
        menu.add(configuracoes);
        menu.add(Box.createVerticalStrut(5));
        menu.add(sobre);
        menu.add(Box.createVerticalStrut(5));
        menu.add(assistente);

        JPanel rodape = new JPanel(new BorderLayout(0, 10));
        rodape.setBackground(new Color(0x101321));
        rodape.setBorder(BorderFactory.createEmptyBorder(10, 10, 12, 10));
        JPanel perfil = new JPanel(new BorderLayout(9, 0));
        perfil.setBackground(new Color(0x191E32));
        perfil.setBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9));
        avatarPerfil = new AvatarLabel(usuario.getNome(), new Color(0x6738F5),
            fotosUsuarios.get(usuario.getNome().toLowerCase(java.util.Locale.ROOT)));
        avatarPerfil.setPreferredSize(new Dimension(34, 34));
        avatarPerfil.setMinimumSize(new Dimension(34, 34));
        avatarPerfil.setMaximumSize(new Dimension(34, 34));
        avatarPerfil.setToolTipText("Clique para alterar ou visualizar sua foto de perfil");
        avatarPerfil.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseAdapter opcoesPerfilFoto = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) mostrarOpcoesFotoPerfil((Component) e.getSource());
            }
        };
        avatarPerfil.addMouseListener(opcoesPerfilFoto);
        perfil.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        perfil.setToolTipText("Clique para alterar ou visualizar sua foto de perfil");
        perfil.addMouseListener(opcoesPerfilFoto);
        JPanel identidade = new JPanel(new GridLayout(2, 1, 0, 3));
        identidade.setOpaque(false);
        JLabel nomeUsuario = new JLabel(usuario.getNome());
        nomeUsuario.setForeground(new Color(0xE5E7EB));
        nomeUsuario.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        nomeUsuario.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        nomeUsuario.addMouseListener(opcoesPerfilFoto);
        JLabel statusUsuario = new JLabel("●  Conectado");
        statusUsuario.setForeground(new Color(0x22C55E));
        statusUsuario.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        statusUsuario.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        statusUsuario.addMouseListener(opcoesPerfilFoto);
        identidade.add(nomeUsuario);
        identidade.add(statusUsuario);
        identidade.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        identidade.setToolTipText("Clique para alterar ou visualizar sua foto de perfil");
        identidade.addMouseListener(opcoesPerfilFoto);
        perfil.add(avatarPerfil, BorderLayout.WEST);
        perfil.add(identidade, BorderLayout.CENTER);
        JButton opcoesFoto = botaoIcone("⋮", "Alterar ou remover foto de perfil");
        opcoesFoto.addActionListener(e -> mostrarOpcoesFotoPerfil(opcoesFoto));
        perfil.add(opcoesFoto, BorderLayout.EAST);

        JButton sair = botaoNavegacao("↪   Sair do canal", false);
        sair.setForeground(new Color(0xED6A75));
        sair.addActionListener(e -> btnSair.doClick());
        rodape.add(sair, BorderLayout.NORTH);
        rodape.add(perfil, BorderLayout.SOUTH);

        navegacao.add(marca, BorderLayout.NORTH);
        navegacao.add(menu, BorderLayout.CENTER);
        navegacao.add(rodape, BorderLayout.SOUTH);
        return navegacao;
    }

    private void abrirOutroUsuario() {
        String sistema = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        String executavel = sistema.startsWith("windows") ? "javaw.exe" : "java";
        Path java = Path.of(System.getProperty("java.home"), "bin", executavel);
        String classpath = System.getProperty("java.class.path", "");
        if (classpath.isBlank()) {
            JOptionPane.showMessageDialog(janela,
                "Não foi possível iniciar outro cliente: o classpath Java está vazio.",
                "Erro ao abrir outro usuário", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            new ProcessBuilder(java.toString(), "-cp", classpath, ClienteGUI.class.getName(),
                    "--novo-usuario")
                .directory(Path.of(System.getProperty("user.dir")).toFile())
                .start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(janela,
                "Não consegui abrir outro cliente Java: " + e.getMessage(),
                "Erro ao abrir outro usuário", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JButton botaoNavegacao(String texto, boolean ativo) {
        JButton botao = new JButton(texto);
        botao.setHorizontalAlignment(JButton.LEFT);
        botao.setAlignmentX(Component.LEFT_ALIGNMENT);
        botao.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        botao.setPreferredSize(new Dimension(190, 40));
        botao.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        botao.setForeground(ativo ? Color.WHITE : new Color(0xA78BFA));
        botao.setBackground(ativo ? new Color(0x6738F5) : new Color(0x101321));
        botao.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 8));
        botao.setFocusPainted(false);
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        botao.setOpaque(true);
        return botao;
    }

    private void enviarPrivada() {
        String destinatario = JOptionPane.showInputDialog(janela,
            "Nome da pessoa que está neste canal:", "Mensagem privada", JOptionPane.QUESTION_MESSAGE);
        if (destinatario == null || destinatario.isBlank()) return;
        enviarPrivada(destinatario.trim());
    }

    private void enviarPrivada(String destinatario) {
        if (destinatario == null || destinatario.isBlank() || destinatario.equalsIgnoreCase(nome)) return;
        String texto = JOptionPane.showInputDialog(janela, "Mensagem:", "Mensagem privada",
            JOptionPane.QUESTION_MESSAGE);
        if (texto != null && !texto.isBlank()) {
            cliente.enviar("/p " + destinatario.trim() + " " + texto.trim());
        }
    }

    private void adicionarContato() {
        String convite = "Convite para o Messenger\nServidor: " + sala + "\nCanal: " + salaChat
            + "\nPeça à pessoa para usar o IP/código do servidor e entrar neste canal.";
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
            new java.awt.datatransfer.StringSelection(convite), null);
        JOptionPane.showMessageDialog(janela,
            "Os dados do convite foram copiados. Envie-os para quem deseja convidar.",
            "Convidar para o canal", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarConfiguracoes(JComboBox<Tema> seletorTema, JButton btnCor) {
        JPanel painel = new JPanel(new GridLayout(0, 1, 0, 8));
        painel.add(new JLabel("Aparência do chat"));
        painel.add(seletorTema);
        JButton papelParede = new JButton("Personalizar conversa");
        papelParede.addActionListener(e -> personalizarConversa());
        painel.add(papelParede);
        JButton cor = new JButton("Escolher cor das mensagens");
        cor.addActionListener(e -> btnCor.doClick());
        painel.add(cor);
        JOptionPane.showMessageDialog(janela, painel, "Configurações",
            JOptionPane.PLAIN_MESSAGE);
    }

    private void carregarPapelParedeSalvo() {
        String chaveArquivo = preferencias.getBoolean(chavePreferenciaPapelAtivo(), false)
            ? arquivoPapelParede().toString() : "";
        escurecimentoPapelParede = preferencias.getInt(chavePreferenciaPapelEscuro(), 30);
        if (chaveArquivo.isBlank()) return;
        try {
            Path caminho = Path.of(chaveArquivo);
            if (!Files.isRegularFile(caminho) || Files.size(caminho) > Protocolo.TAMANHO_MAXIMO_ANEXO) {
                throw new IOException("O arquivo do papel de parede não existe ou excede 10 MB.");
            }
            papelParedeAtual = lerImagemPapelParede(Files.readAllBytes(caminho));
        } catch (IOException | RuntimeException e) {
            System.err.println("Não foi possível carregar o papel de parede: " + e.getMessage());
            preferencias.remove(chavePreferenciaPapelAtivo());
            papelParedeAtual = null;
        }
    }

    private void personalizarConversa() {
        BufferedImage[] selecionada = {papelParedeAtual};
        JPanel painel = new JPanel(new BorderLayout(0, 12));
        painel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JLabel descricao = new JLabel("Escolha uma imagem e ajuste a camada escura para facilitar a leitura.");
        painel.add(descricao, BorderLayout.NORTH);

        PreviaPapelParede previa = new PreviaPapelParede(selecionada[0],
            escurecimentoPapelParede);
        painel.add(previa, BorderLayout.CENTER);
        JButton aplicar = new JButton("Aplicar");
        aplicar.setEnabled(selecionada[0] != null);

        JPanel escolhas = new JPanel(new BorderLayout(0, 6));
        escolhas.add(new JLabel("Galeria"), BorderLayout.NORTH);
        JPanel galeria = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        for (int i = 0; i < 4; i++) {
            BufferedImage opcao = criarImagemGaleria(i);
            JButton miniatura = new JButton(new javax.swing.ImageIcon(
                opcao.getScaledInstance(84, 54, Image.SCALE_SMOOTH)));
            miniatura.setToolTipText(nomeImagemGaleria(i));
            miniatura.setPreferredSize(new Dimension(94, 64));
            miniatura.addActionListener(e -> {
                selecionada[0] = opcao;
                previa.setImagem(selecionada[0]);
                aplicar.setEnabled(true);
            });
            galeria.add(miniatura);
        }
        escolhas.add(galeria, BorderLayout.CENTER);
        JSlider escurecimento = new JSlider(0, 80, escurecimentoPapelParede);
        escurecimento.setMajorTickSpacing(20);
        escurecimento.setMinorTickSpacing(10);
        escurecimento.setPaintTicks(true);
        escurecimento.setPaintLabels(true);
        escurecimento.addChangeListener(e -> previa.setEscurecimento(escurecimento.getValue()));
        JPanel controleEscurecimento = new JPanel(new BorderLayout(8, 0));
        controleEscurecimento.add(new JLabel("Camada escura"), BorderLayout.WEST);
        controleEscurecimento.add(escurecimento, BorderLayout.CENTER);
        escolhas.add(controleEscurecimento, BorderLayout.SOUTH);
        painel.add(escolhas, BorderLayout.SOUTH);

        JDialog dialogo = new JDialog(janela, "Personalizar conversa", true);
        JButton escolher = new JButton("Escolher imagem...");
        escolher.addActionListener(e -> {
            JFileChooser seletor = new JFileChooser();
            seletor.setDialogTitle("Escolher papel de parede");
            java.util.List<String> extensoes = new java.util.ArrayList<>(
                java.util.List.of("jpg", "jpeg", "png"));
            if (ImageIO.getImageReadersBySuffix("webp").hasNext()) extensoes.add("webp");
            seletor.setFileFilter(new FileNameExtensionFilter(
                "Imagens (" + String.join(", ", extensoes) + ")",
                extensoes.toArray(String[]::new)));
            if (seletor.showOpenDialog(dialogo) != JFileChooser.APPROVE_OPTION) return;
            try {
                Path caminho = seletor.getSelectedFile().toPath();
                if (Files.size(caminho) > Protocolo.TAMANHO_MAXIMO_ANEXO) {
                    throw new IOException("Escolha uma imagem com até 10 MB.");
                }
                selecionada[0] = lerImagemPapelParede(Files.readAllBytes(caminho));
                previa.setImagem(selecionada[0]);
                aplicar.setEnabled(true);
            } catch (IOException | IllegalArgumentException erro) {
                JOptionPane.showMessageDialog(dialogo, erro.getMessage(),
                    "Imagem incompatível", JOptionPane.ERROR_MESSAGE);
            }
        });
        aplicar.addActionListener(e -> {
            if (selecionada[0] == null) return;
            try {
                salvarPapelParede(selecionada[0], escurecimento.getValue());
                dialogo.dispose();
            } catch (IOException erro) {
                JOptionPane.showMessageDialog(dialogo, erro.getMessage(),
                    "Não foi possível salvar o papel de parede", JOptionPane.ERROR_MESSAGE);
            }
        });
        JButton remover = new JButton("Remover papel de parede");
        remover.addActionListener(e -> {
            removerPapelParede(false);
            dialogo.dispose();
        });
        JButton restaurar = new JButton("Restaurar fundo original");
        restaurar.addActionListener(e -> {
            removerPapelParede(true);
            dialogo.dispose();
        });
        JButton fechar = new JButton("Cancelar");
        fechar.addActionListener(e -> dialogo.dispose());
        JPanel botoes = new JPanel(new GridLayout(2, 1, 0, 4));
        JPanel linhaSelecao = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        linhaSelecao.add(escolher);
        linhaSelecao.add(aplicar);
        JPanel linhaAcoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        linhaAcoes.add(remover);
        linhaAcoes.add(restaurar);
        linhaAcoes.add(fechar);
        botoes.add(linhaSelecao);
        botoes.add(linhaAcoes);
        JPanel conteudo = new JPanel(new BorderLayout());
        conteudo.add(painel, BorderLayout.CENTER);
        conteudo.add(botoes, BorderLayout.SOUTH);
        dialogo.setContentPane(conteudo);
        dialogo.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialogo.setSize(650, 650);
        dialogo.setMinimumSize(new Dimension(580, 540));
        dialogo.setLocationRelativeTo(janela);
        dialogo.setVisible(true);
    }

    private void salvarPapelParede(BufferedImage imagem, int escurecimento) throws IOException {
        Path arquivo = arquivoPapelParede();
        Files.createDirectories(arquivo.getParent());
        byte[] dados = codificarPapelParede(imagem);
        Files.write(arquivo, dados);
        papelParedeAtual = lerImagemPapelParede(dados);
        escurecimentoPapelParede = escurecimento;
        preferencias.putBoolean(chavePreferenciaPapelAtivo(), true);
        preferencias.putInt(chavePreferenciaPapelEscuro(), escurecimento);
        aplicarPapelParedeNaConversa();
    }

    private void removerPapelParede(boolean restaurarOriginal) {
        try {
            Files.deleteIfExists(arquivoPapelParede());
            papelParedeAtual = null;
            escurecimentoPapelParede = 0;
            preferencias.remove(chavePreferenciaPapelAtivo());
            preferencias.remove(chavePreferenciaPapelEscuro());
            if (restaurarOriginal && comboTemaFundo != null) {
                comboTemaFundo.setSelectedItem(Tema.MESSENGER);
                fundo.setTema(Tema.MESSENGER);
                aplicarTemaConversa();
            }
            aplicarPapelParedeNaConversa();
        } catch (IOException erro) {
            JOptionPane.showMessageDialog(janela, erro.getMessage(),
                "Não foi possível remover o papel de parede", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void aplicarPapelParedeNaConversa() {
        if (viewportConversa != null) {
            viewportConversa.setPapelParede(papelParedeAtual, escurecimentoPapelParede);
        }
    }

    private Path arquivoPapelParede() {
        return Path.of(System.getProperty("user.home"), ".chat-multiusuario",
            "wallpapers", chavePapelParede + ".jpg");
    }

    private String chavePreferenciaPapelAtivo() {
        return "papel_ativo_" + chavePapelParede;
    }

    private String chavePreferenciaPapelEscuro() {
        return "papel_escuro_" + chavePapelParede;
    }

    private static String criarChavePapelParede(String servidor, String usuario, String conversa) {
        String chave = servidor.toLowerCase(java.util.Locale.ROOT) + "|"
            + usuario.toLowerCase(java.util.Locale.ROOT) + "|"
            + conversa.toLowerCase(java.util.Locale.ROOT);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(chave.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não está disponível para salvar a conversa.", e);
        }
    }

    private static BufferedImage lerImagemPapelParede(byte[] conteudo) throws IOException {
        try (javax.imageio.stream.ImageInputStream entrada =
                ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(conteudo))) {
            if (entrada == null) throw new IOException("O arquivo não é uma imagem válida.");
            Iterator<javax.imageio.ImageReader> leitores = ImageIO.getImageReaders(entrada);
            if (!leitores.hasNext()) throw new IOException("O formato desta imagem não é compatível.");
            javax.imageio.ImageReader leitor = leitores.next();
            try {
                leitor.setInput(entrada, true, true);
                int largura = leitor.getWidth(0);
                int altura = leitor.getHeight(0);
                if (largura < 1 || altura < 1 || largura > 10000 || altura > 10000
                        || (long) largura * altura > 40_000_000L) {
                    throw new IOException("A imagem excede as dimensões permitidas.");
                }
                BufferedImage imagem = leitor.read(0);
                if (imagem == null) throw new IOException("Não foi possível decodificar a imagem.");
                return imagem;
            } finally {
                leitor.dispose();
            }
        }
    }

    private static byte[] codificarPapelParede(BufferedImage original) throws IOException {
        double escala = Math.min(1.0, 2048.0 / Math.max(original.getWidth(), original.getHeight()));
        int largura = Math.max(1, (int) Math.round(original.getWidth() * escala));
        int altura = Math.max(1, (int) Math.round(original.getHeight() * escala));
        BufferedImage preparada = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = preparada.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, largura, altura);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(original, 0, 0, largura, altura, null);
        g.dispose();

        Iterator<ImageWriter> escritores = ImageIO.getImageWritersByFormatName("jpeg");
        if (!escritores.hasNext()) throw new IOException("Não há suporte para salvar imagens JPEG.");
        ImageWriter escritor = escritores.next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream saida = ImageIO.createImageOutputStream(bytes)) {
            escritor.setOutput(saida);
            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(0.88f);
            escritor.write(null, new IIOImage(preparada, null, null), parametros);
        } finally {
            escritor.dispose();
        }
        if (bytes.size() > Protocolo.TAMANHO_MAXIMO_ANEXO) {
            throw new IOException("A imagem preparada excede o limite de 10 MB.");
        }
        return bytes.toByteArray();
    }

    private static String nomeImagemGaleria(int indice) {
        return switch (indice) {
            case 0 -> "Aurora";
            case 1 -> "Oceano";
            case 2 -> "Jardim";
            default -> "Noite";
        };
    }

    private static BufferedImage criarImagemGaleria(int indice) {
        int largura = 900;
        int altura = 600;
        BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color[][] paletas = {
            {new Color(0x302B63), new Color(0x24243E)},
            {new Color(0x136A8A), new Color(0x267871)},
            {new Color(0x134E5E), new Color(0x71B280)},
            {new Color(0x232526), new Color(0x414345)}
        };
        Color[] cores = paletas[Math.floorMod(indice, paletas.length)];
        g.setPaint(new LinearGradientPaint(0, 0, largura, altura,
            new float[] {0f, 1f}, cores));
        g.fillRect(0, 0, largura, altura);
        g.setColor(new Color(255, 255, 255, 24));
        for (int x = -altura; x < largura; x += 110) {
            g.setStroke(new java.awt.BasicStroke(2f));
            g.drawLine(x, 0, x + altura, altura);
        }
        g.setColor(new Color(255, 255, 255, 18));
        for (int y = 40; y < altura; y += 100) {
            for (int x = 50; x < largura; x += 100) {
                g.fillOval(x, y, 34, 34);
            }
        }
        g.dispose();
        return imagem;
    }

    private JButton botaoIcone(String icone, String dica) {
        JButton botao = new JButton(icone);
        botao.setToolTipText(dica);
        botao.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        botao.setForeground(new Color(0xE5E7EB));
        botao.setBackground(new Color(0x191E32));
        botao.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        botao.setFocusPainted(false);
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return botao;
    }

    private void buscarMensagens() {
        String termo = JOptionPane.showInputDialog(janela, "Palavra ou frase para procurar neste canal:",
            "Buscar mensagens", JOptionPane.QUESTION_MESSAGE);
        if (termo != null && !termo.isBlank()) {
            cliente.enviar("/buscar " + termo.trim());
        }
    }

    private void escolherAnexo() {
        JFileChooser seletor = new JFileChooser();
        seletor.setDialogTitle("Escolher foto ou arquivo (máximo 10 MB)");
        if (seletor.showOpenDialog(janela) != JFileChooser.APPROVE_OPTION) return;
        Path caminho = seletor.getSelectedFile().toPath();
        try {
            cliente.enviarAnexo(caminho);
        } catch (IOException | IllegalArgumentException e) {
            JOptionPane.showMessageDialog(janela, e.getMessage(), "Não foi possível enviar o anexo",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private BufferedImage carregarFotoPerfilSalva(String usuarioAtual) {
        String usuarioLegado = preferencias.get("nome", "");
        if (usuarioAtual == null || !usuarioAtual.equalsIgnoreCase(usuarioLegado)) return null;
        String caminhoSalvo = preferencias.get("fotoPerfil", "");
        if (caminhoSalvo.isBlank()) return null;
        try {
            Path caminho = Path.of(caminhoSalvo);
            if (!Files.isRegularFile(caminho)
                    || Files.size(caminho) > Protocolo.TAMANHO_MAXIMO_ANEXO) {
                throw new IOException("O arquivo salvo não existe ou excede 10 MB.");
            }
            return prepararFotoPerfil(lerImagem(Files.readAllBytes(caminho)));
        } catch (IOException | RuntimeException e) {
            System.err.println("Não foi possível carregar a foto de perfil salva: " + e.getMessage());
            preferencias.remove("fotoPerfil");
            return null;
        }
    }

    private void selecionarFotoPerfil() {
        JFileChooser seletor = new JFileChooser();
        seletor.setDialogTitle("Escolher foto de perfil");
        java.util.List<String> extensoes = java.util.List.of("jpg", "jpeg", "png");
        seletor.setFileFilter(new FileNameExtensionFilter(
            "Imagens (" + String.join(", ", extensoes) + ")",
            extensoes.toArray(String[]::new)));
        seletor.setAcceptAllFileFilterUsed(false);
        if (seletor.showOpenDialog(janela) != JFileChooser.APPROVE_OPTION) return;
        Path caminho = seletor.getSelectedFile().toPath();
        try {
            String nomeArquivo = caminho.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
            if (extensoes.stream().noneMatch(ext -> nomeArquivo.endsWith("." + ext))) {
                throw new IOException("Escolha uma imagem JPG, JPEG ou PNG.");
            }
            if (Files.size(caminho) > Protocolo.TAMANHO_MAXIMO_ANEXO) {
                throw new IOException("Escolha uma imagem com até 10 MB.");
            }
            BufferedImage foto = prepararFotoPerfil(lerImagem(Files.readAllBytes(caminho)));
            confirmarFotoPerfil(foto);
        } catch (IOException | IllegalArgumentException e) {
            JOptionPane.showMessageDialog(janela, e.getMessage(), "Foto de perfil",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void confirmarFotoPerfil(BufferedImage foto) {
        JDialog dialogo = new JDialog(janela, "Prévia da foto de perfil", true);
        JPanel painel = new JPanel(new BorderLayout(0, 12));
        painel.setBackground(new Color(0x191E32));
        painel.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));
        JLabel descricao = new JLabel("Confira a imagem antes de salvar.");
        descricao.setForeground(new Color(0xE5E7EB));
        descricao.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));

        double escala = Math.min(1.0, 240.0 / Math.max(foto.getWidth(), foto.getHeight()));
        int largura = Math.max(1, (int) Math.round(foto.getWidth() * escala));
        int altura = Math.max(1, (int) Math.round(foto.getHeight() * escala));
        Image previaImagem = foto.getScaledInstance(largura, altura, Image.SCALE_SMOOTH);
        JLabel previa = new JLabel(new javax.swing.ImageIcon(previaImagem), JLabel.CENTER);
        previa.setOpaque(true);
        previa.setBackground(new Color(0x101321));
        previa.setPreferredSize(new Dimension(280, 270));
        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.add(previa, BorderLayout.CENTER);

        JButton cancelar = new JButton("Cancelar");
        JButton salvar = new JButton("Salvar foto");
        configurarBotaoTopo(salvar, new Color(0x6738F5), Color.WHITE);
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acoes.setOpaque(false);
        acoes.add(cancelar);
        acoes.add(salvar);
        painel.add(descricao, BorderLayout.NORTH);
        painel.add(centro, BorderLayout.CENTER);
        painel.add(acoes, BorderLayout.SOUTH);
        cancelar.addActionListener(e -> dialogo.dispose());
        salvar.addActionListener(e -> {
            try {
                cliente.enviarAvatar(serializarFoto(foto));
                dialogo.dispose();
            } catch (IOException erro) {
                JOptionPane.showMessageDialog(dialogo, erro.getMessage(),
                    "Não foi possível salvar a foto", JOptionPane.ERROR_MESSAGE);
            }
        });
        dialogo.setContentPane(painel);
        dialogo.getRootPane().setDefaultButton(salvar);
        dialogo.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialogo.pack();
        dialogo.setMinimumSize(new Dimension(340, 360));
        dialogo.setLocationRelativeTo(janela);
        dialogo.setVisible(true);
    }

    private void mostrarOpcoesFotoPerfil(Component origem) {
        JPopupMenu menu = new JPopupMenu();
        javax.swing.JMenuItem alterar = new javax.swing.JMenuItem("Alterar foto de perfil");
        alterar.addActionListener(e -> selecionarFotoPerfil());
        menu.add(alterar);
        javax.swing.JMenuItem visualizar = new javax.swing.JMenuItem("Visualizar foto de perfil");
        visualizar.addActionListener(e -> mostrarFotoUsuario(nome));
        menu.add(visualizar);
        javax.swing.JMenuItem remover = new javax.swing.JMenuItem("Remover foto");
        remover.setEnabled(fotosUsuarios.containsKey(nome.toLowerCase(java.util.Locale.ROOT)));
        remover.addActionListener(e -> removerFotoPerfil());
        menu.add(remover);
        menu.show(origem, 0, origem.getHeight());
    }

    private void removerFotoPerfil() {
        try {
            cliente.removerAvatar();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(janela, e.getMessage(), "Foto de perfil",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private static BufferedImage lerImagem(byte[] conteudo) throws IOException {
        try (javax.imageio.stream.ImageInputStream entrada =
                ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(conteudo))) {
            if (entrada == null) throw new IOException("O arquivo selecionado não é uma imagem válida.");
            Iterator<javax.imageio.ImageReader> leitores = ImageIO.getImageReaders(entrada);
            if (!leitores.hasNext()) throw new IOException("O formato desta imagem não é compatível.");
            javax.imageio.ImageReader leitor = leitores.next();
            try {
                leitor.setInput(entrada, true, true);
                int largura = leitor.getWidth(0);
                int altura = leitor.getHeight(0);
                if (largura < 1 || altura < 1 || largura > 2048 || altura > 2048) {
                    throw new IOException("A imagem precisa ter dimensões menores que 2048 × 2048.");
                }
                BufferedImage imagem = leitor.read(0);
                if (imagem == null) throw new IOException("Não foi possível decodificar a imagem.");
                return imagem;
            } finally {
                leitor.dispose();
            }
        }
    }

    private static BufferedImage prepararFotoPerfil(BufferedImage original) {
        double escala = Math.min(1.0, 512.0 / Math.max(original.getWidth(), original.getHeight()));
        int largura = Math.max(1, (int) Math.round(original.getWidth() * escala));
        int altura = Math.max(1, (int) Math.round(original.getHeight() * escala));
        BufferedImage preparada = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = preparada.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, largura, altura);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(original, 0, 0, largura, altura, null);
        g.dispose();
        return preparada;
    }

    private static byte[] serializarFoto(BufferedImage imagem) throws IOException {
        BufferedImage atual = imagem;
        while (true) {
            Iterator<ImageWriter> escritores = ImageIO.getImageWritersByFormatName("jpeg");
            if (!escritores.hasNext()) throw new IOException("Não há suporte para preparar imagens JPEG.");
            ImageWriter escritor = escritores.next();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ImageOutputStream saida = ImageIO.createImageOutputStream(bytes)) {
                escritor.setOutput(saida);
                ImageWriteParam parametros = escritor.getDefaultWriteParam();
                parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parametros.setCompressionQuality(0.82f);
                escritor.write(null, new IIOImage(atual, null, null), parametros);
            } finally {
                escritor.dispose();
            }
            if (bytes.size() <= Protocolo.TAMANHO_MAXIMO_AVATAR) return bytes.toByteArray();
            if (Math.max(atual.getWidth(), atual.getHeight()) <= 64) {
                throw new IOException("A foto preparada excedeu o limite de envio.");
            }
            atual = redimensionarProporcional(atual, 0.85);
        }
    }

    private static BufferedImage redimensionarProporcional(BufferedImage original, double escala) {
        int largura = Math.max(1, (int) Math.round(original.getWidth() * escala));
        int altura = Math.max(1, (int) Math.round(original.getHeight() * escala));
        BufferedImage reduzida = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = reduzida.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(original, 0, 0, largura, altura, null);
        g.dispose();
        return reduzida;
    }

    private void atualizarAvatarNaLista(String usuario, BufferedImage imagem) {
        String chave = usuario.toLowerCase(java.util.Locale.ROOT);
        java.util.List<AvatarLabel> labels = avataresRenderizados.get(chave);
        if (labels != null) {
            for (AvatarLabel label : labels) label.setImagem(imagem);
        }
        if (usuario.equalsIgnoreCase(nome) && avatarPerfil != null) avatarPerfil.setImagem(imagem);
        if (listaUsuariosOnline == null) return;
        for (int i = 0; i < modeloUsuariosOnline.size(); i++) {
            if (modeloUsuariosOnline.get(i).equalsIgnoreCase(usuario)) {
                modeloUsuariosOnline.set(i, modeloUsuariosOnline.get(i));
                break;
            }
        }
        listaUsuariosOnline.repaint();
    }

    private void mostrarFotoUsuario(String usuario) {
        String chave = usuario.toLowerCase(java.util.Locale.ROOT);
        Image imagem = fotosUsuarios.get(chave);
        if (imagem == null) imagem = AvatarPortrait.para(usuario);
        JDialog dialogo = new JDialog(janela, "Foto de " + usuario, true);
        dialogo.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel conteudo = new JPanel(new BorderLayout(0, 10));
        conteudo.setBackground(new Color(0x191E32));
        conteudo.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        PainelVisualizacaoFoto visualizacao = new PainelVisualizacaoFoto(imagem);
        visualizacao.setBackground(new Color(0x101321));
        JScrollPane rolagem = new JScrollPane(visualizacao);
        rolagem.setPreferredSize(new Dimension(560, 420));
        rolagem.getViewport().setBackground(new Color(0x101321));
        rolagem.setBorder(BorderFactory.createEmptyBorder());
        conteudo.add(rolagem, BorderLayout.CENTER);

        JButton diminuir = botaoIcone("−", "Diminuir zoom");
        JButton aumentar = botaoIcone("+", "Aumentar zoom");
        JButton fechar = new JButton("Fechar");
        diminuir.addActionListener(e -> visualizacao.ajustarZoom(0.8));
        aumentar.addActionListener(e -> visualizacao.ajustarZoom(1.25));
        fechar.addActionListener(e -> dialogo.dispose());
        JPanel controles = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        controles.setOpaque(false);
        controles.add(diminuir);
        controles.add(aumentar);
        controles.add(fechar);
        conteudo.add(controles, BorderLayout.SOUTH);
        dialogo.setContentPane(conteudo);
        dialogo.pack();
        dialogo.setMinimumSize(new Dimension(430, 360));
        dialogo.setLocationRelativeTo(janela);
        dialogo.setVisible(true);
    }

    private void sairDoCanal() {
        int escolha = JOptionPane.showConfirmDialog(janela,
            "Você vai sair de #" + salaChat + " e voltar à tela de entrada. Continuar?",
            "Sair do canal", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (escolha != JOptionPane.YES_OPTION) return;
        saindo = true;
        cliente.sair();
        removerIconeBandeja();
        janela.dispose();
        SwingUtilities.invokeLater(() -> new ClienteGUI().iniciar());
    }

    private void configurarIconeBandeja() {
        if (!SystemTray.isSupported()) return;
        BufferedImage imagem = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagem.createGraphics();
        g.setColor(new Color(0x2E7D32));
        g.fillOval(1, 1, 30, 30);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.drawString("C", 9, 23);
        g.dispose();

        PopupMenu menu = new PopupMenu();
        MenuItem abrir = new MenuItem("Abrir chat");
        abrir.addActionListener(e -> restaurarJanela());
        MenuItem sair = new MenuItem("Sair do chat");
        sair.addActionListener(e -> {
            saindo = true;
            cliente.sair();
            removerIconeBandeja();
            janela.dispose();
            System.exit(0);
        });
        menu.add(abrir);
        menu.addSeparator();
        menu.add(sair);

        iconeBandeja = new TrayIcon(imagem, "Chat - " + nome, menu);
        iconeBandeja.setImageAutoSize(true);
        iconeBandeja.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) restaurarJanela();
            }
        });
        try {
            SystemTray.getSystemTray().add(iconeBandeja);
        } catch (AWTException | SecurityException e) {
            iconeBandeja = null;
            System.err.println("Não foi possível ativar as notificações da bandeja: " + e.getMessage());
        }
    }

    private void notificarMensagem(String remetente, boolean privada) {
        boolean minimizada = janela != null
            && (janela.getExtendedState() & JFrame.ICONIFIED) != 0;
        if (janela == null) return;
        String nomeRemetente = remetente == null || remetente.isBlank()
            ? "Alguém" : remetente;
        String resumo = nomeRemetente + (privada ? " enviou uma mensagem privada" : " enviou uma mensagem");
        registrarNotificacao(resumo, true);
        if (janela.isFocused() && janela.isVisible() && !minimizada) return;
        mensagensNaoLidas++;
        atualizarTitulo();
        String tipo = privada ? "mensagem privada" : "nova mensagem";
        String aviso = nomeRemetente + " enviou " + tipo + " no canal #" + salaChat + ".";
        if (iconeBandeja != null) {
            iconeBandeja.displayMessage("Chat: nova mensagem", aviso, TrayIcon.MessageType.INFO);
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void registrarNotificacao(String mensagem, boolean exibirToast) {
        notificacoes.addFirst(java.time.LocalTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + "  " + mensagem);
        while (notificacoes.size() > 12) notificacoes.removeLast();
        atualizarBotaoSino();
        if (exibirToast) mostrarToast(mensagem);
    }

    private void atualizarBotaoSino() {
        if (botaoSino != null) {
            botaoSino.setText(notificacoes.isEmpty() ? "♢" : "♢ " + notificacoes.size());
        }
    }

    private void mostrarNotificacoes() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(new Color(0x191E32));
        JLabel titulo = new JLabel("  Notificações recentes");
        titulo.setForeground(new Color(0xA78BFA));
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        titulo.setBorder(BorderFactory.createEmptyBorder(10, 8, 8, 8));
        menu.add(titulo);
        if (notificacoes.isEmpty()) {
            JLabel vazio = new JLabel("  Nenhuma notificação ainda");
            vazio.setForeground(new Color(0x9CA3AF));
            vazio.setBorder(BorderFactory.createEmptyBorder(8, 8, 12, 8));
            menu.add(vazio);
        } else {
            for (String item : notificacoes) {
                JLabel linha = new JLabel("  " + item);
                linha.setForeground(new Color(0xE5E7EB));
                linha.setBorder(BorderFactory.createEmptyBorder(7, 8, 7, 14));
                menu.add(linha);
            }
        }
        JButton limpar = new JButton("Limpar notificações");
        limpar.addActionListener(e -> {
            notificacoes.clear();
            mensagensNaoLidas = 0;
            atualizarBotaoSino();
            atualizarTitulo();
            menu.setVisible(false);
        });
        menu.addSeparator();
        menu.add(limpar);
        menu.show(botaoSino, 0, botaoSino.getHeight());
    }

    private void mostrarToast(String mensagem) {
        if (GraphicsEnvironment.isHeadless() || janela == null) return;
        if (timerNotificacao != null) timerNotificacao.stop();
        if (janelaNotificacao != null) janelaNotificacao.dispose();
        janelaNotificacao = new JWindow(janela);
        JPanel cartao = new JPanel(new BorderLayout(10, 0));
        cartao.setBackground(new Color(0x191E32));
        cartao.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x6738F5)),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)));
        JLabel icone = new JLabel("●");
        icone.setForeground(new Color(0xA78BFA));
        JLabel texto = new JLabel("<html><b>Messenger</b><br>"
            + escaparHtml(mensagem) + "</html>");
        texto.setForeground(new Color(0xE5E7EB));
        cartao.add(icone, BorderLayout.WEST);
        cartao.add(texto, BorderLayout.CENTER);
        janelaNotificacao.setContentPane(cartao);
        janelaNotificacao.pack();
        java.awt.Point local = janela.getLocationOnScreen();
        janelaNotificacao.setLocation(local.x + janela.getWidth() - janelaNotificacao.getWidth() - 24,
            local.y + janela.getHeight() - janelaNotificacao.getHeight() - 48);
        janelaNotificacao.setVisible(true);
        timerNotificacao = new javax.swing.Timer(4200, e -> {
            if (janelaNotificacao != null) janelaNotificacao.dispose();
        });
        timerNotificacao.setRepeats(false);
        timerNotificacao.start();
    }

    private void limparNotificacoes() {
        mensagensNaoLidas = 0;
        atualizarTitulo();
    }

    private void atualizarTitulo() {
        if (janela == null) return;
        String pendentes = mensagensNaoLidas == 0 ? "" : " (" + mensagensNaoLidas + " novas)";
        janela.setTitle("Chat - " + nome + " | #" + salaChat
            + " (" + pessoasOnline + " online)" + pendentes);
        if (rotuloOnline != null) {
            rotuloOnline.setText(pessoasOnline + (pessoasOnline == 1 ? " pessoa online" : " pessoas online"));
        }
        if (rotuloCanalOnline != null) {
            rotuloCanalOnline.setText("●  " + pessoasOnline + " online");
        }
        if (iconeBandeja != null) {
            iconeBandeja.setToolTip("Chat - #" + salaChat + pendentes);
        }
    }

    private void restaurarJanela() {
        if (janela == null) return;
        janela.setVisible(true);
        janela.setExtendedState(janela.getExtendedState() & ~JFrame.ICONIFIED);
        janela.toFront();
        janela.requestFocus();
    }

    private void removerIconeBandeja() {
        if (iconeBandeja != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(iconeBandeja);
            iconeBandeja = null;
        }
    }

    private void mostrarEmojis(Component origem) {
        if (popupEmoji == null) {
            popupEmoji = new JPopupMenu();
            javax.swing.JTabbedPane categorias = new javax.swing.JTabbedPane();
            Font f = new Font(FONTE_EMOJI, Font.PLAIN, 22);
            String[][] grupos = {
                {"😀","😃","😄","😁","😆","😅","🤣","😂","🙂","🙃","😉","😊","😇","🥰","😍","🤩","😘","😗","😚","😋","😛","🤪","😎","🤓","🥳","😏","😔","😢","😭","😡","🤔","🤗","😴","🤒","🥺"},
                {"👋","🤚","🖐️","✋","🖖","👌","🤌","🤏","✌️","🤞","🫰","🤟","🤘","🤙","👈","👉","👆","👇","☝️","👍","👎","✊","👊","👏","🙌","👐","🤲","🙏","💪","🫶","🦾","🖕"},
                {"❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔","❣️","💕","💞","💓","💗","💖","💘","💝","💟","💌","💋","💯","💢","💥","💫","💦","💨","💤","✨","⭐","🌟","🔥","🎉"},
                {"🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐨","🐯","🦁","🐮","🐷","🐸","🐵","🐔","🐧","🐦","🦄","🐝","🦋","🌷","🌹","🌻","🌈","☀️","🌙","⚡","☕","🍕","🍔","🍟","🍰","🍓","🍉"},
                {"⚽","🏀","🏈","⚾","🎾","🏐","🎱","🏓","🏸","🥊","🎯","🎮","🕹️","🎲","🎸","🎹","🎤","🎧","🎬","🚗","🚕","🚌","🚲","✈️","🚀","⏰","📱","💻","📷","💡","🎁","🏆","🎂","☔","✅"}
            };
            String[] icones = {"😀", "👋", "❤️", "🐶", "⚽"};
            for (int i = 0; i < grupos.length; i++) {
                JPanel grade = new JPanel(new GridLayout(0, 7, 3, 3));
                for (String em : grupos[i]) {
                    JButton b = new JButton(em);
                    b.setFont(f);
                    b.setPreferredSize(new Dimension(38, 36));
                    b.setMargin(new Insets(0, 0, 0, 0));
                    b.setFocusPainted(false);
                    b.addActionListener(e -> {
                        campo.replaceSelection(em);
                        popupEmoji.setVisible(false);
                        campo.requestFocusInWindow();
                    });
                    grade.add(b);
                }
                JScrollPane rolagem = new JScrollPane(grade);
                rolagem.setBorder(BorderFactory.createEmptyBorder());
                categorias.addTab(icones[i], rolagem);
            }
            categorias.setPreferredSize(new Dimension(310, 205));
            popupEmoji.add(categorias);
        }
        popupEmoji.show(origem, 0, -popupEmoji.getPreferredSize().height);
    }

    // ------------------------------------------------- receber da rede (thread de rede)
    private void aoReceber(Protocolo.Recebida r) {
        SwingUtilities.invokeLater(() -> {
            if (Protocolo.IA_RESULTADO.equals(r.tipo())) {
                try {
                    Protocolo.HistoricoAssistente conversa =
                        Protocolo.lerResultadoAssistenteIa(r.texto());
                    salvarHistoricoAssistente(conversa.pergunta(), conversa.resposta());
                    adicionarMensagemAssistente("Assistente IA", conversa.resposta());
                } catch (IllegalArgumentException | IllegalStateException e) {
                    JOptionPane.showMessageDialog(janela, e.getMessage(),
                        "Não foi possível salvar o histórico", JOptionPane.ERROR_MESSAGE);
                }
            } else if (Protocolo.AVATAR.equals(r.tipo())) {
                try {
                    byte[] bytes = java.util.Base64.getUrlDecoder().decode(r.cor());
                    BufferedImage imagem = lerImagem(bytes);
                    if (imagem.getWidth() > 512 || imagem.getHeight() > 512) {
                        throw new IOException("A foto recebida excede as dimensões permitidas.");
                    }
                    String chave = r.texto().toLowerCase(java.util.Locale.ROOT);
                    fotosUsuarios.put(chave, imagem);
                    if (r.texto().equalsIgnoreCase(nome)) avatarProprioRecebido = true;
                    atualizarAvatarNaLista(r.texto(), imagem);
                } catch (IOException | IllegalArgumentException e) {
                    System.err.println("Foto de perfil recebida inválida: " + e.getMessage());
                }
            } else if (Protocolo.AVATAR_REMOVIDO.equals(r.tipo())) {
                fotosUsuarios.remove(r.texto().toLowerCase(java.util.Locale.ROOT));
                atualizarAvatarNaLista(r.texto(), null);
            } else if (Protocolo.AVATARES_SINCRONIZADOS.equals(r.tipo())) {
                if (!avatarProprioRecebido && fotoPerfilLegada != null) {
                    try {
                        cliente.enviarAvatar(serializarFoto(fotoPerfilLegada));
                        preferencias.remove("fotoPerfil");
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(janela, e.getMessage(),
                            "Não foi possível migrar a foto de perfil", JOptionPane.ERROR_MESSAGE);
                    }
                } else if (fotoPerfilLegada != null) {
                    preferencias.remove("fotoPerfil");
                }
                fotoPerfilLegada = null;
            } else if (Protocolo.MSG.equals(r.tipo())) {
                anexar(r.texto(), Color.decode(r.cor()), false);
                mostrarRespostaBola(r.texto());
                if (!enviadaPorMim(r.texto())) {
                    notificarMensagem(remetenteDaMensagem(r.texto()), false);
                    if (janela != null && janela.isFocused()) Toolkit.getDefaultToolkit().beep();
                }
            } else if (Protocolo.ONLINE.equals(r.tipo())) {
                try {
                    pessoasOnline = Integer.parseInt(r.texto());
                    atualizarTitulo();
                } catch (NumberFormatException ignored) { }
            } else if (Protocolo.DIGITANDO.equals(r.tipo())) {
                String[] estado = r.texto().split("\\|", 2);
                if (estado.length == 2) {
                    if (Boolean.parseBoolean(estado[1])) usuariosDigitando.add(estado[0]);
                    else usuariosDigitando.remove(estado[0]);
                    atualizarEstadoDigitacao();
                }
            } else if (Protocolo.USERS.equals(r.tipo())) {
                modeloUsuariosOnline.clear();
                for (String usuarioOnline : r.usuarios()) {
                    modeloUsuariosOnline.addElement(usuarioOnline);
                }
                pessoasOnline = r.usuarios().size();
                atualizarTitulo();
            } else if (Protocolo.ANEXO.equals(r.tipo()) && r.anexo() != null) {
                registrarNotificacao("Arquivo recebido: " + r.anexo().nome(), true);
                mostrarAnexo(r.anexo());
            } else {
                if (r.texto().contains("[privado de ")) {
                    notificarMensagem(remetentePrivado(r.texto()), true);
                }
                if (r.texto().contains(" entrou no chat")) {
                    registrarNotificacao(r.texto().replace("*** ", "").trim(), true);
                } else if (r.texto().contains(" saiu do chat")) {
                    registrarNotificacao(r.texto().replace("*** ", "").trim(), true);
                }
                if (r.texto().contains(" saiu do chat")) {
                    int inicio = r.texto().indexOf("*** ") + 4;
                    int fim = r.texto().indexOf(" saiu do chat");
                    if (inicio >= 4 && fim > inicio) usuariosDigitando.remove(r.texto().substring(inicio, fim));
                    atualizarEstadoDigitacao();
                }
                anexar(r.texto(), new Color(0xB5BAC1), true);
            }
        });
    }

    private void salvarHistoricoAssistente(String pergunta, String resposta) {
        try {
            historicoAssistente.salvar(nome, sala, salaChat, pergunta, resposta);
        } catch (IllegalArgumentException | IllegalStateException e) {
            JOptionPane.showMessageDialog(janela,
                "A resposta chegou, mas não foi possível salvar a conversa: " + e.getMessage(),
                "Erro ao salvar histórico da IA", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean enviadaPorMim(String texto) {
        int inicioNome = texto.indexOf("] ");
        return inicioNome >= 0 && texto.startsWith(nome + ": ", inicioNome + 2);
    }

    private static String remetenteDaMensagem(String texto) {
        int fimHorario = texto.indexOf("] ");
        int separador = texto.indexOf(": ", fimHorario + 2);
        if (fimHorario < 0 || separador < 0) return "";
        return texto.substring(fimHorario + 2, separador).trim();
    }

    private static String remetentePrivado(String texto) {
        String marcador = "[privado de ";
        int inicio = texto.indexOf(marcador);
        if (inicio < 0) return "";
        inicio += marcador.length();
        int fim = texto.indexOf(']', inicio);
        return fim < 0 ? "" : texto.substring(inicio, fim).trim();
    }

    private void atualizarEstadoDigitacao() {
        if (usuariosDigitando.isEmpty()) {
            estadoDigitacao.setText(" ");
        } else if (usuariosDigitando.size() == 1) {
            estadoDigitacao.setText(usuariosDigitando.iterator().next() + " está digitando...");
        } else {
            estadoDigitacao.setText(String.join(", ", usuariosDigitando) + " estão digitando...");
        }
    }

    private void aplicarTemaConversa() {
        if (fundo == null || area == null) return;
        Color destaque = fundo.corTema();
        Color superficie = misturar(new Color(0x11131D), destaque, 0.30f);
        Color superficiePainel = misturar(superficie, new Color(0xFFFFFF), 0.08f);
        area.setBackground(superficie);
        if (rolagemMensagens != null) {
            rolagemMensagens.setBackground(superficie);
            rolagemMensagens.getViewport().setBackground(superficie);
        }
        aplicarPapelParedeNaConversa();
        if (painelConversaTema != null) painelConversaTema.setBackground(superficie);
        if (painelCabecalhoTema != null) painelCabecalhoTema.setBackground(superficiePainel);
        if (painelOnlineTema != null) painelOnlineTema.setBackground(superficiePainel);
        if (painelCentroTema != null) painelCentroTema.setBackground(superficie);
        area.repaint();
    }

    private static Color misturar(Color base, Color destaque, float proporcao) {
        float restante = 1f - proporcao;
        return new Color(
            Math.round(base.getRed() * restante + destaque.getRed() * proporcao),
            Math.round(base.getGreen() * restante + destaque.getGreen() * proporcao),
            Math.round(base.getBlue() * restante + destaque.getBlue() * proporcao));
    }

    private void atualizarDigitacao() {
        if (campo.getText().isBlank()) {
            timerDigitando.stop();
            pararDeDigitar();
            return;
        }
        if (!avisandoDigitacao) {
            cliente.digitando(true);
            avisandoDigitacao = true;
        }
        timerDigitando.restart();
    }

    private void pararDeDigitar() {
        if (avisandoDigitacao) {
            cliente.digitando(false);
            avisandoDigitacao = false;
        }
    }

    private void aoDesconectar() {
        if (saindo) return;
        SwingUtilities.invokeLater(() -> {
            anexar("*** Desconectado do servidor ***", Color.RED, true);
            campo.setEnabled(false);
        });
    }

    /** Adiciona mensagens como balões e avisos como eventos compactos da conversa. */
    private void anexar(String texto, Color cor, boolean italico) {
        if (cartaoBoasVindas != null) {
            area.remove(cartaoBoasVindas);
            cartaoBoasVindas = null;
        }
        Matcher mensagem = PADRAO_MENSAGEM.matcher(texto);
        JPanel linha;
        if (!italico && mensagem.matches()) {
            linha = criarBalao(mensagem.group(2), mensagem.group(1), mensagem.group(3), cor);
        } else {
            linha = criarAviso(texto, cor, italico);
        }
        linha.setAlignmentX(Component.LEFT_ALIGNMENT);
        area.add(linha);
        area.add(Box.createVerticalStrut(12));
        area.revalidate();
        area.repaint();
        SwingUtilities.invokeLater(() -> {
            if (rolagemMensagens != null) {
                rolagemMensagens.getVerticalScrollBar().setValue(
                    rolagemMensagens.getVerticalScrollBar().getMaximum());
            }
        });
    }

    private JPanel criarBalao(String autor, String horario, String conteudo, Color cor) {
        MessageRow linha = new MessageRow();
        linha.setOpaque(false);
        linha.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 8));

        JPanel colunaTexto = new JPanel();
        colunaTexto.setOpaque(false);
        colunaTexto.setLayout(new BoxLayout(colunaTexto, BoxLayout.Y_AXIS));
        JPanel cabecalho = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        cabecalho.setOpaque(false);
        JLabel nomeAutor = new JLabel(autor);
        nomeAutor.setForeground(cor);
        nomeAutor.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        JLabel hora = new JLabel(horario);
        hora.setForeground(new Color(0x858BA0));
        hora.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        cabecalho.add(nomeAutor);
        cabecalho.add(hora);
        cabecalho.setAlignmentX(Component.LEFT_ALIGNMENT);

        RoundedPanel balao = new RoundedPanel(new BorderLayout());
        balao.setBackground(autor.equalsIgnoreCase(nome)
            ? new Color(0x29234F) : new Color(0x191E32));
        balao.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JTextPane texto = new JTextPane();
        texto.setEditorKit(new QuebraLinhaEditorKit());
        texto.setEditable(false);
        texto.setOpaque(false);
        texto.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        texto.setForeground(new Color(0xE7E9F2));
        texto.setMargin(new Insets(0, 0, 0, 0));
        boolean respostaIa = autor.equalsIgnoreCase("Assistente IA");
        String conteudoIa = conteudo;
        FormatadorRespostaIA.Resultado respostaFormatada = respostaIa
            ? FormatadorRespostaIA.formatar(conteudoIa) : null;
        String conteudoExibido = respostaIa ? respostaFormatada.texto() : conteudo;
        texto.setText(conteudoExibido);
        StyledDocument documento = texto.getStyledDocument();
        if (respostaIa) {
            SimpleAttributeSet negrito = new SimpleAttributeSet();
            StyleConstants.setBold(negrito, true);
            for (FormatadorRespostaIA.Trecho trecho : respostaFormatada.negrito()) {
                documento.setCharacterAttributes(trecho.inicio(), trecho.comprimento(), negrito, false);
            }
            SimpleAttributeSet estiloTitulo = new SimpleAttributeSet();
            StyleConstants.setBold(estiloTitulo, true);
            StyleConstants.setFontSize(estiloTitulo, 16);
            StyleConstants.setForeground(estiloTitulo, new Color(0xAFCBFF));
            for (FormatadorRespostaIA.Trecho trecho : respostaFormatada.titulos()) {
                documento.setCharacterAttributes(
                    trecho.inicio(), trecho.comprimento(), estiloTitulo, false);
            }
        }
        java.awt.FontMetrics metricasTexto = texto.getFontMetrics(texto.getFont());
        int largura = larguraBalao(conteudoExibido, metricasTexto);
        View raizTexto = texto.getUI().getRootView(texto);
        raizTexto.setSize(largura, Integer.MAX_VALUE);
        int altura = Math.max(metricasTexto.getHeight(),
            (int) Math.ceil(raizTexto.getPreferredSpan(View.Y_AXIS)));
        altura += respostaIa ? respostaFormatada.titulos().size() * 4 : 0;
        texto.setPreferredSize(new Dimension(largura, altura));
        texto.setSize(new Dimension(largura, altura));
        balao.setPreferredSize(new Dimension(largura + 24, altura + 16));
        balao.setMaximumSize(new Dimension(largura + 24, altura + 16));
        balao.setMinimumSize(new Dimension(largura + 24, altura + 16));
        if (anexoProximaMensagem != null) {
            SimpleAttributeSet anexo = new SimpleAttributeSet();
            anexo.addAttribute("chatAnexoId", anexoProximaMensagem);
            documento.setCharacterAttributes(0, documento.getLength(), anexo, false);
            anexoProximaMensagem = null;
        }
        try {
            registrarLinks(documento, 0);
        } catch (BadLocationException e) {
            throw new IllegalStateException("Não foi possível aplicar os links da mensagem.", e);
        }
        texto.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                abrirLink(texto, e);
            }
        });
        texto.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                texto.setCursor(Cursor.getPredefinedCursor(
                    atributoNoPonto(texto, e.getPoint(), ATRIBUTO_LINK) instanceof String
                        || atributoNoPonto(texto, e.getPoint(), "chatAnexoId") instanceof String
                            ? Cursor.HAND_CURSOR : Cursor.TEXT_CURSOR));
            }
        });
        balao.add(texto, BorderLayout.CENTER);
        balao.setAlignmentX(Component.LEFT_ALIGNMENT);

        colunaTexto.add(cabecalho);
        colunaTexto.add(Box.createVerticalStrut(4));
        colunaTexto.add(balao);
        colunaTexto.setMaximumSize(new Dimension(largura + 24, Integer.MAX_VALUE));
        AvatarLabel avatar = new AvatarLabel(autor, corAvatar(autor),
            fotosUsuarios.get(autor.toLowerCase(java.util.Locale.ROOT)));
        String chaveAvatar = autor.toLowerCase(java.util.Locale.ROOT);
        avataresRenderizados.computeIfAbsent(chaveAvatar, ignorada -> new java.util.ArrayList<>())
            .add(avatar);
        avatar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        avatar.setToolTipText("Clique para visualizar a foto de " + autor);
        avatar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) mostrarFotoUsuario(autor);
            }
        });
        avatar.setAlignmentY(Component.TOP_ALIGNMENT);
        colunaTexto.setAlignmentY(Component.TOP_ALIGNMENT);
        if (autor.equalsIgnoreCase(nome)) {
            linha.add(Box.createHorizontalGlue());
            linha.add(colunaTexto);
            linha.add(Box.createHorizontalStrut(9));
            linha.add(avatar);
        } else {
            linha.add(avatar);
            linha.add(Box.createHorizontalStrut(9));
            linha.add(colunaTexto);
            linha.add(Box.createHorizontalGlue());
        }
        return linha;
    }

    private JPanel criarAviso(String texto, Color cor, boolean destaque) {
        JPanel linha = new MessageRow();
        linha.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 2));
        linha.setOpaque(false);
        JTextPane aviso = new JTextPane();
        aviso.setEditable(false);
        aviso.setOpaque(false);
        aviso.setFont(new Font(Font.SANS_SERIF, destaque ? Font.PLAIN : Font.BOLD,
            destaque ? 12 : 11));
        aviso.setForeground(cor);
        aviso.setText(texto);
        int largura = larguraBalao(texto, aviso.getFontMetrics(aviso.getFont()));
        aviso.setPreferredSize(new Dimension(largura, alturaBalao(texto,
            aviso.getFontMetrics(aviso.getFont()), largura)));
        StyledDocument documento = aviso.getStyledDocument();
        if (destaque) {
            SimpleAttributeSet italico = new SimpleAttributeSet();
            StyleConstants.setItalic(italico, true);
            documento.setCharacterAttributes(0, documento.getLength(), italico, false);
        }
        if (anexoProximaMensagem != null) {
            SimpleAttributeSet anexo = new SimpleAttributeSet();
            anexo.addAttribute("chatAnexoId", anexoProximaMensagem);
            documento.setCharacterAttributes(0, documento.getLength(), anexo, false);
            anexoProximaMensagem = null;
        }
        try {
            registrarLinks(documento, 0);
        } catch (BadLocationException e) {
            throw new IllegalStateException("Não foi possível aplicar os links do aviso.", e);
        }
        aviso.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                abrirLink(aviso, e);
            }
        });
        aviso.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                aviso.setCursor(Cursor.getPredefinedCursor(
                    atributoNoPonto(aviso, e.getPoint(), ATRIBUTO_LINK) instanceof String
                        || atributoNoPonto(aviso, e.getPoint(), "chatAnexoId") instanceof String
                            ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        });
        RoundedPanel cartaoAviso = new RoundedPanel(new BorderLayout());
        cartaoAviso.setBackground(new Color(0x191E32));
        cartaoAviso.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        cartaoAviso.add(aviso, BorderLayout.CENTER);
        if (texto.startsWith("***")) {
            cartaoAviso.setBackground(new Color(0x33263A));
        }
        linha.add(cartaoAviso);
        return linha;
    }

    private int larguraBalao(String texto, java.awt.FontMetrics metricas) {
        int maior = 0;
        for (String linha : texto.split("\\R", -1)) {
            maior = Math.max(maior, metricas.stringWidth(linha));
        }
        return Math.max(24, Math.min(560, maior + 2));
    }

    private int alturaBalao(String texto, java.awt.FontMetrics metricas, int largura) {
        int linhas = 0;
        for (String linha : texto.split("\\R", -1)) {
            int medida = Math.max(1, metricas.stringWidth(linha));
            linhas += Math.max(1, (medida + largura - 1) / largura);
        }
        return Math.max(metricas.getHeight() + 2, linhas * metricas.getHeight() + 2);
    }

    private Color corAvatar(String autor) {
        if (autor.equalsIgnoreCase(nome)) return new Color(0x6738F5);
        Color[] cores = {
            new Color(0xEC4899), new Color(0x3B82F6), new Color(0xF59E0B),
            new Color(0x22C55E), new Color(0xA78BFA)
        };
        return cores[Math.floorMod(autor.toLowerCase(java.util.Locale.ROOT).hashCode(), cores.length)];
    }

    private static final class QuebraLinhaEditorKit extends StyledEditorKit {
        private final ViewFactory fabrica = elemento -> {
            String tipo = elemento.getName();
            if (AbstractDocument.ContentElementName.equals(tipo)) {
                return new LabelView(elemento) {
                    @Override
                    public float getMinimumSpan(int eixo) {
                        return eixo == View.X_AXIS ? 0 : super.getMinimumSpan(eixo);
                    }
                };
            }
            if (AbstractDocument.ParagraphElementName.equals(tipo)) return new ParagraphView(elemento);
            if (AbstractDocument.SectionElementName.equals(tipo)) return new BoxView(elemento, View.Y_AXIS);
            if (StyleConstants.ComponentElementName.equals(tipo)) return new ComponentView(elemento);
            if (StyleConstants.IconElementName.equals(tipo)) return new IconView(elemento);
            return new LabelView(elemento);
        };

        @Override
        public ViewFactory getViewFactory() {
            return fabrica;
        }
    }

    private static class AvatarLabel extends JLabel {
        private final String nome;
        private Image imagem;

        AvatarLabel(String nome, Color cor) {
            this(nome, cor, null);
        }

        AvatarLabel(String nome, Color cor, Image imagem) {
            super(nome.isBlank() ? "?" : nome.substring(0, 1).toUpperCase(java.util.Locale.ROOT),
                JLabel.CENTER);
            this.nome = nome;
            this.imagem = imagem == null ? AvatarPortrait.para(nome) : imagem;
            setForeground(Color.WHITE);
            setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            setPreferredSize(new Dimension(32, 32));
            setMinimumSize(new Dimension(32, 32));
            setMaximumSize(new Dimension(32, 32));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            desenharImagemCircular(g2, imagem, 0, 0, Math.min(getWidth(), getHeight()));
            g2.dispose();
        }

        void setImagem(Image imagem) {
            this.imagem = imagem == null ? AvatarPortrait.para(nome) : imagem;
            repaint();
        }
    }

    private static final class MascoteBola extends JPanel {
        private final int tamanho;
        private final BufferedImage imagem = AvatarPortrait.para("Bola");

        MascoteBola(int tamanho) {
            this.tamanho = tamanho;
            setOpaque(false);
            setPreferredSize(new Dimension(tamanho, tamanho));
            setMinimumSize(new Dimension(tamanho, tamanho));
            setMaximumSize(new Dimension(tamanho, tamanho));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int lado = Math.min(tamanho, Math.min(getWidth(), getHeight()));
            int x = (getWidth() - lado) / 2;
            int y = (getHeight() - lado) / 2;
            g2.drawImage(imagem, x, y, lado, lado, null);
            g2.dispose();
        }
    }

    private static class AvatarIcon implements javax.swing.Icon {
        private final int tamanho;
        private final Image imagem;

        AvatarIcon(String nome, Color cor, int tamanho) {
            this(nome, cor, tamanho, null);
        }

        AvatarIcon(String nome, Color cor, int tamanho, Image imagem) {
            this.tamanho = tamanho;
            this.imagem = imagem == null ? AvatarPortrait.para(nome) : imagem;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            desenharImagemCircular(g2, imagem, x, y, tamanho);
            g2.setColor(new Color(0x22C55E));
            g2.fillOval(x + tamanho - 8, y + tamanho - 8, 8, 8);
            g2.dispose();
        }

        @Override public int getIconWidth() { return tamanho; }
        @Override public int getIconHeight() { return tamanho; }
    }

    private static void desenharImagemCircular(Graphics2D g, Image imagem, int x, int y, int tamanho) {
        int larguraOriginal = imagem.getWidth(null);
        int alturaOriginal = imagem.getHeight(null);
        if (larguraOriginal < 1 || alturaOriginal < 1 || tamanho < 1) return;
        java.awt.Shape clip = g.getClip();
        g.setClip(new java.awt.geom.Ellipse2D.Float(x, y, tamanho, tamanho));
        double escala = Math.max((double) tamanho / larguraOriginal, (double) tamanho / alturaOriginal);
        int largura = (int) Math.ceil(larguraOriginal * escala);
        int altura = (int) Math.ceil(alturaOriginal * escala);
        g.drawImage(imagem, x + (tamanho - largura) / 2, y + (tamanho - altura) / 2,
            largura, altura, null);
        g.setClip(clip);
    }

    private static final class PainelVisualizacaoFoto extends JPanel {
        private final Image imagem;
        private final double escalaInicial;
        private double zoom = 1.0;

        PainelVisualizacaoFoto(Image imagem) {
            this.imagem = imagem;
            int largura = imagem.getWidth(null);
            int altura = imagem.getHeight(null);
            escalaInicial = Math.min(1.0, Math.min(532.0 / largura, 392.0 / altura));
            addMouseWheelListener(e -> ajustarZoom(e.getWheelRotation() < 0 ? 1.1 : 0.9));
            atualizarTamanho();
        }

        void ajustarZoom(double fator) {
            zoom = Math.max(0.5, Math.min(4.0, zoom * fator));
            atualizarTamanho();
        }

        private void atualizarTamanho() {
            int largura = Math.max(1, (int) Math.round(imagem.getWidth(null) * escalaInicial * zoom));
            int altura = Math.max(1, (int) Math.round(imagem.getHeight(null) * escalaInicial * zoom));
            setPreferredSize(new Dimension(largura + 20, altura + 20));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int largura = (int) Math.round(imagem.getWidth(null) * escalaInicial * zoom);
            int altura = (int) Math.round(imagem.getHeight(null) * escalaInicial * zoom);
            int x = (getWidth() - largura) / 2;
            int y = (getHeight() - altura) / 2;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(imagem, x, y, largura, altura, null);
            g2.dispose();
        }
    }

    private static final class AvatarPortrait {
        private static final int TAMANHO = 64;
        private static final Map<String, BufferedImage> CACHE = new LinkedHashMap<>();
        private static final Set<String> NOMES_MASCULINOS = Set.of(
            "adriano", "alex", "anderson", "andre", "antonio", "arthur", "bernardo", "bruno",
            "carlos", "daniel", "davi", "diego", "eduardo", "felipe", "fernando",
            "francisco", "gabriel", "gustavo", "henrique", "joao", "jose", "leonardo",
            "lucas", "luciano", "marcelo", "marcos", "matheus", "miguel", "murilo",
            "paulo", "pedro", "rafael", "roberto", "thiago", "victor", "vinicius",
            "william");
        private static final Set<String> NOMES_FEMININOS = Set.of(
            "aline", "amanda", "ana", "andrea", "beatriz", "bruna", "camila",
            "carla", "carolina", "caroline", "claudia", "eduarda", "fernanda",
            "gabriela", "isabela", "jessica", "juliana", "julia", "larissa",
            "leticia", "luana", "luiza", "mariana", "marina", "maria", "natalia",
            "patricia", "rafaela", "sabrina", "sofia", "tais", "thayse", "valentina",
            "vanessa", "viviane");
        private static final Color[][] FUNDOS = {
            {new Color(0x5334A5), new Color(0xAA70E8)},
            {new Color(0x075985), new Color(0x38BDF8)},
            {new Color(0x9D174D), new Color(0xF472B6)},
            {new Color(0x166534), new Color(0x4ADE80)},
            {new Color(0x9A3412), new Color(0xFDBA74)},
            {new Color(0x3730A3), new Color(0x818CF8)}
        };
        private static final Color[] PELES = {
            new Color(0xF1C7A5), new Color(0xD99B72), new Color(0xA96845),
            new Color(0x75452F), new Color(0xE7B58E), new Color(0xC9825D)
        };
        private static final Color[] CABELOS = {
            new Color(0x30231F), new Color(0x171717), new Color(0x6B3F2A),
            new Color(0xB77932), new Color(0x49365F), new Color(0x303B55)
        };
        private static final Color[] ROUPAS = {
            new Color(0xF5F3FF), new Color(0x172554), new Color(0x312E81),
            new Color(0x831843), new Color(0x14532D), new Color(0x7C2D12)
        };

        private enum Genero { MASCULINO, FEMININO, NAO_IDENTIFICADO }

        private AvatarPortrait() { }

        static synchronized BufferedImage para(String nome) {
            String chave = normalizar(nome);
            return CACHE.computeIfAbsent(chave, AvatarPortrait::criar);
        }

        private static String normalizar(String nome) {
            String limpo = nome == null ? "" : nome.trim().toLowerCase(java.util.Locale.ROOT);
            return Normalizer.normalize(limpo, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        }

        private static BufferedImage criar(String nome) {
            if (nome.equals("bola") || nome.equals("bola ia")) return criarMascote();
            String primeiroNome = nome.split("[\\s-]", 2)[0];
            char inicial = primeiroNome.isEmpty() ? '?' : Character.toUpperCase(primeiroNome.charAt(0));
            int variante = Math.floorMod(inicial, FUNDOS.length);
            Genero genero = NOMES_MASCULINOS.contains(primeiroNome)
                ? Genero.MASCULINO
                : NOMES_FEMININOS.contains(primeiroNome)
                    ? Genero.FEMININO : Genero.NAO_IDENTIFICADO;
            BufferedImage retrato = new BufferedImage(TAMANHO, TAMANHO, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = retrato.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, TAMANHO, TAMANHO));

            g.setPaint(new java.awt.GradientPaint(0, 0, FUNDOS[variante][0],
                TAMANHO, TAMANHO, FUNDOS[variante][1]));
            g.fillRect(0, 0, TAMANHO, TAMANHO);
            g.setColor(new Color(255, 255, 255, 36));
            g.fillOval(5 + variante * 2, 4, 22, 22);
            g.fillOval(45, 14 + variante, 8, 8);

            g.setColor(ROUPAS[variante]);
            g.fillOval(7, 42, 50, 37);
            g.setColor(misturar(ROUPAS[variante], Color.BLACK, 0.13f));
            g.fillOval(19, 47, 27, 24);

            g.setColor(PELES[variante]);
            g.fillRoundRect(26, 34, 13, 15, 6, 6);
            g.fillOval(17, 13, 31, 33);
            g.fillOval(12, 27, 8, 12);
            g.fillOval(45, 27, 8, 12);

            g.setColor(CABELOS[variante]);
            if (genero == Genero.FEMININO) {
                g.fillOval(13, 7, 38, 45);
                g.setColor(PELES[variante]);
                g.fillOval(19, 20, 27, 25);
                g.setColor(CABELOS[variante]);
                g.fillRoundRect(14, 23, 8, 24, 5, 5);
                g.fillRoundRect(42, 23, 8, 24, 5, 5);
                g.fillArc(17, 7, 32, 24, 0, 180);
            } else if (genero == Genero.MASCULINO) {
                g.fillOval(15, 8, 35, 26);
                g.setColor(PELES[variante]);
                g.fillOval(19, 21, 27, 24);
                g.setColor(CABELOS[variante]);
                g.fillArc(17, 8, 33, 22, 0, 180);
            } else switch (variante % 3) {
                case 0 -> {
                    g.fillOval(15, 8, 35, 25);
                    g.setColor(PELES[variante]);
                    g.fillOval(19, 20, 27, 25);
                    g.setColor(CABELOS[variante]);
                    g.fillArc(17, 8, 33, 23, 0, 180);
                }
                case 1 -> {
                    g.fillOval(15, 9, 35, 26);
                    g.fillRoundRect(15, 17, 8, 22, 5, 5);
                    g.fillRoundRect(43, 17, 8, 22, 5, 5);
                    g.setColor(PELES[variante]);
                    g.fillOval(19, 21, 27, 24);
                    g.setColor(CABELOS[variante]);
                    g.fillArc(18, 8, 30, 20, 0, 180);
                }
                default -> {
                    g.fillOval(16, 8, 34, 28);
                    g.setColor(PELES[variante]);
                    g.fillOval(19, 21, 27, 24);
                    g.setColor(CABELOS[variante]);
                    g.fillOval(18, 8, 30, 15);
                    g.fillPolygon(new int[] {20, 32, 44}, new int[] {22, 17, 22}, 3);
                }
            }

            g.setColor(new Color(0x34251F));
            g.fillOval(25, 30, 2, 2);
            g.fillOval(38, 30, 2, 2);
            g.setColor(new Color(0x9A4F55));
            g.drawArc(28, 33, 9, 5, 205, 130);
            g.setClip(null);
            g.setColor(new Color(255, 255, 255, 90));
            g.setStroke(new java.awt.BasicStroke(1.5f));
            g.drawOval(1, 1, TAMANHO - 3, TAMANHO - 3);
            g.dispose();
            return retrato;
        }

        private static BufferedImage criarMascote() {
            BufferedImage retrato = new BufferedImage(TAMANHO, TAMANHO, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = retrato.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, TAMANHO, TAMANHO));
            g.setPaint(new java.awt.GradientPaint(0, 0, new Color(0x123C67),
                TAMANHO, TAMANHO, new Color(0x542B91)));
            g.fillRect(0, 0, TAMANHO, TAMANHO);
            g.setColor(new Color(108, 99, 255, 58));
            g.fillOval(3, 5, 58, 58);
            g.setColor(new Color(0x17345A));
            g.fillRoundRect(27, 8, 10, 11, 5, 5);
            g.setColor(new Color(0x75E6FF));
            g.fillOval(30, 5, 4, 5);
            g.setColor(new Color(0xA8C6E8));
            g.fillRoundRect(11, 18, 42, 38, 13, 13);
            g.setColor(new Color(0xD6E7F5));
            g.fillRoundRect(14, 17, 36, 34, 11, 11);
            g.setColor(new Color(0x12223E));
            g.fillRoundRect(18, 23, 28, 19, 8, 8);
            g.setColor(new Color(0x50D9FF));
            g.fillOval(23, 29, 6, 6);
            g.fillOval(35, 29, 6, 6);
            g.setColor(new Color(0xC6FAFF));
            g.fillOval(24, 30, 2, 2);
            g.fillOval(36, 30, 2, 2);
            g.setColor(new Color(0x8DEBFF));
            g.setStroke(new java.awt.BasicStroke(2f, java.awt.BasicStroke.CAP_ROUND,
                java.awt.BasicStroke.JOIN_ROUND));
            g.drawArc(26, 34, 12, 6, 200, 140);
            g.setColor(new Color(0x75C9FF));
            g.fillRoundRect(6, 30, 7, 13, 4, 4);
            g.fillRoundRect(51, 30, 7, 13, 4, 4);
            g.setColor(new Color(0xA78BFA));
            g.fillRoundRect(18, 53, 28, 15, 7, 7);
            g.setColor(new Color(0xD9CEFF));
            g.fillOval(29, 56, 6, 6);
            g.dispose();
            return retrato;
        }
    }

    private void registrarLinks(StyledDocument doc, int inicio) throws BadLocationException {
        String texto = doc.getText(inicio, doc.getLength() - inicio);
        Matcher matcher = PADRAO_LINK.matcher(texto);
        while (matcher.find()) {
            String url = matcher.group();
            while (!url.isEmpty() && ".,;:!?)]}".indexOf(url.charAt(url.length() - 1)) >= 0) {
                url = url.substring(0, url.length() - 1);
            }
            if (url.isEmpty()) continue;
            SimpleAttributeSet estilo = new SimpleAttributeSet();
            StyleConstants.setForeground(estilo, new Color(0x7EA4FF));
            StyleConstants.setUnderline(estilo, true);
            estilo.addAttribute(ATRIBUTO_LINK, url);
            doc.setCharacterAttributes(inicio + matcher.start(), url.length(), estilo, false);
        }
    }

    private void abrirLink(JTextPane texto, MouseEvent evento) {
        int posicao = texto.viewToModel2D(evento.getPoint());
        if (posicao < 0) return;
        Object link = atributoNaPosicao(texto, posicao, ATRIBUTO_LINK);
        if (!(link instanceof String url)) {
            Object idAnexo = atributoNaPosicao(texto, posicao, "chatAnexoId");
            if (idAnexo instanceof String id) salvarAnexo(id);
            return;
        }
        try {
            if (!Desktop.isDesktopSupported()) {
                throw new IOException("Este computador não oferece suporte para abrir links.");
            }
            Desktop.getDesktop().browse(URI.create(url));
        } catch (IOException | IllegalArgumentException | UnsupportedOperationException e) {
            JOptionPane.showMessageDialog(janela, "Não foi possível abrir o link: " + e.getMessage(),
                "Erro ao abrir notícia", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Object atributoNaPosicao(JTextPane texto, int posicao, String atributo) {
        return texto.getStyledDocument().getCharacterElement(posicao)
            .getAttributes().getAttribute(atributo);
    }

    private Object atributoNoPonto(JTextPane texto, java.awt.Point ponto, String atributo) {
        int posicao = texto.viewToModel2D(ponto);
        return posicao < 0 ? null : atributoNaPosicao(texto, posicao, atributo);
    }

    private void mostrarAnexo(Protocolo.Anexo anexo) {
        String id = UUID.randomUUID().toString();
        anexosRecebidos.put(id, anexo);
        while (anexosRecebidos.size() > 5) {
            Iterator<String> antigos = anexosRecebidos.keySet().iterator();
            antigos.next();
            antigos.remove();
        }

        int bytes = anexo.conteudo().length;
        String tamanho = bytes >= 1024 * 1024
            ? String.format(java.util.Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0))
            : Math.max(1, bytes / 1024) + " KB";
        String rotulo = (anexo.mime().startsWith("image/") ? "[Foto] " : "[Arquivo] ")
            + anexo.nome() + " (" + tamanho + ") — clique para salvar";
        anexoProximaMensagem = id;
        anexar(rotulo, new Color(0x7EA4FF), true);
    }

    private void salvarAnexo(String id) {
        Protocolo.Anexo anexo = anexosRecebidos.get(id);
        if (anexo == null) {
            JOptionPane.showMessageDialog(janela,
                "Este anexo não está mais disponível. Ele fica temporariamente na memória do chat.",
                "Anexo indisponível", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser seletor = new JFileChooser();
        seletor.setDialogTitle("Salvar anexo");
        seletor.setSelectedFile(new java.io.File(anexo.nome()));
        if (seletor.showSaveDialog(janela) != JFileChooser.APPROVE_OPTION) return;
        Path destino = seletor.getSelectedFile().toPath();
        if (Files.exists(destino)) {
            int sobrescrever = JOptionPane.showConfirmDialog(janela,
                "O arquivo já existe. Deseja substituir?", "Confirmar substituição",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (sobrescrever != JOptionPane.YES_OPTION) return;
        }
        try {
            Files.write(destino, anexo.conteudo());
            anexar("Anexo salvo em: " + destino, new Color(0xB5BAC1), true);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(janela, "Não foi possível salvar o arquivo: " + e.getMessage(),
                "Erro ao salvar anexo", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------------- utilidades
    private static String hex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static JButton botaoEstilizado(String texto) {
        JButton botao = new BotaoArredondado(texto);
        botao.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        botao.setForeground(new Color(0xDBDEE1));
        botao.setBackground(new Color(0x4E5058));
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        botao.setFocusPainted(false);
        botao.setContentAreaFilled(false);
        botao.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        return botao;
    }

    private static class RoundedPanel extends JPanel {
        RoundedPanel(java.awt.LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class MessageRow extends JPanel {
        private float progresso;
        private final javax.swing.Timer animacao;

        MessageRow() {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            animacao = new javax.swing.Timer(16, e -> {
                progresso = Math.min(1f, progresso + 0.09f);
                repaint();
                if (progresso >= 1f) ((javax.swing.Timer) e.getSource()).stop();
            });
            animacao.start();
        }

        @Override
        protected void paintChildren(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(progresso));
            g2.translate(0, Math.round((1f - progresso) * 10));
            super.paintChildren(g2);
            g2.dispose();
        }
    }

    private static class PainelOndas extends JPanel {
        private float fase;
        private final javax.swing.Timer animacao = new javax.swing.Timer(40, e -> {
            fase += 0.025f;
            repaint();
        });

        PainelOndas() {
            setOpaque(true);
            setBackground(new Color(0x17192D));
        }

        private static class PainelCabecalho extends JPanel {
            private float fase;
            private final javax.swing.Timer animacao = new javax.swing.Timer(40, e -> {
                fase += 0.018f;
                repaint();
            });

            PainelCabecalho() {
                super(new BorderLayout(12, 0));
                setOpaque(true);
                setBackground(new Color(0x23243A));
            }

            @Override
            public void addNotify() {
                super.addNotify();
                animacao.start();
            }

            @Override
            public void removeNotify() {
                animacao.stop();
                super.removeNotify();
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int width = getWidth();
                int height = getHeight();
                if (width <= 0 || height <= 0) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new LinearGradientPaint(0, 0, width, height,
                    new float[] {0f, 1f}, new Color[] {new Color(0x15182A), new Color(0x27264C)}));
                g2.fillRect(0, 0, width, height);
                for (int layer = 0; layer < 3; layer++) {
                    Path2D.Float onda = new Path2D.Float();
                    onda.moveTo(0, height);
                    for (int x = 0; x <= width; x += 10) {
                        double y = height * (0.32 + layer * 0.19)
                            + Math.sin(x * 0.006 + fase + layer) * 13
                            + Math.sin(x * 0.012 - fase * 0.7) * 7;
                        onda.lineTo(x, y);
                    }
                    onda.lineTo(width, height);
                    onda.closePath();
                    g2.setColor(layer == 0 ? new Color(64, 61, 190, 90)
                        : layer == 1 ? new Color(44, 94, 210, 78) : new Color(126, 48, 188, 65));
                    g2.fill(onda);
                }
                g2.dispose();
            }
        }

        @Override
        public void addNotify() {
            super.addNotify();
            animacao.start();
        }

        @Override
        public void removeNotify() {
            animacao.stop();
            super.removeNotify();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int base = (int) (height * 0.78);
            for (int layer = 0; layer < 3; layer++) {
                Path2D.Float onda = new Path2D.Float();
                onda.moveTo(0, height);
                onda.lineTo(0, base + layer * 24);
                for (int x = 0; x <= width; x += 8) {
                    double y = base + layer * 24
                        + Math.sin(x * 0.018 + fase + layer * 1.4) * (18 + layer * 5)
                        + Math.sin(x * 0.009 - fase * 0.8) * 12;
                    onda.lineTo(x, y);
                }
                onda.lineTo(width, height);
                onda.closePath();
                Color cor = layer == 0 ? new Color(76, 67, 220, 95)
                    : layer == 1 ? new Color(42, 91, 214, 80) : new Color(115, 49, 194, 70);
                g2.setColor(cor);
                g2.fill(onda);
            }
            g2.dispose();
        }
    }

    private static void configurarBotaoTopo(JButton botao, Color fundo, Color texto) {
        botao.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        botao.setForeground(texto);
        botao.setBackground(fundo);
        botao.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        botao.setContentAreaFilled(false);
        botao.setFocusPainted(false);
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** Abre o seletor de cores. Para texto, recusa cores claras demais (ficariam ilegíveis). */
    private static Color escolherCor(Component pai, Color atual, boolean paraTexto) {
        Color c = JColorChooser.showDialog(pai, "Escolha a cor", atual);
        if (c == null) return null;
        double lum = (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255.0;
        if (paraTexto && lum < 0.25) {
            JOptionPane.showMessageDialog(pai,
                "Essa cor é escura demais para o tema do chat. Escolha uma cor mais clara.",
                "Cor difícil de ler", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return c;
    }

    private static String descobrirFonteEmoji() {
        Set<String> instaladas = new HashSet<>(Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String f : new String[] {"Segoe UI Emoji", "Apple Color Emoji", "Noto Color Emoji", "Symbola"}) {
            if (instaladas.contains(f)) return f;
        }
        return Font.DIALOG;
    }

    // ------------------------------------------------------------ classes internas
    private static class ChatFeed extends JPanel implements Scrollable {
        ChatFeed() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(new Color(0x11131D));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(720, 440);
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(18, visibleRect.height - 18);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static final class PapelParedeViewport extends JViewport {
        private BufferedImage imagem;
        private int escurecimento;

        void setPapelParede(BufferedImage imagem, int escurecimento) {
            this.imagem = imagem;
            this.escurecimento = Math.max(0, Math.min(80, escurecimento));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (imagem == null || getWidth() <= 0 || getHeight() <= 0) return;
            Graphics2D g2 = (Graphics2D) g.create();
            pintarImagemCobertura(g2, imagem, getWidth(), getHeight());
            if (escurecimento > 0) {
                g2.setColor(new Color(0, 0, 0, Math.round(escurecimento * 255f / 100f)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.dispose();
        }
    }

    private static final class PreviaPapelParede extends JPanel {
        private BufferedImage imagem;
        private int escurecimento;

        PreviaPapelParede(BufferedImage imagem, int escurecimento) {
            this.imagem = imagem;
            this.escurecimento = escurecimento;
            setPreferredSize(new Dimension(560, 270));
            setBackground(new Color(0x17192D));
        }

        void setImagem(BufferedImage imagem) {
            this.imagem = imagem;
            repaint();
        }

        void setEscurecimento(int escurecimento) {
            this.escurecimento = escurecimento;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            if (imagem != null) pintarImagemCobertura(g2, imagem, getWidth(), getHeight());
            if (escurecimento > 0) {
                g2.setColor(new Color(0, 0, 0, Math.round(escurecimento * 255f / 100f)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            int x = Math.max(16, getWidth() - 230);
            g2.setColor(new Color(0x29234F));
            g2.fillRoundRect(x, 42, 206, 54, 16, 16);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            g2.drawString("Oi! Este é o papel de parede", x + 12, 72);
            g2.setColor(new Color(0x191E32));
            g2.fillRoundRect(20, 130, 212, 66, 16, 16);
            g2.setColor(Color.WHITE);
            g2.drawString("A prévia mantém a proporção", 32, 158);
            g2.drawString("e preenche a área da conversa.", 32, 179);
            g2.dispose();
        }
    }

    private static void pintarImagemCobertura(Graphics2D g, BufferedImage imagem,
                                               int larguraArea, int alturaArea) {
        if (larguraArea <= 0 || alturaArea <= 0) return;
        double escala = Math.max((double) larguraArea / imagem.getWidth(),
            (double) alturaArea / imagem.getHeight());
        int largura = (int) Math.ceil(imagem.getWidth() * escala);
        int altura = (int) Math.ceil(imagem.getHeight() * escala);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(imagem, (larguraArea - largura) / 2, (alturaArea - altura) / 2,
            largura, altura, null);
    }

    private static class BotaoArredondado extends JButton {
        BotaoArredondado(String texto) {
            super(texto);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (getWidth() <= 1 || getHeight() <= 1) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isEnabled() ? getBackground() : new Color(0xE5E9ED));
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
            g2.setColor(new Color(255, 255, 255, 38));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 2, getHeight() - 2, 12, 12));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Temas de fundo. Os "animados" ficam deslizando as cores. */
    private enum Tema {
        MESSENGER("Messenger classico", false, 0xDCEEFF, 0xEEF7FF, 0xC9E5F8),
        ARCO_IRIS("Arco-iris (animado)", true, 0xFF6B6B, 0xFFD93D, 0x6BCB77, 0x4D96FF, 0x9D4EDD),
        OCEANO("Oceano (animado)", true, 0x00B4DB, 0x0083B0, 0x48C6EF, 0x6F86D6),
        POR_DO_SOL("Por do sol (animado)", true, 0xFF512F, 0xF09819, 0xFF5E98, 0x8E2DE2),
        NEON("Neon (animado)", true, 0xF72585, 0x7209B7, 0x3A0CA3, 0x4CC9F0),
        CHICLETE("Chiclete (fixo)", false, 0xFFC1E3, 0xC9E4FF, 0xFFF3B0),
        ESCURO("Escuro", false, 0x17191D, 0x20242A, 0x30343B),
        CAMPO_FUTEBOL("Campo de futebol", false, 0x267A35, 0x328C3E),
        SOLIDA("Cor solida...", false);

        private final String rotulo;
        private final boolean animado;
        private final int[] rgb;

        Tema(String rotulo, boolean animado, int... rgb) {
            this.rotulo = rotulo;
            this.animado = animado;
            this.rgb = rgb;
        }

        Color[] cores() {
            Color[] c = new Color[rgb.length];
            for (int i = 0; i < rgb.length; i++) c[i] = new Color(rgb[i]);
            return c;
        }

        Color corDestaque() {
            return rgb.length == 0 ? new Color(0x5865F2) : new Color(rgb[0]);
        }

        @Override
        public String toString() {
            return rotulo;
        }
    }

    /** Painel de fundo que desenha o degradê e, nos temas animados, o desloca com um Timer. */
    private static class FundoPanel extends JPanel {
        private Tema tema = Tema.MESSENGER;
        private Color solida = new Color(0xFFE0F0);
        private float fase = 0;

        FundoPanel() {
            super(new BorderLayout());
            new javax.swing.Timer(40, e -> {
                if (tema.animado) {
                    fase = (fase + 3) % 100000;
                    repaint();
                }
            }).start();
        }

        Tema getTema() { return tema; }
        boolean escuro() { return tema == Tema.ESCURO; }
        Color corTema() { return tema == Tema.SOLIDA ? solida : tema.corDestaque(); }

        void setTema(Tema t) {
            tema = t;
            repaint();
        }

        void setSolida(Color c) {
            solida = c;
            tema = Tema.SOLIDA;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            Graphics2D g2 = (Graphics2D) g.create();
            if (tema == Tema.CAMPO_FUTEBOL) {
                g2.setColor(new Color(0x287D32));
                g2.fillRect(0, 0, w, h);
                int faixa = Math.max(1, w / 10);
                for (int x = 0; x < w; x += faixa * 2) {
                    g2.setColor(new Color(0x328C3E));
                    g2.fillRect(x, 0, faixa, h);
                }
                g2.setColor(new Color(255, 255, 255, 150));
                g2.setStroke(new java.awt.BasicStroke(3f));
                int margemX = Math.max(14, w / 16);
                int margemY = Math.max(14, h / 12);
                int campoW = w - margemX * 2;
                int campoH = h - margemY * 2;
                g2.drawRect(margemX, margemY, campoW, campoH);
                g2.drawLine(w / 2, margemY, w / 2, h - margemY);
                int diametro = Math.min(campoH / 3, campoW / 4);
                g2.drawOval(w / 2 - diametro / 2, h / 2 - diametro / 2, diametro, diametro);
                g2.drawRect(margemX, h / 2 - campoH / 4, campoW / 6, campoH / 2);
                g2.drawRect(w - margemX - campoW / 6, h / 2 - campoH / 4, campoW / 6, campoH / 2);
            } else if (tema == Tema.SOLIDA) {
                g2.setColor(solida);
            } else {
                Color[] cores = tema.cores();
                float[] fracoes = new float[cores.length];
                for (int i = 0; i < cores.length; i++) fracoes[i] = i / (float) (cores.length - 1);
                float dx = w, dy = h * 0.5f;
                double len = Math.hypot(dx, dy);
                float s = tema.animado ? (float) (fase % (2 * len)) : 0f;
                float ux = (float) (dx / len), uy = (float) (dy / len);
                Point2D ini = new Point2D.Float(ux * s, uy * s);
                Point2D fim = new Point2D.Float(ux * s + dx, uy * s + dy);
                g2.setPaint(new LinearGradientPaint(ini, fim, fracoes, cores,
                    MultipleGradientPaint.CycleMethod.REFLECT));
            }
            g2.fillRect(0, 0, w, h);
            g2.dispose();
        }
    }

}
