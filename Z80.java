/*
*   Emulador del ordenador Sinclair ZX-Spectrum 48K
*   Versión 1.0
*   
*   La clase Z-80 emula el microprocesador del ZX-Spectrum 48K
*
 */
package spectrum;

/**
 *
 * @author Esteban Porqueras
 */
public class Z80 {

    private Cargar cargar;
    private Pantalla pantalla;
    private Sonido sonido;
    private boolean reset;

    // Registros Z-80
    // Set de registros principales
    private int af;
    private int bc;
    private int de;
    private int hl;

    // Set de registros alternativos
    private int af_;
    private int bc_;
    private int de_;
    private int hl_;

    // Set de registros de propósito general
    private int i;// Vector de interrupciones
    private int r;// Registro de refresco de memoria
    private int ix;
    private int iy;
    private int sp;
    private int pc;

    // Registro de instrucción
    private int ir;

    // Registros auxiliares
    int aux;// Registro auxiliar para el intercambio de registros
    int desp;// Valor de desplazamiento de los registros IX e IY
    int desplazamiento;
    int indice;

    // Códigos de los registros
    public static final int A = 7;
    public static final int B = 0;
    public static final int C = 1;
    public static final int D = 2;
    public static final int E = 3;
    public static final int H = 4;
    public static final int L = 5;

    public static final int BC = 10;
    public static final int DE = 11;
    public static final int HL = 12;
    public static final int SP = 13;
    public static final int IX = 14;
    public static final int IY = 15;

    public static final int IX_D = 16;
    public static final int IY_D = 17;

    public static final int IXH = 20;
    public static final int IXL = 21;
    public static final int IYH = 22;
    public static final int IYL = 23;

    // Códigos de los Flags
    public static final int FLAG_C = 1;
    public static final int FLAG_N = 2;
    public static final int FLAG_PV = 4;
    public static final int FLAG_X = 8;
    public static final int FLAG_H = 16;
    public static final int FLAG_Y = 32;
    public static final int FLAG_Z = 64;
    public static final int FLAG_S = 128;

    // Bus de direcciones
    public static int a0a7;// A0-A7
    public static int a8a15;// A8-A15

    // Bus de datos
    public static int d0d7;// D0-D7

    //Valor por defecto del puerto 0xFE
    public static int valorPortFE;

    // Señales CPU
    private boolean iorq;// Input output request. Se pone a cero para acceder a los periféricos
    private boolean halt;// Halt Detiene la CPU.

    // Flip flops de estado de las interrupciones
    private boolean IFF1;// Interrupciones hailitadas=1, desabilitadas=0
    private boolean IFF2;// Guarda IFF1 durante el servicio de una interrupción no enmascarable NMI

    // Flip flops de modos de interrupción
    // IMFa=0 IMFb=0 Interrupción modo 0
    // IMFa=0 IMFb=1 No utilizado
    // IMFa=1 IMFb=0 Interrupción modo 1
    // IMFa=1 IMFb=1 Interrupción modo 2
    private boolean IMFa;
    private boolean IMFb;

    //T-States
    private int tStates;

    public Z80() {
        resetZ80();
    }

    public void clock() {
        if (reset) {
            resetZ80();
            reset = false;
        }
        if (halt) {//poner halt
            Emulacion();
        } else {
            //NOP
            tStates += 4;//HALT ejecuta instrucciones NOP
            incR();//La CPU deja de funcionar y sigue con los ciclos de refresco de la memoria
        }
    }

    public void Emulacion() {
        //Carga de programas mediante LOAD
        if (this.getPc() == 0x056B) {
            if (cargar.getNumBloqueActual() >= cargar.getNumBloques()) {
                //No hay datos para cargar
            } else {
                setCFlag();//Bandera a 1 LOAD, bandera a 0 VERIFY
                bc = 0xB001;
                af = 0x0145;
                int dirIx = ix;//Dirección donde se guardan los datos
                int longitudBloque = de;//Longitud de los datos
                if (longitudBloque > cargar.getTamBloque()) {//Si el valor que indica el registro DE es mayor que el tamaño del bloque de datos
                    longitudBloque = cargar.getTamBloque();  //se toma como referencia la longitud del bloque de datos
                }
                int contDatos = 0;
                while (contDatos <= longitudBloque) {
                    int dato = cargar.getDatoBloqueActual(contDatos + 1);
                    Memoria.escribe(ix, dato);
                    incIX();
                    decDE();
                    contDatos++;
                }
                //System.out.println("Longitud del bloque=" + longitudBloque);
                cargar.IncNumBloqueActual();
            }
            setPc(0x05e2);
        }
        ir = Memoria.lee(pc);// Lee el contenido de la memoria FETCH T1
        incPC();// Incrementa el contador del programa T2
        incR();//Ciclos de refresco la memoria
        switch (ir) {// Decodificar T3-T4
            // NOP
            case 0x00:
                tStates += 4;
                break;
            // LD BC,HHLL
            case 0x01:
                tStates += 10;
                escribeC(Memoria.lee(pc));// C=Registro menor peso (7..0)
                incPC();
                escribeB(Memoria.lee(pc));// B=Registro mayor peso (15..8)
                incPC();
                break;
            // LD (BC),A
            case 0x02:
                tStates += 7;
                Memoria.escribe(bc, leeA());
                break;
            // INC BC
            case 0x03:
                tStates += 6;
                incBC();
                break;
            // INC B
            case 0x04:
                tStates += 4;
                inc(B);
                break;
            // DEC B
            case 0x05:
                tStates += 4;
                dec(B);
                break;
            // LD B,NN
            case 0x06:
                tStates += 7;
                escribeB(Memoria.lee(pc));
                incPC();
                break;
            // RLCA
            case 0x07:
                tStates += 4;
                rlca();
                break;
            // EX AF,AF’
            case 0x08:
                tStates += 4;
                aux = af_;
                af_ = af;
                af = aux;
                break;
            // ADD HL,BC
            case 0x09:
                tStates += 11;
                addHl(BC);
                break;
            // LD A,(BC)
            case 0x0A:
                tStates += 7;
                escribeA(Memoria.lee(bc));
                break;
            // DEC BC
            case 0x0B:
                tStates += 6;
                decBC();
                break;
            // INC C
            case 0x0C:
                tStates += 4;
                inc(C);
                break;
            // DEC C
            case 0x0D:
                tStates += 4;
                dec(C);
                break;
            // LD C,NN
            case 0x0E:
                tStates += 7;
                escribeC(Memoria.lee(pc));
                incPC();
                break;
            // RRCA
            case 0x0F:
                tStates += 4;
                rrca();
                break;
            // DJNZ NN
            case 0x10:
                int flagFTemp = leeF();//Se guarda el registro F
                dec(B);//para no alterar el estado
                escribeF(flagFTemp);//de los Flags
                if (leeB() != 0) {
                    tStates += 1;//1 ciclo + 12 de JR = 13 de DJNZ
                    jr();
                } else {
                    tStates += 8;
                    incPC();// Si no se cumple la condición ignora el byte de salto
                }
                break;
            // LD DE,HHLL
            case 0x11:
                tStates += 10;
                escribeE(Memoria.lee(pc));// E=Registro menor peso (7..0)
                incPC();
                escribeD(Memoria.lee(pc));// D=Registro mayor peso (15..8)
                incPC();
                break;
            // LD (DE),A
            case 0x12:
                tStates += 7;
                Memoria.escribe(de, leeA());
                break;
            // INC DE
            case 0x13:
                tStates += 6;
                incDE();
                break;
            // INC D
            case 0x14:
                tStates += 4;
                inc(D);
                break;
            // DEC D
            case 0x15:
                tStates += 4;
                dec(D);
                break;
            // LD D,NN
            case 0x16:
                tStates += 7;
                escribeD(Memoria.lee(pc));
                incPC();
                break;
            // RLA
            case 0x17:
                tStates += 4;
                rla();
                break;
            // JR NN
            case 0x18:
                jr();
                break;
            // ADD HL,DE
            case 0x19:
                tStates += 11;
                addHl(DE);
                break;
            // LD A,(DE)
            case 0x1A:
                tStates += 7;
                escribeA(Memoria.lee(de));
                break;
            // DEC DE
            case 0x1B:
                tStates += 6;
                decDE();
                break;
            // INC E
            case 0x1C:
                tStates += 4;
                inc(E);
                break;
            // DEC E
            case 0x1D:
                tStates += 4;
                dec(E);
                break;
            // LD E,NN
            case 0x1E:
                tStates += 7;
                escribeE(Memoria.lee(pc));
                incPC();
                break;
            // RRA
            case 0x1F:
                tStates += 4;
                rra();
                break;
            // JR NZ,NN
            case 0x20:
                if (!getFlagZ()) {
                    jr();
                } else {
                    tStates += 7;
                    incPC();
                }
                break;
            // LD HL,HHLL
            case 0x21:
                tStates += 10;
                escribeL(Memoria.lee(pc));// L=Registro menor peso (7..0)
                incPC();
                escribeH(Memoria.lee(pc));// H=Registro mayor peso (15..8)
                incPC();
                break;
            // LD (HHLL),HL
            case 0x22:
                tStates += 16;
                int regLL = (Memoria.lee(pc));// LL=Registro menor peso (7..0)
                incPC();
                int regHH = (Memoria.lee(pc));// HH=Registro mayor peso (15..8)
                incPC();
                int direccion = (regHH * 256) + regLL;
                Memoria.escribe(direccion, leeL());
                Memoria.escribe((direccion + 1) & 0xffff, leeH());
                break;
            // INC HL
            case 0x23:
                tStates += 6;
                incHL();
                break;
            // INC H
            case 0x24:
                tStates += 4;
                inc(H);
                break;
            // DEC H
            case 0x25:
                tStates += 4;
                dec(H);
                break;
            // LD H,NN
            case 0x26:
                tStates += 7;
                escribeH(Memoria.lee(pc));
                incPC();
                break;
            // DAA
            case 0x27:
                tStates += 4;
                daa();
                break;
            // JR Z,NN
            case 0x28:
                if (getFlagZ()) {
                    jr();
                } else {
                    tStates += 7;
                    incPC();
                }
                break;
            // ADD HL,HL
            case 0x29:
                tStates += 11;
                addHl(HL);
                break;
            // LD HL,(HHLL)
            case 0x2A:
                tStates += 16;
                regLL = Memoria.lee(pc);
                incPC();
                regHH = Memoria.lee(pc);
                incPC();
                direccion = (regHH * 256) + regLL;
                escribeL(Memoria.lee(direccion));
                escribeH(Memoria.lee((direccion + 1) & 0xffff));
                break;
            // DEC HL
            case 0x2B:
                tStates += 6;
                decHL();
                break;
            // INC L
            case 0x2C:
                tStates += 4;
                inc(L);
                break;
            // DEC L
            case 0x2D:
                tStates += 4;
                dec(L);
                break;
            // LD L,NN
            case 0x2E:
                tStates += 7;
                escribeL(Memoria.lee(pc));
                incPC();
                break;
            // CPL
            case 0x2F:
                tStates += 4;
                cpl();
                break;
            // JR NC,NN
            case 0x30:
                if (!getFlagC()) {
                    jr();
                } else {
                    tStates += 7;
                    incPC();
                }
                break;
            // LD SP,HHLL
            case 0x31:
                tStates += 10;
                escribeP(Memoria.lee(pc));// P=Registro menor peso (7..0)
                incPC();
                escribeS(Memoria.lee(pc));// S=Registro mayor peso (15..8)
                incPC();
                break;
            // LD (HHLL),A
            case 0x32:
                tStates += 13;
                int byteL = Memoria.lee(pc);
                incPC();
                int byteH = Memoria.lee(pc);
                incPC();
                direccion = (byteH * 256) + byteL;
                Memoria.escribe(direccion, leeA());
                break;
            // INC SP
            case 0x33:
                tStates += 6;
                incSp();
                break;
            // INC (HL)
            case 0x34:
                tStates += 11;
                inc(HL);
                break;
            // DEC (HL)
            case 0x35:
                tStates += 11;
                dec(HL);
                break;
            // LD (HL),NN
            case 0x36:
                tStates += 10;
                int dato = Memoria.lee(pc);
                Memoria.escribe(hl, dato);
                incPC();
                break;
            // SCF
            case 0x37:
                tStates += 4;
                scf();
                break;
            // JR C,NN
            case 0x38:
                if (getFlagC()) {
                    jr();
                } else {
                    tStates += 7;
                    incPC();
                }
                break;
            // ADD HL,SP
            case 0x39:
                tStates += 11;
                addHl(SP);
                break;
            // LD A,(HHLL)
            case 0x3A:
                tStates += 13;
                byteL = Memoria.lee(pc);
                incPC();
                byteH = Memoria.lee(pc);
                incPC();
                direccion = (byteH * 256) + byteL;
                escribeA(Memoria.lee(direccion));
                break;
            // DEC SP
            case 0x3B:
                tStates += 6;
                decSp();
                break;
            // INC A
            case 0x3C:
                tStates += 4;
                inc(A);
                break;
            // DEC A
            case 0x3D:
                tStates += 4;
                dec(A);
                break;
            // LD A,NN
            case 0x3E:
                tStates += 7;
                dato = Memoria.lee(pc);
                escribeA(dato);
                incPC();
                break;
            // CCF
            case 0x3F:
                tStates += 4;
                ccf();
                break;
            // LD B,B
            case 0x40:
                tStates += 4;
                break;
            // LD B,C
            case 0x41:
                tStates += 4;
                escribeB(leeC());
                break;
            // LD B,D
            case 0x42:
                tStates += 4;
                escribeB(leeD());
                break;
            // LD B,E
            case 0x43:
                tStates += 4;
                escribeB(leeE());
                break;
            // LD B,H
            case 0x44:
                tStates += 4;
                escribeB(leeH());
                break;
            // LD B,L
            case 0x45:
                tStates += 4;
                escribeB(leeL());
                break;
            // LD B,(HL)
            case 0x46:
                tStates += 7;
                escribeB(Memoria.lee(hl));
                break;
            // LD B,A
            case 0x47:
                tStates += 4;
                escribeB(leeA());
                break;
            // LD C,B
            case 0x48:
                tStates += 4;
                escribeC(leeB());
                break;
            // LD C,C
            case 0x49:
                tStates += 4;
                break;
            // LD C,D
            case 0x4A:
                tStates += 4;
                escribeC(leeD());
                break;
            // LD C,E
            case 0x4B:
                tStates += 4;
                escribeC(leeE());
                break;
            // LD C,H
            case 0x4C:
                tStates += 4;
                escribeC(leeH());
                break;
            // LD C,L
            case 0x4D:
                tStates += 4;
                escribeC(leeL());
                break;
            // LD C,(HL)
            case 0x4E:
                tStates += 7;
                escribeC(Memoria.lee(hl));
                break;
            // LD C,A
            case 0x4F:
                tStates += 4;
                escribeC(leeA());
                break;
            // LD D,B
            case 0x50:
                tStates += 4;
                escribeD(leeB());
                break;
            // LD D,C
            case 0x51:
                tStates += 4;
                escribeD(leeC());
                break;
            // LD D,D
            case 0x52:
                tStates += 4;
                break;
            // LD D,E
            case 0x53:
                tStates += 4;
                escribeD(leeE());
                break;
            // LD D,H
            case 0x54:
                tStates += 4;
                escribeD(leeH());
                break;
            // LD D,L
            case 0x55:
                tStates += 4;
                escribeD(leeL());
                break;
            // LD D,(HL)
            case 0x56:
                tStates += 7;
                escribeD(Memoria.lee(hl));
                break;
            // LD D,A
            case 0x57:
                tStates += 4;
                escribeD(leeA());
                break;
            // LD E,B
            case 0x58:
                tStates += 4;
                escribeE(leeB());
                break;
            // LD E,C
            case 0x59:
                tStates += 4;
                escribeE(leeC());
                break;
            // LD E,D
            case 0x5A:
                tStates += 4;
                escribeE(leeD());
                break;
            // LD E,E
            case 0x5B:
                tStates += 4;
                break;
            // LD E,H
            case 0x5C:
                tStates += 4;
                escribeE(leeH());
                break;
            // LD E,L
            case 0x5D:
                tStates += 4;
                escribeE(leeL());
                break;
            // LD E,(HL)
            case 0x5E:
                tStates += 7;
                escribeE(Memoria.lee(hl));
                break;
            // LD E,A
            case 0x5F:
                tStates += 4;
                escribeE(leeA());
                break;
            // LD H,B
            case 0x60:
                tStates += 4;
                escribeH(leeB());
                break;
            // LD H,C
            case 0x61:
                tStates += 4;
                escribeH(leeC());
                break;
            // LD H,D
            case 0x62:
                tStates += 4;
                escribeH(leeD());
                break;
            // LD H,E
            case 0x63:
                tStates += 4;
                escribeH(leeE());
                break;
            // LD H,H
            case 0x64:
                tStates += 4;
                break;
            // LD H,L
            case 0x65:
                tStates += 4;
                escribeH(leeL());
                break;
            // LD H,(HL)
            case 0x66:
                tStates += 7;
                escribeH(Memoria.lee(hl));
                break;
            // LD H,A
            case 0x67:
                tStates += 4;
                escribeH(leeA());
                break;
            // LD L,B
            case 0x68:
                tStates += 4;
                escribeL(leeB());
                break;
            // LD L,C
            case 0x69:
                tStates += 4;
                escribeL(leeC());
                break;
            // LD L,D
            case 0x6A:
                tStates += 4;
                escribeL(leeD());
                break;
            // LD L,E
            case 0x6B:
                tStates += 4;
                escribeL(leeE());
                break;
            // LD L,H
            case 0x6C:
                tStates += 4;
                escribeL(leeH());
                break;
            // LD L,L
            case 0x6D:
                tStates += 4;
                break;
            // LD L,(HL)
            case 0x6E:
                tStates += 7;
                escribeL(Memoria.lee(hl));
                break;
            // LD L,A
            case 0x6F:
                tStates += 4;
                escribeL(leeA());
                break;
            // LD (HL),B
            case 0x70:
                tStates += 7;
                Memoria.escribe(hl, leeB());
                break;
            // LD (HL),C
            case 0x71:
                tStates += 7;
                Memoria.escribe(hl, leeC());
                break;
            // LD (HL),D
            case 0x72:
                tStates += 7;
                Memoria.escribe(hl, leeD());
                break;
            // LD (HL),E
            case 0x73:
                tStates += 7;
                Memoria.escribe(hl, leeE());
                break;
            // LD (HL),H
            case 0x74:
                tStates += 7;
                Memoria.escribe(hl, leeH());
                break;
            // LD (HL),L
            case 0x75:
                tStates += 7;
                Memoria.escribe(hl, leeL());
                break;
            // HALT
            case 0x76:
                tStates += 4;
                halt();
                break;
            // LD (HL),A
            case 0x77:
                tStates += 7;
                Memoria.escribe(hl, leeA());
                break;
            // LD A,B
            case 0x78:
                tStates += 4;
                escribeA(leeB());
                break;
            // LD A,C
            case 0x79:
                tStates += 4;
                escribeA(leeC());
                break;
            // LD A,D
            case 0x7A:
                tStates += 4;
                escribeA(leeD());
                break;
            // LD A,E
            case 0x7B:
                tStates += 4;
                escribeA(leeE());
                break;
            // LD A,H
            case 0x7C:
                tStates += 4;
                escribeA(leeH());
                break;
            // LD A,L
            case 0x7D:
                tStates += 4;
                escribeA(leeL());
                break;
            // LD A,(HL)
            case 0x7E:
                tStates += 7;
                escribeA(Memoria.lee(hl));
                break;
            // LD A,A
            case 0x7F:
                tStates += 4;
                break;
            // ADD A,B
            case 0x80:
                tStates += 4;
                addA(B);
                break;
            // ADD A,C
            case 0x81:
                tStates += 4;
                addA(C);
                break;
            // ADD A,D
            case 0x82:
                tStates += 4;
                addA(D);
                break;
            // ADD A,E
            case 0x83:
                tStates += 4;
                addA(E);
                break;
            // ADD A,H
            case 0x84:
                tStates += 4;
                addA(H);
                break;
            // ADD A,L
            case 0x85:
                tStates += 4;
                addA(L);
                break;
            // ADD A,(HL)
            case 0x86:
                tStates += 7;
                addA(HL);
                break;
            // ADD A,A
            case 0x87:
                tStates += 4;
                addA(A);
                break;
            // ADC A,B
            case 0x88:
                tStates += 4;
                adcA(B);
                break;
            // ADC A,C
            case 0x89:
                tStates += 4;
                adcA(C);
                break;
            // ADC A,D
            case 0x8A:
                tStates += 4;
                adcA(D);
                break;
            // ADC A,E
            case 0x8B:
                tStates += 4;
                adcA(E);
                break;
            // ADC A,H
            case 0x8C:
                tStates += 4;
                adcA(H);
                break;
            // ADC A,L
            case 0x8D:
                tStates += 4;
                adcA(L);
                break;
            // ADC A,(HL)
            case 0x8E:
                tStates += 7;
                adcA(HL);
                break;
            // ADC A,A
            case 0x8F:
                tStates += 4;
                adcA(A);
                break;
            // SUB A,B
            case 0x90:
                tStates += 4;
                subA(B);
                break;
            // SUB A,C
            case 0x91:
                tStates += 4;
                subA(C);
                break;
            // SUB A,D
            case 0x92:
                tStates += 4;
                subA(D);
                break;
            // SUB A,E
            case 0x93:
                tStates += 4;
                subA(E);
                break;
            // SUB A,H
            case 0x94:
                tStates += 4;
                subA(H);
                break;
            // SUB A,L
            case 0x95:
                tStates += 4;
                subA(L);
                break;
            // SUB A,(HL)
            case 0x96:
                tStates += 7;
                subA(HL);
                break;
            // SUB A,A
            case 0x97:
                tStates += 4;
                subA(A);
                break;
            // SBC A,B
            case 0x98:
                tStates += 4;
                sbcA(B);
                break;
            // SBC A,C
            case 0x99:
                tStates += 4;
                sbcA(C);
                break;
            // SBC A,D
            case 0x9A:
                tStates += 4;
                sbcA(D);
                break;
            // SBC A,E
            case 0x9B:
                tStates += 4;
                sbcA(E);
                break;
            // SBC A,H
            case 0x9C:
                tStates += 4;
                sbcA(H);
                break;
            // SBC A,L
            case 0x9D:
                tStates += 4;
                sbcA(L);
                break;
            // SBC A,(HL)
            case 0x9E:
                tStates += 7;
                sbcA(HL);
                break;
            // SBC A,A
            case 0x9F:
                tStates += 4;
                sbcA(A);
                break;
            // AND B
            case 0xA0:
                tStates += 4;
                and(B);
                break;
            // AND C
            case 0xA1:
                tStates += 4;
                and(C);
                break;
            // AND D
            case 0xA2:
                tStates += 4;
                and(D);
                break;
            // AND E
            case 0xA3:
                tStates += 4;
                and(E);
                break;
            // AND H
            case 0xA4:
                tStates += 4;
                and(H);
                break;
            // AND L
            case 0xA5:
                tStates += 4;
                and(L);
                break;
            // AND (HL)
            case 0xA6:
                tStates += 7;
                andHl();
                break;
            // AND A
            case 0xA7:
                tStates += 4;
                and(A);
                break;
            // XOR B
            case 0xA8:
                tStates += 4;
                xor(B);
                break;
            // XOR C
            case 0xA9:
                tStates += 4;
                xor(C);
                break;
            // XOR D
            case 0xAA:
                tStates += 4;
                xor(D);
                break;
            // XOR E
            case 0xAB:
                tStates += 4;
                xor(E);
                break;
            // XOR H
            case 0xAC:
                tStates += 4;
                xor(H);
                break;
            // XOR L
            case 0xAD:
                tStates += 4;
                xor(L);
                break;
            // XOR (HL)
            case 0xAE:
                tStates += 7;
                xorHl();
                break;
            // XOR A
            case 0xAF:
                tStates += 4;
                xor(A);
                break;
            // OR B
            case 0xB0:
                tStates += 4;
                or(B);
                break;
            // OR C
            case 0xB1:
                tStates += 4;
                or(C);
                break;
            // OR D
            case 0xB2:
                tStates += 4;
                or(D);
                break;
            // OR E
            case 0xB3:
                tStates += 4;
                or(E);
                break;
            // OR H
            case 0xB4:
                tStates += 4;
                or(H);
                break;
            // OR L
            case 0xB5:
                tStates += 4;
                or(L);
                break;
            // OR (HL)
            case 0xB6:
                tStates += 7;
                orHl();
                break;
            // OR A
            case 0xB7:
                tStates += 4;
                or(A);
                break;
            // CP B
            case 0xB8:
                tStates += 4;
                cp(B);
                break;
            // CP C
            case 0xB9:
                tStates += 4;
                cp(C);
                break;
            // CP D
            case 0xBA:
                tStates += 4;
                cp(D);
                break;
            // CP E
            case 0xBB:
                tStates += 4;
                cp(E);
                break;
            // CP H
            case 0xBC:
                tStates += 4;
                cp(H);
                break;
            // CP L
            case 0xBD:
                tStates += 4;
                cp(L);
                break;
            // CP (HL)
            case 0xBE:
                tStates += 7;
                cp(HL);
                break;
            // CP A
            case 0xBF:
                tStates += 4;
                cp(A);
                break;
            // RET NZ
            case 0xC0:
                if (!getFlagZ()) {
                    tStates += 11;
                    ret();
                } else {
                    tStates += 5;
                }
                break;
            // POP BC
            case 0xC1:
                tStates += 10;
                escribeC(Memoria.lee(sp));
                incSp();
                escribeB(Memoria.lee(sp));
                incSp();
                break;
            // JP NZ,HHLL
            case 0xC2:
                tStates += 10;
                if (!getFlagZ()) {
                    byteL = Memoria.lee(pc);
                    incPC();
                    byteH = Memoria.lee(pc);
                    incPC();
                    direccion = (byteH * 256) + byteL;
                    pc = direccion;
                } else {
                    incPC();
                    incPC();
                }
                break;
            // JP HHLL
            case 0xC3:
                tStates += 10;
                byteL = Memoria.lee(pc);
                incPC();
                byteH = Memoria.lee(pc);
                incPC();
                direccion = (byteH * 256) + byteL;
                pc = direccion;
                break;
            // CALL NZ,HHLL
            case 0xC4:
                if (!getFlagZ()) {
                    tStates += 17;
                    call();
                } else {
                    tStates += 10;
                    incPC();
                    incPC();
                }
                break;
            // PUSH BC
            case 0xC5:
                tStates += 11;
                decSp();
                Memoria.escribe(sp, leeB());
                decSp();
                Memoria.escribe(sp, leeC());
                break;
            // ADD A,NN
            case 0xC6:
                tStates += 7;
                addAnn();
                incPC();
                break;
            // RST 00
            case 0xC7:
                tStates += 11;
                rst(0x00);
                break;
            // RET Z
            case 0xC8:
                if (getFlagZ()) {
                    tStates += 11;
                    ret();
                } else {
                    tStates += 5;
                }
                break;
            // RET
            case 0xC9:
                tStates += 10;
                ret();
                break;
            // JP Z,HHLL
            case 0xCA:
                tStates += 10;
                if (getFlagZ()) {
                    byteL = Memoria.lee(pc);
                    incPC();
                    byteH = Memoria.lee(pc);
                    incPC();
                    direccion = (byteH * 256) + byteL;
                    pc = direccion;
                } else {
                    incPC();
                    incPC();
                }
                break;
            // Cb Opcodes
            case 0xCB:
                cbOpcodes();
                break;
            // CALL Z,HHLL
            case 0xCC:
                if (getFlagZ()) {
                    tStates += 17;
                    call();
                } else {
                    tStates += 10;
                    incPC();
                    incPC();
                }
                break;
            // CALL HHLL
            case 0xCD:
                tStates += 17;
                call();
                break;
            // ADC A,NN
            case 0xCE:
                tStates += 7;
                adcAnn();
                incPC();
                break;
            // RST 08
            case 0xCF:
                tStates += 11;
                rst(0x08);
                break;
            // RET NC
            case 0xD0:
                if (!getFlagC()) {
                    tStates += 11;
                    ret();
                } else {
                    tStates += 5;
                }
                break;
            // POP DE
            case 0xD1:
                tStates += 10;
                escribeE(Memoria.lee(sp));
                incSp();
                escribeD(Memoria.lee(sp));
                incSp();
                break;
            // JP NC,HHLL
            case 0xD2:
                tStates += 10;
                if (!getFlagC()) {
                    byteL = Memoria.lee(pc);
                    incPC();
                    byteH = Memoria.lee(pc);
                    incPC();
                    direccion = (byteH * 256) + byteL;
                    pc = direccion;
                } else {
                    incPC();
                    incPC();
                }
                break;
            // OUT (NN),A
            case 0xD3:
                tStates += 11;
                outNnA();
                break;
            // CALL NC,HHLL
            case 0xD4:
                if (!getFlagC()) {
                    tStates += 17;
                    call();
                } else {
                    tStates += 10;
                    incPC();
                    incPC();
                }
                break;
            // PUSH DE
            case 0xD5:
                tStates += 11;
                decSp();
                Memoria.escribe(sp, leeD());
                decSp();
                Memoria.escribe(sp, leeE());
                break;
            // SUB A,NN
            case 0xD6:
                tStates += 7;
                subAnn();
                incPC();
                break;
            // RST 10
            case 0xD7:
                tStates += 11;
                rst(0x10);
                break;
            // RET C
            case 0xD8:
                if (getFlagC()) {
                    tStates += 11;
                    ret();
                } else {
                    tStates += 5;
                }
                break;
            // EXX
            case 0xD9:
                tStates += 4;
                aux = bc;
                bc = bc_;
                bc_ = aux;
                aux = de;
                de = de_;
                de_ = aux;
                aux = hl;
                hl = hl_;
                hl_ = aux;
                break;
            // JP C,HHLL
            case 0xDA:
                tStates += 10;
                if (getFlagC()) {
                    byteL = Memoria.lee(pc);
                    incPC();
                    byteH = Memoria.lee(pc);
                    incPC();
                    direccion = (byteH * 256) + byteL;
                    pc = direccion;
                } else {
                    incPC();
                    incPC();
                }
                break;
            // IN A,(NN)
            case 0xDB:
                tStates += 11;
                inAnn();
                break;
            // CALL C,HHLL
            case 0xDC:
                if (getFlagC()) {
                    tStates += 17;
                    call();
                } else {
                    tStates += 10;
                    incPC();
                    incPC();
                }
                break;
            // DD Opcodes
            case 0xDD:
                ddOpcodes();
                break;
            // SBC A,NN
            case 0xDE:
                tStates += 7;
                sbcAnn();
                incPC();
                break;
            // RST 18
            case 0xDF:
                tStates += 11;
                rst(0x18);
                break;
            // RET PO
            case 0xE0:
                if (!getFlagPV()) {
                    tStates += 11;
                    ret();
                } else {
                    tStates = 5;
                }
                break;
            // POP HL
            case 0xE1:
                tStates += 10;
                escribeL(Memoria.lee(sp));
                incSp();
                escribeH(Memoria.lee(sp));
                incSp();
                break;
            // JP PO,HHLL
            case 0xE2:
                tStates += 10;
                if (!getFlagPV()) {
                    byteL = Memoria.lee(pc);
                    incPC();
                    byteH = Memoria.lee(pc);
                    incPC();
                    direccion = (byteH * 256) + byteL;
                    pc = direccion;
                } else {
                    incPC();
                    incPC();
                }
                break;
            // EX (SP),HL
            case 0xE3:
                tStates += 19;
                int l = leeL();
                int h = leeH();
                int p = Memoria.lee(sp);
                int s = Memoria.lee((sp + 1) & 0xffff);
                Memoria.escribe(sp, l);
                Memoria.escribe((sp + 1) & 0xffff, h);
                escribeL(p);
                escribeH(s);
                break;
            // CALL PO,HHLL
            case 0xE4:
                if (!getFlagPV()) {
                    tStates += 17;
                    call();
                } else {
                    tStates += 10;
                    incPC();
                    incPC();
                }
                break;
            // PUSH HL
            case 0xE5:
                tStates += 11;
                decSp();
                Memoria.escribe(sp, leeH());
                decSp();
                Memoria.escribe(sp, leeL());
                break;
            // AND NN
            case 0xE6:
                tStates += 7;
                andNn();
                incPC();
                break;
            // RST 20
            case 0xE7:
                tStates += 11;
                rst(0x20);
                break;
            // RET PE
            case 0xE8:
                if (getFlagPV()) {
                    tStates += 11;
                    ret();
                } else {
                    tStates += 5;
                }
                break;
            // JP (HL)
            case 0xE9:
                tStates += 4;
                pc = hl;
                break;
            // JP PE,HHLL
            case 0xEA:
                tStates += 10;
                if (getFlagPV()) {
                    byteL = Memoria.lee(pc);
                    incPC();
                    byteH = Memoria.lee(pc);
                    incPC();
                    direccion = (byteH * 256) + byteL;
                    pc = direccion;
                } else {
                    incPC();
                    incPC();
                }
                break;
            // EX DE,HL
            case 0xEB:
                tStates += 4;
                aux = hl;
                hl = de;
                de = aux;
                break;
            // CALL PE,HHLL
            case 0xEC:
                if (getFlagPV()) {
                    tStates += 17;
                    call();
                } else {
                    tStates += 10;
                    incPC();
                    incPC();
                }
                break;
            // ED Opcodes
            case 0xED:
                edOpcodes();
                break;
            // XOR NN
            case 0xEE:
                tStates += 7;
                xorNn();
                incPC();
                break;
            // RST 28
            case 0xEF:
                tStates += 11;
                rst(0x28);
                break;
            // RET P
            case 0xF0:
                if (!getFlagS()) {
                    tStates += 11;
                    ret();
                } else {
                    tStates += 5;
                }
                break;
            // POP AF
            case 0xF1:
                tStates += 10;
                escribeF(Memoria.lee(sp));
                incSp();
                escribeA(Memoria.lee(sp));
                incSp();
                break;
            // JP P,HHLL
            case 0xF2:
                tStates += 10;
                if (!getFlagS()) {
                    byteL = Memoria.lee(pc);
                    incPC();
                    byteH = Memoria.lee(pc);
                    incPC();
                    direccion = (byteH * 256) + byteL;
                    pc = direccion;
                } else {
                    incPC();
                    incPC();
                }
                break;
            // DI
            case 0xF3:
                tStates += 4;
                IFF1 = false;
                break;
            // CALL P,HHLL
            case 0xF4:
                if (!getFlagS()) {
                    tStates += 17;
                    call();
                } else {
                    tStates += 10;
                    incPC();
                    incPC();
                }
                break;
            // PUSH AF
            case 0xF5:
                tStates += 11;
                decSp();
                Memoria.escribe(sp, leeA());
                decSp();
                Memoria.escribe(sp, leeF());
                break;
            // OR NN
            case 0xF6:
                tStates += 7;
                orNn();
                incPC();
                break;
            // RST 30
            case 0xF7:
                tStates += 11;
                rst(0x30);
                break;
            // RET M
            case 0xF8:
                if (getFlagS()) {
                    tStates += 11;
                    ret();
                } else {
                    tStates += 5;
                }
                break;
            // LD SP,HL
            case 0xF9:
                tStates += 10;
                sp = hl;
                break;
            // JP M,HHLL
            case 0xFA:
                tStates += 10;
                if (getFlagS()) {
                    byteL = Memoria.lee(pc);
                    incPC();
                    byteH = Memoria.lee(pc);
                    incPC();
                    direccion = (byteH * 256) + byteL;
                    pc = direccion;
                } else {
                    incPC();
                    incPC();
                }
                break;
            // EI
            case 0xFB:
                tStates += 4;
                setIFF();
                break;
            // CALL M,HHLL
            case 0xFC:
                if (getFlagS()) {
                    tStates += 17;
                    call();
                } else {
                    tStates += 10;
                    incPC();
                    incPC();
                }
                break;
            // FD Opcodes
            case 0xFD:
                fdOpcodes();
                break;
            // CP NN
            case 0xFE:
                tStates += 7;
                cpNn();
                incPC();
                break;
            // RST 38
            case 0xFF:
                tStates += 11;
                rst(0x38);
                break;
        }
    }

