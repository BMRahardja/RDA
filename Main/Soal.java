/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Main;

/**
 *
 * @author Necron
 */
public class Soal {
    private int[][] soal;

    public Soal(int vertikal, int horizontal) {
        soal = new int[vertikal][horizontal];
    }

    public int[][] getSoal() {
        return soal;
    }

    public void setSoal(int[][] soal) {
        this.soal = soal;
    }
}
