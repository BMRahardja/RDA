package RDA;

import Main.Individu;
import FSS.FSS;
import java.util.*;

public class RDA {
    private int[][] processingTimes;
    private int nPop, nStags, nHinds, maxGen;
    private int roarMoves;
    private double crossoverProb;
    private double mutationProb;
    private int nJobs, nMachines;
    private Random rand;

    // population data
    private List<int[]> population;
    private int[] msPopulation;   // makespan of each member
    private int[] tfPopulation;   // total flow time of each member

    private List<int[]> stags;
    private List<int[]> hinds;
    private List<List<int[]>> harems;

    // Global best values for relative deviation calculation
    private int globalMinMS = Integer.MAX_VALUE;
    private int globalMinTF = Integer.MAX_VALUE;

    // Best solution and its objectives (according to combined fitness)
    private int[] bestPermutation;
    private int bestMS, bestTF;
    private double bestCombinedFitness = Double.MAX_VALUE;

    public RDA(int[][] processingTimes, int nPop, int nStags, int maxGen,
               int roarMoves, double crossoverProb, double mutationProb) {
        this.processingTimes = processingTimes;
        this.nPop = nPop;
        this.nStags = nStags;
        this.nHinds = nPop - nStags;
        this.maxGen = maxGen;
        this.roarMoves = roarMoves;
        this.crossoverProb = crossoverProb;
        this.mutationProb = mutationProb;
        this.nJobs = processingTimes[0].length;
        this.nMachines = processingTimes.length;
        this.rand = new Random();
        this.population = new ArrayList<>();
        this.msPopulation = new int[nPop];
        this.tfPopulation = new int[nPop];

        // Initialisation – completely random
        for (int i = 0; i < nPop; i++) {
            population.add(randomPermutation(nJobs));
        }
        evaluateAll();
        classify();

        // Main loop
        for (int gen = 0; gen < maxGen; gen++) {
            roaring();
            fightAndFormHarems();
            List<int[]> offspring = mating();
            evaluateList(offspring);
            selection(offspring);
            classify();
        }
    }

    // ---------- Evaluation helpers (multi‑objective) ----------
    private void evaluateAll() {
        for (int i = 0; i < population.size(); i++) {
            int[] obj = evaluate(population.get(i));
            msPopulation[i] = obj[0];
            tfPopulation[i] = obj[1];
            updateGlobals(obj[0], obj[1]);
        }
    }

    private void evaluateList(List<int[]> list) {
        for (int[] perm : list) {
            int[] obj = evaluate(perm);
            updateGlobals(obj[0], obj[1]);
        }
    }

    // Returns [makespan, totalFlowTime]
    private int[] evaluate(int[] perm) {
        Individu ind = new Individu(perm, nMachines);
        FSS fss = new FSS(ind, processingTimes);
        return new int[] { fss.getMakespan(), fss.getTotalFlowTime() };
    }

    private void updateGlobals(int ms, int tf) {
        if (ms < globalMinMS) globalMinMS = ms;
        if (tf < globalMinTF) globalMinTF = tf;

        // Compute combined fitness relative to current global minima
        double fitness = combinedFitness(ms, tf);
        if (fitness < bestCombinedFitness) {
            bestCombinedFitness = fitness;
            bestMS = ms;
            bestTF = tf;
            // store the permutation – we need it when the global best is found
            // We'll store it separately; but here we don't have the permutation.
            // So we record the best permutation when we actually evaluate it.
        }
    }

    // Track the best permutation whenever we find a new global best or better combined fitness
    private void trackBest(int[] perm, int ms, int tf) {
        double fit = combinedFitness(ms, tf);
        if (fit < bestCombinedFitness) {
            bestCombinedFitness = fit;
            bestMS = ms;
            bestTF = tf;
            bestPermutation = perm.clone();
        }
    }

    // Fitness = (MS - minMS)/minMS + (TF - minTF)/minTF
    private double combinedFitness(int ms, int tf) {
        return (ms - globalMinMS) / (double) globalMinMS
                + (tf - globalMinTF) / (double) globalMinTF;
    }

    // Compute fitness for a population member by index
    private double fitnessOf(int idx) {
        return combinedFitness(msPopulation[idx], tfPopulation[idx]);
    }

    // ---------- RDA phases ----------
    private void classify() {
        Integer[] indices = new Integer[nPop];
        for (int i = 0; i < nPop; i++) indices[i] = i;
        // Sort by combined fitness (ascending) – lower is better
        Arrays.sort(indices, (a, b) -> Double.compare(fitnessOf(a), fitnessOf(b)));

        stags = new ArrayList<>();
        hinds = new ArrayList<>();
        for (int i = 0; i < nPop; i++) {
            if (i < nStags) {
                stags.add(population.get(indices[i]));
            } else {
                hinds.add(population.get(indices[i]));
            }
        }
    }

    private void roaring() {
        for (int s = 0; s < stags.size(); s++) {
            int[] stag = stags.get(s);
            int[] stagObj = evaluate(stag);
            double stagFit = combinedFitness(stagObj[0], stagObj[1]);

            for (int r = 0; r < roarMoves; r++) {
                int[] candidate = stag.clone();
                int from = rand.nextInt(nJobs);
                int to = rand.nextInt(nJobs);
                if (from == to) continue;
                int job = candidate[from];
                if (from < to) {
                    System.arraycopy(candidate, from + 1, candidate, from, to - from);
                } else {
                    System.arraycopy(candidate, to, candidate, to + 1, from - to);
                }
                candidate[to] = job;

                int[] candObj = evaluate(candidate);
                updateGlobals(candObj[0], candObj[1]);
                double candFit = combinedFitness(candObj[0], candObj[1]);

                if (candFit < stagFit) {
                    stags.set(s, candidate);
                    stagFit = candFit;
                    trackBest(candidate, candObj[0], candObj[1]);
                }
            }
        }
    }

