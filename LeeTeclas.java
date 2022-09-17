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
    public static int kempston = 0;

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
        //System.out.printf("KEY " + c + "\n");
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
            KROW0 &= 254;//Caps Shift (SHIFT)
        }
        if (c == 17) {
            KROW7 &= 253;//Symbol Shift (CTRL)
        }

        //Joystick Kempstop
        if (c == 39) {
            kempston |= 1;//Derecha cursor
        }
        if (c == 37) {
            kempston |= 2;//Izquierda cursor
        }
        if (c == 40) {
            kempston |= 4;//Abajo cursor
        }
        if (c == 38) {
            kempston |= 8;//Arriba cursor
        }
        if (c == 153) {
            kempston |= 16;//Disparo tecla <>
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int c = e.getKeyCode();
        if (c == 90) {
            KROW0 |= (byte) (0xff - 189); //Z
        }
        if (c == 88) {
            KROW0 |= (byte) (0xff - 187); //X
        }
        if (c == 67) {
            KROW0 |= (byte) (0xff - 183); //C
        }
        if (c == 86) {
            KROW0 |= (byte) (0xff - 175); //V
        }
        if (c == 65) {
            KROW1 |= (byte) (0xff - 190); //A
        }
        if (c == 83) {
            KROW1 |= (byte) (0xff - 189); //S
        }
        if (c == 68) {
            KROW1 |= (byte) (0xff - 187); //D
        }
        if (c == 70) {
            KROW1 |= (byte) (0xff - 183); //F
        }
        if (c == 71) {
            KROW1 |= (byte) (0xff - 175); //G
        }
        if (c == 81) {
            KROW2 |= (byte) (0xff - 190); //Q
        }
        if (c == 87) {
            KROW2 |= (byte) (0xff - 189); //W
        }
        if (c == 69) {
            KROW2 |= (byte) (0xff - 187); //E
        }
        if (c == 82) {
            KROW2 |= (byte) (0xff - 183); //R
        }
        if (c == 84) {
            KROW2 |= (byte) (0xff - 175); //T
        }
        if (c == 49) {
            KROW3 |= (byte) (0xff - 190); //1
        }
        if (c == 50) {
            KROW3 |= (byte) (0xff - 189); //2
        }
        if (c == 51) {
            KROW3 |= (byte) (0xff - 187); //3
        }
        if (c == 52) {
            KROW3 |= (byte) (0xff - 183); //4
        }
        if (c == 53) {
            KROW3 |= (byte) (0xff - 175); //5
        }
        if (c == 48) {
            KROW4 |= (byte) (0xff - 190); //0
        }
        if (c == 57) {
            KROW4 |= (byte) (0xff - 189); //9
        }
        if (c == 56) {
            KROW4 |= (byte) (0xff - 187); //8
        }
        if (c == 55) {
            KROW4 |= (byte) (0xff - 183); //7
        }
        if (c == 54) {
            KROW4 |= (byte) (0xff - 175); //6
        }
        if (c == 80) {
            KROW5 |= (byte) (0xff - 190); //P
        }
        if (c == 79) {
            KROW5 |= (byte) (0xff - 189); //O
        }
        if (c == 73) {
            KROW5 |= (byte) (0xff - 187); //I
        }
        if (c == 85) {
            KROW5 |= (byte) (0xff - 183); //U
        }
        if (c == 89) {
            KROW5 |= (byte) (0xff - 175); //Y
        }
        if (c == 10) {
            KROW6 |= (byte) (0xff - 190); //ENTER
        }
        if (c == 76) {
            KROW6 |= (byte) (0xff - 189); //L
        }
        if (c == 75) {
            KROW6 |= (byte) (0xff - 187); //K
        }
        if (c == 74) {
            KROW6 |= (byte) (0xff - 183); //J
        }
        if (c == 72) {
            KROW6 |= (byte) (0xff - 175); //H
        }
        if (c == 32) {
            KROW7 |= (byte) (0xff - 190); //SPACE
        }
        if (c == 77) {
            KROW7 |= (byte) (0xff - 187); //M
        }
        if (c == 78) {
            KROW7 |= (byte) (0xff - 183); //N
        }
        if (c == 66) {
            KROW7 |= (byte) (0xff - 175); //B
        }
        if (c == 16) {
            KROW0 |= 1;//Caps Shift (SHIFT)
        }
        if (c == 17) {
            KROW7 |= 2;//Symbol Shift (CTRL)
        }

        //Joystick Kempstop
        if (c == 39) {
            kempston &= (0xff - 1);//Derecha cursor
        }
        if (c == 37) {
            kempston &= (0xff - 2);//Izquierda cursor
        }
        if (c == 40) {
            kempston &= (0xff - 4);//Abajo cursor
        }
        if (c == 38) {
            kempston &= (0xff - 8);//Arriba cursor
        }
        if (c == 153) {
            kempston &= (0xff - 16);////Disparo tecla <>
        }
    }
}
