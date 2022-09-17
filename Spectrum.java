/*
*   Emulador del ordenador Sinclair ZX-Spectrum 48K
*   Versión 1.0
*
*   La clase Spectrum es la clase principal
*
 */
package spectrum;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 *
 * @author Esteban Porqueras
 */
public class Spectrum extends JFrame implements ActionListener {

    public static final String TITULO_EMULADOR = "Emulador de ZX-Spectrum 48K";
    private JFrame vp;
    private JMenuBar barraMenu;
    public Z80 z;
    private LeeTeclas teclado;
    private Cargar cargar;
    private Pantalla p;
    private Sonido sonido;
    private int frames;
    public static boolean reproduciendoSonido = false;
    public static boolean sonidoActivado = true;
    private int tiempoFrame = 20;//Número de frames por segundo
    public static boolean run = true;
    public static boolean espiar = false;
    public static boolean pantULA = false;//La ULA está leyendo la pantalla
    public static boolean memoContenida = false;//El procesador está accediento a la memoría en contienda
    public static int[] contenida = new int[224 * 192];
    public static int[] flotante = new int[224 * 192];
    public static final int CONTENT_INI = 14335;//Frame inicial de la memoria contenida=14335
    public static final int CONTENT_FIN = CONTENT_INI + (224 * 192);
    public static final int FLOAT_INI = 14337;//Frame inicial del bus flotante=14337
    public static final int FLOAT_FIN = FLOAT_INI + (224 * 192);
    public static double tiempoTotal = 0;//Tiempo en ms para calcular la velocidad del emulador
    public static int velocidadEmu = 0;
    JTextArea textArea;
    JLabel velocidad;

    public static void main(String[] args) {
        Spectrum s = new Spectrum();
        s.emulacion();
    }