    private void fightAndFormHarems() {
        double[] power = new double[nStags];
        double sumPower = 0.0;
        int[] stagMS = new int[nStags];
        int[] stagTF = new int[nStags];
        for (int s = 0; s < nStags; s++) {
            int[] obj = evaluate(stags.get(s));
            updateGlobals(obj[0], obj[1]);
            stagMS[s] = obj[0];
            stagTF[s] = obj[1];
            double fit = combinedFitness(obj[0], obj[1]);
            power[s] = 1.0 / (1.0 + fit);
            sumPower += power[s];
        }

        int[] haremSize = new int[nStags];
        int assigned = 0;
        for (int s = 0; s < nStags; s++) {
            haremSize[s] = (int) Math.round((power[s] / sumPower) * nHinds);
            assigned += haremSize[s];
        }
        int bestStagIdx = 0;
        double minFit = Double.MAX_VALUE;
        for (int s = 0; s < nStags; s++) {
            double fit = combinedFitness(stagMS[s], stagTF[s]);
            if (fit < minFit) {
                minFit = fit;
                bestStagIdx = s;
            }
        }
        haremSize[bestStagIdx] += (nHinds - assigned);

        List<int[]> shuffledHinds = new ArrayList<>(hinds);
        Collections.shuffle(shuffledHinds, rand);
        harems = new ArrayList<>();
        int hindIdx = 0;
        for (int s = 0; s < nStags; s++) {
            List<int[]> harem = new ArrayList<>();
            for (int h = 0; h < haremSize[s]; h++) {
                if (hindIdx < shuffledHinds.size()) {
                    harem.add(shuffledHinds.get(hindIdx));
                    hindIdx++;
                }
            }
            harems.add(harem);
        }
    }

    private List<int[]> mating() {
        List<int[]> offspring = new ArrayList<>();
        for (int s = 0; s < nStags; s++) {
            int[] stag = stags.get(s);
            List<int[]> harem = harems.get(s);
            for (int[] hind : harem) {
                if (rand.nextDouble() < crossoverProb) {
                    int[] child = orderCrossover(stag, hind);
                    if (rand.nextDouble() < mutationProb) {
                        mutate(child);
                    }
                    offspring.add(child);
                } else {
                    offspring.add(stag.clone());
                }
            }
        }
        return offspring;
    }

    private int[] orderCrossover(int[] p1, int[] p2) {
        int n = p1.length;
        int[] child = new int[n];
        Arrays.fill(child, -1);
        int cut1 = rand.nextInt(n);
        int cut2 = rand.nextInt(n);
        if (cut1 > cut2) { int tmp = cut1; cut1 = cut2; cut2 = tmp; }
        System.arraycopy(p1, cut1, child, cut1, cut2 - cut1 + 1);
        int current = (cut2 + 1) % n;
        for (int i = 0; i < n; i++) {
            int idx = (cut2 + 1 + i) % n;
            int gene = p2[idx];
            if (!contains(child, gene)) {
                child[current] = gene;
                current = (current + 1) % n;
            }
        }
        return child;
    }

    private boolean contains(int[] arr, int value) {
        for (int v : arr) if (v == value) return true;
        return false;
    }

    private void mutate(int[] perm) {
        int i = rand.nextInt(perm.length);
        int j = rand.nextInt(perm.length);
        int temp = perm[i];
        perm[i] = perm[j];
        perm[j] = temp;
    }

    private void selection(List<int[]> offspring) {
        // Combine current population and offspring
        List<int[]> combined = new ArrayList<>(population);
        combined.addAll(offspring);

        int[] msCombined = new int[combined.size()];
        int[] tfCombined = new int[combined.size()];
        for (int i = 0; i < combined.size(); i++) {
            int[] obj = evaluate(combined.get(i));
            updateGlobals(obj[0], obj[1]);
            msCombined[i] = obj[0];
            tfCombined[i] = obj[1];
        }

        Integer[] idx = new Integer[combined.size()];
        for (int i = 0; i < combined.size(); i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(
                combinedFitness(msCombined[a], tfCombined[a]),
                combinedFitness(msCombined[b], tfCombined[b])
        ));

        population.clear();
        for (int i = 0; i < nPop; i++) {
            int pos = idx[i];
            population.add(combined.get(pos));
            msPopulation[i] = msCombined[pos];
            tfPopulation[i] = tfCombined[pos];
            // track best
            trackBest(combined.get(pos), msCombined[pos], tfCombined[pos]);
        }
    }

    private int[] randomPermutation(int n) {
        List<Integer> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) list.add(i);
        Collections.shuffle(list, rand);
        return list.stream().mapToInt(i -> i).toArray();
    }

    // Public getters
    public int[] getBestPermutation() { return bestPermutation.clone(); }
    public int getBestMakespan() { return bestMS; }
    public int getBestTotalFlowTime() { return bestTF; }
    public double getBestCombinedFitness() { return bestCombinedFitness; }
}
