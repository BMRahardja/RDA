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

    private List<int[]> population;
    private int[] fitness;
    private List<int[]> stags;
    private List<int[]> hinds;
    private List<List<int[]>> harems;   // temporary for mating

    private int[] bestPermutation;
    private int bestFlowTime;

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
        this.fitness = new int[nPop];
        this.bestFlowTime = Integer.MAX_VALUE;

        // -------- Initialisation (completely random) --------
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

    private void evaluateAll() {
        for (int i = 0; i < population.size(); i++) {
            int flow = computeFlowTime(population.get(i));
            fitness[i] = flow;
            if (flow < bestFlowTime) {
                bestFlowTime = flow;
                bestPermutation = population.get(i).clone();
            }
        }
    }

    private void evaluateList(List<int[]> list) {
        for (int[] perm : list) {
            int flow = computeFlowTime(perm);
            if (flow < bestFlowTime) {
                bestFlowTime = flow;
                bestPermutation = perm.clone();
            }
        }
    }

    private int computeFlowTime(int[] perm) {
        Individu ind = new Individu(perm, nMachines);
        FSS fss = new FSS(ind, processingTimes);
        return fss.getTotalFlowTime();
    }

    private void classify() {
        Integer[] indices = new Integer[nPop];
        for (int i = 0; i < nPop; i++) indices[i] = i;
        Arrays.sort(indices, (a, b) -> Integer.compare(fitness[a], fitness[b]));

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
            int stagFitness = computeFlowTime(stag);
            for (int r = 0; r < roarMoves; r++) {
                int[] candidate = stag.clone();
                // random insertion move
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

                int newFlow = computeFlowTime(candidate);
                if (newFlow < stagFitness) {
                    stags.set(s, candidate);
                    stagFitness = newFlow;
                    if (newFlow < bestFlowTime) {
                        bestFlowTime = newFlow;
                        bestPermutation = candidate.clone();
                    }
                }
            }
        }
    }

    private void fightAndFormHarems() {
        double[] power = new double[nStags];
        double sumPower = 0.0;
        int[] stagFitness = new int[nStags];
        for (int s = 0; s < nStags; s++) {
            stagFitness[s] = computeFlowTime(stags.get(s));
            power[s] = 1.0 / (1.0 + stagFitness[s]);
            sumPower += power[s];
        }

        int[] haremSize = new int[nStags];
        int assigned = 0;
        for (int s = 0; s < nStags; s++) {
            haremSize[s] = (int) Math.round((power[s] / sumPower) * nHinds);
            assigned += haremSize[s];
        }
        // give remaining hinds to the best stag
        int bestStagIdx = 0;
        int minFit = Integer.MAX_VALUE;
        for (int s = 0; s < nStags; s++) {
            if (stagFitness[s] < minFit) {
                minFit = stagFitness[s];
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
        List<int[]> combined = new ArrayList<>(population);
        combined.addAll(offspring);
        int[] combinedFit = new int[combined.size()];
        for (int i = 0; i < combined.size(); i++) {
            combinedFit[i] = computeFlowTime(combined.get(i));
            if (combinedFit[i] < bestFlowTime) {
                bestFlowTime = combinedFit[i];
                bestPermutation = combined.get(i).clone();
            }
        }
        Integer[] idx = new Integer[combined.size()];
        for (int i = 0; i < combined.size(); i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Integer.compare(combinedFit[a], combinedFit[b]));
        population.clear();
        for (int i = 0; i < nPop; i++) {
            population.add(combined.get(idx[i]));
            fitness[i] = combinedFit[idx[i]];
        }
    }

    private int[] randomPermutation(int n) {
        List<Integer> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) list.add(i);
        Collections.shuffle(list, rand);
        return list.stream().mapToInt(i -> i).toArray();
    }

    public int[] getBestPermutation() { return bestPermutation.clone(); }
    public int getBestFlowTime() { return bestFlowTime; }
}