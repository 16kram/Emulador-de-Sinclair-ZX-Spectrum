/*
*   Emulador del ordenador Sinclair ZX-Spectrum 48K
*   Versión 1.0
*   
*   La clase Cargar carga la ROM y permite la carga de programas en formato TAP en el ZX-Spectrum 48K
 */
package spectrum;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author Esteban Porqueras
 */
public class Cargar {

    //Carga la ROM del Spectrum
    private int[] buffer = new int[0xfffff];
    private ArrayList<BloqueDatos> bloque = new ArrayList();
    private int numBloque;
    private Z80 z;
    private Pantalla p;
    private JFrame vp;
    private String archivoROM = "./Roms/Spectrum.rom";

    //Carga la ROM del Spectrum
    public void loadRom() {
        int contador = 0;//Inicio memoria Spectrum
        try {
            FileInputStream in = new FileInputStream(archivoROM);
            int c;
            while ((c = in.read()) != -1) {
                Memoria.romRam[contador] = c;
                contador++;
            }
            in.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(null, "Error en la carga de la ROM", "No se puede cargar el fichero", JOptionPane.ERROR_MESSAGE);
            seleccionaROM();
        }
    }

    //Selecciona la ROM del Spectrum
    public void seleccionaROM() {
        JFileChooser fileChooser = new JFileChooser(".");//Directorio actual
        FileFilter filtro = new FileNameExtensionFilter("Archivos ROM (.rom)", "rom");
        fileChooser.setFileFilter(filtro);
        fileChooser.showOpenDialog(fileChooser);
        try {
            archivoROM = fileChooser.getSelectedFile().getAbsolutePath();
            loadRom();
            z.resetZ80();
        } catch (NullPointerException e) {
            //vp.setTitle(Spectrum.TITULO_EMULADOR);
        }
    }

    //Carga un archivo ZX-Interface 2 - ROM
    public void interface2ROM() {
        JFileChooser fileChooser = new JFileChooser(".");//Directorio actual
        FileFilter filtro = new FileNameExtensionFilter("Archivos Zx Interface 2 ROM (.bin)", "bin");
        fileChooser.setFileFilter(filtro);
        fileChooser.showOpenDialog(fileChooser);
        int contador = 0;
        try {
            String ruta = fileChooser.getSelectedFile().getAbsolutePath();
            vp.setTitle(fileChooser.getSelectedFile().getName());
            FileInputStream in = new FileInputStream(ruta);
            int c;
            while ((c = in.read()) != -1) {
                Memoria.romRam[contador] = c;
                contador++;
            }
            in.close();
            z.setPc(0);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(null, "Error de carga", "No se puede leer el fichero", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        } catch (NullPointerException e) {
            //vp.setTitle(Spectrum.TITULO_EMULADOR);
        }
    }

    //Carga un archivo TAP
    public void cargaCinta() {
        numBloque = 0;
        bloque.clear();
        JFileChooser fileChooser = new JFileChooser(".");//Directorio actual
        FileFilter filtro = new FileNameExtensionFilter("Archivos TAP (.tap)", "tap");
        fileChooser.setFileFilter(filtro);
        fileChooser.showOpenDialog(fileChooser);
        BloqueDatos bloqueDatos;
        int contador = 0;
        try {
            String ruta = fileChooser.getSelectedFile().getAbsolutePath();
            vp.setTitle(fileChooser.getSelectedFile().getName());
            FileInputStream in = new FileInputStream(ruta);
            int c;
            while ((c = in.read()) != -1) {
                buffer[contador] = c;
                contador++;
            }
            in.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(null, "Error de carga", "No se puede leer el fichero", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        } catch (NullPointerException e) {
            //vp.setTitle(Spectrum.TITULO_EMULADOR);
        }
        int posDato;
        int posBuffer = 0;
        while ((buffer[posBuffer] + (buffer[posBuffer + 1] * 256)) != 0) {//Si hay bloques de datos sigue añadiéndolos
            bloqueDatos = new BloqueDatos();
            int longitudDatos = buffer[posBuffer] + (buffer[posBuffer + 1] * 256);//Longitud del bloque de datos a cargar
            posBuffer += 2;//Saltamos la longitud del bloque para no añadir estos datos al bloque de datos
            int finalDatos = posBuffer + longitudDatos;
            posDato = 0;
            do {
                bloqueDatos.setDato(posDato, buffer[posBuffer]);
                posDato++;
                posBuffer++;
            } while (posBuffer < finalDatos);
            bloque.add(bloqueDatos);
        }
    }

    //Devuelve el dato indicado del bloque actual
    public int getDatoBloqueActual(int numDato) {
        return bloque.get(this.numBloque).getDato(numDato);
    }

    //Incrementa el número de bloque actual
    public void IncNumBloqueActual() {
        numBloque++;
    }

    //Retorna el número de bloque actual
    public int getNumBloqueActual() {
        return numBloque;
    }

    //Añade el número de bloque pasado como parámetro
    public void setNumBloque(int pos) {
        numBloque = pos;
    }

    //Retorna el tamaño del número de bloques de datos que hay amacenados en el ArrayList bloque
    public int getNumBloques() {
        return bloque.size();
    }

    //Carga un archivo SnapShot
    public void cargarSnapShot() {
        JFileChooser fileChooser = new JFileChooser(".");//Directorio actual
        FileFilter filtro = new FileNameExtensionFilter("Archivos SnapShot (.sna)", "sna");
        fileChooser.setFileFilter(filtro);
        fileChooser.showOpenDialog(fileChooser);
        String ruta = fileChooser.getSelectedFile().getAbsolutePath();
        vp.setTitle(fileChooser.getSelectedFile().getName());
        try {
            FileInputStream in = new FileInputStream(ruta);
            z.setI(in.read());
            z.escribeL_(in.read());
            z.escribeH_(in.read());
            z.escribeE_(in.read());
            z.escribeD_(in.read());
            z.escribeC_(in.read());
            z.escribeB_(in.read());
            z.escribeF_(in.read());
            z.escribeA_(in.read());
            z.escribeL(in.read());
            z.escribeH(in.read());
            z.escribeE(in.read());
            z.escribeD(in.read());
            z.escribeC(in.read());
            z.escribeB(in.read());
            z.escribeIyL(in.read());
            z.escribeIyH(in.read());
            z.escribeIxL(in.read());
            z.escribeIxH(in.read());
            int interrupt = in.read();
            z.setIFF();//Habilita las interrupciones
            z.escribeR(in.read());
            z.escribeF(in.read());
            z.escribeA(in.read());
            z.escribeP(in.read());
            z.escribeS(in.read());
            int intMode = in.read();//Modo de interupción
            switch (intMode) {
                case 0:
                    z.setIM0();
                    break;
                case 1:
                    z.setIM1();
                    break;
                case 2:
                    z.setIM2();
            }
            int border = in.read();
            for (int n = 16384; n < 65536; n++) {
                Memoria.escribe(n, in.read());
            }
            p.llenaBorder(border);
            p.repaint();
            p.repaint();
            p.repaint();
            z.retN();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Cargar.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Cargar.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NullPointerException e) {
            //vp.setTitle(Spectrum.TITULO_EMULADOR);
        }
    }

    public void setZ80(Z80 z) {
        this.z = z;
    }

    public void setPantalla(Pantalla p) {
        this.p = p;
    }

    public void setFrame(JFrame vp) {
        this.vp = vp;
    }
}
