/*
*   Emulador del ordenador Sinclair ZX-Spectrum 48K
*   Versión 1.0
*   
*   La clase Pantalla emula la pantalla de TV del ZX-Spectrum 48K.
*
 */
package spectrum;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JPanel;

/**
 *
 * @author Esteban Porqueras Araque
 */
public class Pantalla extends JPanel {

    private static final int DIR_MEMORIA_VIDEO = 16384;
    private static final int DIR_MEMORIA_ATRIBUTOS = 6144;
    public static final int TAM = 1, Y = 192, X = 256;
    private int astable = 0;//Parpadeo del Flash
    private int borde = 0;//Color del borde

    @Override
    public void paint(Graphics g) {
        actualiza(g);
    }

    public void actualiza(Graphics g) {
        int anchoVentana = g.getClipBounds().width;//Ancho de la ventana actual
        int altoVentana = g.getClipBounds().height;//Alto de la ventana actual
        int offsetX = (anchoVentana - X) / 2;
        int offsetY = (altoVentana - Y) / 2;
        int posX = 0 + offsetX;
        int posY = 0 + offsetY;
        int dato;//Dato de la memoria de pantalla
        int dirMemoria = DIR_MEMORIA_VIDEO;//Dirección memoria de video
        int dirAtributos = dirMemoria + DIR_MEMORIA_ATRIBUTOS;//Direcció memoria atributos color
        int scan = 0;
        int posMemX = 0;
        int posMemY = 0;
        int color, paper, ink, bright, flash;
        g.setColor(ponColorSinBrillo(borde & 7));// Pone el color del borde
        g.fillRect(0, 0, anchoVentana, altoVentana);//Añade el borde
        while (posY < Y + offsetY) {
            while (posX < (X + offsetX)) {
                dato = Memoria.lee(dirMemoria + (256 * scan) + posMemX);
                color = Memoria.lee(dirAtributos + posMemX + ((posMemY) * 32));
                ink = color & 0b00000111;
                paper = color & 0b00111000;
                paper = paper >> 3;
                bright = color & 0b01000000;
                flash = color & 0b10000000;
                if (flash == 128 && astable == 1) {
                    int aux = paper;
                    paper = ink;
                    ink = aux;
                }
                for (int z = 0; z < 8; z++) {
                    if ((dato & 0x80) == 0x80) {
                        if (bright == 0x40) {
                            g.setColor(ponColorConBrillo(ink));
                        } else {
                            g.setColor(ponColorSinBrillo(ink));
                        }
                    } else {
                        if (bright == 0x40) {
                            g.setColor(ponColorConBrillo(paper));
                        } else {
                            g.setColor(ponColorSinBrillo(paper));
                        }
                    }
                    dato = dato << 1;
                    g.fillRect(posX, posY, TAM, TAM);
                    posX++;
                }
                posMemX++;
            }
            posY++;
            posMemX = 0;
            posX = 0 + offsetX;
            scan++;
            if (scan > 7) {
                scan = 0;
                dirMemoria = dirMemoria + 32;
                posMemY++;
            }
            if (posY == 64 + offsetY) {
                dirMemoria = DIR_MEMORIA_VIDEO + 2048;
                scan = 0;
            }
            if (posY == 128 + offsetY) {
                dirMemoria = DIR_MEMORIA_VIDEO + 4096;
                scan = 0;
            }
        }
    }

    public Color ponColorConBrillo(int color) {
        switch (color) {
            case 0:
                return Color.black.brighter();
            case 1:
                return Color.blue.brighter();
            case 2:
                return Color.red.brighter();
            case 3:
                return Color.magenta.brighter();
            case 4:
                return Color.green.brighter();
            case 5:
                return Color.cyan.brighter();
            case 6:
                return Color.yellow.brighter();
            case 7:
                return Color.white.brighter();
            default:
                return Color.white.brighter();
        }
    }

    public Color ponColorSinBrillo(int color) {
        switch (color) {
            case 0:
                return Color.black.darker();
            case 1:
                return Color.blue.darker();
            case 2:
                return Color.red.darker();
            case 3:
                return Color.magenta.darker();
            case 4:
                return Color.green.darker();
            case 5:
                return Color.cyan.darker();
            case 6:
                return Color.yellow.darker();
            case 7:
                return Color.white.darker();
            default:
                return Color.white.darker();
        }
    }

    public int getAstable() {
        return astable;
    }

    public void setAstable(int astable) {
        this.astable = astable;
    }

    public int getBorde() {
        return borde;
    }

    public void setBorde(int borde) {
        this.borde = borde;
    }

}
