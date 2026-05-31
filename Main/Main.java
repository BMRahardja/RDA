package Main;

import java.util.Scanner;
import RDA.RDA;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Algoritma Red Deer (RDA) untuk minimasi Total Flow Time");

        System.out.print("Nama file yang dijalankan: ");
        String lokasiFile = sc.nextLine();
        System.out.print("Nomor soal: ");
        int soalTerpilih = Integer.parseInt(sc.nextLine()) - 1;

        TestRead kasusTaillard = new TestRead(lokasiFile);
        int[][] jadwalSoal = kasusTaillard.getKumpulanSoal()[soalTerpilih].getSoal();
        Individu pemenang = null;
        int nilaiObjektifPemenang = 0;

        System.out.print("Jumlah Populasi (NPop): ");
        int nPop = Integer.parseInt(sc.nextLine());
        System.out.print("Jumlah Stags (jantan, <= NPop): ");
        int nStags = Integer.parseInt(sc.nextLine());
        System.out.print("Max Generasi: ");
        int maxGen = Integer.parseInt(sc.nextLine());
        System.out.print("Intensitas Roaring (jumlah local search moves): ");
        int roarMoves = Integer.parseInt(sc.nextLine());
        System.out.print("Probabilitas Crossover (0-1): ");
        double crossoverProb = Double.parseDouble(sc.nextLine());
        System.out.print("Probabilitas Mutasi (0-1): ");
        double mutationProb = Double.parseDouble(sc.nextLine());

        RDA rda = new RDA(jadwalSoal, nPop, nStags, maxGen,
                          roarMoves, crossoverProb, mutationProb);

        pemenang = new Individu(rda.getBestPermutation(), jadwalSoal.length);
        nilaiObjektifPemenang = rda.getBestFlowTime();

        Soal tampungSoal = new Soal(jadwalSoal.length, jadwalSoal[0].length);
        int[][] urutanKerja = new int[jadwalSoal.length][jadwalSoal[0].length];
        int[] urutanJob = pemenang.getUrutanPekerjaan();
        for (int j = 0; j < jadwalSoal.length; j++) {
            for (int k = 0; k < urutanJob.length; k++) {
                urutanKerja[j][k] = jadwalSoal[j][urutanJob[k] - 1];
            }
        }
        tampungSoal.setSoal(urutanKerja);

        System.out.println("\n========== HASIL ==========");
        System.out.print("Urutan pekerjaan terbaik: ");
        for (int j : urutanJob) System.out.print(j + " ");

        System.out.println("\n\nJadwal matriks (mesin x pekerjaan):");
        int[][] jadwal = tampungSoal.getSoal();
        for (int i = 0; i < jadwal.length; i++) {
            for (int j = 0; j < jadwal[0].length; j++) {
                System.out.print(jadwal[i][j] + "\t");
            }
            System.out.println();
        }

        System.out.println("\nTotal Flow Time Terbaik: " + nilaiObjektifPemenang);
        System.out.println("Batas atas             : " + kasusTaillard.getBatasAtas()[soalTerpilih]);
        System.out.println("Batas bawah            : " + kasusTaillard.getBatasBawah()[soalTerpilih]);

        sc.close();
    }
}