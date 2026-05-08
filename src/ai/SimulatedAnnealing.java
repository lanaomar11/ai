package ai;

import java.util.*;


public class SimulatedAnnealing {


    static class GardenPlant {
        int    id;
        int    x, y;
        double soilMoisture;
        double lastWatered;
        int    plantType;
        int    needsWater;   

        public GardenPlant(int id, int x, int y,
                           double soilMoisture, double lastWatered,
                           int plantType, int needsWater) {
            this.id           = id;
            this.x            = x;
            this.y            = y;
            this.soilMoisture = soilMoisture;
            this.lastWatered  = lastWatered;
            this.plantType    = plantType;
            this.needsWater   = needsWater;
        }

        public static double distance(GardenPlant a, GardenPlant b) {
            int dx = a.x - b.x, dy = a.y - b.y;
            return Math.sqrt(dx * dx + dy * dy);
        }

        public String getTypeName() {
            return plantType == 0 ? "Cactus" : plantType == 1 ? "Flower" : "Herb";
        }

        @Override
        public String toString() {
            return String.format("Plant#%d(%s) pos=(%d,%d) needsWater=%d",
                    id, getTypeName(), x, y, needsWater);
        }
    }

    //  SA parameters
  
    private final double initialTemp;
    private final double coolingRate;
    private final int    maxIterations;

    // Penalty weights
    private static final double W_MISSED = 100.0;
    private static final double W_EXTRA  =  50.0;
    private static final double W_DIST   =   1.0;

    public SimulatedAnnealing(double initialTemp, double coolingRate, int maxIterations) {
        this.initialTemp    = initialTemp;
        this.coolingRate    = coolingRate;
        this.maxIterations  = maxIterations;
    }

    //  Cost function
    public double calculateCost(List<GardenPlant> sequence,
                                List<GardenPlant> allGardenPlants) {
        
        Set<Integer> seqIds = new HashSet<>();
        for (GardenPlant p : sequence) seqIds.add(p.id);

        int missed = 0;
        for (GardenPlant p : allGardenPlants) {
            if (p.needsWater == 1 && !seqIds.contains(p.id)) missed++;
        }

        // 2. total distance walked
        double totalDist = 0.0;
        for (int i = 0; i < sequence.size() - 1; i++) {
            totalDist += GardenPlant.distance(sequence.get(i), sequence.get(i + 1));
        }

        // 3. extra watering — in sequence but needsWater=0
        int extra = 0;
        for (GardenPlant p : sequence) {
            if (p.needsWater == 0) extra++;
        }

        return (W_MISSED * missed) + (W_DIST * totalDist) + (W_EXTRA * extra);
    }

    //  Swap two random positions
    private List<GardenPlant> swapTwo(List<GardenPlant> sequence, Random rand) {
        List<GardenPlant> newSeq = new ArrayList<>(sequence);
        int i = rand.nextInt(newSeq.size());
        int j;
        do { 
        	j = rand.nextInt(newSeq.size());
        	} while (j == i);
        GardenPlant temp = newSeq.get(i);
        newSeq.set(i, newSeq.get(j));
        newSeq.set(j, temp);
        return newSeq;
    }


    private boolean accept(double oldCost, double newCost,
                           double temperature, Random rand) {
        if (newCost < oldCost) return true;
        double delta       = newCost - oldCost;
        double probability = Math.exp(-delta / temperature);
        return rand.nextDouble() < probability;
    }

    //  Main SA optimisation loop
   
    public List<GardenPlant> optimize(List<GardenPlant> inputSequence,
                                      List<GardenPlant> allGardenPlants) {
        Random rand = new Random(42);

        // Step 1 — start with random order
        List<GardenPlant> current = new ArrayList<>(inputSequence);
        Collections.shuffle(current, rand);

        // Step 2 — calculate initial cost
        double currentCost = calculateCost(current, allGardenPlants);

        List<GardenPlant> best     = new ArrayList<>(current);
        double            bestCost = currentCost;
        double            temperature = initialTemp;

        List<double[]> history = new ArrayList<>(); 

        System.out.println("========================================");
        System.out.println("     SIMULATED ANNEALING OPTIMIZER      ");
        System.out.println("========================================");
        System.out.printf("Initial Temp   : %.1f%n",  initialTemp);
        System.out.printf("Cooling Rate   : %.4f%n",  coolingRate);
        System.out.printf("Max Iterations : %d%n",    maxIterations);
        System.out.printf("Sequence Size  : %d%n",    inputSequence.size());
        System.out.printf("Initial Cost   : %.2f%n",  currentCost);
        System.out.println("----------------------------------------");

        //  SA loop
        for (int iter = 1; iter <= maxIterations; iter++) {

            // Step 3 — swap two plants 
            List<GardenPlant> candidate     = swapTwo(current, rand);

            // Step 4 — calculate candidate cost
            double            candidateCost = calculateCost(candidate, allGardenPlants);

            // Step 5 — accept or reject
            if (accept(currentCost, candidateCost, temperature, rand)) {
                current     = candidate;
                currentCost = candidateCost;
            }

            // update best
            if (currentCost < bestCost) {
                best     = new ArrayList<>(current);
                bestCost = currentCost;
            }

            // Step 6 — cool down
            temperature *= coolingRate;

            if (iter % 100 == 0 || iter == 1) {
                history.add(new double[]{iter, currentCost, temperature});
            }

            // Step 7 — converged?
            if (temperature < 0.001) {
                System.out.printf("Converged at iteration %d (T=%.5f)%n", iter, temperature);
                break;
            }
        }

        // print summary
        System.out.println("\nCost Convergence (sample):");
        System.out.printf("  %-8s | %-10s | %s%n", "Iter", "Cost", "Temp");
        System.out.println("---------|-----------|----------");
        for (double[] h : history) {
            System.out.printf("  %-8.0f | %-10.2f | %.4f%n", h[0], h[1], h[2]);
        }

        System.out.println("\n========================================");
        System.out.println("       OPTIMAL WATERING SEQUENCE        ");
        System.out.println("========================================");
        System.out.printf("Best Cost : %.2f%n", bestCost);

        double totalDist = 0.0;
        for (int i = 0; i < best.size(); i++) {
            GardenPlant p    = best.get(i);
            String      flag = p.needsWater == 1 ? " [NEEDS WATER]" : " [extra]";
            System.out.printf("  %d. %s%s%n", i + 1, p, flag);
            if (i < best.size() - 1) {
                double d = GardenPlant.distance(p, best.get(i + 1));
                totalDist += d;
                System.out.printf("     --> %.1f px to next%n", d);
            }
        }
        System.out.printf("%nTotal Distance : %.1f px%n", totalDist);
        System.out.println("========================================");

        return best;
    }

    
}