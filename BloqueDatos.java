/*
*   Emulador del ordenador Sinclair ZX-Spectrum 48K
*   Versión 1.0
*   
*   La clase BloqueDatos sirve para almacenar los bloques de datos de la clase Cargar
 */
package spectrum;

import java.util.ArrayList;

/**
 *
 * @author Esteban Porqueras
 */
public class BloqueDatos {

    private final ArrayList<Integer> bloqueDatos = new ArrayList();

    public void setDato(int dato) {
        bloqueDatos.add(dato);
    }

    public void setDato(int pos, int dato) {
        bloqueDatos.add(pos, dato);
    }

    public int getDato(int pos) {
        return bloqueDatos.get(pos);
    }

    public int getTam() {
        return bloqueDatos.size();
    }

    //Devuelve el nombre de la cabecera
    public String getNombre() {
        String nombre = "";
        for (int n = 2; n < 12; n++) {
            int valor = bloqueDatos.get(n);
            nombre = nombre + Character.toString((char) valor);
        }
        if (bloqueDatos.get(0) == 0) {//Es una cabecera
            return nombre;
        } else {
            return "Es un bloque de datos";
        }
    }
}
