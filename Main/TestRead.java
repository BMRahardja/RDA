//Tidak ada perubahan
package Main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

public class TestRead {

    private String isi;
    private String[] isiTiapSoal;
    private Soal[] kumpulanSoal;
    private int banyakPekerjaan;
    private int banyakMesin;
    private String lokasiFile;
    private int[] batasAtas;
    private int[] batasBawah;

    public TestRead(String lokasiFile) {
        this.lokasiFile = lokasiFile;
        this.batasAtas = new int[10];
        this.batasBawah = new int[10];
        this.isi = new String();
        this.ubahKeString();
        this.kumpulanSoal = new Soal[10];
        for (int i = 0; i < this.kumpulanSoal.length; i++) {
            this.kumpulanSoal[i] = new Soal(this.banyakPekerjaan, this.banyakMesin);
        }
        this.masukanKumpulanSoal();
    }

    public Soal[] getKumpulanSoal() {
        return kumpulanSoal;
    }

    public int getBanyakPekerjaan() {
        return banyakPekerjaan;
    }

    public int getBanyakMesin() {
        return banyakMesin;
    }

    public int[] getBatasAtas() {
        return batasAtas;
    }

    public int[] getBatasBawah() {
        return batasBawah;
    }

    private void ubahKeString() {
        BufferedReader br = null;
        StringTokenizer st = null;
        int i = 0;
        int hitungBatas = 0;
        try {
            String baris;
            br = new BufferedReader(new FileReader(this.lokasiFile));
            while ((baris = br.readLine()) != null) {
                if (i == 1) {
                    st = new StringTokenizer(baris);
                    this.banyakPekerjaan = Integer.parseInt(st.nextToken());
                    this.banyakMesin = Integer.parseInt(st.nextToken());
                    st.nextToken();
                    this.batasAtas[hitungBatas] = Integer.parseInt(st.nextToken());
                    this.batasBawah[hitungBatas] = Integer.parseInt(st.nextToken());
                    hitungBatas++;
                }
                else if((i-1) % (this.banyakMesin+3) == 0){
                    st = new StringTokenizer(baris);
                    st.nextToken();
                    st.nextToken();
                    st.nextToken();
                    this.batasAtas[hitungBatas] = Integer.parseInt(st.nextToken());
                    this.batasBawah[hitungBatas] = Integer.parseInt(st.nextToken());
                    hitungBatas++;
                }
                if (i % (this.banyakMesin+3) != 0 && (i - 1) % (this.banyakMesin+3) != 0) {
                    isi += baris + "\n";
                }
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        this.isiTiapSoal = this.isi.split("processing times :" + "\n");
    }

    private void masukanKumpulanSoal() {
        StringTokenizer st = null;
        String[] tampungBaris = null;
        int[][] urutanPekerjaan = new int[this.banyakMesin][this.banyakPekerjaan];
        for (int i = 0; i < this.isiTiapSoal.length; i++) {
            if (this.isiTiapSoal[i].length() != 0) {
                for (int j = 0; j < this.banyakMesin; j++) {
                    tampungBaris = this.isiTiapSoal[i].split("\n");
                    st = new StringTokenizer(tampungBaris[j]);
                    for (int k = 0; k < this.banyakPekerjaan; k++) {
                        urutanPekerjaan[j][k] = Integer.parseInt(st.nextToken());
                    }
                }
                if(i<11){
                    this.kumpulanSoal[i-1].setSoal(urutanPekerjaan);
                }
                urutanPekerjaan = new int[this.banyakMesin][this.banyakPekerjaan];
            }
        }
    }
}