    // CB Prefixed Opcodes
    public void cbOpcodes() {
        ir = Memoria.lee(pc);// Lee el contenido de la memoria FETCH T1
        incPC();// Incrementa el contador del programa T2
        incR();//Ciclos de refresco la memoria
        switch (ir) {// Decodificar T3-T4
            // RLC B
            case 0x00:
                tStates += 8;
                rlc(B);
                break;
            // RLC C
            case 0x01:
                tStates += 8;
                rlc(C);
                break;
            // RLC D
            case 0x02:
                tStates += 8;
                rlc(D);
                break;
            // RLC E
            case 0x03:
                tStates += 8;
                rlc(E);
                break;
            // RLC H
            case 0x04:
                tStates += 8;
                rlc(H);
                break;
            // RLC L
            case 0x05:
                tStates += 8;
                rlc(L);
                break;
            // RLC (HL)
            case 0x06:
                tStates += 15;
                rlc(HL);
                break;
            // RLC A
            case 0x07:
                tStates += 8;
                rlc(A);
                break;
            // RRC B
            case 0x08:
                tStates += 8;
                rrc(B);
                break;
            // RRC C
            case 0x09:
                tStates += 8;
                rrc(C);
                break;
            // RRC D
            case 0x0A:
                tStates += 8;
                rrc(D);
                break;
            // RRC E
            case 0x0B:
                tStates += 8;
                rrc(E);
                break;
            // RRC H
            case 0x0C:
                tStates += 8;
                rrc(H);
                break;
            // RRC L
            case 0x0D:
                tStates += 8;
                rrc(L);
                break;
            // RRC (HL)
            case 0x0E:
                tStates += 15;
                rrc(HL);
                break;
            // RRC A
            case 0x0F:
                tStates += 8;
                rrc(A);
                break;
            // RL B
            case 0x10:
                tStates += 8;
                rl(B);
                break;
            // RL C
            case 0x11:
                tStates += 8;
                rl(C);
                break;
            // RL D
            case 0x12:
                tStates += 8;
                rl(D);
                break;
            // RL E
            case 0x13:
                tStates += 8;
                rl(E);
                break;
            // RL H
            case 0x14:
                tStates += 8;
                rl(H);
                break;
            // RL L
            case 0x15:
                tStates += 8;
                rl(L);
                break;
            // RL (HL)
            case 0x16:
                tStates += 15;
                rl(HL);
                break;
            // RL A
            case 0x17:
                tStates += 8;
                rl(A);
                break;
            // RR B
            case 0x18:
                tStates += 8;
                rr(B);
                break;
            // RR C
            case 0x19:
                tStates += 8;
                rr(C);
                break;
            // RR D
            case 0x1A:
                tStates += 8;
                rr(D);
                break;
            // RR E
            case 0x1B:
                tStates += 8;
                rr(E);
                break;
            // RR H
            case 0x1C:
                tStates += 8;
                rr(H);
                break;
            // RR L
            case 0x1D:
                tStates += 8;
                rr(L);
                break;
            // RR (HL)
            case 0x1E:
                tStates += 15;
                rr(HL);
                break;
            // RR A
            case 0x1F:
                tStates += 8;
                rr(A);
                break;
            // SLA B
            case 0x20:
                tStates += 8;
                sla(B);
                break;
            // SLA C
            case 0x21:
                tStates += 8;
                sla(C);
                break;
            // SLA D
            case 0x22:
                tStates += 8;
                sla(D);
                break;
            // SLA E
            case 0x23:
                tStates += 8;
                sla(E);
                break;
            // SLA H
            case 0x24:
                tStates += 8;
                sla(H);
                break;
            // SLA L
            case 0x25:
                tStates += 8;
                sla(L);
                break;
            // SLA (HL)
            case 0x26:
                tStates += 15;
                sla(HL);
                break;
            // SLA A
            case 0x27:
                tStates += 8;
                sla(A);
                break;
            // SRA B
            case 0x28:
                tStates += 8;
                sra(B);
                break;
            // SRA C
            case 0x29:
                tStates += 8;
                sra(C);
                break;
            // SRA D
            case 0x2A:
                tStates += 8;
                sra(D);
                break;
            // SRA E
            case 0x2B:
                tStates += 8;
                sra(E);
                break;
            // SRA H
            case 0x2C:
                tStates += 8;
                sra(H);
                break;
            // SRA L
            case 0x2D:
                tStates += 8;
                sra(L);
                break;
            // SRA (HL)
            case 0x2E:
                tStates += 15;
                sra(HL);
                break;
            // SRA A
            case 0x2F:
                tStates += 8;
                sra(A);
                break;
            // SLL B
            case 0x30:
                tStates += 8;
                sll(B);
                break;
            // SLL C
            case 0x31:
                tStates += 8;
                sll(C);
                break;
            // SLL D
            case 0x32:
                tStates += 8;
                sll(D);
                break;
            // SLL E
            case 0x33:
                tStates += 8;
                sll(E);
                break;
            // SLL H
            case 0x34:
                tStates += 8;
                sll(H);
                break;
            // SLL L
            case 0x35:
                tStates += 8;
                sll(L);
                break;
            // SLL (HL)
            case 0x36:
                tStates += 15;
                sll(HL);
                break;
            // SLL A
            case 0x37:
                tStates += 8;
                sll(A);
                break;
            // SRL B
            case 0x38:
                tStates += 8;
                srl(B);
                break;
            // SRL C
            case 0x39:
                tStates += 8;
                srl(C);
                break;
            // SRL D
            case 0x3A:
                tStates += 8;
                srl(D);
                break;
            // SRL E
            case 0x3B:
                tStates += 8;
                srl(E);
                break;
            // SRL H
            case 0x3C:
                tStates += 8;
                srl(H);
                break;
            // SRL L
            case 0x3D:
                tStates += 8;
                srl(L);
                break;
            // SRL (HL)
            case 0x3E:
                tStates += 15;
                srl(HL);
                break;
            // SRL A
            case 0x3F:
                tStates += 8;
                srl(A);
                break;
            // BIT 0,B
            case 0x40:
                tStates += 8;
                bit(0, B);
                break;
            // BIT 0,C
            case 0x41:
                tStates += 8;
                bit(0, C);
                break;
            // BIT 0,D
            case 0x42:
                tStates += 8;
                bit(0, D);
                break;
            // BIT 0,E
            case 0x43:
                tStates += 8;
                bit(0, E);
                break;
            // BIT 0,H
            case 0x44:
                tStates += 8;
                bit(0, H);
                break;
            // BIT 0,L
            case 0x45:
                tStates += 8;
                bit(0, L);
                break;
            // BIT 0,(HL)
            case 0x46:
                tStates += 12;
                bit(0, HL);
                break;
            // BIT 0,A
            case 0x47:
                tStates += 8;
                bit(0, A);
                break;
            // BIT 1,B
            case 0x48:
                tStates += 8;
                bit(1, B);
                break;
            // BIT 1,C
            case 0x49:
                tStates += 8;
                bit(1, C);
                break;
            // BIT 1,D
            case 0x4A:
                tStates += 8;
                bit(1, D);
                break;
            // BIT 1,E
            case 0x4B:
                tStates += 8;
                bit(1, E);
                break;
            // BIT 1,H
            case 0x4C:
                tStates += 8;
                bit(1, H);
                break;
            // BIT 1,L
            case 0x4D:
                tStates += 8;
                bit(1, L);
                break;
            // BIT 1,(HL)
            case 0x4E:
                tStates += 12;
                bit(1, HL);
                break;
            // BIT 1,A
            case 0x4F:
                tStates += 8;
                bit(1, A);
                break;
            // BIT 2,B
            case 0x50:
                tStates += 8;
                bit(2, B);
                break;
            // BIT 2,C
            case 0x51:
                tStates += 8;
                bit(2, C);
                break;
            // BIT 2,D
            case 0x52:
                tStates += 8;
                bit(2, D);
                break;
            // BIT 2,E
            case 0x53:
                tStates += 8;
                bit(2, E);
                break;
            // BIT 2,H
            case 0x54:
                tStates += 8;
                bit(2, H);
                break;
            // BIT 2,L
            case 0x55:
                tStates += 8;
                bit(2, L);
                break;
            // BIT 2,(HL)
            case 0x56:
                tStates += 12;
                bit(2, HL);
                break;
            // BIT 2,A
            case 0x57:
                tStates += 8;
                bit(2, A);
                break;
            // BIT 3,B
            case 0x58:
                tStates += 8;
                bit(3, B);
                break;
            // BIT 3,C
            case 0x59:
                tStates += 8;
                bit(3, C);
                break;
            // BIT 3,D
            case 0x5A:
                tStates += 8;
                bit(3, D);
                break;
            // BIT 3,E
            case 0x5B:
                tStates += 8;
                bit(3, E);
                break;
            // BIT 3,H
            case 0x5C:
                tStates += 8;
                bit(3, H);
                break;
            // BIT 3,L
            case 0x5D:
                tStates += 8;
                bit(3, L);
                break;
            // BIT 3,(HL)
            case 0x5E:
                tStates += 12;
                bit(3, HL);
                break;
            // BIT 3,A
            case 0x5F:
                tStates += 8;
                bit(3, A);
                break;
            // BIT 4,B
            case 0x60:
                tStates += 8;
                bit(4, B);
                break;
            // BIT 4,C
            case 0x61:
                tStates += 8;
                bit(4, C);
                break;
            // BIT 4,D
            case 0x62:
                tStates += 8;
                bit(4, D);
                break;
            // BIT 4,E
            case 0x63:
                tStates += 8;
                bit(4, E);
                break;
            // BIT 4,H
            case 0x64:
                tStates += 8;
                bit(4, H);
                break;
            // BIT 4,L
            case 0x65:
                tStates += 8;
                bit(4, L);
                break;
            // BIT 4,(HL)
            case 0x66:
                tStates += 12;
                bit(4, HL);
                break;
            // BIT 4,A
            case 0x67:
                tStates += 8;
                bit(4, A);
                break;
            // BIT 5,B
            case 0x68:
                tStates += 8;
                bit(5, B);
                break;
            // BIT 5,C
            case 0x69:
                tStates += 8;
                bit(5, C);
                break;
            // BIT 5,D
            case 0x6A:
                tStates += 8;
                bit(5, D);
                break;
            // BIT 5,E
            case 0x6B:
                tStates += 8;
                bit(5, E);
                break;
            // BIT 5,H
            case 0x6C:
                tStates += 8;
                bit(5, H);
                break;
            // BIT 5,L
            case 0x6D:
                tStates += 8;
                bit(5, L);
                break;
            // BIT 5,(HL)
            case 0x6E:
                tStates += 12;
                bit(5, HL);
                break;
            // BIT 5,A
            case 0x6F:
                tStates += 8;
                bit(5, A);
                break;
            // BIT 6,B
            case 0x70:
                tStates += 8;
                bit(6, B);
                break;
            // BIT 6,C
            case 0x71:
                tStates += 8;
                bit(6, C);
                break;
            // BIT 6,D
            case 0x72:
                tStates += 8;
                bit(6, D);
                break;
            // BIT 6,E
            case 0x73:
                tStates += 8;
                bit(6, E);
                break;
            // BIT 6,H
            case 0x74:
                tStates += 8;
                bit(6, H);
                break;
            // BIT 6,L
            case 0x75:
                tStates += 8;
                bit(6, L);
                break;
            // BIT 6,(HL)
            case 0x76:
                tStates += 12;
                bit(6, HL);
                break;
            // BIT 6,A
            case 0x77:
                tStates += 8;
                bit(6, A);
                break;
            // BIT 7,B
            case 0x78:
                tStates += 8;
                bit(7, B);
                break;
            // BIT 7,C
            case 0x79:
                tStates += 8;
                bit(7, C);
                break;
            // BIT 7,D
            case 0x7A:
                tStates += 8;
                bit(7, D);
                break;
            // BIT 7,E
            case 0x7B:
                tStates += 8;
                bit(7, E);
                break;
            // BIT 7,H
            case 0x7C:
                tStates += 8;
                bit(7, H);
                break;
            // BIT 7,L
            case 0x7D:
                tStates += 8;
                bit(7, L);
                break;
            // BIT 7,(HL)
            case 0x7E:
                tStates += 12;
                bit(7, HL);
                break;
            // BIT 7,A
            case 0x7F:
                tStates += 8;
                bit(7, A);
                break;
            // RES 0,B
            case 0x80:
                tStates += 8;
                res(0, B);
                break;
            // RES 0,C
            case 0x081:
                tStates += 8;
                res(0, C);
                break;
            // RES 0,D
            case 0x82:
                tStates += 8;
                res(0, D);
                break;
            // RES 0,E
            case 0x83:
                tStates += 8;
                res(0, E);
                break;
            // RES 0,H
            case 0x84:
                tStates += 8;
                res(0, H);
                break;
            // RES 0,L
            case 0x85:
                tStates += 8;
                res(0, L);
                break;
            // RES 0,(HL)
            case 0x86:
                tStates += 15;
                res(0, HL);
                break;
            // RES 0,A
            case 0x87:
                tStates += 8;
                res(0, A);
                break;
            // RES 1,B
            case 0x88:
                tStates += 8;
                res(1, B);
                break;
            // RES 1,C
            case 0x89:
                tStates += 8;
                res(1, C);
                break;
            // RES 1,D
            case 0x8A:
                tStates += 8;
                res(1, D);
                break;
            // RES 1,E
            case 0x8B:
                tStates += 8;
                res(1, E);
                break;
            // RES 1,H
            case 0x8C:
                tStates += 8;
                res(1, H);
                break;
            // RES 1,L
            case 0x8D:
                tStates += 8;
                res(1, L);
                break;
            // RES 1,(HL)
            case 0x8E:
                tStates += 15;
                res(1, HL);
                break;
            // RES 1,A
            case 0x8F:
                tStates += 8;
                res(1, A);
                break;
            // RES 2,B
            case 0x90:
                tStates += 8;
                res(2, B);
                break;
            // RES 2,C
            case 0x91:
                tStates += 8;
                res(2, C);
                break;
            // RES 2,D
            case 0x92:
                tStates += 8;
                res(2, D);
                break;
            // RES 2,E
            case 0x93:
                tStates += 8;
                res(2, E);
                break;
            // RES 2,H
            case 0x94:
                tStates += 8;
                res(2, H);
                break;
            // RES 2,L
            case 0x95:
                tStates += 8;
                res(2, L);
                break;
            // RES 2,(HL)
            case 0x96:
                tStates += 15;
                res(2, HL);
                break;
            // RES 2,A
            case 0x97:
                tStates += 8;
                res(2, A);
                break;
            // RES 3,B
            case 0x98:
                tStates += 8;
                res(3, B);
                break;
            // RES 3,C
            case 0x99:
                tStates += 8;
                res(3, C);
                break;
            // RES 3,D
            case 0x9A:
                tStates += 8;
                res(3, D);
                break;
            // RES 3,E
            case 0x9B:
                tStates += 8;
                res(3, E);
                break;
            // RES 3,H
            case 0x9C:
                tStates += 8;
                res(3, H);
                break;
            // RES 3,L
            case 0x9D:
                tStates += 8;
                res(3, L);
                break;
            // RES 3,(HL)
            case 0x9E:
                tStates += 15;
                res(3, HL);
                break;
            // RES 3,A
            case 0x9F:
                tStates += 8;
                res(3, A);
                break;
            // RES 4,B
            case 0xA0:
                tStates += 8;
                res(4, B);
                break;
            // RES 4,C
            case 0xA1:
                tStates += 8;
                res(4, C);
                break;
            // RES 4,D
            case 0xA2:
                tStates += 8;
                res(4, D);
                break;
            // RES 4,E
            case 0xA3:
                tStates += 8;
                res(4, E);
                break;
            // RES 4,H
            case 0xA4:
                tStates += 8;
                res(4, H);
                break;
            // RES 4,L
            case 0xA5:
                tStates += 8;
                res(4, L);
                break;
            // RES 4,(HL)
            case 0xA6:
                tStates += 15;
                res(4, HL);
                break;
            // RES 4,A
            case 0xA7:
                tStates += 8;
                res(4, A);
                break;
            // RES 5,B
            case 0xA8:
                tStates += 8;
                res(5, B);
                break;
            // RES 5,C
            case 0xA9:
                tStates += 8;
                res(5, C);
                break;
            // RES 5,D
            case 0xAA:
                tStates += 8;
                res(5, D);
                break;
            // RES 5,E
            case 0xAB:
                tStates += 8;
                res(5, E);
                break;
            // RES 5,H
            case 0xAC:
                tStates += 8;
                res(5, H);
                break;
            // RES 5,L
            case 0xAD:
                tStates += 8;
                res(5, L);
                break;
            // RES 5,(HL)
            case 0xAE:
                tStates += 15;
                res(5, HL);
                break;
            // RES 5,A
            case 0xAF:
                tStates += 8;
                res(5, A);
                break;
            // RES 6,B
            case 0xB0:
                tStates += 8;
                res(6, B);
                break;
            // RES 6,C
            case 0xB1:
                tStates += 8;
                res(6, C);
                break;
            // RES 6,D
            case 0xB2:
                tStates += 8;
                res(6, D);
                break;
            // RES 6,E
            case 0xB3:
                tStates += 8;
                res(6, E);
                break;
            // RES 6,H
            case 0xB4:
                tStates += 8;
                res(6, H);
                break;
            // RES 6,L
            case 0xB5:
                tStates += 8;
                res(6, L);
                break;
            // RES 6,(HL)
            case 0xB6:
                tStates += 15;
                res(6, HL);
                break;
            // RES 6,A
            case 0xB7:
                tStates += 8;
                res(6, A);
                break;
            // RES 7,B
            case 0xB8:
                tStates += 8;
                res(7, B);
                break;
            // RES 7,C
            case 0xB9:
                tStates += 8;
                res(7, C);
                break;
            // RES 7,D
            case 0xBA:
                tStates += 8;
                res(7, D);
                break;
            // RES 7,E
            case 0xBB:
                tStates += 8;
                res(7, E);
                break;
            // RES 7,H
            case 0xBC:
                tStates += 8;
                res(7, H);
                break;
            // RES 7,L
            case 0xBD:
                tStates += 8;
                res(7, L);
                break;
            // RES 7,(HL)
            case 0xBE:
                tStates += 15;
                res(7, HL);
                break;
            // RES 7,A
            case 0xBF:
                tStates += 8;
                res(7, A);
                break;
            // SET 0,B
            case 0xC0:
                tStates += 8;
                set(0, B);
                break;
            // SET 0,C
            case 0xC1:
                tStates += 8;
                set(0, C);
                break;
            // SET 0,D
            case 0xC2:
                tStates += 8;
                set(0, D);
                break;
            // SET 0,E
            case 0xC3:
                tStates += 8;
                set(0, E);
                break;
            // SET 0,H
            case 0xC4:
                tStates += 8;
                set(0, H);
                break;
            // SET 0,L
            case 0xC5:
                tStates += 8;
                set(0, L);
                break;
            // SET 0,(HL)
            case 0xC6:
                tStates += 15;
                set(0, HL);
                break;
            // SET 0,A
            case 0xC7:
                tStates += 8;
                set(0, A);
                break;
            // SET 1,B
            case 0xC8:
                tStates += 8;
                set(1, B);
                break;
            // SET 1,C
            case 0xC9:
                tStates += 8;
                set(1, C);
                break;
            // SET 1,D
            case 0xCA:
                tStates += 8;
                set(1, D);
                break;
            // SET 1,E
            case 0xCB:
                tStates += 8;
                set(1, E);
                break;
            // SET 1,H
            case 0xCC:
                tStates += 8;
                set(1, H);
                break;
            // SET 1,L
            case 0xCD:
                tStates += 8;
                set(1, L);
                break;
            // SET 1,(HL)
            case 0xCE:
                tStates += 15;
                set(1, HL);
                break;
            // SET 1,A
            case 0xCF:
                tStates += 8;
                set(1, A);
                break;
            // SET 2,B
            case 0xD0:
                tStates += 8;
                set(2, B);
                break;
            // SET 2,C
            case 0xD1:
                tStates += 8;
                set(2, C);
                break;
            // SET 2,D
            case 0xD2:
                tStates += 8;
                set(2, D);
                break;
            // SET 2,E
            case 0xD3:
                tStates += 8;
                set(2, E);
                break;
            // SET 2,H
            case 0xD4:
                tStates += 8;
                set(2, H);
                break;
            // SET 2,L
            case 0xD5:
                tStates += 8;
                set(2, L);
                break;
            // SET 2,(HL)
            case 0xD6:
                tStates += 15;
                set(2, HL);
                break;
            // SET 2,A
            case 0xD7:
                tStates += 8;
                set(2, A);
                break;
            // SET 3,B
            case 0xD8:
                tStates += 8;
                set(3, B);
                break;
            // SET 3,C
            case 0xD9:
                tStates += 8;
                set(3, C);
                break;
            // SET 3,D
            case 0xDA:
                tStates += 8;
                set(3, D);
                break;
            // SET 3,E
            case 0xDB:
                tStates += 8;
                set(3, E);
                break;
            // SET 3,H
            case 0xDC:
                tStates += 8;
                set(3, H);
                break;
            // SET 3,L
            case 0xDD:
                tStates += 8;
                set(3, L);
                break;
            // SET 3,(HL)
            case 0xDE:
                tStates += 15;
                set(3, HL);
                break;
            // SET 3,A
            case 0xDF:
                tStates += 8;
                set(3, A);
                break;
            // SET 4,B
            case 0xE0:
                tStates += 8;
                set(4, B);
                break;
            // SET 4,C
            case 0xE1:
                tStates += 8;
                set(4, C);
                break;
            // SET 4,D
            case 0xE2:
                tStates += 8;
                set(4, D);
                break;
            // SET 4,E
            case 0xE3:
                tStates += 8;
                set(4, E);
                break;
            // SET 4,H
            case 0xE4:
                tStates += 8;
                set(4, H);
                break;
            // SET 4,L
            case 0xE5:
                tStates += 8;
                set(4, L);
                break;
            // SET 4,(HL)
            case 0xE6:
                tStates += 15;
                set(4, HL);
                break;
            // SET 4,A
            case 0xE7:
                tStates += 8;
                set(4, A);
                break;
            // SET 5,B
            case 0xE8:
                tStates += 8;
                set(5, B);
                break;
            // SET 5,C
            case 0xE9:
                tStates += 8;
                set(5, C);
                break;
            // SET 5,D
            case 0xEA:
                tStates += 8;
                set(5, D);
                break;
            // SET 5,E
            case 0xEB:
                tStates += 8;
                set(5, E);
                break;
            // SET 5,H
            case 0xEC:
                tStates += 8;
                set(5, H);
                break;
            // SET 5,L
            case 0xED:
                tStates += 8;
                set(5, L);
                break;
            // SET 5,(HL)
            case 0xEE:
                tStates += 15;
                set(5, HL);
                break;
            // SET 5,A
            case 0xEF:
                tStates += 8;
                set(5, A);
                break;
            // SET 6,B
            case 0xF0:
                tStates += 8;
                set(6, B);
                break;
            // SET 6,C
            case 0xF1:
                tStates += 8;
                set(6, C);
                break;
            // SET 6,D
            case 0xF2:
                tStates += 8;
                set(6, D);
                break;
            // SET 6,E
            case 0xF3:
                tStates += 8;
                set(6, E);
                break;
            // SET 6,H
            case 0xF4:
                tStates += 8;
                set(6, H);
                break;
            // SET 6,L
            case 0xF5:
                tStates += 8;
                set(6, L);
                break;
            // SET 6,(HL)
            case 0xF6:
                tStates += 15;
                set(6, HL);
                break;
            // SET 6,A
            case 0xF7:
                tStates += 8;
                set(6, A);
                break;
            // SET 7,B
            case 0xF8:
                tStates += 8;
                set(7, B);
                break;
            // SET 7,C
            case 0xF9:
                tStates += 8;
                set(7, C);
                break;
            // SET 7,D
            case 0xFA:
                tStates += 8;
                set(7, D);
                break;
            // SET 7,E
            case 0xFB:
                tStates += 8;
                set(7, E);
                break;
            // SET 7,H
            case 0xFC:
                tStates += 8;
                set(7, H);
                break;
            // SET 7,L
            case 0xFD:
                tStates += 8;
                set(7, L);
                break;
            // SET 7,(HL)
            case 0xFE:
                tStates += 15;
                set(7, HL);
                break;
            // SET 7,A
            case 0xFF:
                tStates += 8;
                set(7, A);
                break;
        }
    }

