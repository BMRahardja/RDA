package Main;

import java.util.Scanner;
import RDA.RDA;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Algoritma Red Deer (RDA) multi-objektif (Rajendran)");
        System.out.println("Meminimasi Makespan + Total Flow Time");

        System.out.print("Nama file yang dijalankan: ");
        String lokasiFile = sc.nextLine();
        System.out.print("Nomor soal: ");
        int soalTerpilih = Integer.parseInt(sc.nextLine()) - 1;

        TestRead kasusTaillard = new TestRead(lokasiFile);
        int[][] jadwalSoal = kasusTaillard.getKumpulanSoal()[soalTerpilih].getSoal();

        System.out.print("Jumlah Populasi (NPop): ");
        int nPop = Integer.parseInt(sc.nextLine());
        System.out.print("Jumlah Stags (jantan, <= NPop): ");
        int nStags = Integer.parseInt(sc.nextLine());
        System.out.print("Max Generasi: ");
        int maxGen = Integer.parseInt(sc.nextLine());
        System.out.print("Intensitas Roaring (local search moves): ");
        int roarMoves = Integer.parseInt(sc.nextLine());
        System.out.print("Probabilitas Crossover (0-1): ");
        double crossoverProb = Double.parseDouble(sc.nextLine());
        System.out.print("Probabilitas Mutasi (0-1): ");
        double mutationProb = Double.parseDouble(sc.nextLine());

        RDA rda = new RDA(jadwalSoal, nPop, nStags, maxGen,
                          roarMoves, crossoverProb, mutationProb);

        int[] bestJobs = rda.getBestPermutation();
        int bestMS   = rda.getBestMakespan();
        int bestTF   = rda.getBestTotalFlowTime();

        System.out.println("\n========== HASIL ==========");
        System.out.print("Urutan pekerjaan terbaik: ");
        for (int j : bestJobs) System.out.print(j + " ");

        // Optional: display processing matrix ordered by this permutation
        System.out.println("\n\nJadwal matriks (mesin x pekerjaan):");
        for (int i = 0; i < jadwalSoal.length; i++) {
            for (int k = 0; k < bestJobs.length; k++) {
                System.out.print(jadwalSoal[i][bestJobs[k] - 1] + "\t");
            }
            System.out.println();
        }

        System.out.println("\nMakespan terbaik  : " + bestMS);
        System.out.println("Total Flow Time   : " + bestTF);
        System.out.println("Combined Fitness  : " + rda.getBestCombinedFitness());
        System.out.println("Batas atas (TF)   : " + kasusTaillard.getBatasAtas()[soalTerpilih]);
        System.out.println("Batas bawah (TF)  : " + kasusTaillard.getBatasBawah()[soalTerpilih]);

        sc.close();
    }
}
