/*
*   Emulador del ordenador Sinclair ZX-Spectrum 48K
*   Versión 1.0
*   
*   La clase Cargar carga la ROM y permite la carga de programas en formato TAP en el ZX-Spectrum 48K
 */
package spectrum;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.JFileChooser;
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

    public void loadRom() {
        int contador = 0;//Inicio memoria Spectrum
        try {
            FileInputStream in = new FileInputStream("/home/esteban/NetBeansProjects/Spectrum/src/ROM/spectrum.rom");
            int c;
            while ((c = in.read()) != -1) {
                Memoria.romRam[contador] = c;
                contador++;
            }
            in.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
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
            FileInputStream in = new FileInputStream(ruta);
            int c;
            while ((c = in.read()) != -1) {
                buffer[contador] = c;
                contador++;
            }
            in.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
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

}
