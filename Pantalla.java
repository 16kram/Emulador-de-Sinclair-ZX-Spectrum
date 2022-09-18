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
    public static int TAM = 2, Y = 192 * TAM, X = 256 * TAM;
    private int astable = 0;//Parpadeo del Flash
    private int borde = 7;//Color del borde
    private int[] border = new int[312];
    private int ptrBorder = 0;

    @Override
    public void paint(Graphics g) {
        super.paint(g);//Elimina parpadeos en la pantalla con la llamada super.paint(g)
        ponBorde(g);
        actualiza(g);
    }

    public synchronized void actualiza(Graphics g) {
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
        int posMaxX = X + offsetX;
        int posMaxY = Y + offsetY;
        while (posY < (posMaxY)) {
            while (posX < posMaxX) {
                dato = Memoria.leePantalla(dirMemoria + (256 * scan) + posMemX);
                color = Memoria.leePantalla(dirAtributos + posMemX + ((posMemY) * 32));
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
                    posX = posX + TAM;
                }
                posMemX++;
            }
            posY = posY + TAM;
            posMemX = 0;
            posX = 0 + offsetX;
            scan++;
            if (scan > 7) {
                scan = 0;
                dirMemoria = dirMemoria + 32;
                posMemY++;
            }
            if (posY == ((64 * TAM) + offsetY)) {
                dirMemoria = DIR_MEMORIA_VIDEO + 2048;
                scan = 0;
            }
            if (posY == ((128 * TAM) + offsetY)) {
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

    public void ponBorde(Graphics g) {
        int anchoVentanaBorde = g.getClipBounds().width;//Ancho de la ventana actual
        int altoVentanaBorde = g.getClipBounds().height;//Alto de la ventana actual
        int posXBorde = 0;
        int posYBorde = 0;
        int ptrBorde = 0;
        while (posYBorde < altoVentanaBorde) {
            borde = border[ptrBorde];
            g.setColor(ponColorSinBrillo(borde & 7));
            g.fillRect(posXBorde, posYBorde, anchoVentanaBorde, TAM);
            posYBorde = posYBorde + TAM;
            ptrBorde++;
        }
    }

    public void actualizaBorde() {
        border[ptrBorder] = borde;
        ptrBorder++;
    }

    public void finBorde() {
        ptrBorder = 0;
        //Arrays.fill(border, borde);
    }

    //Rellena el borde, se utiliza para poner el color del borde en las cargas SNA y cuando se hace un Reset
    public void llenaBorder(int color) {
        //Arrays.fill(border,color);
        for (int n = 0; n < 312; n++) {
            border[n] = color;
        }
    }
}