    // DD Prefixed Opcodes
    public void ddOpcodes() {
        ir = Memoria.lee(pc);// Lee el contenido de la memoria FETCH T1
        incPC();// Incrementa el contador del programa T2
        incR();//Ciclos de refresco la memoria
        switch (ir) {// Decodificar T3-T4
            //INC B
            case 0x04:
                tStates = 8;
                inc(B);
                break;
            //DEC B
            case 0x05:
                tStates = 8;
                dec(B);
                break;
            // LD B,NN
            case 0x06:
                tStates += 7;
                escribeB(Memoria.lee(pc));
                incPC();
                break;
            // ADD IX,BC
            case 0x09:
                tStates += 15;
                addIX(BC);
                break;
            //INC C
            case 0x0C:
                tStates = 8;
                inc(C);
                break;
            //DEC C
            case 0x0D:
                tStates = 8;
                dec(C);
                break;
            // LD C,NN
            case 0x0E:
                tStates += 7;
                escribeC(Memoria.lee(pc));
                incPC();
                break;
            //INC D
            case 0x14:
                tStates = 8;
                inc(D);
                break;
            //DEC D
            case 0x15:
                tStates = 8;
                dec(D);
                break;
            // LD D,NN
            case 0x16:
                tStates += 7;
                escribeD(Memoria.lee(pc));
                incPC();
            // ADD IX,DE
            case 0x19:
                tStates += 15;
                addIX(DE);
                break;
            //INC E
            case 0x1C:
                tStates = 8;
                inc(E);
                break;
            //DEC E
            case 0x1D:
                tStates = 8;
                dec(E);
                break;
            // LD E,NN
            case 0x1E:
                tStates += 7;
                escribeE(Memoria.lee(pc));
                incPC();
                break;
            // LD IX,HHLL
            case 0x21:
                tStates += 14;
                escribeIxL(Memoria.lee(pc));// L=Registro menor peso (7..0)
                incPC();
                escribeIxH(Memoria.lee(pc));// H=Registro mayor peso (15..8)
                incPC();
                break;
            // LD (HHLL),IX
            case 0x22:
                tStates += 20;
                int regLL = (Memoria.lee(pc));// LL=Registro menor peso (7..0)
                incPC();
                int regHH = (Memoria.lee(pc));// HH=Registro mayor peso (15..8)
                incPC();
                int direccion = (regHH * 256) + regLL;
                Memoria.escribe(direccion, leeIxL());
                Memoria.escribe((direccion + 1) & 0xffff, leeIxH());
                break;
            // INC IX
            case 0x23:
                tStates += 10;
                incIX();
                break;
            //INC IXH
            case 0x24:
                tStates += 8;
                inc(IXH);
                break;
            //DEC IXH
            case 0x25:
                tStates += 8;
                dec(IXH);
                break;
            //LD IXH,NN
            case 0x26:
                tStates += 11;
                regLL = Memoria.lee(pc);
                incPC();
                escribeIxH(regLL);
                break;
            // ADD IX,IX
            case 0x29:
                tStates += 15;
                addIX(IX);
                break;
            // LD IX,(HHLL)
            case 0x2A:
                tStates += 20;
                regLL = Memoria.lee(pc);
                incPC();
                regHH = Memoria.lee(pc);
                incPC();
                direccion = (regHH * 256) + regLL;
                escribeIxL(Memoria.lee(direccion));
                escribeIxH(Memoria.lee((direccion + 1) & 0xffff));
                break;
            // DEC IX
            case 0x2B:
                tStates += 10;
                decIX();
                break;
            //INC IXL
            case 0x2C:
                tStates += 8;
                inc(IXL);
                break;
            //DEC IXL
            case 0x2D:
                tStates += 8;
                dec(IXL);
                break;
            //LD IXL,NN
            case 0x2E:
                tStates += 11;
                regLL = Memoria.lee(pc);
                incPC();
                escribeIxL(regLL);
                break;
            // INC (IX+NN)
            case 0x34:
                tStates += 23;
                incIXd();
                break;
            // DEC (IX+NN)
            case 0x35:
                tStates += 23;
                decIXd();
                break;
            // LD (IX+NN),NN
            case 0x36:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                int dato = Memoria.lee(pc);
                Memoria.escribe((ix + (byte) desplazamiento) & 0xffff, dato);
                incPC();
                break;
            // ADD IX,SP
            case 0x39:
                tStates += 15;
                addIX(SP);
                break;
            //INC A
            case 0x3C:
                tStates = 8;
                inc(A);
                break;
            //DEC A
            case 0x3D:
                tStates = 8;
                dec(A);
                break;
            //LD A,N
            case 0x3E:
                tStates += 7;
                dato = Memoria.lee(pc);
                escribeA(dato);
                incPC();
                break;
            // LD B,B
            case 0x40:
                tStates += 8;
                break;
            // LD B,C
            case 0x41:
                tStates += 8;
                escribeB(leeC());
                break;
            // LD B,D
            case 0x42:
                tStates += 8;
                escribeB(leeD());
                break;
            // LD B,E
            case 0x43:
                tStates += 8;
                escribeB(leeE());
                break;
            //LD B,IXH
            case 0x44:
                tStates += 8;
                escribeB(leeIxH());
                break;
            //LD B,IXL
            case 0x45:
                tStates += 8;
                escribeB(leeIxL());
                break;
            // LD B,(IX+NN)
            case 0x46:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                escribeB(Memoria.lee((ix + (byte) desplazamiento) & 0xffff));
                break;
            // LD B,A
            case 0x47:
                tStates += 8;
                escribeB(leeA());
                break;
            // LD C,B
            case 0x48:
                tStates += 8;
                escribeC(leeB());
                break;
            // LD C,C
            case 0x49:
                tStates += 8;
                break;
            // LD C,D
            case 0x4A:
                tStates += 8;
                escribeC(leeD());
                break;
            // LD C,E
            case 0x4B:
                tStates += 8;
                escribeC(leeE());
                break;
            //LD C,IXH
            case 0x4C:
                tStates += 8;
                escribeC(leeIxH());
                break;
            //LD C,IXL
            case 0x4D:
                tStates += 8;
                escribeC(leeIxL());
                break;
            // LD C,(IX+NN)
            case 0x4E:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                escribeC(Memoria.lee((ix + (byte) desplazamiento) & 0xffff));
                break;
            // LD C,A
            case 0x4F:
                tStates += 8;
                escribeC(leeA());
                break;
            // LD D,B
            case 0x50:
                tStates += 8;
                escribeD(leeB());
                break;
            // LD D,C
            case 0x51:
                tStates += 8;
                escribeD(leeC());
                break;
            // LD D,D
            case 0x52:
                tStates += 8;
                break;
            // LD D,E
            case 0x53:
                tStates += 8;
                escribeD(leeE());
                break;
            //LD D,IXH
            case 0x54:
                tStates += 8;
                escribeD(leeIxH());
                break;
            //LD D,IXL
            case 0x55:
                tStates += 8;
                escribeD(leeIxH());
                break;
            // LD D,(IX+NN)
            case 0x56:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                escribeD(Memoria.lee((ix + (byte) desplazamiento) & 0xffff));
                break;
            // LD D,A
            case 0x57:
                tStates += 8;
                escribeD(leeA());
                break;
            // LD E,B
            case 0x58:
                tStates += 8;
                escribeE(leeB());
                break;
            // LD E,C
            case 0x59:
                tStates += 8;
                escribeE(leeC());
                break;
            // LD E,D
            case 0x5A:
                tStates += 8;
                escribeE(leeD());
                break;
            // LD E,E
            case 0x5B:
                tStates += 8;
                break;
            // LD E,IXH
            case 0x5C:
                tStates += 8;
                escribeE(leeIxH());
                break;
            //LD E,IXL
            case 0x5D:
                tStates += 8;
                escribeE(leeIxL());
                break;
            // LD E,(IX+NN)
            case 0x5E:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                escribeE(Memoria.lee((ix + (byte) desplazamiento) & 0xffff));
                break;
            // LD E,A
            case 0x5F:
                tStates += 8;
                escribeE(leeA());
                break;
            //LD IXH,B
            case 0x60:
                tStates += 8;
                escribeIxH(leeB());
                break;
            //LD IXH,C
            case 0x61:
                tStates += 8;
                escribeIxH(leeC());
                break;
            //LD IXH,D
            case 0x62:
                tStates += 8;
                escribeIxH(leeD());
                break;
            //LD IXH,E
            case 0x63:
                tStates += 8;
                escribeIxH(leeE());
                break;
            //LD IXH,IXH
            case 0x64:
                tStates += 8;
                break;
            //LD IXH,IXL
            case 0x65:
                tStates += 8;
                escribeIxH(leeIxL());
                break;
            // LD H,(IX+NN)
            case 0x66:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                escribeH(Memoria.lee((ix + (byte) desplazamiento) & 0xffff));
                break;
            //LD IXH,A
            case 0x67:
                tStates += 8;
                escribeIxH(leeA());
                break;
            //LD IXL,B
            case 0x68:
                tStates += 8;
                escribeIxL(leeB());
                break;
            //LD IXL,C
            case 0x69:
                tStates += 8;
                escribeIxL(leeC());
                break;
            //LD IXL,D
            case 0x6A:
                tStates += 8;
                escribeIxL(leeD());
                break;
            //LD IXL,E
            case 0x6B:
                tStates += 8;
                escribeIxL(leeE());
                break;
            //LD IXL,H
            case 0x6C:
                tStates += 8;
                escribeIxL(leeIxH());
                break;
            //LD IXL,IXL
            case 0x6D:
                tStates += 8;
                break;
            // LD L,(IX+NN)
            case 0x6E:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                escribeL(Memoria.lee((ix + (byte) desplazamiento) & 0xffff));
                break;
            //LD IXL,A  
            case 0x6F:
                tStates += 8;
                escribeIxL(leeA());
                break;
            // LD (IX+NN),B
            case 0x70:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                Memoria.escribe((ix + (byte) desplazamiento) & 0xffff, leeB());
                break;
            // LD (IX+NN),C
            case 0x71:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                Memoria.escribe((ix + (byte) desplazamiento) & 0xffff, leeC());
                break;
            // LD (IX+NN),D
            case 0x72:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                Memoria.escribe((ix + (byte) desplazamiento) & 0xffff, leeD());
                break;
            // LD (IX+NN),E
            case 0x73:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                Memoria.escribe((ix + (byte) desplazamiento) & 0xffff, leeE());
                break;
            // LD (IX+NN),H
            case 0x74:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                Memoria.escribe((ix + (byte) desplazamiento) & 0xffff, leeH());
                break;
            // LD (IX+NN),L
            case 0x75:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                Memoria.escribe((ix + (byte) desplazamiento) & 0xffff, leeL());
                break;
            // LD (IX+NN),A
            case 0x77:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                Memoria.escribe((ix + (byte) desplazamiento) & 0xffff, leeA());
                break;
            // LD A,B
            case 0x78:
                tStates += 8;
                escribeA(leeB());
                break;
            // LD A,C
            case 0x79:
                tStates += 8;
                escribeA(leeC());
                break;
            // LD A,D
            case 0x7A:
                tStates += 8;
                escribeA(leeD());
                break;
            // LD A,E
            case 0x7B:
                tStates += 8;
                escribeA(leeE());
                break;
            //LD A,IXH
            case 0x7C:
                tStates += 8;
                escribeA(leeIxH());
                break;
            //LD A,IXL
            case 0x7D:
                tStates += 8;
                escribeA(leeIxL());
                break;
            // LD A,(IX+NN)
            case 0x7E:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                escribeA(Memoria.lee((ix + (byte) desplazamiento) & 0xffff));
                break;
            // LD A,A
            case 0x7F:
                tStates += 8;
                break;
            //ADD A,B
            case 0x80:
                tStates += 8;
                addA(B);
                break;
            // ADD A,C
            case 0x81:
                tStates += 8;
                addA(C);
                break;
            //ADD A,D
            case 0x82:
                tStates += 8;
                addA(D);
                break;
            // ADD A,E
            case 0x83:
                tStates += 8;
                addA(E);
                break;
            //ADD A,IXH
            case 0x84:
                tStates += 8;
                addA(IXH);
                break;
            //ADD A,IXL
            case 0x85:
                tStates += 8;
                addA(IXL);
                break;
            // ADD A,(IX+NN)
            case 0x86:
                tStates += 19;
                addAIXd();
                break;
            //ADD A,A
            case 0x87:
                tStates += 8;
                addA(A);
                break;
            // ADC A,B
            case 0x88:
                tStates += 8;
                adcA(B);
                break;
            // ADC A,C
            case 0x89:
                tStates += 8;
                adcA(C);
                break;
            // ADC A,D
            case 0x8A:
                tStates += 8;
                adcA(D);
                break;
            // ADC A,E
            case 0x8B:
                tStates += 8;
                adcA(E);
                break;
            //ADC A,IXH
            case 0x8C:
                tStates += 8;
                adcA(IXH);
                break;
            //ADD A,IXL
            case 0x8D:
                tStates += 8;
                addA(IXL);
                break;
            // ADC A,(IX+NN)
            case 0x8E:
                tStates += 19;
                adcAIXd();
                break;
            // ADC A,A
            case 0x8F:
                tStates += 8;
                adcA(A);
                break;
            // SUB B
            case 0x90:
                tStates += 8;
                subA(B);
                break;
            // SUB C
            case 0x91:
                tStates += 8;
                subA(C);
                break;
            // SUB D
            case 0x92:
                tStates += 8;
                subA(D);
                break;
            // SUB E
            case 0x93:
                tStates += 8;
                subA(E);
                break;
            //SUB A,IXH
            case 0x94:
                tStates += 8;
                subA(IXH);
                break;
            //SUB A,IXL
            case 0x95:
                tStates += 8;
                subA(IXL);
                break;
            // SUB A,(IX+NN)
            case 0x96:
                tStates += 19;
                subAIXd();
                break;
            // SUB A
            case 0x97:
                tStates += 8;
                subA(A);
                break;
            // SBC A,B
            case 0x98:
                tStates += 8;
                sbcA(B);
                break;
            // SBC A,C
            case 0x99:
                tStates += 8;
                sbcA(C);
                break;
            // SBC A,D
            case 0x9A:
                tStates += 8;
                sbcA(D);
                break;
            // SBC A,E
            case 0x9B:
                tStates += 8;
                sbcA(E);
                break;
            //SBC A,IXH
            case 0x9C:
                tStates += 8;
                sbcA(IXH);
                break;
            //SBC A,IXL
            case 0x9D:
                tStates += 8;
                sbcA(IXL);
                break;
            // SBC A,(IX+NN)
            case 0x9E:
                tStates += 19;
                sbcAIXd();
                break;
            // SBC A,A
            case 0x9F:
                tStates += 8;
                sbcA(A);
                break;
            // AND B
            case 0xA0:
                tStates += 8;
                and(B);
                break;
            // AND C
            case 0xA1:
                tStates += 8;
                and(C);
                break;
            // AND D
            case 0xA2:
                tStates += 8;
                and(D);
                break;
            // AND E
            case 0xA3:
                tStates += 8;
                and(E);
                break;
            //AND IXH
            case 0xA4:
                tStates += 8;
                and(IXH);
                break;
            //AND IXL
            case 0xA5:
                tStates += 8;
                and(IXL);
                break;
            // AND (IX+NN)
            case 0xA6:
                tStates += 19;
                and(IX);
                break;
            // AND A
            case 0xA7:
                tStates += 8;
                and(A);
                break;
            // XOR B
            case 0xA8:
                tStates += 8;
                xor(B);
                break;
            // XOR C
            case 0xA9:
                tStates += 8;
                xor(C);
                break;
            // XOR D
            case 0xAA:
                tStates += 8;
                xor(D);
                break;
            // XOR E
            case 0xAB:
                tStates += 8;
                xor(E);
                break;
            //XOR IXH
            case 0xAC:
                tStates += 8;
                xor(IXH);
                break;
            //XOR IXL
            case 0xAD:
                tStates += 8;
                xor(IXL);
                break;
            // XOR (IX+NN)
            case 0xAE:
                tStates += 19;
                xor(IX);
                break;
            // XOR A
            case 0xAF:
                tStates += 8;
                xor(A);
                break;
            // OR B
            case 0xB0:
                tStates += 8;
                or(B);
                break;
            // OR C
            case 0xB1:
                tStates += 8;
                or(C);
                break;
            // OR D
            case 0xB2:
                tStates += 8;
                or(D);
                break;
            // OR E
            case 0xB3:
                tStates += 8;
                or(E);
                break;
            //OR IXH
            case 0xB4:
                tStates += 8;
                or(IXH);
                break;
            //OR IXL
            case 0xB5:
                tStates += 8;
                or(IXL);
                break;
            // OR (IX+NN)
            case 0xB6:
                tStates += 19;
                or(IX);
                break;
            // OR A
            case 0xB7:
                tStates += 8;
                or(A);
                break;
            // CP B
            case 0xB8:
                tStates += 8;
                cp(B);
                break;
            // CP C
            case 0xB9:
                tStates += 8;
                cp(C);
                break;
            // CP D
            case 0xBA:
                tStates += 8;
                cp(D);
                break;
            // CP E
            case 0xBB:
                tStates += 8;
                cp(E);
                break;
            //CP IXH
            case 0xBC:
                tStates += 8;
                cp(IXH);
                break;
            //OR IXL
            case 0xBD:
                tStates += 8;
                or(IXL);
                break;
            // CP (IX+NN)
            case 0xBE:
                tStates += 19;
                cp(IX);
                break;
            // CP A
            case 0xBF:
                tStates += 8;
                cp(A);
                break;
            //DDCB Opcodes
            case 0xCB:
                desp = Memoria.lee(pc);
                incPC();
                ddCbOpcodes();
                break;
            // POP IX
            case 0xE1:
                tStates += 14;
                escribeIxL(Memoria.lee(sp));
                incSp();
                escribeIxH(Memoria.lee(sp));
                incSp();
                break;
            // EX (SP),IX
            case 0xE3:
                tStates += 23;
                int ixl = leeIxL();
                int ixh = leeIxH();
                int p = Memoria.lee(sp);
                int s = Memoria.lee((sp + 1) & 0xffff);
                Memoria.escribe(sp, ixl);
                Memoria.escribe((sp + 1) & 0xffff, ixh);
                escribeIxL(p);
                escribeIxH(s);
                break;
            // PUSH IX
            case 0xE5:
                tStates += 15;
                decSp();
                Memoria.escribe(sp, leeIxH());
                decSp();
                Memoria.escribe(sp, leeIxL());
                break;
            // JP (IX)
            case 0xE9:
                tStates += 8;
                pc = ix;
                break;
            // LD SP,IX
            case 0xF9:
                tStates += 10;
                sp = ix;
                break;
            default:
                System.out.println("DD Instrucción " + Integer.toHexString(ir) + " no implementada en dirección: " + pc + "-->" + Memoria.lee(pc));
                break;
        }
    }

    // ED Prefixed Opcodes
    public void edOpcodes() {
        ir = Memoria.lee(pc);// Lee el contenido de la memoria FETCH T1
        incPC();// Incrementa el contador del programa T2
        incR();//Ciclos de refresco la memoria
        switch (ir) {// Decodificar T3-T4
            // IN B,(C)
            case 0x40:
                tStates += 12;
                inRC(B);
                break;
            // OUT (C),B
            case 0x41:
                tStates += 12;
                outCR(B);
                break;
            // SBC HL,BC
            case 0x42:
                tStates += 15;
                sbcHl(BC);
                break;
            // LD (HHLL),BC
            case 0x43:
                tStates += 20;
                ldNNDD(BC);
                break;
            // NEG
            case 0x44:
                tStates += 8;
                neg();
                break;
            // RETN
            case 0x45:
                tStates += 14;
                retN();
                break;
            // IM 0
            case 0x46:
                tStates += 8;
                setIM0();
                break;
            // LD I,A
            case 0x47:
                tStates += 9;
                ldIA();
                break;
            // IN C,(C)
            case 0x48:
                tStates += 12;
                inRC(C);
                break;
            // OUT (C),C
            case 0x49:
                tStates += 12;
                outCR(C);
                break;
            // ADC HL,BC
            case 0x4A:
                tStates += 15;
                adcHl(BC);
                break;
            // LD BC,(HHLL)
            case 0x4B:
                tStates += 20;
                ldDDNN(BC);
                break;
            //NEG
            case 0x4C:
                tStates += 8;
                neg();
                break;
            // RETI
            case 0x4D:
                tStates += 14;
                retI();
                break;
            //IM 0/1
            case 0x4E:
                tStates += 8;
                break;
            // LD R,A
            case 0x4F:
                tStates += 9;
                ldRA();
                break;
            // IN D,(C)
            case 0x50:
                tStates += 12;
                inRC(D);
                break;
            // OUT (C),D
            case 0x51:
                tStates += 12;
                outCR(D);
                break;
            // SBC HL,DE
            case 0x52:
                tStates += 15;
                sbcHl(DE);
                break;
            // LD (HHLL),DE
            case 0x53:
                tStates += 20;
                ldNNDD(DE);
                break;
            //NEG
            case 0x54:
                tStates += 8;
                neg();
                break;
            //RET N
            case 0x55:
                tStates += 14;
                retN();
                break;
            // IM 1
            case 0x56:
                tStates += 8;
                setIM1();
                break;
            // LD A,I
            case 0x57:
                tStates += 9;
                ldAI();
                break;
            // IN E,(C)
            case 0x58:
                tStates += 12;
                inRC(E);
                break;
            // OUT (C),E
            case 0x59:
                tStates += 12;
                outCR(E);
                break;
            // ADC HL,DE
            case 0x5A:
                tStates += 15;
                adcHl(DE);
                break;
            // LD DE,(HHLL)
            case 0x5B:
                tStates += 20;
                ldDDNN(DE);
                break;
            //NEG
            case 0x5C:
                tStates += 8;
                neg();
                break;
            //RET N
            case 0x5D:
                tStates += 14;
                retN();
                break;
            // IM 2
            case 0x5E:
                tStates += 8;
                setIM2();
                break;
            // LD A,R
            case 0x5F:
                tStates += 9;
                ldAR();
                break;
            // IN H,(C)
            case 0x60:
                tStates += 12;
                inRC(H);
                break;
            // OUT (C),H
            case 0x61:
                tStates += 12;
                outCR(H);
                break;
            // SBC HL,HL
            case 0x62:
                tStates += 15;
                sbcHl(HL);
                break;
            // LD (HHLL),HL
            case 0x63:
                tStates += 20;
                ldNNDD(HL);
                break;
            //NEG
            case 0x64:
                tStates += 8;
                neg();
                break;
            //RET N
            case 0x65:
                tStates += 14;
                retN();
                break;
            //IM0
            case 0x66:
                tStates += 8;
                setIM1();
                break;
            // RRD
            case 0x67:
                tStates += 18;
                rrd();
                break;
            // IN L,(C)
            case 0x68:
                tStates += 12;
                inRC(L);
                break;
            // OUT (C),L
            case 0x69:
                tStates += 12;
                outCR(L);
                break;
            // ADC HL,HL
            case 0x6A:
                tStates += 15;
                adcHl(HL);
                break;
            // LD HL,(HL)
            case 0x6B:
                tStates += 20;
                ldDDNN(HL);
                break;
            //NEG
            case 0x6C:
                tStates += 8;
                neg();
                break;
            //RET N
            case 0x6D:
                tStates += 14;
                retN();
                break;
            //IM 0/1
            case 0x6E:
                tStates += 8;
                break;
            // RLD
            case 0x6F:
                tStates += 18;
                rld();
                break;
            // IN (C)
            case 0x70:
                tStates += 12;
                escribeA0A7(leeC());// Se carga el valor del puerto byte L
                escribeA8A15(leeB());// Se carga el valor del puerto byte H
                //int reg = leeD0D7();
                int reg = (byte) -65;
                if (reg == 0) {
                    setZFlag();
                } else {
                    ceroZFlag();
                }
                if (reg > 127) {
                    setSFlag();
                } else {
                    ceroSFlag();
                }
                paridad(reg);
                ceroHFlag();
                ceroNFlag();
                break;
            //
            case 0x71:
                tStates += 12;
                break;
            // SBC HL,SP
            case 0x72:
                tStates += 15;
                sbcHl(SP);
                break;
            // LD (HHLL),SP
            case 0x73:
                tStates += 20;
                ldNNDD(SP);
                break;
            //NEG
            case 0x74:
                tStates += 8;
                neg();
                break;
            //RET N
            case 0x75:
                tStates += 14;
                retN();
                break;
            //IM1
            case 0x76:
                tStates += 8;
                setIM1();
                break;
            // IN A,(C)
            case 0x78:
                tStates += 12;
                inRC(A);
                break;
            // OUT (C),A
            case 0x79:
                tStates += 12;
                outCR(A);
                break;
            // ADC HL,SP
            case 0x7A:
                tStates += 15;
                adcHl(SP);
                break;
            // LD SP,(HHLL)
            case 0x7B:
                tStates += 20;
                ldDDNN(SP);
                break;
            //NEG
            case 0x7C:
                tStates += 8;
                neg();
                break;
            //RET N
            case 0x7D:
                tStates += 14;
                retN();
                break;
            //IM2
            case 0x7E:
                tStates += 8;
                setIM2();
                break;
            // LDI
            case 0xA0:
                tStates += 16;
                ldi();
                break;
            // CPI
            case 0xA1:
                tStates += 16;
                cpi();
                break;
            // INI
            case 0xA2:
                tStates += 16;
                ini();
                break;
            // OUTI
            case 0xA3:
                tStates += 16;
                outi();
                break;
            // LDD
            case 0xA8:
                tStates += 16;
                ldd();
                break;
            // CPD
            case 0xA9:
                tStates += 16;
                cpd();
                break;
            // IND
            case 0xAA:
                tStates += 16;
                ind();
                break;
            // OUTD
            case 0xAB:
                tStates += 16;
                outd();
                break;
            // LDIR
            case 0xB0:
                tStates += 21;
                ldir();
                break;
            // CPIR
            case 0xB1:
                tStates += 21;
                cpir();
                break;
            // INIR
            case 0xB2:
                tStates += 21;
                inir();
                break;
            // OTIR
            case 0xB3:
                tStates += 21;
                otir();
                break;
            // LDDR
            case 0xB8:
                tStates += 21;
                lddr();
                break;
            // CPDR
            case 0xB9:
                tStates += 21;
                cpdr();
                break;
            // INDR
            case 0xBA:
                tStates += 21;
                indr();
                break;
            // OTDR
            case 0xBB:
                tStates += 21;
                otdr();
                break;
            default:
                System.out.println("ED Instrucción " + Integer.toHexString(ir) + " no implementada en dirección: " + pc + "-->" + Memoria.lee(pc));
                break;
        }
    }

