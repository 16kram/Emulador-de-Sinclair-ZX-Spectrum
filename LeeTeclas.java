/*
*   Emulador del ordenador Sinclair ZX-Spectrum 48K
*   Versión 1.0
*   
*   La clase LeeTeclas emula el teclado del ZX-Spectrum 48K
 */
package spectrum;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 *
 * @author Esteban Porqueras
 */
public class LeeTeclas extends KeyAdapter {

    public static byte KROW0, KROW1, KROW2, KROW3, KROW4, KROW5, KROW6, KROW7;
    public static boolean capsShift, symbolShift = false;
    private int cont = 0;
    private final int MAX_TECLAS = 8;
    private final int[] teclasPulsadas = new int[MAX_TECLAS];

    LeeTeclas() {
        LeeTeclas.KROW0 = -65;
        LeeTeclas.KROW1 = -65;
        LeeTeclas.KROW2 = -65;
        LeeTeclas.KROW3 = -65;
        LeeTeclas.KROW4 = -65;
        LeeTeclas.KROW5 = -65;
        LeeTeclas.KROW6 = -65;
        LeeTeclas.KROW7 = -65;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int c = e.getKeyCode();
        //System.out.printf("KEY "+c+"\n");

        //Revisa si la tecla pulsada ya existe en el array, y si está devuelve true
        boolean hayTecla = false;
        for (int n = 0; n < MAX_TECLAS; n++) {
            if (teclasPulsadas[n] == c) {
                hayTecla = true;
            }
        }

        //Si las teclas no existen en el array las añade
        if (teclasPulsadas[cont] != c && cont == 0 && !hayTecla) {
            teclasPulsadas[cont++] = c;
        }
        if (cont >= MAX_TECLAS - 2) {
            cont = MAX_TECLAS - 2;
        }
        if (teclasPulsadas[cont - 1] != c && cont > 0 && cont < MAX_TECLAS - 2 && !hayTecla) {
            teclasPulsadas[cont++] = c;
        }

        /*for(int n=0;n<MAX_TECLAS;n++){
                    System.out.print("Pulsada-->"+teclasPulsadas[n]+", ");
                }
                System.out.println();*/
        if (c == 90) {
            KROW0 &= (byte) 189; //Z
        }
        if (c == 88) {
            KROW0 &= (byte) 187; //X
        }
        if (c == 67) {
            KROW0 &= (byte) 183; //C
        }
        if (c == 86) {
            KROW0 &= (byte) 175; //V
        }
        if (c == 65) {
            KROW1 &= (byte) 190; //A
        }
        if (c == 83) {
            KROW1 &= (byte) 189; //S
        }
        if (c == 68) {
            KROW1 &= (byte) 187; //D
        }
        if (c == 70) {
            KROW1 &= (byte) 183; //F
        }
        if (c == 71) {
            KROW1 &= (byte) 175; //G
        }
        if (c == 81) {
            KROW2 &= (byte) 190; //Q
        }
        if (c == 87) {
            KROW2 &= (byte) 189; //W
        }
        if (c == 69) {
            KROW2 &= (byte) 187; //E
        }
        if (c == 82) {
            KROW2 &= (byte) 183; //R
        }
        if (c == 84) {
            KROW2 &= (byte) 175; //T
        }
        if (c == 49) {
            KROW3 &= (byte) 190; //1
        }
        if (c == 50) {
            KROW3 &= (byte) 189; //2
        }
        if (c == 51) {
            KROW3 &= (byte) 187; //3
        }
        if (c == 52) {
            KROW3 &= (byte) 183; //4
        }
        if (c == 53) {
            KROW3 &= (byte) 175; //5
        }
        if (c == 48) {
            KROW4 &= (byte) 190; //0
        }
        if (c == 57) {
            KROW4 &= (byte) 189; //9
        }
        if (c == 56) {
            KROW4 &= (byte) 187; //8
        }
        if (c == 55) {
            KROW4 &= (byte) 183; //7
        }
        if (c == 54) {
            KROW4 &= (byte) 175; //6
        }
        if (c == 80) {
            KROW5 &= (byte) 190; //P
        }
        if (c == 79) {
            KROW5 &= (byte) 189; //O
        }
        if (c == 73) {
            KROW5 &= (byte) 187; //I
        }
        if (c == 85) {
            KROW5 &= (byte) 183; //U
        }
        if (c == 89) {
            KROW5 &= (byte) 175; //Y
        }
        if (c == 10) {
            KROW6 &= (byte) 190; //ENTER
        }
        if (c == 76) {
            KROW6 &= (byte) 189; //L
        }
        if (c == 75) {
            KROW6 &= (byte) 187; //K
        }
        if (c == 74) {
            KROW6 &= (byte) 183; //J
        }
        if (c == 72) {
            KROW6 &= (byte) 175; //H
        }
        if (c == 32) {
            KROW7 &= (byte) 190; //SPACE
        }
        if (c == 77) {
            KROW7 &= (byte) 187; //M
        }
        if (c == 78) {
            KROW7 &= (byte) 183; //N
        }
        if (c == 66) {
            KROW7 &= (byte) 175; //B
        }
        if (c == 16) {
            capsShift = true; //CAPS SHIFT
        }
        if (c == 17) {
            symbolShift = true; //SYMBOL SHIFT (CTRL)
        }
        if (capsShift) {
            KROW0 &= 254;
        }
        if (symbolShift) {
            KROW7 &= 253;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int c = e.getKeyCode();

        //Borra del array las teclas que se han liberado
        for (int n = 0; n < MAX_TECLAS; n++) {
            if (teclasPulsadas[n] == c) {
                teclasPulsadas[n] = 0;
            }
        }

        /*for(int n=0;n<MAX_TECLAS;n++){
                System.out.print("Liberada-->"+teclasPulsadas[n]+", ");
                }
                System.out.println();*/
        if (c == 16) {
            capsShift = false;
        }
        if (c == 17) {
            symbolShift = false;
        }
        LeeTeclas.KROW0 = -65;
        LeeTeclas.KROW1 = -65;
        LeeTeclas.KROW2 = -65;
        LeeTeclas.KROW3 = -65;
        LeeTeclas.KROW4 = -65;
        LeeTeclas.KROW5 = -65;
        LeeTeclas.KROW6 = -65;
        LeeTeclas.KROW7 = -65;

        for (int n = 0; n < MAX_TECLAS; n++) {
            int t = teclasPulsadas[n];
            if (t == 90) {
                KROW0 &= (byte) 189; //Z
            }
            if (t == 88) {
                KROW0 &= (byte) 187; //X
            }
            if (t == 67) {
                KROW0 &= (byte) 183; //C
            }
            if (t == 86) {
                KROW0 &= (byte) 175; //V
            }
            if (t == 65) {
                KROW1 &= (byte) 190; //A
            }
            if (t == 83) {
                KROW1 &= (byte) 189; //S
            }
            if (t == 68) {
                KROW1 &= (byte) 187; //D
            }
            if (t == 70) {
                KROW1 &= (byte) 183; //F
            }
            if (t == 71) {
                KROW1 &= (byte) 175; //G
            }
            if (t == 81) {
                KROW2 &= (byte) 190; //Q
            }
            if (t == 87) {
                KROW2 &= (byte) 189; //W
            }
            if (t == 69) {
                KROW2 &= (byte) 187; //E
            }
            if (t == 82) {
                KROW2 &= (byte) 183; //R
            }
            if (t == 84) {
                KROW2 &= (byte) 175; //T
            }
            if (t == 49) {
                KROW3 &= (byte) 190; //1
            }
            if (t == 50) {
                KROW3 &= (byte) 189; //2
            }
            if (t == 51) {
                KROW3 &= (byte) 187; //3
            }
            if (t == 52) {
                KROW3 &= (byte) 183; //4
            }
            if (t == 53) {
                KROW3 &= (byte) 175; //5
            }
            if (t == 48) {
                KROW4 &= (byte) 190; //0
            }
            if (t == 57) {
                KROW4 &= (byte) 189; //9
            }
            if (t == 56) {
                KROW4 &= (byte) 187; //8
            }
            if (t == 55) {
                KROW4 &= (byte) 183; //7
            }
            if (t == 54) {
                KROW4 &= (byte) 175; //6
            }
            if (t == 80) {
                KROW5 &= (byte) 190; //P
            }
            if (t == 79) {
                KROW5 &= (byte) 189; //O
            }
            if (t == 73) {
                KROW5 &= (byte) 187; //I
            }
            if (t == 85) {
                KROW5 &= (byte) 183; //U
            }
            if (t == 89) {
                KROW5 &= (byte) 175; //Y
            }
            if (t == 10) {
                KROW6 &= (byte) 190; //ENTER
            }
            if (t == 76) {
                KROW6 &= (byte) 189; //L
            }
            if (t == 75) {
                KROW6 &= (byte) 187; //K
            }
            if (t == 74) {
                KROW6 &= (byte) 183; //J
            }
            if (t == 72) {
                KROW6 &= (byte) 175; //H
            }
            if (t == 32) {
                KROW7 &= (byte) 190; //SPACE
            }
            if (t == 77) {
                KROW7 &= (byte) 187; //M
            }
            if (t == 78) {
                KROW7 &= (byte) 183; //N
            }
            if (t == 66) {
                KROW7 &= (byte) 175; //B
            }
        }
        if (cont > 1) {
            cont--;
        }
    }
}
