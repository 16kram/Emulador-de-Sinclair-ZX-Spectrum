/*
*   Emulador del ordenador Sinclair ZX-Spectrum 48K
*   Versión 1.0
*   
*   La clase Spectrum es la clase principal
*
 */
package spectrum;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.Timer;

/**
 *
 * @author Esteban Porqueras
 */
public class Spectrum extends JFrame implements ActionListener {

    private JFrame vp;
    private JMenuBar barraMenu;
    private Z80 z;
    private LeeTeclas teclado;
    private Cargar cargar;
    private int frames;
    private boolean keyboard = false;

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
        //Ventana principal del emulador
        vp = new JFrame("Emulador de ZX-Spectrum 48K");
        vp.setBounds(0, 0, Pantalla.X + 60, Pantalla.Y + 80);
        barraMenu = new JMenuBar();
        JMenu menuArchivo = new JMenu("Archivo");
        barraMenu.add(menuArchivo);
        JMenu menuControl = new JMenu("Control");
        barraMenu.add(menuControl);
        JMenu menuAyuda = new JMenu("Ayuda");
        barraMenu.add(menuAyuda);
        JMenuItem abrir = new JMenuItem("Abrir archivo TAP");
        JMenuItem salir = new JMenuItem("Salir");
        menuArchivo.add(abrir);
        menuArchivo.add(new JSeparator());
        menuArchivo.add(salir);
        JMenuItem reset = new JMenuItem("Reset Zx Spectrum");
        menuControl.add(reset);
        JMenuItem acercaDe = new JMenuItem("Acerca de...");
        menuAyuda.add(acercaDe);
        abrir.setActionCommand("Abrir");
        abrir.addActionListener(this);
        salir.setActionCommand("Salir");
        salir.addActionListener(this);
        reset.setActionCommand("Reset");
        reset.addActionListener(this);
        acercaDe.setActionCommand("acercaDe");
        acercaDe.addActionListener(this);
        vp.setJMenuBar(barraMenu);
        vp.setVisible(true);
        vp.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Pantalla p = new Pantalla();
        z.setPantalla(p);
        vp.addKeyListener(teclado);
        vp.add(p);
        Timer timer = new Timer(15, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                keyboard = true;
            }
        });
        timer.start();
        frames = 0;
        long inicioTemp = System.currentTimeMillis();//Guarda el tiempo inicial
        long finTemp;
        while (true) {
            z.clock();
            if (keyboard) {
                z.Int();
                keyboard = false;
            }
            if (z.gettStates() > 69888) {//A los 69888 T-States se completa un cuadro de TV 
                frames++;//Incrementa el número de cuadros 
                finTemp = System.currentTimeMillis();//Averigua el tiempo que
                long difTemp = finTemp - inicioTemp;//han tardado en ejecutarse 69888 T-States
                try {
                    if (difTemp < 20) {
                        Thread.sleep(20 - difTemp);//Si el tiempo es menor que 20ms hace una pausa hasta que se cumplan 20ms
                    }
                    if (frames > 16) {
                        p.setAstable(p.getAstable() ^ 1);//Realiza el parpadeo de la pantalla (FLASH)
                        frames = 0;
                    }
                    inicioTemp = System.currentTimeMillis();//Guarda el tiempo inicial
                    p.repaint();//Actualiza la pantalla
                } catch (InterruptedException ex) {
                    Logger.getLogger(Spectrum.class.getName()).log(Level.SEVERE, null, ex);
                }
                z.settStates(0);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Abrir")) {
            cargar.cargaCinta();
        }
        if (e.getActionCommand().equals("Salir")) {
            System.exit(0);
        }
        if (e.getActionCommand().equals("Reset")) {
            for (int n = 16385; n < 65535; n++) {
                Memoria.romRam[n] = 0;//Borra la RAM
            }
            z.setResetZ80();
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