    // FD Prefixed Opcodes
    public void fdOpcodes() {
        ir = Memoria.lee(pc);// Lee el contenido de la memoria FETCH T1
        incPC();// Incrementa el contador del programa T2
        incR();//Ciclos de refresco la memoria
        switch (ir) {// Decodificar T3-T4
            //INC B
            case 0x04:
                tStates = 8;
                inc(B);
                break;
            //DEC B
            case 0x05:
                tStates = 8;
                dec(B);
                break;
            // LD B,NN
            case 0x06:
                tStates += 7;
                escribeB(Memoria.lee(pc));
                incPC();
                break;
            // ADD IY,BC
            case 0x09:
                tStates += 15;
                addIY(BC);
                break;
            //INC C
            case 0x0C:
                tStates = 8;
                inc(C);
                break;
            //DEC C
            case 0x0D:
                tStates = 8;
                dec(C);
                break;
            // LD C,NN
            case 0x0E:
                tStates += 7;
                escribeC(Memoria.lee(pc));
                incPC();
                break;
            //INC D
            case 0x14:
                tStates = 8;
                inc(D);
                break;
            //DEC D
            case 0x15:
                tStates = 8;
                dec(D);
                break;
            // LD D,NN
            case 0x16:
                tStates += 7;
                escribeD(Memoria.lee(pc));
                incPC();
            // ADD IY,DE
            case 0x19:
                tStates += 15;
                addIY(DE);
                break;
            //INC E
            case 0x1C:
                tStates = 8;
                inc(E);
                break;
            //DEC E
            case 0x1D:
                tStates = 8;
                dec(E);
                break;
            // LD E,NN
            case 0x1E:
                tStates += 7;
                escribeE(Memoria.lee(pc));
                incPC();
                break;
            // LD IY,HHLL
            case 0x21:
                tStates += 14;
                escribeIyL(Memoria.lee(pc));// L=Registro menor peso (7..0)
                incPC();
                escribeIyH(Memoria.lee(pc));// H=Registro mayor peso (15..8)
                incPC();
                break;
            // LD (HHLL),IY
            case 0x22:
                tStates += 20;
                int regLL = (Memoria.lee(pc));// LL=Registro menor peso (7..0)
                incPC();
                int regHH = (Memoria.lee(pc));// HH=Registro mayor peso (15..8)
                incPC();
                int direccion = (regHH * 256) + regLL;
                Memoria.escribe(direccion, leeIyL());
                Memoria.escribe((direccion + 1) & 0xffff, leeIyH());
                break;
            // INC IY
            case 0x23:
                tStates += 10;
                incIY();
                break;
            //INC IYH
            case 0x24:
                tStates += 8;
                inc(IYH);
                break;
            //DEC IYH
            case 0x25:
                tStates += 8;
                dec(IYH);
                break;
            //LD IYH,NN
            case 0x26:
                tStates += 11;
                regLL = Memoria.lee(pc);
                incPC();
                escribeIyH(regLL);
                break;
            // ADD IY,IY
            case 0x29:
                tStates += 15;
                addIY(IY);
                break;
            // LD IY,(HHLL)
            case 0x2A:
                tStates += 20;
                regLL = Memoria.lee(pc);
                incPC();
                regHH = Memoria.lee(pc);
                incPC();
                direccion = (regHH * 256) + regLL;
                escribeIyL(Memoria.lee(direccion));
                escribeIyH(Memoria.lee((direccion + 1) & 0xffff));
                break;
            // DEC IY
            case 0x2B:
                tStates += 10;
                decIY();
                break;
            //INC IYL
            case 0x2C:
                tStates += 8;
                inc(IYL);
                break;
            //DEC IYL
            case 0x2D:
                tStates += 8;
                dec(IYL);
                break;
            //LD IYL,NN
            case 0x2E:
                tStates += 11;
                regLL = Memoria.lee(pc);
                incPC();
                escribeIyL(regLL);
                break;
            // INC (IY+NN)
            case 0x34:
                tStates += 23;
                incIYd();
                break;
            // DEC (IY+NN)
            case 0x35:
                tStates += 23;
                decIYd();
                break;
            // LD (IY+NN),NN
            case 0x36:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                int dato = Memoria.lee(pc);
                Memoria.escribe((iy + (byte) desplazamiento) & 0xffff, dato);
                incPC();
                break;
            // ADD IY,SP
            case 0x39:
                tStates += 15;
                addIY(SP);
                break;
            //INC A
            case 0x3C:
                tStates = 8;
                inc(A);
                break;
            //DEC A
            case 0x3D:
                tStates = 8;
                dec(A);
                break;
            //LD A,N
            case 0x3E:
                tStates += 7;
                dato = Memoria.lee(pc);
                escribeA(dato);
                incPC();
                break;
            // LD B,B
            case 0x40:
                tStates += 8;
                break;
            // LD B,C
            case 0x41:
                tStates += 8;
                escribeB(leeC());
                break;
            // LD B,D
            case 0x42:
                tStates += 8;
                escribeB(leeD());
                break;
            // LD B,E
            case 0x43:
                tStates += 8;
                escribeB(leeE());
                break;
            //LD B,IYH
            case 0x44:
                tStates += 8;
                escribeB(leeIyH());
                break;
            //LD B,IYL
            case 0x45:
                tStates += 8;
                escribeB(leeIyL());
                break;
            // LD B,(IY+NN)
            case 0x46:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                escribeB(Memoria.lee((iy + (byte) desplazamiento) & 0xffff));
                break;
            // LD B,A
            case 0x47:
                tStates += 8;
                escribeB(leeA());
                break;
            // LD C,B
            case 0x48:
                tStates += 8;
                escribeC(leeB());
                break;
            // LD C,C
            case 0x49:
                tStates += 8;
                break;
            // LD C,D
            case 0x4A:
                tStates += 8;
                escribeC(leeD());
                break;
            // LD C,E
            case 0x4B:
                tStates += 8;
                escribeC(leeE());
                break;
            //LD C,IYH
            case 0x4C:
                tStates += 8;
                escribeC(leeIyH());
                break;
            //LD C,IYL
            case 0x4D:
                tStates += 8;
                escribeC(leeIyL());
                break;
            // LD C,(IY+NN)
            case 0x4E:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                escribeC(Memoria.lee((iy + (byte) desplazamiento) & 0xffff));
                break;
            // LD C,A
            case 0x4F:
                tStates += 8;
                escribeC(leeA());
                break;
            // LD D,B
            case 0x50:
                tStates += 8;
                escribeD(leeB());
                break;
            // LD D,C
            case 0x51:
                tStates += 8;
                escribeD(leeC());
                break;
            // LD D,D
            case 0x52:
                tStates += 8;
                break;
            // LD D,E
            case 0x53:
                tStates += 8;
                escribeD(leeE());
                break;
            //LD D,IYH
            case 0x54:
                tStates += 8;
                escribeD(leeIyH());
                break;
            //LD D,IYL
            case 0x55:
                tStates += 8;
                escribeD(leeIyH());
                break;
            // LD D,(IY+NN)
            case 0x56:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                escribeD((Memoria.lee((iy + (byte) desplazamiento) & 0xffff)));
                break;
            // LD D,A
            case 0x57:
                tStates += 8;
                escribeD(leeA());
                break;
            // LD E,B
            case 0x58:
                tStates += 8;
                escribeE(leeB());
                break;
            // LD E,C
            case 0x59:
                tStates += 8;
                escribeE(leeC());
                break;
            // LD E,D
            case 0x5A:
                tStates += 8;
                escribeE(leeD());
                break;
            // LD E,E
            case 0x5B:
                tStates += 8;
                break;
            // LD E,IYH
            case 0x5C:
                tStates += 8;
                escribeE(leeIyH());
                break;
            //LD E,IYL
            case 0x5D:
                tStates += 8;
                escribeE(leeIyL());
                break;
            // LD E,(IY+NN)
            case 0x5E:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                escribeE(Memoria.lee((iy + (byte) desplazamiento) & 0xffff));
                break;
            // LD E,A
            case 0x5F:
                tStates += 8;
                escribeE(leeA());
                break;
            //LD IYH,B
            case 0x60:
                tStates += 8;
                escribeIyH(leeB());
                break;
            //LD IYH,C
            case 0x61:
                tStates += 8;
                escribeIyH(leeC());
                break;
            //LD IYH,D
            case 0x62:
                tStates += 8;
                escribeIyH(leeD());
                break;
            //LD IYH,E
            case 0x63:
                tStates += 8;
                escribeIyH(leeE());
                break;
            //LD IYH,IYH
            case 0x64:
                tStates += 8;
                break;
            //LD IYH,IYL
            case 0x65:
                tStates += 8;
                escribeIyH(leeIyL());
                break;
            // LD H,(IY+NN)
            case 0x66:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                escribeH(Memoria.lee((iy + (byte) desplazamiento) & 0xffff));
                break;
            //LD IYH,A
            case 0x67:
                tStates += 8;
                escribeIyH(leeA());
                break;
            //LD IYL,B
            case 0x68:
                tStates += 8;
                escribeIyL(leeB());
                break;
            //LD IYL,C
            case 0x69:
                tStates += 8;
                escribeIyL(leeC());
                break;
            //LD IYL,D
            case 0x6A:
                tStates += 8;
                escribeIyL(leeD());
                break;
            //LD IYL,E
            case 0x6B:
                tStates += 8;
                escribeIyL(leeE());
                break;
            //LD IYL,H
            case 0x6C:
                tStates += 8;
                escribeIyL(leeIyH());
                break;
            //LD IXL,IXL
            case 0x6D:
                tStates += 8;
                break;
            // LD L,(IY+NN)
            case 0x6E:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                escribeL(Memoria.lee((iy + (byte) desplazamiento) & 0xffff));
                break;
            //LD IYL,A  
            case 0x6F:
                tStates += 8;
                escribeIyL(leeA());
                break;
            // LD (IY+NN),B
            case 0x70:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                Memoria.escribe((iy + (byte) desplazamiento) & 0xffff, leeB());
                break;
            // LD (IY+NN),C
            case 0x71:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                Memoria.escribe((iy + (byte) desplazamiento) & 0xffff, leeC());
                break;
            // LD (IY+NN),D
            case 0x72:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                Memoria.escribe((iy + (byte) desplazamiento) & 0xffff, leeD());
                break;
            // LD (IY+NN),E
            case 0x73:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                Memoria.escribe((iy + (byte) desplazamiento) & 0xffff, leeE());
                break;
            // LD (IY+NN),H
            case 0x74:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                Memoria.escribe((iy + (byte) desplazamiento) & 0xffff, leeH());
                break;
            // LD (IY+NN),L
            case 0x75:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                Memoria.escribe((iy + (byte) desplazamiento) & 0xffff, leeL());
                break;
            // LD (IY+NN),A
            case 0x77:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                Memoria.escribe((iy + (byte) desplazamiento) & 0xffff, leeA());
                break;
            // LD A,B
            case 0x78:
                tStates += 8;
                escribeA(leeB());
                break;
            // LD A,C
            case 0x79:
                tStates += 8;
                escribeA(leeC());
                break;
            // LD A,D
            case 0x7A:
                tStates += 8;
                escribeA(leeD());
                break;
            // LD A,E
            case 0x7B:
                tStates += 8;
                escribeA(leeE());
                break;
            //LD A,IYH
            case 0x7C:
                tStates += 8;
                escribeA(leeIyH());
                break;
            //LD A,IYL
            case 0x7D:
                tStates += 8;
                escribeA(leeIyL());
                break;
            // LD A,(IY+NN)
            case 0x7E:
                tStates += 19;
                desplazamiento = Memoria.lee(pc);
                incPC();
                escribeA(Memoria.lee((iy + (byte) desplazamiento) & 0xffff));
                break;
            // LD A,A
            case 0x7F:
                tStates += 8;
                break;
            //ADD A,B
            case 0x80:
                tStates += 8;
                addA(B);
                break;
            // ADD A,C
            case 0x81:
                tStates += 8;
                addA(C);
                break;
            //ADD A,D
            case 0x82:
                tStates += 8;
                addA(D);
                break;
            // ADD A,E
            case 0x83:
                tStates += 8;
                addA(E);
                break;
            //ADD A,IYH
            case 0x84:
                tStates += 8;
                addA(IYH);
                break;
            //ADD A,IYL
            case 0x85:
                tStates += 8;
                addA(IYL);
                break;
            // ADD A,(IY+NN)
            case 0x86:
                tStates += 19;
                addAIYd();
                break;
            //ADD A,A
            case 0x87:
                tStates += 8;
                addA(A);
                break;
            // ADC A,B
            case 0x88:
                tStates += 8;
                adcA(B);
                break;
            // ADC A,C
            case 0x89:
                tStates += 8;
                adcA(C);
                break;
            // ADC A,D
            case 0x8A:
                tStates += 8;
                adcA(D);
                break;
            // ADC A,E
            case 0x8B:
                tStates += 8;
                adcA(E);
                break;
            //ADC A,IYH
            case 0x8C:
                tStates += 8;
                adcA(IYH);
                break;
            //ADD A,IYL
            case 0x8D:
                tStates += 8;
                addA(IYL);
                break;
            // ADC A,(IY+NN)
            case 0x8E:
                tStates += 19;
                adcAIYd();
                break;
            // ADC A,A
            case 0x8F:
                tStates += 8;
                adcA(A);
                break;
            // SUB B
            case 0x90:
                tStates += 8;
                subA(B);
                break;
            // SUB C
            case 0x91:
                tStates += 8;
                subA(C);
                break;
            // SUB D
            case 0x92:
                tStates += 8;
                subA(D);
                break;
            // SUB E
            case 0x93:
                tStates += 8;
                subA(E);
                break;
            //SUB A,IYH
            case 0x94:
                tStates += 8;
                subA(IYH);
                break;
            //SUB A,IYL
            case 0x95:
                tStates += 8;
                subA(IYL);
                break;
            // SUB A,(IY+NN)
            case 0x96:
                tStates += 19;
                subAIYd();
                break;
            // SUB A
            case 0x97:
                tStates += 8;
                subA(A);
                break;
            // SBC A,B
            case 0x98:
                tStates += 8;
                sbcA(B);
                break;
            // SBC A,C
            case 0x99:
                tStates += 8;
                sbcA(C);
                break;
            // SBC A,D
            case 0x9A:
                tStates += 8;
                sbcA(D);
                break;
            // SBC A,E
            case 0x9B:
                tStates += 8;
                sbcA(E);
                break;
            //SBC A,IYH
            case 0x9C:
                tStates += 8;
                sbcA(IYH);
                break;
            //SBC A,IYL
            case 0x9D:
                tStates += 8;
                sbcA(IYL);
                break;
            // SBC A,(IY+NN)
            case 0x9E:
                tStates += 19;
                sbcAIYd();
                break;
            // SBC A,A
            case 0x9F:
                tStates += 8;
                sbcA(A);
                break;
            // AND B
            case 0xA0:
                tStates += 8;
                and(B);
                break;
            // AND C
            case 0xA1:
                tStates += 8;
                and(C);
                break;
            // AND D
            case 0xA2:
                tStates += 8;
                and(D);
                break;
            // AND E
            case 0xA3:
                tStates += 8;
                and(E);
                break;
            //AND IYH
            case 0xA4:
                tStates += 8;
                and(IYH);
                break;
            //AND IYL
            case 0xA5:
                tStates += 8;
                and(IYL);
                break;
            // AND (IY+NN)
            case 0xA6:
                tStates += 19;
                and(IY);
                break;
            // AND A
            case 0xA7:
                tStates += 8;
                and(A);
                break;
            // XOR B
            case 0xA8:
                tStates += 8;
                xor(B);
                break;
            // XOR C
            case 0xA9:
                tStates += 8;
                xor(C);
                break;
            // XOR D
            case 0xAA:
                tStates += 8;
                xor(D);
                break;
            // XOR E
            case 0xAB:
                tStates += 8;
                xor(E);
                break;
            //XOR IYH
            case 0xAC:
                tStates += 8;
                xor(IYH);
                break;
            //XOR IYL
            case 0xAD:
                tStates += 8;
                xor(IYL);
                break;
            // XOR (IY+NN)
            case 0xAE:
                tStates += 19;
                xor(IY);
                break;
            // XOR A
            case 0xAF:
                tStates += 8;
                xor(A);
                break;
            // OR B
            case 0xB0:
                tStates += 8;
                or(B);
                break;
            // OR C
            case 0xB1:
                tStates += 8;
                or(C);
                break;
            // OR D
            case 0xB2:
                tStates += 8;
                or(D);
                break;
            // OR E
            case 0xB3:
                tStates += 8;
                or(E);
                break;
            //OR IYH
            case 0xB4:
                tStates += 8;
                or(IYH);
                break;
            //OR IYL
            case 0xB5:
                tStates += 8;
                or(IYL);
                break;
            // OR (IY+NN)
            case 0xB6:
                tStates += 19;
                or(IY);
                break;
            // OR A
            case 0xB7:
                tStates += 8;
                or(A);
                break;
            // CP B
            case 0xB8:
                tStates += 8;
                cp(B);
                break;
            // CP C
            case 0xB9:
                tStates += 8;
                cp(C);
                break;
            // CP D
            case 0xBA:
                tStates += 8;
                cp(D);
                break;
            // CP E
            case 0xBB:
                tStates += 8;
                cp(E);
                break;
            //CP IYH
            case 0xBC:
                tStates += 8;
                cp(IYH);
                break;
            //OR IYL
            case 0xBD:
                tStates += 8;
                or(IYL);
                break;
            // CP (IY+NN)
            case 0xBE:
                tStates += 19;
                cp(IY);
                break;
            // CP A
            case 0xBF:
                tStates += 8;
                cp(A);
                break;
            //FDCB Opcodes
            case 0xCB:
                desp = Memoria.lee(pc);
                incPC();
                fdCbOpcodes();
                break;
            // POP IY
            case 0xE1:
                tStates += 14;
                escribeIyL(Memoria.lee(sp));
                incSp();
                escribeIyH(Memoria.lee(sp));
                incSp();
                break;
            // EX (SP),IY
            case 0xE3:
                tStates += 23;
                int iyl = leeIyL();
                int iyh = leeIyH();
                int p = Memoria.lee(sp);
                int s = Memoria.lee((sp + 1) & 0xffff);
                Memoria.escribe(sp, iyl);
                Memoria.escribe((sp + 1) & 0xffff, iyh);
                escribeIyL(p);
                escribeIyH(s);
                break;
            // PUSH IY
            case 0xE5:
                tStates += 15;
                decSp();
                Memoria.escribe(sp, leeIyH());
                decSp();
                Memoria.escribe(sp, leeIyL());
                break;
            // JP (IY)
            case 0xE9:
                tStates += 8;
                pc = iy;
                break;
            // LD SP,IY
            case 0xF9:
                tStates += 10;
                sp = iy;
                break;
            default:
                System.out.println("FD Instrucción " + Integer.toHexString(ir) + " no implementada en dirección: " + pc + "-->" + Memoria.lee(pc));
                break;
        }
    }

    // DDCB Prefixed Opcodes
    public void ddCbOpcodes() {
        ir = Memoria.lee(pc);// Lee el contenido de la memoria FETCH T1
        incPC();// Incrementa el contador del programa T2
        incR();//Ciclos de refresco la memoria
        switch (ir) {// Decodificar T3-T4
            //rlc (ix+*)
            case 0x06:
                tStates += 23;
                rlc(IX_D);
                break;
            //rl (ix+*)
            case 0x16:
                tStates += 23;
                rl(IX_D);
                break;
            //sla (ix+*)
            case 0x26:
                tStates += 23;
                sla(IX_D);
                break;
            //sll (ix+*)
            case 0x36:
                tStates += 23;
                sll(IX_D);
                break;
            //bit 0,(ix+*)
            case 0x40:
                tStates += 20;
                bit(0, IX_D);
                break;
            //bit 2,(ix+*)	
            case 0x50:
                tStates += 20;
                bit(2, IX_D);
                break;
            //bit 4,(ix+*)	
            case 0x60:
                tStates += 20;
                bit(4, IX_D);
                break;
            //	bit 6,(ix+*)	
            case 0x70:
                tStates += 20;
                bit(6, IX_D);
                break;
            //bit 0,(ix+*)
            case 0x41:
                tStates += 20;
                bit(0, IX_D);
                break;
            //bit 2,(ix+*)	
            case 0x51:
                tStates += 20;
                bit(2, IX_D);
                break;
            //bit 4,(ix+*)	
            case 0x61:
                tStates += 20;
                bit(4, IX_D);
                break;
            //	bit 6,(ix+*)	
            case 0x71:
                tStates += 20;
                bit(6, IX_D);
                break;
            //bit 0,(ix+*)
            case 0x42:
                tStates += 20;
                bit(0, IX_D);
                break;
            //bit 2,(ix+*)	
            case 0x52:
                tStates += 20;
                bit(2, IX_D);
                break;
            //bit 4,(ix+*)	
            case 0x62:
                tStates += 20;
                bit(4, IX_D);
                break;
            //	bit 6,(ix+*)	
            case 0x72:
                tStates += 20;
                bit(6, IX_D);
                break;
            //bit 0,(ix+*)
            case 0x43:
                tStates += 20;
                bit(0, IX_D);
                break;
            //bit 2,(ix+*)	
            case 0x53:
                tStates += 20;
                bit(2, IX_D);
                break;
            //bit 4,(ix+*)	
            case 0x63:
                tStates += 20;
                bit(4, IX_D);
                break;
            //	bit 6,(ix+*)	
            case 0x73:
                tStates += 20;
                bit(6, IX_D);
                break;
            //bit 0,(ix+*)
            case 0x44:
                tStates += 20;
                bit(0, IX_D);
                break;
            //bit 2,(ix+*)	
            case 0x54:
                tStates += 20;
                bit(2, IX_D);
                break;
            //bit 4,(ix+*)	
            case 0x64:
                tStates += 20;
                bit(4, IX_D);
                break;
            //	bit 6,(ix+*)	
            case 0x74:
                tStates += 20;
                bit(6, IX_D);
                break;
            //bit 0,(ix+*)
            case 0x45:
                tStates += 20;
                bit(0, IX_D);
                break;
            //bit 2,(ix+*)	
            case 0x55:
                tStates += 20;
                bit(2, IX_D);
                break;
            //bit 4,(ix+*)	
            case 0x65:
                tStates += 20;
                bit(4, IX_D);
                break;
            //	bit 6,(ix+*)	
            case 0x75:
                tStates += 20;
                bit(6, IX_D);
                break;
            //bit 0,(ix+*)
            case 0x47:
                tStates += 20;
                bit(0, IX_D);
                break;
            //bit 2,(ix+*)	
            case 0x57:
                tStates += 20;
                bit(2, IX_D);
                break;
            //bit 4,(ix+*)	
            case 0x67:
                tStates += 20;
                bit(4, IX_D);
                break;
            //	bit 6,(ix+*)	
            case 0x77:
                tStates += 20;
                bit(6, IX_D);
                break;
            //bit 1,(ix+*)	
            case 0x48:
                tStates += 20;
                bit(1, IX_D);
                break;
            //bit 3,(ix+*)	
            case 0x58:
                tStates += 20;
                bit(3, IX_D);
                break;
            //bit 5,(ix+*)	
            case 0x68:
                tStates += 20;
                bit(5, IX_D);
                break;
            //bit 7,(ix+*)	
            case 0x78:
                tStates += 20;
                bit(7, IX_D);
                break;
            //bit 1,(ix+*)	
            case 0x49:
                tStates += 20;
                bit(1, IX_D);
                break;
            //bit 3,(ix+*)	
            case 0x59:
                tStates += 20;
                bit(3, IX_D);
                break;
            //bit 5,(ix+*)	
            case 0x69:
                tStates += 20;
                bit(5, IX_D);
                break;
            //bit 7,(ix+*)	
            case 0x79:
                tStates += 20;
                bit(7, IX_D);
                break;
            //bit 1,(ix+*)	
            case 0x4A:
                tStates += 20;
                bit(1, IX_D);
                break;
            //bit 3,(ix+*)	
            case 0x5A:
                tStates += 20;
                bit(3, IX_D);
                break;
            //bit 5,(ix+*)	
            case 0x6A:
                tStates += 20;
                bit(5, IX_D);
                break;
            //bit 7,(ix+*)	
            case 0x7A:
                tStates += 20;
                bit(7, IX_D);
                break;
            //bit 1,(ix+*)	
            case 0x4B:
                tStates += 20;
                bit(1, IX_D);
                break;
            //bit 3,(ix+*)	
            case 0x5B:
                tStates += 20;
                bit(3, IX_D);
                break;
            //bit 5,(ix+*)	
            case 0x6B:
                tStates += 20;
                bit(5, IX_D);
                break;
            //bit 7,(ix+*)	
            case 0x7B:
                tStates += 20;
                bit(7, IX_D);
                break;
            //bit 1,(ix+*)	
            case 0x4C:
                tStates += 20;
                bit(1, IX_D);
                break;
            //bit 3,(ix+*)	
            case 0x5C:
                tStates += 20;
                bit(3, IX_D);
                break;
            //bit 5,(ix+*)	
            case 0x6C:
                tStates += 20;
                bit(5, IX_D);
                break;
            //bit 7,(ix+*)	
            case 0x7C:
                tStates += 20;
                bit(7, IX_D);
                break;
            //bit 1,(ix+*)	
            case 0x4D:
                tStates += 20;
                bit(1, IX_D);
                break;
            //bit 3,(ix+*)	
            case 0x5D:
                tStates += 20;
                bit(3, IX_D);
                break;
            //bit 5,(ix+*)	
            case 0x6D:
                tStates += 20;
                bit(5, IX_D);
                break;
            //bit 7,(ix+*)	
            case 0x7D:
                tStates += 20;
                bit(7, IX_D);
                break;
            //bit 1,(ix+*)	
            case 0x4F:
                tStates += 20;
                bit(1, IX_D);
                break;
            //bit 3,(ix+*)	
            case 0x5F:
                tStates += 20;
                bit(3, IX_D);
                break;
            //bit 5,(ix+*)	
            case 0x6F:
                tStates += 20;
                bit(5, IX_D);
                break;
            //bit 7,(ix+*)	
            case 0x7F:
                tStates += 20;
                bit(7, IX_D);
                break;
            //bit 0,(ix+*)
            case 0x46:
                tStates += 20;
                bit(0, IX_D);
                break;
            //bit 2,(ix+*)	
            case 0x56:
                tStates += 20;
                bit(2, IX_D);
                break;
            //bit 4,(ix+*)	
            case 0x66:
                tStates += 20;
                bit(4, IX_D);
                break;
            //	bit 6,(ix+*)	
            case 0x76:
                tStates += 20;
                bit(6, IX_D);
                break;
            //	res 0,(ix+*)	
            case 0x86:
                tStates += 23;
                res(0, IX_D);
                break;
            //res 2,(ix+*)	
            case 0x96:
                tStates += 23;
                res(2, IX_D);
                break;
            //res 4,(ix+*)	
            case 0xA6:
                tStates += 23;
                res(4, IX_D);
                break;
            //res 6,(ix+*)	
            case 0xB6:
                tStates += 23;
                res(6, IX_D);
                break;
            //set 0,(ix+*)	
            case 0xC6:
                tStates += 23;
                set(0, IX_D);
                break;
            //set 2,(ix+*)	
            case 0xD6:
                tStates += 23;
                set(2, IX_D);
                break;
            //set 4,(ix+*)	
            case 0xE6:
                tStates += 23;
                set(4, IX_D);
                break;
            //set 6,(ix+*)	
            case 0xF6:
                tStates += 23;
                set(6, IX_D);
                break;
            case 0x0E:
                tStates += 23;
                rrc(IX_D);
                break;
            //rr (ix+*)	
            case 0x1E:
                tStates += 23;
                rr(IX_D);
                break;
            //sra (ix+*)	
            case 0x2E:
                tStates += 23;
                sra(IX_D);
                break;
            //srl (ix+*)	
            case 0x3E:
                tStates += 23;
                srl(IX_D);
                break;
            //bit 1,(ix+*)	
            case 0x4E:
                tStates += 20;
                bit(1, IX_D);
                break;
            //bit 3,(ix+*)	
            case 0x5E:
                tStates += 20;
                bit(3, IX_D);
                break;
            //bit 5,(ix+*)	
            case 0x6E:
                tStates += 20;
                bit(5, IX_D);
                break;
            //bit 7,(ix+*)	
            case 0x7E:
                tStates += 20;
                bit(7, IX_D);
                break;
            //res 1,(ix+*)	
            case 0x8E:
                tStates += 23;
                res(1, IX_D);
                break;
            //res 3,(ix+*)	
            case 0x9E:
                tStates += 23;
                res(3, IX_D);
                break;
            //res 5,(ix+*)	
            case 0xAE:
                tStates += 23;
                res(5, IX_D);
                break;
            //res 7,(ix+*)	
            case 0xBE:
                tStates += 23;
                res(7, IX_D);
                break;
            //set 1,(ix+*)	
            case 0xCE:
                tStates += 23;
                set(1, IX_D);
                break;
            //set 3,(ix+*)	
            case 0xDE:
                tStates += 23;
                set(3, IX_D);
                break;
            //set 5,(ix+*)	
            case 0xEE:
                tStates += 23;
                set(5, IX_D);
                break;
            //set 7,(ix+*)	
            case 0xFE:
                tStates += 23;
                set(7, IX_D);
                break;
            default:
                System.out.println("DD CB Instrucción " + Integer.toHexString(ir) + " no implementada en dirección: " + pc + "-->" + Memoria.lee(pc));
                tStates += 8;
                break;
        }
    }

