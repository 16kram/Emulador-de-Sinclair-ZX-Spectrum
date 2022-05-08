/*
*   Emulador del ordenador Sinclair ZX-Spectrum 48K
*   Versión 1.0
*   
*   La clase Spectrum es la clase principal
*
 */
package spectrum;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.Timer;

/**
 *
 * @author Esteban Porqueras
 */
public class Spectrum extends JFrame implements ActionListener {

    public static final String TITULO_EMULADOR = "Emulador de ZX-Spectrum 48K";
    private JFrame vp;
    private JMenuBar barraMenu;
    private Z80 z;
    private LeeTeclas teclado;
    private Cargar cargar;
    private Pantalla p;
    private Sonido sonido;
    private int frames;
    private boolean keyboard = false;
    public static boolean reproduciendoSonido = false;
    public static boolean sonidoActivado = true;
    private int tiempoFrame = 20;
    public static boolean run = true;
    public static boolean espiar = false;
    public static boolean pantULA = false;//La ULA está leyendo la pantalla
    public static boolean memoContenida = false;//El procesador está accediento a la memoría en contienda
    JTextArea textArea;

    public static void main(String[] args) {
        Spectrum s = new Spectrum();
        s.emulacion();
    }

    public void emulacion() {
        z = new Z80();
        teclado = new LeeTeclas();
        cargar = new Cargar();
        z.setCargar(cargar);
        cargar.loadRom();
        cargar.setZ80(z);
        //Ventana principal del emulador
        vp = new JFrame(TITULO_EMULADOR);
        Image icon = new ImageIcon(getClass().getResource("clivesinclair.png")).getImage();
        vp.setIconImage(icon);
        vp.setBounds(0, 0, (Pantalla.X + (64 * Pantalla.TAM)), (Pantalla.Y + (120 * Pantalla.TAM)));
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
        JMenuItem sonidoEncendido = new JMenuItem("Sonido encendido");
        menuControl.add(sonidoEncendido);
        JMenuItem sonidoApagado = new JMenuItem("Sonido apagado");
        menuControl.add(sonidoApagado);
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
        sonidoEncendido.setActionCommand("sonidoEncendido");
        sonidoEncendido.addActionListener(this);
        sonidoApagado.setActionCommand("sonidoApagado");
        sonidoApagado.addActionListener(this);
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
        vp.setVisible(true);
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
        Timer timer = new Timer(50, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Acción a implementar en un futuro
            }
        });
        timer.start();
        frames = 0;
        int muestra = 0;
        int muestraBorder = 0;
        long inicioTemp = System.currentTimeMillis();//Guarda el tiempo inicial
        long finTemp;
        while (true) {
            if (espiar) {
                textArea.setText(z.regDec());
            }
            if (run) {
                z.clock();
            }
            if ((z.gettStates() - muestra) > 73 && sonidoActivado) {//Cada 20uS guarda incrementa la posición del sample del array -->73T-States * (1/3500000Hz CLK Spectrum)=20.8us
                muestra = z.gettStates();//Muestreo 48000Hz/50Hz=960 muestras cada 20ms. 20.8us x 960=19.968ms
                //Incrementa contador Array sample
                sonido.guardaMuestra();
            }
            if ((z.gettStates() - muestraBorder) > 224) {//Cada final de línea actualiza el borde de la pantalla
                muestraBorder = z.gettStates();
                p.actualizaBorde();
            }
            if (z.gettStates() > 69888) {//A los 69888 T-States se completa un cuadro de TV 
                p.repaint();
                finTemp = System.currentTimeMillis();//Averigua el tiempo que
                long difTemp = finTemp - inicioTemp;//han tardado en ejecutarse 69888 T-States
                frames++;//Incrementa el número de cuadros
                long t1 = System.currentTimeMillis();
                if (sonidoActivado) {
                    Thread sound = new Sonido();
                    sound.start();
                    try {
                        sound.join();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Spectrum.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                try {
                    if (difTemp < tiempoFrame && !sonidoActivado) {
                        Thread.sleep(20 - difTemp);//Si el tiempo es menor que 20ms hace una pausa hasta que se cumplan 20ms
                    }

                } catch (InterruptedException ex) {
                    Logger.getLogger(Spectrum.class.getName()).log(Level.SEVERE, null, ex);
                }

                long t2 = System.currentTimeMillis();
                if (frames > 16) {
                    p.setAstable(p.getAstable() ^ 1);//Realiza el parpadeo de la pantalla (FLASH)
                    frames = 0;
                }
                //System.out.println("T=" + (t2 - t1));
                try {
                    if ((t2 - t1) < 12 && sonidoActivado) {
                        Thread.sleep(12 + (t2 - t1));
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(Spectrum.class.getName()).log(Level.SEVERE, null, ex);
                }
                p.repaint();
                z.settStates(0);
                muestra = 0;
                muestraBorder = 0;
                inicioTemp = System.currentTimeMillis();//Guarda el tiempo inicial
                sonido.reset();
                p.finBorde();
                z.Int();
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
            for (int n = 16385; n < 65535; n++) {
                Memoria.romRam[n] = 0;//Borra la RAM
            }
            cargar.loadRom();
            z.setResetZ80();
            p.llenaBorder(7);
            p.repaint();
            vp.setTitle(Spectrum.TITULO_EMULADOR);
        }
        if (e.getActionCommand().equals("sonidoEncendido")) {
            sonidoActivado = true;
        }
        if (e.getActionCommand().equals("sonidoApagado")) {
            sonidoActivado = false;
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
            String direccion = JOptionPane.showInputDialog(null, "Dirección de memoria de 0-65535", "POKE", JOptionPane.INFORMATION_MESSAGE);
            String valor = JOptionPane.showInputDialog(null, "valor 0-255", "POKE", JOptionPane.INFORMATION_MESSAGE);
            int dir = Integer.parseInt(direccion);
            int val = Integer.parseInt(valor);
            Memoria.escribe(dir, val);
        }
        if (e.getActionCommand().equals("Debug")) {
            System.out.println("DEBUG");
            JDialog debugger = new JDialog(vp);
            debugger.setTitle("DEBUG");
            debugger.setLayout(new BorderLayout());
            //Botón parar
            JButton botonParar = new JButton("Parar");
            botonParar.setActionCommand("botonParar");
            botonParar.addActionListener(this);
            debugger.add(botonParar, BorderLayout.LINE_START);
            //Boton continuar
            JButton botonContinuar = new JButton("Continuar");
            botonContinuar.setActionCommand("botonContinuar");
            botonContinuar.addActionListener(this);
            debugger.add(botonContinuar, BorderLayout.CENTER);
            //Boton avanzar
            JButton botonAvanzar = new JButton("Debug");
            botonAvanzar.setActionCommand("botonAvanzar");
            botonAvanzar.addActionListener(this);
            debugger.add(botonAvanzar, BorderLayout.LINE_END);
            textArea = new JTextArea(10, 45);
            debugger.add(textArea, BorderLayout.PAGE_END);
            debugger.pack();
            debugger.setVisible(true);
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
}
