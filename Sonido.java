/*
*   Emulador del ordenador Sinclair ZX-Spectrum 48K
*   Versión 1.0
*   
*   La clase Sonido emula el sonido del ZX-Spectrum 48K.
*
 */
package spectrum;

import java.util.Arrays;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/**
 *
 * @author Esteban Porqueras Araque
 */
public class Sonido extends Thread {

    private static double AMPLITUD_MAXIMA = 32760; //Volumen máximo
    private static int SAMPLE_RATE = 48000; //Número de muestras por segundo
    private static AudioFormat format = null;
    private static SourceDataLine line = null;
    private static byte[] muestras = new byte[40000];
    public static int posMuestra = 0;
    public static int valor = 0;

    public void createOutput() {
        /* 
 PCM_SIGNED:
 SAMPLE_RATE: Muestras por segundo
 Valor 16: Tamaño de la muestra en bits, pueden estar entre 2^15  y - 2^15-1
 Valor 2: Número de canales, en este caso es en stereo
 Valor 4: Tamaño del muestreo en bytes (2 bytes/sample * 2 channels)
 SAMPLE_RATE: Muestras por segundo       
 false: little endian 
         */
        format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                SAMPLE_RATE, 16, 2, 4, SAMPLE_RATE, false);
        System.out.println("Audio format: " + format);
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("Line does not support: " + format);
                System.exit(0);
            }
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    public void setValor(int buz, int mic) {
        int nivel = 0;
        nivel = (buz == 1) ? (int) 16384 : 0;
        nivel += (mic == 1) ? (int) 8192 : 0;
        //valor=valor-(valor/8);
        //valor = valor+(nivel/8);
        valor = nivel;
        //System.out.println(valor);
    }

    public void guardaMuestra() {
        //Canal izquierdo
        muestras[posMuestra + 0] = (byte) (valor & 0xFF); //byte bajo
        muestras[posMuestra + 1] = (byte) ((valor >> 8) & 0xFF); //byte alto
        //Canal derecho
        muestras[posMuestra + 2] = (byte) (valor & 0xFF);//byte bajo
        muestras[posMuestra + 3] = (byte) ((valor >> 8) & 0xFF);//byte alto
        posMuestra += 4;
    }

    // Reproduce el sonido 
    public void play() {
        line.write(muestras, 0, posMuestra);
        posMuestra = 0;
    }

    //Reproduce el sonido
    @Override
    public synchronized void run() {
        line.write(muestras, 0, posMuestra);
        posMuestra = 0;
    }

    //Pone a cero el array de muestras de sonido
    public void reset() {
        Arrays.fill(muestras, (byte) 0);
    }

    public void cerrarSonido() {
        //line.drain();
        line.stop();
        line.close();
    }
}