    // fdCB Prefixed Opcodes
    public void fdCbOpcodes() {
        ir = Memoria.lee(pc);// Lee el contenido de la memoria FETCH T1
        incPC();//Incrementa el contador del programa T2
        incR();//Ciclos de refresco la memoria
        switch (ir) {// Decodificar T3-T4
            //bit 0,(iy+*)
            case 0x40:
                tStates += 20;
                bit(0, IY_D);
                break;
            //bit 2,(iy+*)	
            case 0x50:
                tStates += 20;
                bit(2, IY_D);
                break;
            //bit 4,(iy+*)	
            case 0x60:
                tStates += 20;
                bit(4, IY_D);
                break;
            //	bit 6,(iY+*)	
            case 0x70:
                tStates += 20;
                bit(6, IY_D);
                break;
            //bit 0,(iy+*)
            case 0x41:
                tStates += 20;
                bit(0, IY_D);
                break;
            //bit 2,(iy+*)	
            case 0x51:
                tStates += 20;
                bit(2, IY_D);
                break;
            //bit 4,(iy+*)	
            case 0x61:
                tStates += 20;
                bit(4, IY_D);
                break;
            //	bit 6,(iy+*)	
            case 0x71:
                tStates += 20;
                bit(6, IY_D);
                break;
            //bit 0,(iy+*)
            case 0x42:
                tStates += 20;
                bit(0, IY_D);
                break;
            //bit 2,(iy+*)	
            case 0x52:
                tStates += 20;
                bit(2, IY_D);
                break;
            //bit 4,(iy+*)	
            case 0x62:
                tStates += 20;
                bit(4, IY_D);
                break;
            //	bit 6,(iy+*)	
            case 0x72:
                tStates += 20;
                bit(6, IY_D);
                break;
            //bit 0,(iy+*)
            case 0x43:
                tStates += 20;
                bit(0, IY_D);
                break;
            //bit 2,(iy+*)	
            case 0x53:
                tStates += 20;
                bit(2, IY_D);
                break;
            //bit 4,(iy+*)	
            case 0x63:
                tStates += 20;
                bit(4, IY_D);
                break;
            //	bit 6,(iy+*)	
            case 0x73:
                tStates += 20;
                bit(6, IY_D);
                break;
            //bit 0,(iy+*)
            case 0x44:
                tStates += 20;
                bit(0, IY_D);
                break;
            //bit 2,(iy+*)	
            case 0x54:
                tStates += 20;
                bit(2, IY_D);
                break;
            //bit 4,(iy+*)	
            case 0x64:
                tStates += 20;
                bit(4, IY_D);
                break;
            //	bit 6,(iy+*)	
            case 0x74:
                tStates += 20;
                bit(6, IY_D);
                break;
            //bit 0,(iy+*)
            case 0x45:
                tStates += 20;
                bit(0, IY_D);
                break;
            //bit 2,(iy+*)	
            case 0x55:
                tStates += 20;
                bit(2, IY_D);
                break;
            //bit 4,(iy+*)	
            case 0x65:
                tStates += 20;
                bit(4, IY_D);
                break;
            //	bit 6,(iy+*)	
            case 0x75:
                tStates += 20;
                bit(6, IY_D);
                break;
            //bit 0,(iy+*)
            case 0x47:
                tStates += 20;
                bit(0, IY_D);
                break;
            //bit 2,(iy+*)	
            case 0x57:
                tStates += 20;
                bit(2, IY_D);
                break;
            //bit 4,(iy+*)	
            case 0x67:
                tStates += 20;
                bit(4, IY_D);
                break;
            //	bit 6,(iy+*)	
            case 0x77:
                tStates += 20;
                bit(6, IY_D);
                break;
            //bit 1,(iy+*)	
            case 0x48:
                tStates += 20;
                bit(1, IY_D);
                break;
            //bit 3,(iy+*)	
            case 0x58:
                tStates += 20;
                bit(3, IY_D);
                break;
            //bit 5,(iy+*)	
            case 0x68:
                tStates += 20;
                bit(5, IY_D);
                break;
            //bit 7,(iy+*)	
            case 0x78:
                tStates += 20;
                bit(7, IY_D);
                break;
            //bit 1,(iy+*)	
            case 0x49:
                tStates += 20;
                bit(1, IY_D);
                break;
            //bit 3,(iy+*)	
            case 0x59:
                tStates += 20;
                bit(3, IY_D);
                break;
            //bit 5,(iy+*)	
            case 0x69:
                tStates += 20;
                bit(5, IY_D);
                break;
            //bit 7,(iy+*)	
            case 0x79:
                tStates += 20;
                bit(7, IY_D);
                break;
            //bit 1,(iy+*)	
            case 0x4A:
                tStates += 20;
                bit(1, IY_D);
                break;
            //bit 3,(iy+*)	
            case 0x5A:
                tStates += 20;
                bit(3, IY_D);
                break;
            //bit 5,(iy+*)	
            case 0x6A:
                tStates += 20;
                bit(5, IY_D);
                break;
            //bit 7,(iy+*)	
            case 0x7A:
                tStates += 20;
                bit(7, IY_D);
                break;
            //bit 1,(iy+*)	
            case 0x4B:
                tStates += 20;
                bit(1, IY_D);
                break;
            //bit 3,(iy+*)	
            case 0x5B:
                tStates += 20;
                bit(3, IY_D);
                break;
            //bit 5,(iy+*)	
            case 0x6B:
                tStates += 20;
                bit(5, IY_D);
                break;
            //bit 7,(iy+*)	
            case 0x7B:
                tStates += 20;
                bit(7, IY_D);
                break;
            //bit 1,(iy+*)	
            case 0x4C:
                tStates += 20;
                bit(1, IY_D);
                break;
            //bit 3,(iy+*)	
            case 0x5C:
                tStates += 20;
                bit(3, IY_D);
                break;
            //bit 5,(iy+*)	
            case 0x6C:
                tStates += 20;
                bit(5, IY_D);
                break;
            //bit 7,(iy+*)	
            case 0x7C:
                tStates += 20;
                bit(7, IY_D);
                break;
            //bit 1,(iy+*)	
            case 0x4D:
                tStates += 20;
                bit(1, IY_D);
                break;
            //bit 3,(iy+*)	
            case 0x5D:
                tStates += 20;
                bit(3, IY_D);
                break;
            //bit 5,(iy+*)	
            case 0x6D:
                tStates += 20;
                bit(5, IY_D);
                break;
            //bit 7,(iy+*)	
            case 0x7D:
                tStates += 20;
                bit(7, IY_D);
                break;
            //bit 1,(iy+*)	
            case 0x4F:
                tStates += 20;
                bit(1, IY_D);
                break;
            //bit 3,(iy+*)	
            case 0x5F:
                tStates += 20;
                bit(3, IY_D);
                break;
            //bit 5,(iy+*)	
            case 0x6F:
                tStates += 20;
                bit(5, IY_D);
                break;
            //bit 7,(iy+*)	
            case 0x7F:
                tStates += 20;
                bit(7, IY_D);
                break;
            //rlc (iy+*)
            case 0x06:
                tStates += 23;
                rlc(IY_D);
                break;
            //rl (iy+*)
            case 0x16:
                tStates += 23;
                rl(IY_D);
                break;
            //sla (iy+*)
            case 0x26:
                tStates += 23;
                sla(IY_D);
                break;
            //sll (iy+*)
            case 0x36:
                tStates += 23;
                sll(IY_D);
                break;
            //bit 0,(iy+*)
            case 0x46:
                tStates += 20;
                bit(0, IY_D);
                break;
            //bit 2,(iy+*)	
            case 0x56:
                tStates += 20;
                bit(2, IY_D);
                break;
            //bit 4,(iy+*)	
            case 0x66:
                tStates += 20;
                bit(4, IY_D);
                break;
            //	bit 6,(iy+*)	
            case 0x76:
                tStates += 20;
                bit(6, IY_D);
                break;
            //	res 0,(iy+*)	
            case 0x86:
                tStates += 23;
                res(0, IY_D);
                break;
            //res 2,(iy+*)	
            case 0x96:
                tStates += 23;
                res(2, IY_D);
                break;
            //res 4,(iy+*)	
            case 0xA6:
                tStates += 23;
                res(4, IY_D);
                break;
            //res 6,(iy+*)	
            case 0xB6:
                tStates += 23;
                res(6, IY_D);
                break;
            //set 0,(iy+*)	
            case 0xC6:
                tStates += 23;
                set(0, IY_D);
                break;
            //set 2,(iy+*)	
            case 0xD6:
                tStates += 23;
                set(2, IY_D);
                break;
            //set 4,(iy+*)	
            case 0xE6:
                tStates += 23;
                set(4, IY_D);
                break;
            //set 6,(iy+*)	
            case 0xF6:
                tStates += 23;
                set(6, IY_D);
                break;
            //rrc (iy+*)	
            case 0x0E:
                tStates += 23;
                rrc(IY_D);
                break;
            //rr (iy+*)	
            case 0x1E:
                tStates += 23;
                rr(IY_D);
                break;
            //sra (iy+*)	
            case 0x2E:
                tStates += 23;
                sra(IY_D);
                break;
            //srl (iy+*)	
            case 0x3E:
                tStates += 23;
                srl(IY_D);
                break;
            //bit 1,(iy+*)	
            case 0x4E:
                tStates += 20;
                bit(1, IY_D);
                break;
            //bit 3,(iy+*)	
            case 0x5E:
                tStates += 20;
                bit(3, IY_D);
                break;
            //bit 5,(iy+*)	
            case 0x6E:
                tStates += 20;
                bit(5, IY_D);
                break;
            //bit 7,(iy+*)	
            case 0x7E:
                tStates += 20;
                bit(7, IY_D);
                break;
            //res 1,(iy+*)	
            case 0x8E:
                tStates += 23;
                res(1, IY_D);
                break;
            //res 3,(iy+*)	
            case 0x9E:
                tStates += 23;
                res(3, IY_D);
                break;
            //res 5,(iy+*)	
            case 0xAE:
                tStates += 23;
                res(5, IY_D);
                break;
            //res 7,(iy+*)	
            case 0xBE:
                tStates += 23;
                res(7, IY_D);
                break;
            //set 1,(iy+*)	
            case 0xCE:
                tStates += 23;
                set(1, IY_D);
                break;
            //set 3,(iy+*)	
            case 0xDE:
                tStates += 23;
                set(3, IY_D);
                break;
            //set 5,(iy+*)	
            case 0xEE:
                tStates += 23;
                set(5, IY_D);
                break;
            //set 7,(iy+*)	
            case 0xFE:
                tStates += 23;
                set(7, IY_D);
                break;
            default:
                System.out.println("FD CB Instrucción " + Integer.toHexString(ir) + " no implementada en dirección: " + pc + "-->" + Memoria.lee(pc));
                tStates += 8;
                break;
        }
    }

    //Retorna los registros en decimal
    public String regDec() {
        return "A=" + leeA() + "\tF=" + leeF() + "\tAF=" + af + "\tAF'=" + af_
                + "\nB=" + leeB() + "\tC=" + leeC() + "\tBC=" + bc + "\tBC'=" + bc_
                + "\nD=" + leeD() + "\tE=" + leeE() + "\tDE=" + de + "\tDE'=" + de_
                + "\nH=" + leeH() + "\tL=" + leeL() + "\tHL=" + hl + "\tHL'=" + hl_
                + "\nIX=" + ix + "\t\tIY=" + iy
                + "\nPC=" + pc + "\t\tSP=" + sp
                + "\nInstrucción:" + Memoria.lee(pc) + "\t\tRegistro I=" + leeI() + "\t\tRegistro R=" + leeR()
                + "\nHalt=" + halt + "\tIFF1=" + IFF1 + "\tIFF2=" + IFF2 + "\tIMFa=" + IMFa + "\tIMFb=" + IMFb
                + "\nS=" + getFlagS() + "\tZ=" + getFlagZ() + "\tH=" + getFlagH() + "\tP/V=" + getFlagPV() + "\tN=" + getFlagN() + "\tC=" + getFlagC();
    }

    //Muestra los registros en la consola en decimal
    public void muestraRegistros() {
        System.out.println("A=" + leeA() + "\tF=" + leeF() + "\tAF=" + af + "\tAF'=" + af_);
        System.out.println("B=" + leeB() + "\tC=" + leeC() + "\tBC=" + bc + "\tBC'=" + bc_);
        System.out.println("D=" + leeD() + "\tE=" + leeE() + "\tDE=" + de + "\tDE'=" + de_);
        System.out.println("H=" + leeH() + "\tL=" + leeL() + "\tHL=" + hl + "\tHL'=" + hl_);
        System.out.println("IX=" + ix + "\t\tIY=" + iy);
        System.out.println("PC=" + pc + "\t\tSP=" + sp);
        System.out.println("Instrucción:" + Memoria.lee(pc) + "\t\tRegistro I=" + leeI());
        System.out.println("Halt=" + halt + "\tIFF1=" + IFF1 + "\tIFF2=" + IFF2 + "\tIMFa=" + IMFa + "\tIMFb=" + IMFb);
        System.out.println("S=" + getFlagS() + "\t\tZ=" + getFlagZ() + "\t\tH=" + getFlagH() + "\t\tP/V=" + getFlagPV() + "\t\tN=" + getFlagN() + "\t\tC=" + getFlagC());
        System.out.println("-----------------------------------------------------------------------------------------------");
    }

    //Muestra los registros en la consola en hexadecimal
    public void muestraRegistrosHex() {
        System.out.println("A=" + Integer.toHexString(leeA()) + "\tF=" + Integer.toHexString(leeF()) + "\tAF=" + Integer.toHexString(af) + "\tAF'=" + Integer.toHexString(af_));
        System.out.println("B=" + Integer.toHexString(leeB()) + "\tC=" + Integer.toHexString(leeC()) + "\tBC=" + Integer.toHexString(bc) + "\tBC'=" + Integer.toHexString(bc_));
        System.out.println("D=" + Integer.toHexString(leeD()) + "\tE=" + Integer.toHexString(leeE()) + "\tDE=" + Integer.toHexString(de) + "\tDE'=" + Integer.toHexString(de_));
        System.out.println("H=" + Integer.toHexString(leeH()) + "\tL=" + Integer.toHexString(leeL()) + "\tHL=" + Integer.toHexString(hl) + "\tHL'=" + Integer.toHexString(hl_));
        System.out.println("IX=" + Integer.toHexString(ix) + "\t\tIY=" + Integer.toHexString(iy));
        System.out.println("PC=" + Integer.toHexString(pc) + "\t\tSP=" + Integer.toHexString(sp));
        System.out.println("Halt=" + halt + "\tIFF1=" + IFF1 + "\tIFF2=" + IFF2 + "\tIMFa=" + IMFa + "\tIMFb=" + IMFb);
        System.out.println("S=" + getFlagS() + "\t\tZ=" + getFlagZ() + "\t\tH=" + getFlagH() + "\t\tP/V=" + getFlagPV() + "\t\tN=" + getFlagN() + "\t\tC=" + getFlagC());
        System.out.println("-----------------------------------------------------------------------------------------------");
    }

    // Flag C=1
    public void setCFlag() {
        int f = leeF();
        f = f | FLAG_C;
        escribeF(f);
    }

    // Flag C=0;
    public void ceroCFlag() {
        int f = leeF();
        f = f & ~FLAG_C;
        escribeF(f);
    }

    // Flag N=1
    public void setNFlag() {
        int f = leeF();
        f = f | FLAG_N;
        escribeF(f);
    }

    // Flag N=0;
    public void ceroNFlag() {
        int f = leeF();
        f = f & ~FLAG_N;
        escribeF(f);
    }

    // Flag PV=1
    public void setPVFlag() {
        int f = leeF();
        f = f | FLAG_PV;
        escribeF(f);
    }

    // Flag PV=0;
    public void ceroPVFlag() {
        int f = leeF();
        f = f & ~FLAG_PV;
        escribeF(f);
    }

    // Flag H=1
    public void setHFlag() {
        int f = leeF();
        f = f | FLAG_H;
        escribeF(f);
    }

    // Flag H=0;
    public void ceroHFlag() {
        int f = leeF();
        f = f & ~FLAG_H;
        escribeF(f);
    }

    // Flag Z=1
    public void setZFlag() {
        int f = leeF();
        f = f | FLAG_Z;
        escribeF(f);
    }

    // Flag Z=0;
    public void ceroZFlag() {
        int f = leeF();
        f = f & ~FLAG_Z;
        escribeF(f);
    }

    // Flag S=1
    public void setSFlag() {
        int f = leeF();
        f = f | FLAG_S;
        escribeF(f);
    }

    // Flag S=0;
    public void ceroSFlag() {
        int f = leeF();
        f = f & ~FLAG_S;
        escribeF(f);
    }

    // Flag_X=1
    public void setXFlag() {
        int f = leeF();
        f = f | FLAG_X;
        escribeF(f);
    }

    // Flag_X=0;
    public void ceroXFlag() {
        int f = leeF();
        f = f & ~FLAG_X;
        escribeF(f);
    }

    // Flag_Y=1
    public void setYFlag() {
        int f = leeF();
        f = f | FLAG_Y;
        escribeF(f);
    }

    // Flag_Y=0;
    public void ceroYFlag() {
        int f = leeF();
        f = f & ~FLAG_Y;
        escribeF(f);
    }

    // Lee el Flag C
    public boolean getFlagC() {
        int f = leeF();
        f = f & FLAG_C;
        if (f == FLAG_C) {
            return true;
        } else {
            return false;
        }
    }

    // Lee el Flag N
    public boolean getFlagN() {
        int f = leeF();
        f = f & FLAG_N;
        if (f == FLAG_N) {
            return true;
        } else {
            return false;
        }
    }

    // Lee el Flag PV
    public boolean getFlagPV() {
        int f = leeF();
        f = f & FLAG_PV;
        if (f == FLAG_PV) {
            return true;
        } else {
            return false;
        }
    }

    // Lee el Flag H
    public boolean getFlagH() {
        int f = leeF();
        f = f & FLAG_H;
        if (f == FLAG_H) {
            return true;
        } else {
            return false;
        }
    }

    // Lee el Flag Z
    public boolean getFlagZ() {
        int f = leeF();
        f = f & FLAG_Z;
        if (f == FLAG_Z) {
            return true;
        } else {
            return false;
        }
    }

    // Lee el Flag S
    public boolean getFlagS() {
        int f = leeF();
        f = f & FLAG_S;
        if (f == FLAG_S) {
            return true;
        } else {
            return false;
        }
    }

    //Escribe el registro I
    public void escribeI(int i) {
        this.i = i;
    }

    //Lee el registro R
    public int leeR() {
        return r;
    }

    //Escribe el registro R
    public void escribeR(int r) {
        this.r = r;
    }

    //Lee el registro I
    public int leeI() {
        return i;
    }

    // Lee el registro A
    public int leeA() {
        int dato = af & 0xFF00;
        dato = dato >> 8;
        return dato;
    }

    // Lee el registro F
    public int leeF() {
        int dato = af & 0x00FF;
        return dato;
    }

    // Escribe el registro A
    public void escribeA(int valor) {
        valor = valor << 8;
        af = valor | (af & 0x00ff);
        //af = af & 0x00FF;
        //af = af + valor;
    }

    // Escribe el registro F
    public void escribeF(int valor) {
        af = valor | (af & 0xff00);
        //af = af & 0xFF00;
        //af = af + valor;
    }

    // Escribe el registro A'
    public void escribeA_(int valor) {
        valor = valor << 8;
        af_ = valor | (af_ & 0x00ff);
        //af_ = af_ & 0x00FF;
        //af_ = af_ + valor;
    }

    // Escribe el registro F'
    public void escribeF_(int valor) {
        af_ = valor | (af_ & 0xff00);
        //af_ = af_ & 0xFF00;
        //af_ = af_ + valor;
    }

    // Lee el registro B
    public int leeB() {
        int b = bc & 0xFF00;
        b = b >> 8;
        return b;
    }

    // Lee el registro C
    public int leeC() {
        int c = bc & 0x00FF;
        return c;
    }

    // Escribe el registro B
    public void escribeB(int valor) {
        valor = valor << 8;
        bc = valor | (bc & 0x00ff);
        //bc = bc & 0x00FF;
        //bc = bc + valor;
    }

    // Escribe el registro C
    public void escribeC(int valor) {
        bc = valor | (bc & 0xff00);
        //bc = bc & 0xFF00;
        //bc = bc + valor;
    }

    // Escribe el registro B'
    public void escribeB_(int valor) {
        valor = valor << 8;
        bc_ = valor | (bc_ & 0x00ff);
        //bc_ = bc_ & 0x00FF;
        //bc_ = bc_ + valor;
    }

    // Escribe el registro C'
    public void escribeC_(int valor) {
        bc_ = valor | (bc_ & 0xff00);
        //bc_ = bc_ & 0xFF00;
        //bc_ = bc_ + valor;
    }

    // Lee el registro D
    public int leeD() {
        int d = de & 0xFF00;
        d = d >> 8;
        return d;
    }

    // Lee el registro E
    public int leeE() {
        int e = de & 0x00FF;
        return e;
    }

    // Escribe el registro D
    public void escribeD(int valor) {
        valor = valor << 8;
        de = valor | (de & 0x00ff);
        //de = de & 0x00FF;
        //de = de + valor;
    }

    // Escribe el registro E
    public void escribeE(int valor) {
        de = valor | (de & 0xff00);
        //de = de & 0xFF00;
        //de = de + valor;
    }

    // Escribe el registro D'
    public void escribeD_(int valor) {
        valor = valor << 8;
        de_ = valor | (de_ & 0x00ff);
        //de_ = de_ & 0x00FF;
        //de_ = de_ + valor;
    }

    // Escribe el registro E'
    public void escribeE_(int valor) {
        de_ = valor | (de_ & 0xff00);
        //de_ = de_ & 0xFF00;
        //de_ = de_ + valor;
    }

    // Lee el registro H
    public int leeH() {
        int h = hl & 0xFF00;
        h = h >> 8;
        return h;
    }

    // Lee el registro L
    public int leeL() {
        int l = hl & 0x00FF;
        return l;
    }

    // Escribe el registro H
    public void escribeH(int valor) {
        valor = valor << 8;
        hl = valor | (hl & 0x00ff);
        //hl = hl & 0x00FF;
        //hl = hl + valor;
    }

    // Escribe el registro L
    public void escribeL(int valor) {
        hl = valor | (hl & 0xff00);
        //hl = hl & 0xFF00;
        //hl = hl + valor;
    }

    // Escribe el registro H'
    public void escribeH_(int valor) {
        valor = valor << 8;
        hl_ = valor | (hl_ & 0x00ff);
        //hl_ = hl_ & 0x00FF;
        //hl_ = hl_ + valor;
    }

    // Escribe el registro L'
    public void escribeL_(int valor) {
        hl_ = valor | (hl_ & 0xff00);
        //hl_ = hl_ & 0xFF00;
        //hl_ = hl_ + valor;
    }

    // Lee el registro IX H
    public int leeIxH() {
        int ixh = ix & 0xFF00;
        ixh = ixh >> 8;
        return ixh;
    }

    // Lee el registro IX L
    public int leeIxL() {
        int ixl = ix & 0x00FF;
        return ixl;
    }

    // Escribe el registro IX H
    public void escribeIxH(int valor) {
        valor = valor << 8;
        ix = valor | (ix & 0x00ff);
        //ix = ix & 0x00FF;
        //ix = ix + valor;
    }

    // Escribe el registro IX L
    public void escribeIxL(int valor) {
        ix = valor | (ix & 0xff00);
        //ix = ix & 0xFF00;
        //ix = ix + valor;
    }

    // Lee el registro IY H
    public int leeIyH() {
        int iyh = iy & 0xFF00;
        iyh = iyh >> 8;
        return iyh;
    }

    // Lee el registro IY L
    public int leeIyL() {
        int iyl = iy & 0x00FF;
        return iyl;
    }

    // Escribe el registro IY H
    public void escribeIyH(int valor) {
        valor = valor << 8;
        iy = valor | (iy & 0x00ff);
        //iy = iy & 0x00FF;
        //iy = iy + valor;
    }

    // Escribe el registro IY L
    public void escribeIyL(int valor) {
        iy = valor | (iy & 0xff00);
        //iy = iy & 0xFF00;
        //iy = iy + valor;
    }

    // Lee el registro IX
    public int leeIX() {
        return ix;
    }

    // Lee el registro IY
    public int leeIY() {
        return iy;
    }

    // Lee el registro SP
    public int leeSP() {
        return sp;
    }

    // Lee el registro S
    public int leeS() {
        int s = sp & 0xFF00;
        s = s >> 8;
        return s;
    }

    // Lee el registro P
    public int leeP() {
        int p = sp & 0x00FF;
        return p;
    }

    // Escribe el registro S
    public void escribeS(int valor) {
        valor = valor << 8;
        sp = valor | (sp & 0x00ff);
        //sp = sp & 0x00FF;
        //sp = sp + valor;
    }

