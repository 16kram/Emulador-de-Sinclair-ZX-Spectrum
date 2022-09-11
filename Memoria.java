/*
*   Emulador del ordenador Sinclair ZX-Spectrum 48K
*   Versión 1.0
*   
*   La clase Memoria emula la memoria del ZX-Spectrum 48K
 */
package spectrum;

/**
 *
 * @author Esteban Porqueras
 */
public class Memoria {

    public static int[] romRam = new int[65536];//Memoria principal Spectrum

    public synchronized static void escribe(int posMemoria, int dato) {
        if (posMemoria > 0x3FFF) {//Se está escribiendo por encima de la ROM
            if (posMemoria > 16384 && posMemoria <= 32768) {
                Spectrum.memoContenida = true;
            } else {
                Spectrum.memoContenida = false;
            }
            romRam[posMemoria] = dato;
        }
    }

    public synchronized static int lee(int posMemoria) {
        if (posMemoria > 16384 && posMemoria <= 32768) {
            Spectrum.memoContenida = true;
        } else {
            Spectrum.memoContenida = false;
        }
        return romRam[posMemoria];
    }

    public static int leePantalla(int posMemoria) {
        return romRam[posMemoria];
    }

}