    public synchronized void emulacion() {
        datosMemoriaContenida();
        datosBusFlotante();
        z = new Z80();
        teclado = new LeeTeclas();
        cargar = new Cargar();
        z.setCargar(cargar);
        cargar.loadRom();
        cargar.setZ80(z);
        //Ventana principal del emulador
        JFrame.setDefaultLookAndFeelDecorated(true);
        vp = new JFrame(TITULO_EMULADOR);
        Image icon = new ImageIcon(getClass().getResource("clivesinclair.png")).getImage();
        vp.setIconImage(icon);
        vp.setBounds(0, 0, (Pantalla.X + (64 * Pantalla.TAM)), (Pantalla.Y + (120 * Pantalla.TAM)));
        vp.setLayout(new BorderLayout());
        //Barra inferior
        velocidad = new JLabel("100%", SwingConstants.RIGHT);
        vp.add(velocidad, BorderLayout.PAGE_END);
        //Barra de Menú
        barraMenu = new JMenuBar();
        JMenu menuArchivo = new JMenu("Archivo");
        barraMenu.add(menuArchivo);
        JMenu menuControl = new JMenu("Control");
        barraMenu.add(menuControl);
        JMenu menuPantalla = new JMenu("Pantalla");
        barraMenu.add(menuPantalla);
        JMenu menuAyuda = new JMenu("Ayuda");
        barraMenu.add(menuAyuda);
        JMenuItem abrir = new JMenuItem("Abrir archivo TAP");
        JMenuItem abrirSna = new JMenuItem("Abrir archivo SNA");
        JMenuItem abrirROM = new JMenuItem("Cargar Zx Interface 2 ROM");
        JMenuItem abrirROMSpectrum = new JMenuItem("Cargar ROM del Spectrum");
        JMenuItem salir = new JMenuItem("Salir");
        menuArchivo.add(abrir);
        menuArchivo.add(abrirSna);
        menuArchivo.add(new JSeparator());
        menuArchivo.add(abrirROM);
        menuArchivo.add(new JSeparator());
        menuArchivo.add(abrirROMSpectrum);
        menuArchivo.add(new JSeparator());
        menuArchivo.add(new JSeparator());
        menuArchivo.add(salir);
        JMenuItem reset = new JMenuItem("Reset Zx Spectrum");
        menuControl.add(reset);
        menuControl.add(new JSeparator());
        JMenuItem poke = new JMenuItem("Poke");
        menuControl.add(poke);
        menuControl.add(new JSeparator());
        JMenuItem slow = new JMenuItem("Slow");
        menuControl.add(slow);
        JMenuItem fast = new JMenuItem("Fast");
        menuControl.add(fast);
        menuControl.add(new JSeparator());
        JMenuItem debug = new JMenuItem("Debug");
        menuControl.add(debug);
        JMenuItem x1 = new JMenuItem("Tamaño x1");
        menuPantalla.add(x1);
        JMenuItem x2 = new JMenuItem("Tamaño x2");
        menuPantalla.add(x2);
        JMenuItem x4 = new JMenuItem("Tamaño x4");
        menuPantalla.add(x4);
        JMenuItem acercaDe = new JMenuItem("Acerca de...");
        menuAyuda.add(acercaDe);
        abrir.setActionCommand("Abrir");
        abrir.addActionListener(this);
        abrirSna.setActionCommand("Abrir SNA");
        abrirSna.addActionListener(this);
        abrirROM.setActionCommand("Abrir ROM");
        abrirROM.addActionListener(this);
        abrirROMSpectrum.setActionCommand("Abrir ROM Spectrum");
        abrirROMSpectrum.addActionListener(this);
        salir.setActionCommand("Salir");
        salir.addActionListener(this);
        reset.setActionCommand("Reset");
        reset.addActionListener(this);
        poke.setActionCommand("Poke");
        poke.addActionListener(this);
        slow.setActionCommand("Slow");
        slow.addActionListener(this);
        fast.setActionCommand("Fast");
        fast.addActionListener(this);
        debug.setActionCommand("Debug");
        debug.addActionListener(this);
        x1.setActionCommand("x1");
        x1.addActionListener(this);
        x2.setActionCommand("x2");
        x2.addActionListener(this);
        x4.setActionCommand("x4");
        x4.addActionListener(this);
        acercaDe.setActionCommand("acercaDe");
        acercaDe.addActionListener(this);
        vp.setJMenuBar(barraMenu);
        //Barra de herramientas
        JMenuBar barraHerramientas = new JMenuBar();//Añade la barra de herramientas
        vp.add(barraHerramientas, BorderLayout.BEFORE_FIRST_LINE);
        //Barra puntitos
        JLabel puntos = new JLabel();
        puntos.setToolTipText("Barra de herramientas del emulador");
        Image iconoPuntos = new ImageIcon(getClass().getResource("puntos.png")).getImage();
        puntos.setIcon(new ImageIcon(iconoPuntos));
        barraHerramientas.add(puntos);
        //Barra abrir fichero
        JLabel abrirFichero = new JLabel();
        abrirFichero.setToolTipText("Carga un archivo");
        Image iconoAbrir = new ImageIcon(getClass().getResource("openfolder.png")).getImage();
        abrirFichero.setIcon(new ImageIcon(iconoAbrir));
        barraHerramientas.add(abrirFichero);
        abrirFichero.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                //z.resetZ80();
                //Carga automática LOAD ""
                cargar.cargaCinta();
                LeeTeclas.KROW6 &= (byte) 183; //J
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Spectrum.class.getName()).log(Level.SEVERE, null, ex);
                }
                LeeTeclas.KROW6 = -65;
                LeeTeclas.KROW7 &= 253;
                LeeTeclas.KROW5 &= (byte) 190; //P
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Spectrum.class.getName()).log(Level.SEVERE, null, ex);
                }
                LeeTeclas.KROW7 = -65;
                LeeTeclas.KROW5 = -65;
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Spectrum.class.getName()).log(Level.SEVERE, null, ex);
                }
                LeeTeclas.KROW7 &= 253;
                LeeTeclas.KROW5 &= (byte) 190; //P
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Spectrum.class.getName()).log(Level.SEVERE, null, ex);
                }
                LeeTeclas.KROW5 = -65;
                LeeTeclas.KROW7 = -65;
                LeeTeclas.KROW6 &= (byte) 190; //ENTER
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Spectrum.class.getName()).log(Level.SEVERE, null, ex);
                }
                LeeTeclas.KROW6 = -65;
            }
        });
        //Barra playPause
        JLabel playPause = new JLabel();
        playPause.setToolTipText("Ejecuta o pausa el emulador");
        Image iconoPlay = new ImageIcon(getClass().getResource("play.png")).getImage();
        Image iconoPause = new ImageIcon(getClass().getResource("pause.png")).getImage();
        playPause.setIcon(new ImageIcon(iconoPlay));
        barraHerramientas.add(playPause);
        playPause.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (run) {
                    playPause.setIcon(new ImageIcon(iconoPause));
                    run = false;
                } else {
                    playPause.setIcon(new ImageIcon(iconoPlay));
                    run = true;
                }
            }
        });
        //Barra Sonido On/Off
        JLabel sonidoOnOff = new JLabel();
        sonidoOnOff.setToolTipText("Volumen On/Off");
        Image iconoSonidoOn = new ImageIcon(getClass().getResource("sonidoon.png")).getImage();
        Image iconoSonidoOff = new ImageIcon(getClass().getResource("sonidooff.png")).getImage();
        sonidoOnOff.setIcon(new ImageIcon(iconoSonidoOn));
        barraHerramientas.add(sonidoOnOff);
        sonidoOnOff.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (sonidoActivado) {
                    sonidoOnOff.setIcon(new ImageIcon(iconoSonidoOff));
                    sonidoActivado = false;
                } else {
                    sonidoOnOff.setIcon(new ImageIcon(iconoSonidoOn));
                    sonidoActivado = true;
                }
            }
        });
        //Barra Reset
        JLabel resetSpectrum = new JLabel();
        resetSpectrum.setToolTipText("Reset emulador");
        Image iconoReset = new ImageIcon(getClass().getResource("reset.png")).getImage();
        resetSpectrum.setIcon(new ImageIcon(iconoReset));
        barraHerramientas.add(resetSpectrum);
        resetSpectrum.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                vp.setTitle(Spectrum.TITULO_EMULADOR);
                cargar.loadRom();
                p.llenaBorder(7);
                z.resetZ80();
            }
        });
        vp.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        p = new Pantalla();
        z.setPantalla(p);
        cargar.setPantalla(p);
        vp.addKeyListener(teclado);
        vp.add(p);
        cargar.setFrame(vp);
        sonido = new Sonido();
        z.setSonido(sonido);
        sonido.createOutput();//Inicializa la tarjeta de sonido*/
        vp.setResizable(false);
        vp.setVisible(true);
        /*Timer timer = new Timer(1, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Acción a implementar en un futuro
            }
        });
        timer.start();*/
        frames = 0;
        int muestra = 0;
        int muestraBorder = 0;
        long inicioTemp = System.currentTimeMillis();//Guarda el tiempo inicial
        while (true) {
            if (espiar) {
                textArea.setText(z.regDec());
            }
            if (run) {
                z.clock();
            }
            //Bus flotante-->Falta implementar correctamente
            //Provisionalmente programado de esta manera para ver como funciona el bus flotante
            //Pero funcionan los juegos como Arcanoid, Cobra, Terra Cresta, etc...
            if (z.gettStates() > FLOAT_INI && z.gettStates() < FLOAT_FIN) {//Control bus flotante
                //System.out.println(flotante[z.gettStates() - 14335]);
                if (flotante[z.gettStates() - (FLOAT_INI + 1)] == 0xff) {
                    pantULA = false;
                } else {
                    pantULA = true;
                }
            }
            if (z.gettStates() > CONTENT_INI && z.gettStates() < CONTENT_FIN && memoContenida) {//Control memoria contenida cuando se intenta acceder a una posición de memoria
                //System.out.println(contenida[z.gettStates() - 14335]);
                z.settStates(z.gettStates() + contenida[z.gettStates() - CONTENT_INI]);
                memoContenida = false;
            }
            if (z.gettStates() > CONTENT_INI && z.gettStates() < CONTENT_FIN && z.getPc() > 16384 && z.getPc() < 32768) {//Control memoria contenida cuando el procesador (PC) está en la memoria baja
                //System.out.println(contenida[z.gettStates() - 14335]);
                z.settStates(z.gettStates() + contenida[z.gettStates() - CONTENT_INI]);
            }
            if ((z.gettStates() - muestra) > 72 && sonidoActivado) {//Cada 20uS guarda incrementa la posición del sample del array -->73T-States * (1/3500000Hz CLK Spectrum)=20.8us
                //System.out.println(z.gettStates()-muestra);
                muestra = z.gettStates();//Muestreo 48000Hz/50Hz=960 muestras cada 20ms. 20.8us x 960=19.968ms
                //Incrementa contador Array sample
                sonido.actualizaMuestra(z.gettStates());
            }
            if ((z.gettStates() - muestraBorder) > 224) {//Cada final de línea actualiza el borde de la pantalla
                muestraBorder = z.gettStates();
                p.actualizaBorde();
            }
            if (z.gettStates() > 69888) {//A los 69888 T-States se completa un cuadro de TV
                if (sonidoActivado) {
                    sonido.play();
                }
                frames++;//Incrementa el número de cuadros
                if (frames > 16) {
                    p.setAstable(p.getAstable() ^ 1);//Realiza el parpadeo de la pantalla (FLASH)
                    frames = 0;
                    if (tiempoTotal != 0) {//El tiempoTotal ha de ser !=0 para que no haya error de división entre cero
                        velocidadEmu = (int) (2000 / tiempoTotal);
                        velocidad.setText(velocidadEmu + "%");
                    }
                }
                //z.settStates(z.gettStates() - 69888);
                z.settStates(0);
                muestra = z.gettStates();
                muestraBorder = 0;
                sonido.reset();
                p.finBorde();
                z.Int();
                p.repaint();
                while ((System.currentTimeMillis() - inicioTemp) < tiempoFrame) {
                }
                tiempoTotal = System.currentTimeMillis() - inicioTemp;
                //System.out.println(tiempoTotal);
                inicioTemp = System.currentTimeMillis();//Guarda el tiempo inicial
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e
    ) {
        if (e.getActionCommand().equals("Abrir")) {
            cargar.cargaCinta();
        }
        if (e.getActionCommand().equals("Abrir SNA")) {
            run = false;
            cargar.cargarSnapShot();
            run = true;
        }
        if (e.getActionCommand().equals("Abrir ROM")) {
            run = false;
            cargar.interface2ROM();
            run = true;
        }
        if (e.getActionCommand().equals("Abrir ROM Spectrum")) {
            run = false;
            cargar.seleccionaROM();
            run = true;
        }
        if (e.getActionCommand().equals("Salir")) {
            sonido.cerrarSonido();
            System.exit(0);
        }
        if (e.getActionCommand().equals("Reset")) {
            /*for (int n = 16385; n < 65535; n++) {
                Memoria.romRam[n] = 0;//Borra la RAM
            }*/
            cargar.loadRom();
            z.setResetZ80();
            p.llenaBorder(7);
            //p.repaint();
            vp.setTitle(Spectrum.TITULO_EMULADOR);
        }
        if (e.getActionCommand().equals("Slow")) {
            tiempoFrame = 20;
            sonidoActivado = true;
        }
        if (e.getActionCommand().equals("Fast")) {
            tiempoFrame = 0;
            sonidoActivado = false;
        }
        if (e.getActionCommand().equals("Poke")) {
            try {
                String direccion = JOptionPane.showInputDialog(null, "Dirección de memoria de 0-65535", "POKE", JOptionPane.INFORMATION_MESSAGE);
                int dir = Integer.parseInt(direccion);
                String valor = JOptionPane.showInputDialog(null, "valor 0-255", "POKE", JOptionPane.INFORMATION_MESSAGE);
                int val = Integer.parseInt(valor);
                Memoria.escribe(dir, val);
            } catch (Exception error) {
                JOptionPane.showMessageDialog(null, "Por favor, vuelva a introducirlos",
                        "El valor o valores no son válidos",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        if (e.getActionCommand().equals("Debug")) {
            System.out.println("DEBUG");
            JDialog debugger = new JDialog(vp);
            debugger.setTitle("DEBUG");
            debugger.setLayout(new FlowLayout(FlowLayout.CENTER));
            //Botón parar
            JButton botonParar = new JButton("Parar");
            botonParar.setActionCommand("botonParar");
            botonParar.addActionListener(this);
            debugger.add(botonParar);
            //Boton continuar
            JButton botonContinuar = new JButton("Continuar");
            botonContinuar.setActionCommand("botonContinuar");
            botonContinuar.addActionListener(this);
            debugger.add(botonContinuar);
            //Boton avanzar
            JButton botonAvanzar = new JButton("Debug");
            botonAvanzar.setActionCommand("botonAvanzar");
            botonAvanzar.addActionListener(this);
            debugger.add(botonAvanzar);
            //Boton Interrupción
            JButton botonInt = new JButton("Interrupción");
            botonInt.setActionCommand("botonInt");
            botonInt.addActionListener(this);
            debugger.add(botonInt);
            //Boton Halt
            JButton botonHalt = new JButton("HALT");
            botonHalt.setActionCommand("botonHalt");
            botonHalt.addActionListener(this);
            debugger.add(botonHalt);
            //Área de escritura para visualizar los registros
            textArea = new JTextArea(10, 45);
            debugger.add(textArea);
            debugger.pack();
            debugger.setVisible(true);
            textArea.setText(z.regDec());
        }
        if (e.getActionCommand().equals("botonParar")) {
            run = false;
            espiar = false;
            //z.muestraRegistros();
            textArea.setText(z.regDec());
        }
        if (e.getActionCommand().equals("botonContinuar")) {
            run = true;
            //z.muestraRegistros();
            textArea.setText(z.regDec());
        }
        if (e.getActionCommand().equals("botonAvanzar")) {
            z.clock();
            //z.muestraRegistros();
            p.repaint();
            espiar = true;
        }
        if (e.getActionCommand().equals("botonInt")) {
            z.Int();
        }
        if (e.getActionCommand().equals("botonHalt")) {
            z.noHalt();
        }
        if (e.getActionCommand().equals("x1")) {
            Pantalla.TAM = 1;
            Pantalla.Y = 192;
            Pantalla.X = 256;
            vp.setBounds(0, 0, (Pantalla.X + (64 * Pantalla.TAM)), (Pantalla.Y + (120 * Pantalla.TAM)));
        }
        if (e.getActionCommand().equals("x2")) {
            Pantalla.TAM = 2;
            Pantalla.Y = 192 * 2;
            Pantalla.X = 256 * 2;
            vp.setBounds(0, 0, (Pantalla.X + (64 * Pantalla.TAM)), (Pantalla.Y + (120 * Pantalla.TAM)));
        }
        if (e.getActionCommand().equals("x4")) {
            Pantalla.TAM = 4;
            Pantalla.Y = 192 * 4;
            Pantalla.X = 256 * 4;
            vp.setBounds(0, 0, (Pantalla.X + (64 * Pantalla.TAM)), (Pantalla.Y + (120 * Pantalla.TAM)));
        }
        if (e.getActionCommand().equals("acercaDe")) {
            JOptionPane.showMessageDialog(null, "~Emulador de Sinclair ZX-Spectrum~"
                    + "\n Versión 1.0 - septiembre 2021"
                    + "\n Realizado por Esteban Porqueras"
                    + "\n e-mail: 16k.ram@gmail.com",
                    "Acerca de...", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    //Creación de la tabla de la memoria contenida
    public void datosMemoriaContenida() {
        int cont = 6;
        int p = 0;
        for (int m = 0; m < 192; m++) {
            for (int n = 0; n < 128; n++) {
                if (cont > 0) {
                    contenida[p++] = cont;
                    //System.out.println(14335 + n + " retraso=" + cont);
                }
                if (cont == -1 || cont == 0) {
                    contenida[p++] = 0;
                    //System.out.println(14335 + n + " Sin demora");
                }
                cont--;
                if (cont < -1) {
                    cont = 6;
                }
            }
            for (int n = 0; n < 96; n++) {
                contenida[p++] = 0;
            }
        }
        /*for(int n=0;n<1000;n++){
            System.out.println((n+14335)+"--"+contenida[n]);
        }*/
    }

    //Creacion de la tabla del bus flotante
    public void datosBusFlotante() {
        int cont = 0;
        for (int m = 0; m < 192; m++) {
            for (int n = 0; n < 16; n++) {
                for (int q = 0; q < 4; q++) {
                    flotante[cont++] = 0;
                }
                for (int q = 0; q < 4; q++) {
                    flotante[cont++] = 0xff;
                }
            }
            for (int q = 0; q < 96; q++) {
                flotante[cont++] = 0xff;
            }
        }
        /*for (int n = 0; n <43008 ; n++) {
            System.out.println((n + 14335) + "--" + flotante[n]);
        }*/
    }
}