    // Escribe el registro P
    public void escribeP(int valor) {
        sp = valor | (sp & 0xff00);
        //sp = sp & 0xFF00;
        //sp = sp + valor;
    }

    // Escribe el registro PC_H
    public void escribePC_H(int valor) {
        valor = valor << 8;
        pc = valor | (pc & 0x00ff);
        //pc = pc & 0x00FF;
        //pc = pc + valor;
    }

    // Escribe el registro PC_L
    public void escribePC_L(int valor) {
        pc = valor | (pc & 0xff00);
        //pc = pc & 0xFF00;
        //pc = pc + valor;
    }

    // Lee el registro PC_H
    public int leePC_H() {
        int p = pc & 0xFF00;
        p = p >> 8;
        return p;
    }

    // Lee el registro PC_L
    public int leePC_L() {
        int c = pc & 0x00FF;
        return c;
    }

    // Lee el registro PC (Contador del programa)
    public int getPc() {
        return pc;
    }

    // Escribe en el registro PC (Contador del programa)
    public void setPc(int pc) {
        this.pc = pc;
    }

    //Escribe en el registro AF'
    public void setAF_(int af_) {
        this.af_ = af_;
    }

    //Lee en el registro AF'
    public int getAF_() {
        return af_;
    }

    //Escribe en el registro BC'
    public void setBC_(int bc_) {
        this.bc_ = bc_;
    }

    //Lee en el registro BC'
    public int getBC_() {
        return bc_;
    }

    //Escribe en el registro DE'
    public void setDE_(int de_) {
        this.de_ = de_;
    }

    //Lee en el registro DE'
    public int getDE_() {
        return de_;
    }

    //Escribe en el registro HL'
    public void setHL_(int hl_) {
        this.hl_ = hl_;
    }

    //Lee en el registro HL'
    public int getHL_() {
        return hl_;
    }

    //Escribe en el registro AF
    public void setAF(int af) {
        this.af = af;
    }

    //Lee el registro AF
    public int getAF() {
        return af;
    }

    //Escribe en el registro BC
    public void setBC(int bc) {
        this.bc = bc;
    }

    //Lee el registro BC
    public int getBC() {
        return bc;
    }

    //Escribe en el registro DE
    public void setDE(int de) {
        this.de = de;
    }

    //Lee el registro DE
    public int getDE() {
        return de;
    }

    //Escribe en el registro HL
    public void setHL(int hl) {
        this.hl = hl;
    }

    //Lee el registro HL
    public int getHL() {
        return hl;
    }

    //Escribe en el registro IX
    public void setIX(int ix) {
        this.ix = ix;
    }

    //Lee el registro IX
    public int getIX() {
        return ix;
    }

    //Escribe en el registro IY
    public void setIY(int iy) {
        this.iy = iy;
    }

    //Lee el registro IY
    public int getIY() {
        return iy;
    }

    //Escribe el registro SP
    public void setSP(int sp) {
        this.sp = sp;
    }

    //Lee el registro SP
    public int getSP() {
        return sp;
    }

    // Lee el registro de instrucciones
    public int getIr() {
        return ir;
    }

    // Lee el puerto de datos D0-D7
    public int leeD0D7() {
        return d0d7;
    }

    // Escribe el puerto de datos D0-D7
    public void escribeD0D7(int dato) {
        d0d7 = dato;
    }

    // Lee el puerto de direcciones A0-A7
    public int leeA0A7() {
        return a0a7;
    }

    // Escribe el puerto de direcciones A0-A7
    public void escribeA0A7(int dato) {
        a0a7 = dato;
    }

    // Lee el puerto de direcciones A8-A15
    public int leeA8A15() {
        return a8a15;
    }

    // Escribe el puerto de direcciones A8-A15
    public void escribeA8A15(int dato) {
        a8a15 = dato;
    }

    //Escribe en el registro de interrupciones
    public void setI(int dato) {
        i = dato;
    }

    //Escribe en el registro de refresco
    public void setR(int dato) {
        r = dato;
    }

    // Incrementa el puntero SP
    public void incSp() {
        sp++;
        sp = sp & 0xffff;
    }

    // Decrementa el puntero SP
    public void decSp() {
        sp--;
        sp = sp & 0xffff;
    }

    // Incrementa BC
    public void incBC() {
        bc++;
        bc = bc & 0xffff;
    }

    // Decrementa BC
    public void decBC() {
        bc--;
        bc = bc & 0xffff;
    }

    // Incrementa DE
    public void incDE() {
        de++;
        de = de & 0xffff;
    }

    // Decrementa DE
    public void decDE() {
        de--;
        de = de & 0xffff;
    }

    // Incrementa HL
    public void incHL() {
        hl++;
        hl = hl & 0xffff;
    }

    // Decrementa HL
    public void decHL() {
        hl--;
        hl = hl & 0xffff;
    }

    // Incrementa IX
    public void incIX() {
        ix++;
        ix = ix & 0xffff;
    }

