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

    public static int[] romRam = new int[65538];//Memoria principal Spectrum

    public static void escribe(int posMemoria, int dato) {
        if (posMemoria > 0x3FFF) {
            romRam[posMemoria] = dato;
        }
    }

    public static int lee(int posMemoria) {
        return romRam[posMemoria];
    }

}