    // Incrementa IX+d
    public void incIXd() {
        int d = Memoria.lee(pc);// Coge el valor de desplazamiento
        incPC();
        int puntero = (ix + (byte) d) & 0xffff;
        int valor = Memoria.lee(puntero);
        // Guarda el valor en valorMemo
        int valorMemo = valor;
        valor++;
        valor = valor & 0xff;
        Memoria.escribe(puntero, valor);// Incrementa el contenido de la posición de memoria IX+d
        // Modifica los flags
        if (valor == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (valor > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        if (((valorMemo & 0xf) + ((1) & 0xf)) > 0xf) {//Flag H
            setHFlag();
        } else {
            ceroHFlag();
        }
        if (((valorMemo <= 127) && (1 <= 127) && ((valorMemo + 1) > 127)) || ((valorMemo > 127) && (1 > 127) && ((valorMemo + 1) <= 127))) {// Flag P/V Paridad/Desbordamiento
            setPVFlag();
        } else {
            ceroPVFlag();
        }
        ceroNFlag();
        flagsXY(valor);
    }

    // Incrementa IY+d
    public void incIYd() {
        int d = Memoria.lee(pc);// Coge el valor de desplazamiento
        incPC();
        int puntero = (iy + (byte) d) & 0xffff;
        int valor = Memoria.lee(puntero);
        // Guarda el valor en valorMemo
        int valorMemo = valor;
        valor++;
        valor = valor & 0xff;
        Memoria.escribe(puntero, valor);// Incrementa el contenido de la posición de memoria IY+d  
        // Incrementa el registro y modifica los flags
        if (valor == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (valor > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        if (((valorMemo & 0xf) + ((1) & 0xf)) > 0xf) {//Flag H
            setHFlag();
        } else {
            ceroHFlag();
        }
        if (((valorMemo <= 127) && (1 <= 127) && ((valorMemo + 1) > 127)) || ((valorMemo > 127) && (1 > 127) && ((valorMemo + 1) <= 127))) {// Flag P/V Paridad/Desbordamiento
            setPVFlag();
        } else {
            ceroPVFlag();
        }
        ceroNFlag();
        flagsXY(valor);
    }

    // Decrementa IX+d
    public void decIXd() {
        int d = Memoria.lee(pc);// Coge el valor de desplazamiento
        incPC();
        int puntero = (ix + (byte) d) & 0xffff;
        int valor = Memoria.lee(puntero);
        // Guarda el valor en valorMemo
        int valorMemo = valor;
        valor--;
        valor = valor & 0xff;
        Memoria.escribe(puntero, valor);// Incrementa el contenido de la posición de memoria IX+d
        // Incrementa el registro y modifica los flags
        if (valor == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (valor > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        if ((valorMemo & 0xF) < ((valorMemo - 1) & 0xF)) {// Flag H Bandera de acarreo mitad
            setHFlag();
        } else {
            ceroHFlag();
        }
        if (((valorMemo > 127) && (1 <= 127) && ((valorMemo - 1) < 128)) || ((valorMemo <= 127) && (1 > 127) && ((valorMemo - 1) >= -128))) {// Flag P/V Paridad/Desbordamiento
            setPVFlag();
        } else {
            ceroPVFlag();
        }
        setNFlag();
        flagsXY(valor);
    }

    // Decrementa IY+d
    public void decIYd() {
        int d = Memoria.lee(pc);// Coge el valor de desplazamiento
        incPC();
        int puntero = (iy + (byte) d) & 0xffff;
        int valor = Memoria.lee(puntero);
        // Coge el valor del bit4 del registro
        int valorMemo = valor;
        valor--;
        valor = valor & 0xff;
        Memoria.escribe(puntero, valor);// Incrementa el contenido de la posición de memoria IY+d
        // Incrementa el registro y modifica los flags
        if (valor == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (valor > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        if ((valorMemo & 0xF) < ((valorMemo - 1) & 0xF)) {// Flag H Bandera de acarreo mitad
            setHFlag();
        } else {
            ceroHFlag();
        }
        if (((valorMemo > 127) && (1 <= 127) && ((valorMemo - 1) < 128)) || ((valorMemo <= 127) && (1 > 127) && ((valorMemo - 1) >= -128))) {// Flag P/V Paridad/Desbordamiento
            setPVFlag();
        } else {
            ceroPVFlag();
        }
        setNFlag();
        flagsXY(valor);
    }

    // Decrementa IX
    public void decIX() {
        ix--;
        ix = ix & 0xffff;
    }

    // Incrementa IY
    public void incIY() {
        iy++;
        iy = iy & 0xffff;
    }

    // Decrementa IY
    public void decIY() {
        iy--;
        iy = iy & 0xffff;
    }

    // Incrementa PC
    public void incPC() {
        pc++;
        pc = pc & 0xffff;
    }

    // Decrementa PC
    public void decPC() {
        pc--;
        pc = pc & 0xffff;
    }

    // Incrementa registro de 1 byte
    public void inc(int registro) {
        int reg = 0;
        switch (registro) {
            case A:
                reg = leeA();
                break;
            case B:
                reg = leeB();
                break;
            case C:
                reg = leeC();
                break;
            case D:
                reg = leeD();
                break;
            case E:
                reg = leeE();
                break;
            case H:
                reg = leeH();
                break;
            case L:
                reg = leeL();
                break;
            case HL:
                reg = Memoria.lee(hl);
                break;
            case IXH:
                reg = leeIxH();
                break;
            case IXL:
                reg = leeIxL();
                break;
            case IYH:
                reg = leeIyH();
                break;
            case IYL:
                reg = leeIyL();
                break;
        }
        // Coge el valor del bit4 del registro
        int regMemo = reg;
        // Incrementa el registro y modifica los flags
        reg++;
        reg = reg & 0xff;
        if (reg == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (reg > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        if (((regMemo & 0xf) + ((1) & 0xf)) > 0xf) {//Flag H
            setHFlag();
        } else {
            ceroHFlag();
        }
        if (((regMemo <= 127) && (1 <= 127) && ((regMemo + 1) > 127)) || ((regMemo > 127) && (1 > 127) && ((regMemo + 1) <= 127))) {// Flag P/V Paridad/Desbordamiento
            setPVFlag();
        } else {
            ceroPVFlag();
        }
        flagsXY(reg);
        ceroNFlag();
        switch (registro) {
            case A:
                escribeA(reg);
                break;
            case B:
                escribeB(reg);
                break;
            case C:
                escribeC(reg);
                break;
            case D:
                escribeD(reg);
                break;
            case E:
                escribeE(reg);
                break;
            case H:
                escribeH(reg);
                break;
            case L:
                escribeL(reg);
                break;
            case HL:
                Memoria.escribe(hl, reg);
                break;
            case IXH:
                escribeIxH(reg);
                break;
            case IXL:
                escribeIxL(reg);
                break;
            case IYH:
                escribeIyH(reg);
                break;
            case IYL:
                escribeIyL(reg);
                break;
        }
    }

    // Decrementa registro de 1 byte
    public void dec(int registro) {
        int reg = 0;
        switch (registro) {
            case A:
                reg = leeA();
                break;
            case B:
                reg = leeB();
                break;
            case C:
                reg = leeC();
                break;
            case D:
                reg = leeD();
                break;
            case E:
                reg = leeE();
                break;
            case H:
                reg = leeH();
                break;
            case L:
                reg = leeL();
                break;
            case HL:
                reg = Memoria.lee(hl);
                break;
            case IXH:
                reg = leeIxH();
                break;
            case IXL:
                reg = leeIxL();
                break;
            case IYH:
                reg = leeIyH();
                break;
            case IYL:
                reg = leeIyL();
                break;
        }
        // Coge el valor del registro
        int regMemo = reg;
        // Decrementa el registro y modifica los flags
        reg--;
        reg = reg & 0xff;
        if (reg == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (reg > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        if (((regMemo & 16) + ((regMemo - 1) & 16)) == 16) {//Flag H
            setHFlag();
        } else {
            ceroHFlag();
        }
        if (((regMemo > 127) && (1 <= 127) && ((regMemo - 1) < 128)) || ((regMemo <= 127) && (1 > 127) && ((regMemo - 1) >= -128))) {// Flag P/V Paridad/Desbordamiento
            setPVFlag();
        } else {
            ceroPVFlag();
        }
        flagsXY(reg);//Actualiza los Flags X e Y
        setNFlag();
        switch (registro) {
            case A:
                escribeA(reg);
                break;
            case B:
                escribeB(reg);
                break;
            case C:
                escribeC(reg);
                break;
            case D:
                escribeD(reg);
                break;
            case E:
                escribeE(reg);
                break;
            case H:
                escribeH(reg);
                break;
            case L:
                escribeL(reg);
                break;
            case HL:
                Memoria.escribe(hl, reg);
                break;
            case IXH:
                escribeIxH(reg);
                break;
            case IXL:
                escribeIxL(reg);
                break;
            case IYH:
                escribeIyH(reg);
                break;
            case IYL:
                escribeIyL(reg);
                break;
        }
    }

    // Suma a A el contenido del registro indicado
    public void addA(int registro) {
        int reg = 0;
        switch (registro) {
            case A:
                reg = leeA();
                break;
            case B:
                reg = leeB();
                break;
            case C:
                reg = leeC();
                break;
            case D:
                reg = leeD();
                break;
            case E:
                reg = leeE();
                break;
            case H:
                reg = leeH();
                break;
            case L:
                reg = leeL();
                break;
            case HL:
                reg = Memoria.lee(hl);
                break;
            case IXH:
                reg = leeIxH();
                break;
            case IXL:
                reg = leeIxL();
                break;
            case IYH:
                reg = leeIyH();
                break;
            case IYL:
                reg = leeIyL();
                break;
        }
        sumaAFlags(reg, false);
    }

    // Suma el acumulador con la posición de memoria indexada
    public void addAIXd() {
        desplazamiento = Memoria.lee(pc);
        incPC();
        int dato = Memoria.lee((ix + (byte) desplazamiento) & 0xffff);
        sumaAFlags(dato, false);
    }

    // Suma el acumulador con la posición de memoria indexada
    public void addAIYd() {
        desplazamiento = Memoria.lee(pc);
        incPC();
        int dato = Memoria.lee((iy + (byte) desplazamiento) & 0xffff);
        sumaAFlags(dato, false);
    }

    // Suma el acumulador con la posición de memoria indexada
    public void adcAIXd() {
        desplazamiento = Memoria.lee(pc);
        incPC();
        int dato = Memoria.lee((ix + (byte) desplazamiento) & 0xffff);
        sumaAFlags(dato, true);
    }

    // Suma el acumulador con la posición de memoria indexada
    public void adcAIYd() {
        desplazamiento = Memoria.lee(pc);
        incPC();
        int dato = Memoria.lee((iy + (byte) desplazamiento) & 0xffff);
        sumaAFlags(dato, true);
    }

    // Suma a A el contenido del registro indicado y el acarreo
    public void adcA(int registro) {
        int reg = 0;
        switch (registro) {
            case A:
                reg = leeA();
                break;
            case B:
                reg = leeB();
                break;
            case C:
                reg = leeC();
                break;
            case D:
                reg = leeD();
                break;
            case E:
                reg = leeE();
                break;
            case H:
                reg = leeH();
                break;
            case L:
                reg = leeL();
                break;
            case HL:
                reg = Memoria.lee(hl);
                break;
            case IXH:
                reg = leeIxH();
                break;
            case IXL:
                reg = leeIxL();
                break;
            case IYH:
                reg = leeIyH();
                break;
            case IYL:
                reg = leeIyL();
                break;
        }
        sumaAFlags(reg, true);
    }

    // Suma a A el valor del contenido en la memoria que indica PC
    public void addAnn() {
        int valor = Memoria.lee(pc);
        sumaAFlags(valor, false);
    }

    // Suma a A el valor del contenido en la memoria que indica PC y suma el acarreo
    public void adcAnn() {
        int valor = Memoria.lee(pc);
        sumaAFlags(valor, true);
    }

    // Suma a HL el registro seleccionado
    public void addHl(int registro) {
        int reg = 0;
        switch (registro) {
            case BC:
                reg = bc;
                break;
            case DE:
                reg = de;
                break;
            case HL:
                reg = hl;
                break;
            case SP:
                reg = sp;
                break;
        }
        // Coge el valor del bit11 del registro
        int valorMemo = hl;
        ceroNFlag();
        hl = hl + reg;
        if (hl > 0xFFFF) {
            setCFlag();
        } else {
            ceroCFlag();
        }
        hl = hl & 0xffff;
        if ((((valorMemo & 0xfff) + (reg & 0xfff)) & 0x1000) == 0x1000) {//Flag H
            setHFlag();
        } else {
            ceroHFlag();
        }
        flagsXY(leeH());
    }

    // Suma a HL el registro seleccionado con el acarreo
    public void adcHl(int registro) {
        int reg = 0;
        switch (registro) {
            case BC:
                reg = bc;
                break;
            case DE:
                reg = de;
                break;
            case HL:
                reg = hl;
                break;
            case SP:
                reg = sp;
                break;
        }
        // Guarda el valor de HL
        int regMemo = hl;
        if (getFlagC()) {
            reg++;
        }
        hl = hl + reg;
        if (hl > 0xFFFF) {
            setCFlag();
        } else {
            ceroCFlag();
        }
        hl = hl & 0xffff;
        if (((((reg & 0xfff) + (regMemo & 0xfff))) & 0x1000) == 0x1000) {//Flag H
            setHFlag();
        } else {
            ceroHFlag();
        }
        if (hl == 0) {// Flag Z
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (((regMemo <= 32767) && (reg <= 32767) && (((regMemo + reg) & 0xffff) > 32767)) || ((regMemo > 32767) && (reg > 32767) && (((regMemo + reg) & 0xffff) <= 32767))) {// Flag P/V Paridad/Desbordamiento
            setPVFlag();
        } else {
            ceroPVFlag();
        }
        if (hl > 32767) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        ceroNFlag();
        flagsXY(leeH());
    }

    // Resta con acarreo a HL el registro seleccionado
    public void sbcHl(int registro) {
        int reg = 0;
        switch (registro) {
            case BC:
                reg = bc;
                break;
            case DE:
                reg = de;
                break;
            case HL:
                reg = hl;
                break;
            case SP:
                reg = sp;
                break;
        }
        //Memoriza el registro HL
        int regMemo = hl;
        setNFlag();
        if (getFlagC()) {
            reg++;
        }
        hl = hl - reg;
        if (hl < 0) {
            setCFlag();
        } else {
            ceroCFlag();
        }
        hl = hl & 0xffff;
        if (hl == 0) {// Flag Z
            setZFlag();
        } else {
            ceroZFlag();
        }
        if ((regMemo & 0xFFF) < (hl & 0xFFF)) {// Flag H Bandera de acarreo mitad
            setHFlag();
        } else {
            ceroHFlag();
        }
        if (((regMemo > 32767) && (reg <= 32767) && ((regMemo - reg) < 32768)) || ((regMemo <= 32767) && (reg > 32767) && ((regMemo - reg) >= -32768))) {// Flag P/V Paridad/Desbordamiento
            setPVFlag();
        } else {
            ceroPVFlag();
        }
        if (hl > 32767) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        flagsXY(leeH());
    }

    // Suma a IX el registro seleccionado
    public void addIX(int registro) {
        int reg = 0;
        switch (registro) {
            case BC:
                reg = bc;
                break;
            case DE:
                reg = de;
                break;
            case IX:
                reg = ix;
                break;
            case SP:
                reg = sp;
                break;
        }
        ceroNFlag();
        ix = ix + reg;
        if (ix > 0xFFFF) {
            setCFlag();
            ix = ix & 0xffff;
        } else {
            ceroCFlag();
        }
    }

    // Suma a IY el registro seleccionado
    public void addIY(int registro) {
        int reg = 0;
        switch (registro) {
            case BC:
                reg = bc;
                break;
            case DE:
                reg = de;
                break;
            case IY:
                reg = iy;
                break;
            case SP:
                reg = sp;
                break;
        }
        ceroNFlag();
        iy = iy + reg;
        if (iy > 0xFFFF) {
            setCFlag();
            iy = iy & 0xffff;
        } else {
            ceroCFlag();
        }
    }

    // Resta a A el contenido del registro indicado
    public void subA(int registro) {
        int reg = 0;
        switch (registro) {
            case A:
                reg = leeA();
                break;
            case B:
                reg = leeB();
                break;
            case C:
                reg = leeC();
                break;
            case D:
                reg = leeD();
                break;
            case E:
                reg = leeE();
                break;
            case H:
                reg = leeH();
                break;
            case L:
                reg = leeL();
                break;
            case HL:
                reg = Memoria.lee(hl);
                break;
            case IXH:
                reg = leeIxH();
                break;
            case IXL:
                reg = leeIxL();
                break;
            case IYH:
                reg = leeIyH();
                break;
            case IYL:
                reg = leeIyL();
                break;
        }
        restaAFlags(reg, false);
    }

    // Resta el acumulador con la posición de memoria indexada
    public void subAIXd() {
        desplazamiento = Memoria.lee(pc);
        incPC();
        int dato = Memoria.lee((ix + (byte) desplazamiento) & 0xffff);
        restaAFlags(dato, false);
    }

    // Resta el acumulador con la posición de memoria indexada
    public void subAIYd() {
        desplazamiento = Memoria.lee(pc);
        incPC();
        int dato = Memoria.lee((iy + (byte) desplazamiento) & 0xffff);
        restaAFlags(dato, false);
    }

    // Resta el acumulador con la posición de memoria indexada
    public void sbcAIXd() {
        desplazamiento = Memoria.lee(pc);
        incPC();
        int dato = Memoria.lee((ix + (byte) desplazamiento) & 0xffff);
        restaAFlags(dato, true);
    }

    // Resta el acumulador con la posición de memoria indexada
    public void sbcAIYd() {
        desplazamiento = Memoria.lee(pc);
        incPC();
        int dato = Memoria.lee((iy + (byte) desplazamiento) & 0xffff);
        restaAFlags(dato, true);
    }

    // Resta a A el valor del contenido en la memoria que indica PC
    public void subAnn() {
        int valor = Memoria.lee(pc);
        restaAFlags(valor, false);
    }

    // Resta a A el contenido del registro indicado y el acarreo
    public void sbcA(int registro) {
        int reg = 0;
        switch (registro) {
            case A:
                reg = leeA();
                break;
            case B:
                reg = leeB();
                break;
            case C:
                reg = leeC();
                break;
            case D:
                reg = leeD();
                break;
            case E:
                reg = leeE();
                break;
            case H:
                reg = leeH();
                break;
            case L:
                reg = leeL();
                break;
            case HL:
                reg = Memoria.lee(hl);
                break;
            case IXH:
                reg = leeIxH();
                break;
            case IXL:
                reg = leeIxL();
                break;
            case IYH:
                reg = leeIyH();
                break;
            case IYL:
                reg = leeIyL();
                break;
        }
        restaAFlags(reg, true);
    }

    // Resta a A el valor del contenido en la memoria que indica PC y suma el
    // acarreo
    public void sbcAnn() {
        int valor = Memoria.lee(pc);
        restaAFlags(valor, true);
    }

    // Hace and A con un registro
    public void and(int registro) {
        int regA = leeA();
        int reg = 0;
        switch (registro) {
            case A:
                reg = leeA();
                break;
            case B:
                reg = leeB();
                break;
            case C:
                reg = leeC();
                break;
            case D:
                reg = leeD();
                break;
            case E:
                reg = leeE();
                break;
            case H:
                reg = leeH();
                break;
            case L:
                reg = leeL();
                break;
            case IX:
                indice = Memoria.lee(pc);
                incPC();
                reg = Memoria.lee((ix + (byte) indice) & 0xffff);
                break;
            case IY:
                indice = Memoria.lee(pc);
                incPC();
                reg = Memoria.lee((iy + (byte) indice) & 0xffff);
                break;
            case IXH:
                reg = leeIxH();
                break;
            case IXL:
                reg = leeIxL();
                break;
            case IYH:
                reg = leeIyH();
                break;
            case IYL:
                reg = leeIyL();
                break;
        }
        int and = regA & reg;
        escribeA(and);
        if (and == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (and > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        paridad(and);
        setHFlag();
        ceroNFlag();
        ceroCFlag();
        flagsXY(and);//Actualiza los Flags X e Y
    }

    // Hace and A con el contenido en memoria indicado por HL
    public void andHl() {
        int regA = leeA();
        int reg = Memoria.lee(hl);
        int and = regA & reg;
        escribeA(and);
        if (and == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (and > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        paridad(and);
        setHFlag();
        ceroNFlag();
        ceroCFlag();
        flagsXY(and);//Actualiza los Flags X e Y
    }

    // Hace or A con un registro
    public void or(int registro) {
        int regA = leeA();
        int reg = 0;
        switch (registro) {
            case A:
                reg = leeA();
                break;
            case B:
                reg = leeB();
                break;
            case C:
                reg = leeC();
                break;
            case D:
                reg = leeD();
                break;
            case E:
                reg = leeE();
                break;
            case H:
                reg = leeH();
                break;
            case L:
                reg = leeL();
                break;
            case IX:
                indice = Memoria.lee(pc);
                incPC();
                reg = Memoria.lee((ix + (byte) indice) & 0xffff);
                break;
            case IY:
                indice = Memoria.lee(pc);
                incPC();
                reg = Memoria.lee((iy + (byte) indice) & 0xffff);
                break;
            case IXH:
                reg = leeIxH();
                break;
            case IXL:
                reg = leeIxL();
                break;
            case IYH:
                reg = leeIyH();
                break;
            case IYL:
                reg = leeIyL();
                break;
        }
        int or = regA | reg;
        escribeA(or);
        if (or == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (or > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        paridad(or);
        ceroHFlag();
        ceroNFlag();
        ceroCFlag();
        flagsXY(or);//Actualiza los Flags X e Y
    }

    // Hace or A con el contenido en memoria indicado por HL
    public void orHl() {
        int regA = leeA();
        int reg = Memoria.lee(hl);
        int or = regA | reg;
        escribeA(or);
        if (or == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (or > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        paridad(or);
        ceroHFlag();
        ceroNFlag();
        ceroCFlag();
        flagsXY(or);//Actualiza los Flags X e Y
    }

    // Hace or A con un dato
    public void orNn() {
        int regA = leeA();
        int reg = Memoria.lee(pc);
        int or = regA | reg;
        escribeA(or);
        if (or == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (or > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        paridad(or);
        ceroHFlag();
        ceroNFlag();
        ceroCFlag();
        flagsXY(or);//Actualiza los Flags X e Y
    }

    // Hace xor A con un registro
    public void xor(int registro) {
        int regA = leeA();
        int reg = 0;
        switch (registro) {
            case A:
                reg = leeA();
                break;
            case B:
                reg = leeB();
                break;
            case C:
                reg = leeC();
                break;
            case D:
                reg = leeD();
                break;
            case E:
                reg = leeE();
                break;
            case H:
                reg = leeH();
                break;
            case L:
                reg = leeL();
                break;
            case IX:
                indice = Memoria.lee(pc);
                incPC();
                reg = Memoria.lee((ix + (byte) indice) & 0xffff);
                break;
            case IY:
                indice = Memoria.lee(pc);
                incPC();
                reg = Memoria.lee((iy + (byte) indice) & 0xffff);
                break;
            case IXH:
                reg = leeIxH();
                break;
            case IXL:
                reg = leeIxL();
                break;
            case IYH:
                reg = leeIyH();
                break;
            case IYL:
                reg = leeIyL();
                break;
        }
        int xor = regA ^ reg;
        escribeA(xor);
        if (xor == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (xor > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        paridad(xor);
        ceroHFlag();
        ceroNFlag();
        ceroCFlag();
        flagsXY(xor);//Actualiza los Flags X e Y
    }

    // Hace xor A con el contenido en memoria indicado por HL
    public void xorHl() {
        int regA = leeA();
        int reg = Memoria.lee(hl);
        int xor = regA ^ reg;
        escribeA(xor);
        if (xor == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (xor > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        paridad(xor);
        ceroHFlag();
        ceroNFlag();
        ceroCFlag();
        flagsXY(xor);//Actualiza los Flags X e Y
    }

    // Hace xor A con un dato
    public void xorNn() {
        int regA = leeA();
        int reg = Memoria.lee(pc);
        int xor = regA ^ reg;
        escribeA(xor);
        if (xor == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (xor > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        paridad(xor);
        ceroHFlag();
        ceroNFlag();
        ceroCFlag();
        flagsXY(xor);//Actualiza los Flags X e Y
    }

    // Hace and A con un dato
    public void andNn() {
        int regA = leeA();
        int reg = Memoria.lee(pc);
        int and = regA & reg;
        escribeA(and);
        if (and == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (and > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        paridad(and);
        setHFlag();
        ceroNFlag();
        ceroCFlag();
        flagsXY(and);//Actualiza los Flags X e Y
    }

    // Pone el flag de paridad P/V a 1 si el número de bits es par y a 0 si es impar
    public void paridad(int registro) {
        int dato = registro;
        int cont = 0;
        for (int n = 0; n < 8; n++) {
            dato = dato << 1;
            if ((dato & 256) == 256) {
                cont++;
            }
        }
        if (cont % 2 == 0) {
            setPVFlag();
        } else {
            ceroPVFlag();
        }
    }

    public void cp(int registro) {
        int reg = 0;
        switch (registro) {
            case A:
                reg = leeA();
                break;
            case B:
                reg = leeB();
                break;
            case C:
                reg = leeC();
                break;
            case D:
                reg = leeD();
                break;
            case E:
                reg = leeE();
                break;
            case H:
                reg = leeH();
                break;
            case L:
                reg = leeL();
                break;
            case HL:
                reg = Memoria.lee(hl);
                break;
            case IX:
                indice = Memoria.lee(pc);
                incPC();
                reg = Memoria.lee((ix + (byte) indice) & 0xffff);
                break;
            case IY:
                indice = Memoria.lee(pc);
                incPC();
                reg = Memoria.lee((iy + (byte) indice) & 0xffff);
                break;
            case IXH:
                reg = leeIxH();
                break;
            case IXL:
                reg = leeIxL();
                break;
            case IYH:
                reg = leeIyH();
                break;
            case IYL:
                reg = leeIyL();
                break;
        }
        cpFlags(reg);
    }

    public void cpNn() {
        int reg = Memoria.lee(pc);
        cpFlags(reg);
    }

    // Rst
    public void rst(int valor) {
        decSp();
        Memoria.escribe(sp, leePC_H());
        decSp();
        Memoria.escribe(sp, leePC_L());
        pc = valor;
    }

    // Call
    public void call() {
        int pcL = Memoria.lee(pc);
        incPC();
        int pcH = Memoria.lee(pc);
        incPC();
        decSp();
        Memoria.escribe(sp, leePC_H());
        decSp();
        Memoria.escribe(sp, leePC_L());
        escribePC_L(pcL);
        escribePC_H(pcH);
    }

    // Ret
    public void ret() {
        escribePC_L(Memoria.lee(sp));
        incSp();
        escribePC_H(Memoria.lee(sp));
        incSp();
    }

    // JR Salto relativo
    public void jr() {
        tStates += 12;
        int valor = (byte) Memoria.lee(pc) + 1;
        pc = pc + valor & 0xffff;
    }

    // CPL El contenido del acumulador se invierte (Complemento a 1)
    public void cpl() {
        int a = leeA();
        a = ~a;// Invierte a
        escribeA(a);
        setNFlag();
        setHFlag();
    }

    // RRCA Rotación a la derecha del acumulador con copia al acarreo
    public void rrca() {
        int a = leeA();
        if ((a & 1) == 1) {
            setCFlag();
        } else {
            ceroCFlag();
        }
        a = a >> 1;
        if (getFlagC()) {
            a = a + 128;
        }
        escribeA(a);
        ceroHFlag();
        ceroNFlag();
        flagsXY(a);
    }

    // RLCA Rotación a la izquierda del acumulador con copia al acarreo
    public void rlca() {
        int a = leeA();
        if ((a & 128) == 128) {
            setCFlag();
        } else {
            ceroCFlag();
        }
        a = a << 1;
        a = a & 255;
        if (getFlagC()) {
            a = a & 255;
            a = a + 1;
        }
        escribeA(a);
        ceroHFlag();
        ceroNFlag();
        flagsXY(a);
    }

    // RRA Rotación a la derecha del acumulador a través del acarreo
    public void rra() {
        int a = leeA();
        boolean carry;
        if ((a & 1) == 1) {
            carry = true;
        } else {
            carry = false;
        }
        a = a >> 1;
        if (getFlagC()) {
            a = a + 128;
            ceroCFlag();
        }
        if (carry) {
            setCFlag();
        }
        escribeA(a);
        ceroHFlag();
        ceroNFlag();
        flagsXY(a);
    }

    // RLA Rotación a la izquierda del acumulador a través del acarreo
    public void rla() {
        int a = leeA();
        boolean carry;
        if ((a & 128) == 128) {
            carry = true;
        } else {
            carry = false;
        }
        a = a << 1;
        a = a & 255;
        if (getFlagC()) {
            a = a + 1;
            ceroCFlag();//Carry pasa a 0 cuando se desplaza el bit al registro
        }
        if (carry) {
            setCFlag();
        }
        escribeA(a);
        ceroHFlag();
        ceroNFlag();
        flagsXY(a);
    }

    // RRC Rotación a la derecha de s con copia al acarreo
    public void rrc(int registro) {
        int reg = 0;
        switch (registro) {
            case A:
                reg = leeA();
                break;
            case B:
                reg = leeB();
                break;
            case C:
                reg = leeC();
                break;
            case D:
                reg = leeD();
                break;
            case E:
                reg = leeE();
                break;
            case H:
                reg = leeH();
                break;
            case L:
                reg = leeL();
                break;
            case HL:
                reg = Memoria.lee(hl);
                break;
            case IX_D:
                reg = Memoria.lee((ix + (byte) desp) & 0xffff);
                break;
            case IY_D:
                reg = Memoria.lee((iy + (byte) desp) & 0xffff);
                break;
        }
        if ((reg & 1) == 1) {
            setCFlag();
        } else {
            ceroCFlag();
        }
        reg = reg >> 1;
        if (getFlagC()) {
            reg = reg + 128;
        }
        if (reg == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (reg > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        paridad(reg);// Modifica el Flag de paridad
        switch (registro) {
            case A:
                escribeA(reg);
                break;
            case B:
                escribeB(reg);
                break;
            case C:
                escribeC(reg);
                break;
            case D:
                escribeD(reg);
                break;
            case E:
                escribeE(reg);
                break;
            case H:
                escribeH(reg);
                break;
            case L:
                escribeL(reg);
                break;
            case HL:
                Memoria.escribe(hl, reg);
                break;
            case IX_D:
                Memoria.escribe((ix + (byte) desp) & 0xffff, reg);
                break;
            case IY_D:
                Memoria.escribe((iy + (byte) desp) & 0xffff, reg);
                break;
        }
        ceroHFlag();
        ceroNFlag();
    }

    // RLC Rotación a la izquierda de s con copia al acarreo
    public void rlc(int registro) {
        int reg = 0;
        switch (registro) {
            case A:
                reg = leeA();
                break;
            case B:
                reg = leeB();
                break;
            case C:
                reg = leeC();
                break;
            case D:
                reg = leeD();
                break;
            case E:
                reg = leeE();
                break;
            case H:
                reg = leeH();
                break;
            case L:
                reg = leeL();
                break;
            case HL:
                reg = Memoria.lee(hl);
                break;
            case IX_D:
                reg = Memoria.lee((ix + (byte) desp) & 0xffff);
                break;
            case IY_D:
                reg = Memoria.lee((iy + (byte) desp) & 0xffff);
                break;
        }
        if ((reg & 128) == 128) {
            setCFlag();
        } else {
            ceroCFlag();
        }
        reg = reg << 1;
        if (getFlagC()) {
            reg = reg & 255;
            reg = reg + 1;
        }
        if (reg == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (reg > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        paridad(reg);// Modifica el Flag de paridad
        switch (registro) {
            case A:
                escribeA(reg);
                break;
            case B:
                escribeB(reg);
                break;
            case C:
                escribeC(reg);
                break;
            case D:
                escribeD(reg);
                break;
            case E:
                escribeE(reg);
                break;
            case H:
                escribeH(reg);
                break;
            case L:
                escribeL(reg);
                break;
            case HL:
                Memoria.escribe(hl, reg);
                break;
            case IX_D:
                Memoria.escribe((ix + (byte) desp) & 0xffff, reg);
                break;
            case IY_D:
                Memoria.escribe((iy + (byte) desp) & 0xffff, reg);
                break;
        }
        ceroHFlag();
        ceroNFlag();
    }

    // RL Rotación a la izquierda de s a través del acarreo
    public void rl(int registro) {
        int reg = 0;
        switch (registro) {
            case A:
                reg = leeA();
                break;
            case B:
                reg = leeB();
                break;
            case C:
                reg = leeC();
                break;
            case D:
                reg = leeD();
                break;
            case E:
                reg = leeE();
                break;
            case H:
                reg = leeH();
                break;
            case L:
                reg = leeL();
                break;
            case HL:
                reg = Memoria.lee(hl);
                break;
            case IX_D:
                reg = Memoria.lee((ix + (byte) desp) & 0xffff);
                break;
            case IY_D:
                reg = Memoria.lee((iy + (byte) desp) & 0xffff);
                break;
        }
        boolean carry;
        if ((reg & 128) == 128) {
            carry = true;
        } else {
            carry = false;
        }
        reg = reg << 1;
        reg = reg & 255;
        if (getFlagC()) {
            reg = reg + 1;
            ceroCFlag();//Carry pasa a 0 cuando el bit se desplaza al registro
        }
        if (carry) {
            setCFlag();
        }
        if (reg == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (reg > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        paridad(reg);// Modifica el Flag de paridad
        switch (registro) {
            case A:
                escribeA(reg);
                break;
            case B:
                escribeB(reg);
                break;
            case C:
                escribeC(reg);
                break;
            case D:
                escribeD(reg);
                break;
            case E:
                escribeE(reg);
                break;
            case H:
                escribeH(reg);
                break;
            case L:
                escribeL(reg);
                break;
            case HL:
                Memoria.escribe(hl, reg);
                break;
            case IX_D:
                Memoria.escribe((ix + (byte) desp) & 0xffff, reg);
                break;
            case IY_D:
                Memoria.escribe((iy + (byte) desp) & 0xffff, reg);
                break;
        }
        ceroHFlag();
        ceroNFlag();
    }

    // RR Rotación a la derecha de s a través del acarreo
    public void rr(int registro) {
        int reg = 0;
        switch (registro) {
            case A:
                reg = leeA();
                break;
            case B:
                reg = leeB();
                break;
            case C:
                reg = leeC();
                break;
            case D:
                reg = leeD();
                break;
            case E:
                reg = leeE();
                break;
            case H:
                reg = leeH();
                break;
            case L:
                reg = leeL();
                break;
            case HL:
                reg = Memoria.lee(hl);
                break;
            case IX_D:
                reg = Memoria.lee((ix + (byte) desp) & 0xffff);
                break;
            case IY_D:
                reg = Memoria.lee((iy + (byte) desp) & 0xffff);
                break;
        }
        boolean carry;
        if ((reg & 1) == 1) {
            carry = true;
        } else {
            carry = false;
        }
        reg = reg >> 1;
        if (getFlagC()) {
            reg = reg + 128;
            ceroCFlag();//Se pone a cero carry cuando se desplaza el bit
        }
        if (carry) {
            setCFlag();
        }
        if (reg == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (reg > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        paridad(reg);// Modifica el Flag de paridad
        switch (registro) {
            case A:
                escribeA(reg);
                break;
            case B:
                escribeB(reg);
                break;
            case C:
                escribeC(reg);
                break;
            case D:
                escribeD(reg);
                break;
            case E:
                escribeE(reg);
                break;
            case H:
                escribeH(reg);
                break;
            case L:
                escribeL(reg);
                break;
            case HL:
                Memoria.escribe(hl, reg);
                break;
            case IX_D:
                Memoria.escribe((ix + (byte) desp) & 0xffff, reg);
                break;
            case IY_D:
                Memoria.escribe((iy + (byte) desp) & 0xffff, reg);
                break;
        }
        ceroHFlag();
        ceroNFlag();
    }

    // SLA Desplazamiento aritmético a la izquierda del operando s
    public void sla(int registro) {
        int reg = 0;
        switch (registro) {
            case A:
                reg = leeA();
                break;
            case B:
                reg = leeB();
                break;
            case C:
                reg = leeC();
                break;
            case D:
                reg = leeD();
                break;
            case E:
                reg = leeE();
                break;
            case H:
                reg = leeH();
                break;
            case L:
                reg = leeL();
                break;
            case HL:
                reg = Memoria.lee(hl);
                break;
            case IX_D:
                reg = Memoria.lee((ix + (byte) desp) & 0xffff);
                break;
            case IY_D:
                reg = Memoria.lee((iy + (byte) desp) & 0xffff);
                break;
        }
        if ((reg & 128) == 128) {
            setCFlag();
        } else {
            ceroCFlag();
        }
        reg = reg << 1;
        reg = reg & 255;
        reg = reg & 254;// Ponemos el bit 1 a 0
        if (reg == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (reg > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        paridad(reg);// Modifica el Flag de paridad
        switch (registro) {
            case A:
                escribeA(reg);
                break;
            case B:
                escribeB(reg);
                break;
            case C:
                escribeC(reg);
                break;
            case D:
                escribeD(reg);
                break;
            case E:
                escribeE(reg);
                break;
            case H:
                escribeH(reg);
                break;
            case L:
                escribeL(reg);
                break;
            case HL:
                Memoria.escribe(hl, reg);
                break;
            case IX_D:
                Memoria.escribe((ix + (byte) desp) & 0xffff, reg);
                break;
            case IY_D:
                Memoria.escribe((iy + (byte) desp) & 0xffff, reg);
                break;
        }
        ceroHFlag();
        ceroNFlag();
    }

    // SRA Desplazamiento aritmético a la derecha del operando s
    public void sra(int registro) {
        int reg = 0;
        switch (registro) {
            case A:
                reg = leeA();
                break;
            case B:
                reg = leeB();
                break;
            case C:
                reg = leeC();
                break;
            case D:
                reg = leeD();
                break;
            case E:
                reg = leeE();
                break;
            case H:
                reg = leeH();
                break;
            case L:
                reg = leeL();
                break;
            case HL:
                reg = Memoria.lee(hl);
                break;
            case IX_D:
                reg = Memoria.lee((ix + (byte) desp) & 0xffff);
                break;
            case IY_D:
                reg = Memoria.lee((iy + (byte) desp) & 0xffff);
                break;
        }
        boolean bit7 = false;
        if ((reg & 128) == 128) {
            bit7 = true;
        }
        if ((reg & 1) == 1) {
            setCFlag();
        } else {
            ceroCFlag();
        }
        reg = reg >> 1;
        if (bit7) {
            reg = reg + 128;
        }
        reg = reg & 255;
        if (reg == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (reg > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        paridad(reg);// Modifica el Flag de paridad
        switch (registro) {
            case A:
                escribeA(reg);
                break;
            case B:
                escribeB(reg);
                break;
            case C:
                escribeC(reg);
                break;
            case D:
                escribeD(reg);
                break;
            case E:
                escribeE(reg);
                break;
            case H:
                escribeH(reg);
                break;
            case L:
                escribeL(reg);
                break;
            case HL:
                Memoria.escribe(hl, reg);
                break;
            case IX_D:
                Memoria.escribe((ix + (byte) desp) & 0xffff, reg);
                break;
            case IY_D:
                Memoria.escribe((iy + (byte) desp) & 0xffff, reg);
                break;
        }
        ceroHFlag();
        ceroNFlag();
    }

    // SRL Desplazamiento lógico a la derecha de s
    public void srl(int registro) {
        int reg = 0;
        switch (registro) {
            case A:
                reg = leeA();
                break;
            case B:
                reg = leeB();
                break;
            case C:
                reg = leeC();
                break;
            case D:
                reg = leeD();
                break;
            case E:
                reg = leeE();
                break;
            case H:
                reg = leeH();
                break;
            case L:
                reg = leeL();
                break;
            case HL:
                reg = Memoria.lee(hl);
                break;
            case IX_D:
                reg = Memoria.lee((ix + (byte) desp) & 0xffff);
                break;
            case IY_D:
                reg = Memoria.lee((iy + (byte) desp) & 0xffff);
                break;
        }
        if ((reg & 1) == 1) {
            setCFlag();
        } else {
            ceroCFlag();
        }
        reg = reg >> 1;
        reg = reg & 255;
        if (reg == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (reg > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        paridad(reg);// Modifica el Flag de paridad
        switch (registro) {
            case A:
                escribeA(reg);
                break;
            case B:
                escribeB(reg);
                break;
            case C:
                escribeC(reg);
                break;
            case D:
                escribeD(reg);
                break;
            case E:
                escribeE(reg);
                break;
            case H:
                escribeH(reg);
                break;
            case L:
                escribeL(reg);
                break;
            case HL:
                Memoria.escribe(hl, reg);
                break;
            case IX_D:
                Memoria.escribe((ix + (byte) desp) & 0xffff, reg);
                break;
            case IY_D:
                Memoria.escribe((iy + (byte) desp) & 0xffff, reg);
                break;
        }
        ceroHFlag();
        ceroNFlag();
    }

    // SLL Desplazamiento lógico a la derecha de s
    public void sll(int registro) {
        int reg = 0;
        switch (registro) {
            case A:
                reg = leeA();
                break;
            case B:
                reg = leeB();
                break;
            case C:
                reg = leeC();
                break;
            case D:
                reg = leeD();
                break;
            case E:
                reg = leeE();
                break;
            case H:
                reg = leeH();
                break;
            case L:
                reg = leeL();
                break;
            case HL:
                reg = Memoria.lee(hl);
                break;
            case IX_D:
                reg = Memoria.lee((ix + (byte) desp) & 0xffff);
                break;
            case IY_D:
                reg = Memoria.lee((iy + (byte) desp) & 0xffff);
                break;
        }
        if ((reg & 128) == 128) {
            setCFlag();
        } else {
            ceroCFlag();
        }
        reg = reg << 1;
        reg++;
        reg = reg & 255;
        if (reg == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (reg > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        paridad(reg);// Modifica el Flag de paridad
        switch (registro) {
            case A:
                escribeA(reg);
                break;
            case B:
                escribeB(reg);
                break;
            case C:
                escribeC(reg);
                break;
            case D:
                escribeD(reg);
                break;
            case E:
                escribeE(reg);
                break;
            case H:
                escribeH(reg);
                break;
            case L:
                escribeL(reg);
                break;
            case HL:
                Memoria.escribe(hl, reg);
                break;
            case IX_D:
                Memoria.escribe((ix + (byte) desp) & 0xffff, reg);
                break;
            case IY_D:
                Memoria.escribe((iy + (byte) desp) & 0xffff, reg);
                break;
        }
        ceroHFlag();
        ceroNFlag();
    }

    // Verifica el bit b del registro r
    public void bit(int bit, int registro) {
        int reg = 0;
        switch (registro) {
            case A:
                reg = leeA();
                break;
            case B:
                reg = leeB();
                break;
            case C:
                reg = leeC();
                break;
            case D:
                reg = leeD();
                break;
            case E:
                reg = leeE();
                break;
            case H:
                reg = leeH();
                break;
            case L:
                reg = leeL();
                break;
            case HL:
                reg = Memoria.lee(hl);
                break;
            case IX_D:
                reg = Memoria.lee((ix + (byte) desp) & 0xffff);
                break;
            case IY_D:
                reg = Memoria.lee((iy + (byte) desp) & 0xffff);
                break;
        }
        /*int num=1;
        for(int n=0;n<bit;n++){
            num=num*2;
        }
        boolean resultado=((reg&num)!=num);
        if(resultado){
            setZFlag();
            setPVFlag();
        }else{
            ceroZFlag();
            ceroPVFlag();
        }*/
        flagsXY(reg);
        for (int n = 0; n < bit; n++) {
            reg = reg >> 1;
        }
        reg = reg & 1;
        if (reg == 0) {
            setZFlag();
            setPVFlag();//Flag P/V efecto no documentado manual Sean Young
        } else {
            ceroZFlag();
            ceroPVFlag();//Flag P/V efecto no ducumentado manual Sean Young
        }
        if (bit == 7 && !getFlagZ()) {//Si el bit seleccionado es 7 y esta a "1" el flag S se pone a "1" --> manual Seang Young
            setSFlag();
        } else {
            ceroSFlag();
        }
        setHFlag();
        ceroNFlag();
    }

    // Poner a 0 el bit del operando s
    public void res(int bit, int registro) {
        int reg = 0;
        switch (registro) {
            case A:
                reg = leeA();
                break;
            case B:
                reg = leeB();
                break;
            case C:
                reg = leeC();
                break;
            case D:
                reg = leeD();
                break;
            case E:
                reg = leeE();
                break;
            case H:
                reg = leeH();
                break;
            case L:
                reg = leeL();
                break;
            case HL:
                reg = Memoria.lee(hl);
                break;
            case IX_D:
                reg = Memoria.lee((ix + (byte) desp) & 0xffff);
                break;
            case IY_D:
                reg = Memoria.lee((iy + (byte) desp) & 0xffff);
                break;
        }
        int aux = 1;
        for (int n = 0; n < bit; n++) {
            aux = aux << 1;
        }
        //aux = aux ^ 255;
        aux = ~aux;
        reg = reg & aux;
        switch (registro) {
            case A:
                escribeA(reg);
                break;
            case B:
                escribeB(reg);
                break;
            case C:
                escribeC(reg);
                break;
            case D:
                escribeD(reg);
                break;
            case E:
                escribeE(reg);
                break;
            case H:
                escribeH(reg);
                break;
            case L:
                escribeL(reg);
                break;
            case HL:
                Memoria.escribe(hl, reg);
                break;
            case IX_D:
                Memoria.escribe((ix + (byte) desp) & 0xffff, reg);
                break;
            case IY_D:
                Memoria.escribe((iy + (byte) desp) & 0xffff, reg);
                break;
        }
    }

    // Activa a 1 el bit del operando s
    public void set(int bit, int registro) {
        int reg = 0;
        switch (registro) {
            case A:
                reg = leeA();
                break;
            case B:
                reg = leeB();
                break;
            case C:
                reg = leeC();
                break;
            case D:
                reg = leeD();
                break;
            case E:
                reg = leeE();
                break;
            case H:
                reg = leeH();
                break;
            case L:
                reg = leeL();
                break;
            case HL:
                reg = Memoria.lee(hl);
                break;
            case IX_D:
                reg = Memoria.lee((ix + (byte) desp) & 0xffff);
                break;
            case IY_D:
                reg = Memoria.lee((iy + (byte) desp) & 0xffff);
                break;
        }
        int aux = 1;
        for (int n = 0; n < bit; n++) {
            aux = aux << 1;
        }
        reg = reg | aux;
        switch (registro) {
            case A:
                escribeA(reg);
                break;
            case B:
                escribeB(reg);
                break;
            case C:
                escribeC(reg);
                break;
            case D:
                escribeD(reg);
                break;
            case E:
                escribeE(reg);
                break;
            case H:
                escribeH(reg);
                break;
            case L:
                escribeL(reg);
                break;
            case HL:
                Memoria.escribe(hl, reg);
                break;
            case IX_D:
                Memoria.escribe((ix + (byte) desp) & 0xffff, reg);
                break;
            case IY_D:
                Memoria.escribe((iy + (byte) desp) & 0xffff, reg);
                break;
        }
    }

    // Carga el acumulador a partir del puerto indicado
    public void inAnn() {
        int puerto = Memoria.lee(pc);// Se carga el número de puerto
        incPC();
        escribeA0A7(puerto);
        escribeA8A15(leeA());
        //Direccionamiento teclado ZX-Spectrum
        //if (true) {//Port 254 o Port251-->if (puerto==0xfe || puerto==251){
        if (puerto == 0xfe || puerto == 0xfb) {
            //int reg = 0xff;
            int reg = (byte) -65;
            if ((leeA() & 1) == 0) {
                reg &= LeeTeclas.KROW0;
            }
            if ((leeA() & 2) == 0) {
                reg &= LeeTeclas.KROW1;
            }
            if ((leeA() & 4) == 0) {
                reg &= LeeTeclas.KROW2;
            }
            if ((leeA() & 8) == 0) {
                reg &= LeeTeclas.KROW3;
            }
            if ((leeA() & 16) == 0) {
                reg &= LeeTeclas.KROW4;
            }
            if ((leeA() & 32) == 0) {
                reg &= LeeTeclas.KROW5;
            }
            if ((leeA() & 64) == 0) {
                reg &= LeeTeclas.KROW6;
            }
            if ((leeA() & 128) == 0) {
                reg &= LeeTeclas.KROW7;
            }
            escribeA(reg);
        } else {
            escribeA(leeD0D7());// Escribe A con el valor del puerto
        }
        //Bus flotante-->Falta implementar correctamente
        //Provisionalmente programado de esta manera para ver como funciona el bus flotante
        //Pero funcionan los juegos como Arcanoid, Cobra, Terra Cresta, etc...
        if (puerto == 0xff) {
            if (Spectrum.pantULA) {
                escribeA(0);
            } else {
                escribeA(0xff);
            }
        }
        if (puerto == 31) {//Joystick Kempston
            escribeA(LeeTeclas.kempston);
        }
    }

    // Salida del acumulador al puerto indicado
    public void outNnA() {
        int puerto = Memoria.lee(pc);// Se carga el número de puerto
        incPC();
        escribeA0A7(puerto);
        escribeA8A15(leeA());
        escribeD0D7(leeA());// El contenido de A pasa a port
        //Envía el color del borde a la Pantalla
        if (puerto == 0xfe) {
            pantalla.setBorde(leeA());
            int buz = 0;//Buzzer
            int mic = 0;//Mic
            if ((leeA() & 16) == 16) {//Puerto 254, BUZ(bit 4)+MIC(bit3)=BUZZER
                //Se envía un 1 al altavoz
                buz = 1;
            } else {
                //Se envía un 0 al altavoz
                buz = 0;
            }
            if ((leeA() & 8) == 8) {
                mic = 1;
            } else {
                mic = 0;
            }
            sonido.setValor(buz, mic);
        }
    }

    // Carga el registro R a partir del puerto C
    public void inRC(int registro) {
        int reg;
        escribeA0A7(leeC());// Se carga el valor del puerto byte L
        escribeA8A15(leeB());// Se carga el valor del puerto byte H
        //reg = leeD0D7();// El contenido del puerto pasa a reg
        //Direccionamiento teclado ZX-Spectrum
        reg = (byte) -65;
        if (leeC() == 0xfe) {//Port 254
            //reg =(byte)-65;//191
            if ((leeB() & 1) == 0) {
                reg &= LeeTeclas.KROW0;
            }
            if ((leeB() & 2) == 0) {
                reg &= LeeTeclas.KROW1;
            }
            if ((leeB() & 4) == 0) {
                reg &= LeeTeclas.KROW2;
            }
            if ((leeB() & 8) == 0) {
                reg &= LeeTeclas.KROW3;
            }
            if ((leeB() & 16) == 0) {
                reg &= LeeTeclas.KROW4;
            }
            if ((leeB() & 32) == 0) {
                reg &= LeeTeclas.KROW5;
            }
            if ((leeB() & 64) == 0) {
                reg &= LeeTeclas.KROW6;
            }
            if ((leeB() & 128) == 0) {
                reg &= LeeTeclas.KROW7;
            }
        }
        //Bus flotante-->Falta implementar correctamente
        //Provisionalmente programado de esta manera para ver como funciona el bus flotante
        //Pero funcionan los juegos como Arcanoid, Cobra, Terra Cresta, etc...
        if (leeC() == 0xff) {
            if (Spectrum.pantULA) {
                reg = 0;
            } else {
                reg = 0xff;
            }
        }
        if (leeC() == 31) {//Joystick Kempston
            reg = LeeTeclas.kempston;
        }
        switch (registro) {
            case A:
                escribeA(reg);
                break;
            case B:
                escribeB(reg);
                break;
            case C:
                escribeC(reg);
                break;
            case D:
                escribeD(reg);
                break;
            case E:
                escribeE(reg);
                break;
            case H:
                escribeH(reg);
                break;
            case L:
                escribeL(reg);
                break;
        }
        if (reg == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (reg > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        paridad(reg);
        ceroHFlag();
        ceroNFlag();
    }

    // Salida del registro R al puerto C
    public void outCR(int registro) {
        escribeA0A7(leeC());// Se carga el valor del puerto byte L
        escribeA8A15(leeB());// Se carga el valor del puerto byte H
        switch (registro) {
            case A:
                escribeD0D7(leeA());
                if (leeC() == 0xfe) {//Cambia el color del borde
                    pantalla.setBorde(leeA());;//con la instrucción OUT de Basic
                    int buz = 0;//Buzzer
                    int mic = 0;//Mic
                    if ((leeA() & 16) == 16) {//Puerto 254, BUZ(bit 4)+MIC(bit3)=BUZZER
                        //Se envía un 1 al altavoz
                        buz = 1;
                    } else {
                        //Se envía un 0 al altavoz
                        buz = 0;
                    }
                    if ((leeA() & 8) == 8) {
                        mic = 1;
                    } else {
                        mic = 0;
                    }
                    sonido.setValor(buz, mic);
                }
                break;
            case B:
                escribeD0D7(leeB());
                break;
            case C:
                escribeD0D7(leeC());
                break;
            case D:
                escribeD0D7(leeD());
                break;
            case E:
                escribeD0D7(leeE());
                break;
            case H:
                escribeD0D7(leeH());
                break;
            case L:
                escribeD0D7(leeL());
                break;
        }
    }

    // Realiza la suma con el registro A y modifica los flags
    public void sumaAFlags(int valor, boolean acarreo) {
        int regA = leeA();
        //Guarda el valor de los  registro A y el flag C
        int regTemp = regA;
        int flagCTemp = leeF() & FLAG_C;
        // suma el registro y modifica los flags
        regA = regA + valor;
        if (getFlagC() && acarreo) {
            regA++;// Suma Carry
        }
        if (regA > 255) {// Flag CARRY
            setCFlag();
        } else {
            ceroCFlag();
        }
        regA = regA & 0xFF;
        escribeA(regA);
        if (regA == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (regA > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        if (acarreo) {
            if ((((regTemp & 0xf) + (valor & 0xf)) + (flagCTemp) & FLAG_H) == FLAG_H) {//Flag H
                setHFlag();
            } else {
                ceroHFlag();
            }
        } else {
            if ((((regTemp & 0xf) + (valor & 0xf)) & FLAG_H) == FLAG_H) {//Flag H
                setHFlag();
            } else {
                ceroHFlag();
            }
        }
        if (acarreo) {
            if (((regTemp <= 127) && (valor <= 127) && (((regTemp + valor + (flagCTemp)) & 0xff) > 127)) || ((regTemp > 127) && (valor > 127) && (((regTemp + valor + (flagCTemp) & 0xff)) <= 127))) {// Flag P/V Paridad/Desbordamiento
                setPVFlag();
            } else {
                ceroPVFlag();
            }
        } else {
            if (((regTemp <= 127) && (valor <= 127) && (((regTemp + valor) & 0xff) > 127)) | ((regTemp > 127) && (valor > 127) && (((regTemp + valor) & 0xff) <= 127))) {// Flag P/V Paridad/Desbordamiento
                setPVFlag();
            } else {
                ceroPVFlag();
            }
        }
        flagsXY(regA);
        ceroNFlag();
    }

    // Realiza la resta con el registro A y modifica los flags
    public void restaAFlags(int valor, boolean acarreo) {
        int regA = leeA();
        //Memoriza el valor del registro A
        int regAmemo = regA;
        if (getFlagC() && acarreo) {
            regA--;// Resta Carry
        }
        // Resta el registro y modifica los flags
        regA = regA - valor;
        if (regA < 0) {
            setCFlag();
        } else {
            ceroCFlag();
        }
        regA = regA & 0xff;
        escribeA(regA);
        if (regA == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (regA > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        if ((regAmemo & 0xF) < (regA & 0xF)) {// Flag H Bandera de acarreo mitad
            setHFlag();
        } else {
            ceroHFlag();
        }
        if (((regAmemo > 127) && (regA <= 127) && ((regAmemo - regA) < 128)) || ((regAmemo <= 127) && (regA > 127) && ((regAmemo - regA) >= -128))) {// Flag P/V Paridad/Desbordamiento
            setPVFlag();
        } else {
            ceroPVFlag();
        }
        flagsXY(regA);//Actualiza los Flags X e Y
        setNFlag();
    }

    // Compara el registro A con el dato y modifica los Flags
    public void cpFlags(int reg) {
        int regA = leeA();
        //Memoriza el valor del registro A
        int regAmemo = regA;
        // Resta Reg de regA y modifica los flags
        regA = regA - reg;
        if (regA < 0) {
            //regA = regA + 256;
            setCFlag();
        } else {
            ceroCFlag();
        }
        regA = regA & 0xff;
        if (regA == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (regA > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        if ((regAmemo & 0xF) < (regA & 0xF)) {// Flag H Bandera de acarreo mitad
            setHFlag();
        } else {
            ceroHFlag();
        }
        if (((regAmemo > 127) && (reg <= 127) && ((regAmemo - reg) < 128)) || ((regAmemo <= 127) && (reg > 127) && ((regAmemo - reg) >= -128))) {// Flag P/V Paridad/Desbordamiento    
            setPVFlag();
        } else {
            ceroPVFlag();
        }
        flagsXY(regA);
        setNFlag();
    }

    // Carga el par de registros DD a partir de las posiciones de memoria
    // direccionadas por NN
    public void ldDDNN(int reg) {
        int regLL = Memoria.lee(pc);
        incPC();
        int regHH = Memoria.lee(pc);
        incPC();
        int direccion = (regHH * 256) + regLL;
        switch (reg) {
            case BC:
                escribeC(Memoria.lee(direccion));
                escribeB(Memoria.lee((direccion + 1) & 0xffff));
                break;
            case DE:
                escribeE(Memoria.lee(direccion));
                escribeD(Memoria.lee((direccion + 1) & 0xffff));
                break;
            case HL:
                escribeL(Memoria.lee(direccion));
                escribeH(Memoria.lee((direccion + 1) & 0xffff));
                break;
            case SP:
                escribeP(Memoria.lee(direccion));
                escribeS(Memoria.lee((direccion + 1) & 0xffff));
                break;
        }
    }

    // Carga las posiciones de memoria direccionadas por NN a partir del par de
    // registros DD
    public void ldNNDD(int reg) {
        int regLL = Memoria.lee(pc);
        incPC();
        int regHH = Memoria.lee(pc);
        incPC();
        int direccion = (regHH * 256) + regLL;
        switch (reg) {
            case BC:
                Memoria.escribe(direccion, leeC());
                Memoria.escribe((direccion + 1) & 0xffff, leeB());
                break;
            case DE:
                Memoria.escribe(direccion, leeE());
                Memoria.escribe((direccion + 1) & 0xffff, leeD());
                break;
            case HL:
                Memoria.escribe(direccion, leeL());
                Memoria.escribe((direccion + 1) & 0xffff, leeH());
                break;
            case SP:
                Memoria.escribe(direccion, leeP());
                Memoria.escribe((direccion + 1) & 0xffff, leeS());
                break;
        }
    }

    // Retorno de una interrupción no enmascarable
    public void retN() {
        escribePC_L(Memoria.lee(sp));
        incSp();
        escribePC_H(Memoria.lee(sp));
        incSp();
        IFF1 = IFF2;// Restaura el IFF
    }

    // Retorno de interrupción enmascarable
    public void retI() {
        // Restaura el contador de programa
        escribePC_L(Memoria.lee(sp));
        incSp();
        escribePC_H(Memoria.lee(sp));
        incSp();
    }

    // Activa el modo de interrupción 0
    public void setIM0() {
        IMFa = false;
        IMFb = false;
    }

    // Activa el modo de interrupción 1
    public void setIM1() {
        IMFa = true;
        IMFb = false;
    }

    // Activa el modo de interrupción 2
    public void setIM2() {
        IMFa = true;
        IMFb = true;
    }

    // Habilita el biestable de interrupciones, permite que se produzcan
    // interrupciones enmascarables
    public void setIFF() {
        IFF1 = true;
        IFF2 = true;
    }

    // Carga el acumulador a partir del registro vector de interrupciones I
    public void ldAI() {
        if (IFF2) {
            ceroPVFlag();
        } else {
            setPVFlag();
        }
        if (i == 0) {// Flag Z
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (i > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        escribeA(i);
        ceroNFlag();
        ceroHFlag();
    }

    // Carga el acumulador a partir del registro de refresco de memoria R
    public void ldAR() {
        if (IFF2) {
            setPVFlag();
        } else {
            ceroPVFlag();
        }
        if (r == 0) {// Flag Z
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (r > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        escribeA(r);
        ceroNFlag();
        ceroHFlag();
    }

    // Carga el registro vector de interrupciones a partir del acumulador
    public void ldIA() {
        i = leeA();
    }

    // Carga el registro de refresco de memoria R a partir del acumulador
    public void ldRA() {
        r = leeA();
    }

    // Negativiza el acumulador
    public void neg() {
        int reg = leeA();
        // Guarda el valor del registro A en un registro temporal
        int regTemp = reg;
        reg = -reg & 0xff;
        escribeA(reg);
        if (regTemp != 0x00) {// C se activa si A era distinto de 0 antes de NEG
            setCFlag();
        } else {
            ceroCFlag();
        }
        if (regTemp == 0x80) {// P se activa si A era 80H antes de NEG
            setPVFlag();
        } else {
            ceroPVFlag();
        }
        if (reg == 0) {// Flag Z
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (reg > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        if (((reg & 0xf) + (regTemp & 0xf)) == 16) {//Flag H
            setHFlag();
        } else {
            ceroHFlag();
        }
        setNFlag();
        flagsXY(reg);
    }

    // Carga de bloque con incremento
    public void ldi() {
        ceroHFlag();
        ceroNFlag();
        Memoria.escribe(de, Memoria.lee(hl));
        incDE();
        incHL();
        decBC();
        if (bc == 0) {
            ceroPVFlag();
        } else {
            setPVFlag();
        }
    }

    // Carga de bloque con incremento repetida
    public void ldir() {
        ceroHFlag();
        ceroNFlag();
        ceroPVFlag();
        /*do {
            Memoria.escribe(de, Memoria.lee(hl));
            incDE();
            incHL();
            decBC();
        } while (bc > 0);*/
        Memoria.escribe(de, Memoria.lee(hl));
        incDE();
        incHL();
        decBC();
        if (bc != 0) {
            pc = pc - 2;
        } else {
            tStates -= 5;
        }
    }

    // Comparación de bloques con incremento
    public void cpir() {
        setNFlag();
        int regAmemo = leeA();
        int resta;
        /*do {
            resta = leeA() - Memoria.lee(hl);
            if (resta < 0) {
                resta = resta + 256;
            }
            incHL();
            decBC();
        } while (bc > 0 && resta != 0);*/
        resta = leeA() - Memoria.lee(hl);
        if (resta < 0) {
            resta = resta + 256;
        }
        incHL();
        decBC();
        if (bc != 0 && resta != 0) {
            pc = pc - 2;
        } else {
            tStates -= 5;
        }
        if (bc == 0) {
            ceroPVFlag();
        } else {
            setPVFlag();
        }
        if (resta == 0) {
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (resta > 127) {
            setSFlag();
        } else {
            ceroSFlag();
        }
        if ((regAmemo & 0xF) < (resta & 0xF)) {// Flag H Bandera de acarreo mitad
            setHFlag();
        } else {
            ceroHFlag();
        }
    }

    // Comparación con incremento
    public void cpi() {
        int regAmemo = leeA();
        int resta;
        resta = leeA() - Memoria.lee(hl);
        if (resta < 0) {
            resta = resta + 256;
        }
        incHL();
        decBC();
        if (bc == 0) {
            ceroPVFlag();
        } else {
            setPVFlag();
        }
        if (resta == 0) {
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (resta > 127) {
            setSFlag();
        } else {
            ceroSFlag();
        }
        if ((regAmemo & 0xF) < (resta & 0xF)) {// Flag H Bandera de acarreo mitad
            setHFlag();
        } else {
            ceroHFlag();
        }
        setNFlag();
    }

    // Entrada con incremento
    public void ini() {
        escribeA0A7(leeC());// Se carga el valor del puerto byte L
        escribeA8A15(leeB());// Se carga el valor del puerto byte H
        Memoria.escribe(hl, (byte) -65);// El contenido del puerto pasa a la posición de memoria que indica HL
        dec(B);
        incHL();
        if (leeB() == 0) {
            setZFlag();
        } else {
            ceroZFlag();
        }
        setNFlag();
    }

    // Entrada de bloque con incremento
    public void inir() {
        /*do {
            escribeA0A7(leeC());// Se carga el valor del puerto byte L
            escribeA8A15(leeB());// Se carga el valor del puerto byte H
            Memoria.escribe(hl, leeD0D7());// El contenido del puerto pasa a la posición de memoria que indica HL
            dec(B);
            incHL();
        } while (leeB() != 0);*/
        escribeA0A7(leeC());// Se carga el valor del puerto byte L
        escribeA8A15(leeB());// Se carga el valor del puerto byte H
        Memoria.escribe(hl, (byte) -65);// El contenido del puerto pasa a la posición de memoria que indica HL
        dec(B);
        incHL();
        if (leeB() != 0) {
            pc = pc - 2;
        } else {
            tStates -= 5;
        }
        setZFlag();
        setNFlag();
    }

    // Salida de bloque con incremento
    public void otir() {
        setNFlag();
        /*do {
            escribeA0A7(leeC());// Se carga el valor del puerto byte L
            escribeA8A15(leeB());// Se carga el valor del puerto byte H
            escribeD0D7(Memoria.lee(hl));// El contenido de HL se lleva al puerto de salida
            dec(B);
            incHL();
        } while (leeB() != 0);*/
        escribeA0A7(leeC());// Se carga el valor del puerto byte L
        escribeA8A15(leeB());// Se carga el valor del puerto byte H
        escribeD0D7(Memoria.lee(hl));// El contenido de HL se lleva al puerto de salida
        dec(B);
        incHL();
        if (leeB() != 0) {
            pc = pc - 2;
        } else {
            tStates -= 5;
        }
        setZFlag();
    }

    // Salida con incremento
    public void outi() {
        escribeA0A7(leeC());// Se carga el valor del puerto byte L
        escribeA8A15(leeB());// Se carga el valor del puerto byte H
        escribeD0D7(Memoria.lee(hl));// El contenido de HL se lleva al puerto de salida
        dec(B);
        incHL();
        if (leeB() == 0) {
            setZFlag();
        } else {
            ceroZFlag();
        }
        setNFlag();
    }

    // Salida con decremento
    public void outd() {
        escribeA0A7(leeC());// Se carga el valor del puerto byte L
        escribeA8A15(leeB());// Se carga el valor del puerto byte H
        escribeD0D7(Memoria.lee(hl));// El contenido de HL se lleva al puerto de salida
        dec(B);
        decHL();
        if (leeB() == 0) {
            setZFlag();
        } else {
            ceroZFlag();
        }
        setNFlag();
    }

    // Salida de bloque con incremento
    public void otdr() {
        /*do {
            escribeA0A7(leeC());// Se carga el valor del puerto byte L
            escribeA8A15(leeB());// Se carga el valor del puerto byte H
            escribeD0D7(Memoria.lee(hl));// El contenido de HL se lleva al puerto de salida
            dec(B);
            decHL();
        } while (leeB() != 0);*/
        escribeA0A7(leeC());// Se carga el valor del puerto byte L
        escribeA8A15(leeB());// Se carga el valor del puerto byte H
        escribeD0D7(Memoria.lee(hl));// El contenido de HL se lleva al puerto de salida
        dec(B);
        decHL();
        if (leeB() != 0) {
            pc = pc - 2;
        } else {
            tStates -= 5;
        }
        setNFlag();
        setZFlag();
    }

    // Entrada de bloque con decrento
    public void indr() {
        /*do {
            escribeA0A7(leeC());// Se carga el valor del puerto byte L
            escribeA8A15(leeB());// Se carga el valor del puerto byte H
            Memoria.escribe(hl, leeD0D7());// El contenido del puerto pasa a la posición de memoria que indica HL
            dec(B);
            decHL();
        } while (leeB() != 0);*/
        escribeA0A7(leeC());// Se carga el valor del puerto byte L
        escribeA8A15(leeB());// Se carga el valor del puerto byte H
        Memoria.escribe(hl, (byte) -65);// El contenido del puerto pasa a la posición de memoria que indica HL
        dec(B);
        decHL();
        if (leeB() != 0) {
            pc = pc - 2;
        } else {
            tStates -= 5;
        }
        setZFlag();
        setNFlag();
    }

    // Entrada con decremento
    public void ind() {
        escribeA0A7(leeC());// Se carga el valor del puerto byte L
        escribeA8A15(leeB());// Se carga el valor del puerto byte H
        Memoria.escribe(hl, (byte) -65);// El contenido del puerto pasa a la posición de memoria que indica HL
        dec(B);
        decHL();
        if (leeB() == 0) {
            setZFlag();
        } else {
            ceroZFlag();
        }
        setNFlag();
    }

    // Carga de bloque con decremento
    public void ldr() {
        ceroHFlag();
        ceroNFlag();
        Memoria.escribe(de, Memoria.lee(hl));
        decDE();
        decHL();
        decBC();
        if (bc == 0) {
            ceroPVFlag();
        } else {
            setPVFlag();
        }
    }

    // Carga de bloque con decremento repetida
    public void lddr() {
        ceroHFlag();
        ceroNFlag();
        ceroPVFlag();
        /*do {
            Memoria.escribe(de, Memoria.lee(hl));
            decDE();
            decHL();
            decBC();
        } while (bc > 0);*/
        Memoria.escribe(de, Memoria.lee(hl));
        decDE();
        decHL();
        decBC();
        if (bc != 0) {
            pc = pc - 2;
        } else {
            tStates -= 5;
        }

    }

    // Comparación con decremento
    public void cpd() {
        int regAmemo = leeA();
        int resta;
        resta = leeA() - Memoria.lee(hl);
        if (resta < 0) {
            resta = resta + 256;
        }
        decHL();
        decBC();
        if (bc == 0) {
            ceroPVFlag();
        } else {
            setPVFlag();
        }
        if (resta == 0) {
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (resta > 127) {
            setSFlag();
        } else {
            ceroSFlag();
        }
        if ((regAmemo & 0xF) < (resta & 0xF)) {// Flag H Bandera de acarreo mitad
            setHFlag();
        } else {
            ceroHFlag();
        }
        setNFlag();
    }

    // Comparación de bloques con decremento
    public void cpdr() {
        int regAmemo = leeA();
        int resta;
        /*do {
            resta = leeA() - Memoria.lee(hl);
            if (resta < 0) {
                resta = resta + 256;
            }
            decHL();
            decBC();
        } while (bc > 0 && resta != 0);*/
        resta = leeA() - Memoria.lee(hl);
        if (resta < 0) {
            resta = resta + 256;
        }
        decHL();
        decBC();
        if (bc != 0 && resta != 0) {
            pc = pc - 2;
        } else {
            tStates -= 5;
        }
        if (bc == 0) {
            ceroPVFlag();
        } else {
            setPVFlag();
        }
        if (resta == 0) {
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (resta > 127) {
            setSFlag();
        } else {
            ceroSFlag();
        }
        if ((regAmemo & 0xF) < (resta & 0xF)) {// Flag H Bandera de acarreo mitad
            setHFlag();
        } else {
            ceroHFlag();
        }
        setNFlag();
    }

    // Rotación decimal a la derecha
    public void rrd() {
        int byteHL = Memoria.lee(hl);
        int byteA = leeA();
        int byteAL = byteA & 0b00001111;
        int byteAH = byteA & 0b11110000;
        int byteHLL = byteHL & 0b00001111;
        int byteHLH = byteHL & 0b11110000;
        byteHLH = byteHLH >> 4;
        byteAL = byteAL << 4;
        byteHL = byteHLH | byteAL;
        byteA = byteAH | byteHLL;
        escribeA(byteA);
        Memoria.escribe(hl, byteHL);
        paridad(byteA);
        if (byteA == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (byteA > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        ceroNFlag();
        ceroHFlag();
    }

    // Rotación decimal a la izquierda
    public void rld() {
        int byteHL = Memoria.lee(hl);
        int byteA = leeA();
        int byteAL = byteA & 0b00001111;
        int byteAH = byteA & 0b11110000;
        int byteHLL = byteHL & 0b00001111;
        int byteHLH = byteHL & 0b11110000;
        byteHLL = byteHLL << 4;
        byteHL = byteHLL | byteAL;
        byteHLH = byteHLH >> 4;
        byteA = byteAH | byteHLH;
        escribeA(byteA);
        Memoria.escribe(hl, byteHL);
        paridad(byteA);
        if (byteA == 0) {// Flag CERO
            setZFlag();
        } else {
            ceroZFlag();
        }
        if (byteA > 127) {// Flag SIGNO
            setSFlag();
        } else {
            ceroSFlag();
        }
        ceroNFlag();
        ceroHFlag();
    }

    // //Ajuste decimal 
    public void daa() {
        int reg = leeA();
        int ln = reg & 0x0f;
        int hn = reg & 0xf0;
        hn = hn >> 4;
        int diff = 0;
        if (getFlagH() || ln > 0x9) {
            diff += 0x06;
        }
        if (getFlagC()) {
            diff += 0x60;
        } else {
            if ((hn > 0x8 && ln > 0x09) || hn > 9) {
                diff += 0x60;
            }
        }
        if (getFlagN()) {
            diff = -diff;
        }
        if ((hn > 0x8 && ln > 0x9) || (hn > 0x9 && ln < 0x0A) || (getFlagC())) {
            setCFlag();
        } else {
            ceroCFlag();
        }

        if ((!getFlagN() && ln > 0x9) || (getFlagN() && getFlagH() && ln < 0x6)) {
            setHFlag();
        } else {
            ceroHFlag();
        }
        escribeA(reg + diff);
        if (leeA() > 127) {
            setSFlag();
        } else {
            ceroSFlag();
        }
        if (leeA() == 0) {
            setZFlag();
        } else {
            ceroZFlag();
        }
        paridad(leeA());
        flagsXY(leeA());
    }

    // Detiene la CPU
    public void halt() {
        halt = false;
    }

    //Pone en marcha la CPU
    public void noHalt() {
        halt = true;
    }

    // Carga de bloque con decremento
    public void ldd() {
        Memoria.escribe(de, Memoria.lee(hl));
        decDE();
        decHL();
        decBC();
        if (bc == 0) {
            ceroPVFlag();
        } else {
            setPVFlag();
        }
        ceroHFlag();
        ceroNFlag();
    }

    // Incrementa el registro de refresco de memorias R
    public void incR() {
        int rMemo = r;
        r = r + 1 & 0x7f;//Cuenta de 0 a 127 sin alterar el bit número 7
        r |= rMemo & 0x80;
    }

    //Actualiza los Flags X e Y
    public void flagsXY(int reg) {
        if ((reg & 8) == 8) {
            setXFlag();
        } else {
            ceroXFlag();
        }
        if ((reg & 32) == 32) {
            setYFlag();
        } else {
            ceroYFlag();
        }
    }

    //Petición de interrupción no enmascarable
    public void nmi() {
        decSp();
        Memoria.escribe(sp, leePC_H());
        decSp();
        Memoria.escribe(sp, leePC_L());
        IFF2 = IFF1;//Salvaguarda IFF
        IFF1 = false;//Reinicia IFF
        pc = 0x66;
        halt = true;//si HALT está activo esperando una interrupción, HALT se pone a true para poner en marcha el procesador
        tStates += 11;
        incR();
    }

    //Petición de interrupción enmascarable
    public void Int() {
        if (IFF1) {
            IFF1 = false;//Ignorar las
            IFF2 = false;//interrupciones
            // IMFa=0 IMFb=0 Interrupción modo 0
            if (!IMFa && !IMFb) {
                decSp();
                Memoria.escribe(sp, leePC_H());
                decSp();
                Memoria.escribe(sp, leePC_L());
                pc = 0x38;
                tStates += 13;
            }
            // IMFa=1 IMFb=0 Interrupción modo 1
            if (IMFa && !IMFb) {
                decSp();
                Memoria.escribe(sp, leePC_H());
                decSp();
                Memoria.escribe(sp, leePC_L());
                pc = 0x38;
                tStates += 13;
            }
            // IMFa=1 IMFb=1 Interrupción modo 2
            if (IMFa && IMFb) {
                decSp();
                Memoria.escribe(sp, leePC_H());
                decSp();
                Memoria.escribe(sp, leePC_L());
                int direccion = (leeI() * 256) + 255;
                escribePC_L(Memoria.lee(direccion));
                escribePC_H(Memoria.lee((direccion + 1) & 0xffff));
                tStates += 19;
            }
        }
        halt = true;//si HALT está activo esperando una interrupción, HALT se pone a true para poner en marcha el procesador
        incR();
    }

    //Complementacion de la bandera de acarro
    public void ccf() {
        if (getFlagC()) {
            ceroCFlag();
        } else {
            setCFlag();
        }
        ceroNFlag();
    }

    //Pone a 1 la bandera de acarreo
    public void scf() {
        setCFlag();
        ceroHFlag();
        ceroNFlag();
    }

    //Apunta al objeto Cargar
    public void setCargar(Cargar cargar) {
        this.cargar = cargar;
    }

    //Reset Z-80
    public void resetZ80() {
        // Set de registros principales
        af = 0;
        bc = 0;
        de = 0;
        hl = 0;
        // Set de registros alternativos
        af_ = 0;
        bc_ = 0;
        de_ = 0;
        hl_ = 0;
        // Set de registros de propósito general
        i = 0;// Vector de interrupciones
        r = 0;// Registro de refresco de memoria
        ix = 0;
        iy = 0;
        sp = 0xffff;
        pc = 0;
        // Registro de instrucción
        ir = 0;
        // Registros auxiliares
        aux = 0;// Registro auxiliar para el intercambio de registros
        desp = 0;// Valor de desplazamiento de los registros IX e IY
        desplazamiento = 0;
        indice = 0;
        // Bus de direcciones
        a0a7 = 0;// A0-A7
        a8a15 = 0;// A8-A15
        // Bus de datos
        d0d7 = 0;// D0-D7
        //Valor por defecto del puerto 0xFE
        valorPortFE = 0xff;
        // Señales CPU
        iorq = true;// Input output request. Se pone a cero para acceder a los periféricos
        halt = true;// Halt Detiene la CPU.
        // Flip flops de estado de las interrupciones
        IFF1 = false;// Interrupciones hailitadas=1, desabilitadas=0
        IFF2 = false;// Guarda IFF1 durante el servicio de una interrupción no enmascarable NMI
        // Flip flops de modos de interrupción
        // IMFa=0 IMFb=0 Interrupción modo 0
        // IMFa=0 IMFb=1 No utilizado
        // IMFa=1 IMFb=0 Interrupción modo 1
        // IMFa=1 IMFb=1 Interrupción modo 2
        IMFa = false;
        IMFb = false;
        //T-States
        tStates = 0;
        //Variable para resetear el Z80
        reset = false;
    }

    //Realiza un reset en el Z-80
    public void setResetZ80() {
        resetZ80();
    }

    //Retorna los T-States
    public int gettStates() {
        return tStates;
    }

    //Carga los T-States pasados como parámetro
    public void settStates(int tStates) {
        this.tStates = tStates;
    }

    //Incrementa los T-States
    public void inctStates(int valor) {
        tStates += valor;
    }

    //Guarda el objeto pasado como parámetro en la variable pantalla
    public void setPantalla(Pantalla pantalla) {
        this.pantalla = pantalla;
    }

    public void setSonido(Sonido sonido) {
        this.sonido = sonido;
    }
}
