package ai;

import java.io.*;
import java.util.*;


public class PlantDataLoader {

    //  Plant data structure
    static class Plant {
        double soilMoisture;   
        double lastWatered;    
        int    plantType;      
        int    needsWater;     
        int    x, y;         

        public Plant(double soilMoisture, double lastWatered,
                     int plantType, int needsWater) {
            this.soilMoisture = soilMoisture;
            this.lastWatered  = lastWatered;
            this.plantType    = plantType;
            this.needsWater   = needsWater;
            this.x = -1;
            this.y = -1;
        }

        public Plant(int x, int y,
                     double soilMoisture, double lastWatered, int plantType) {
            this.x            = x;
            this.y            = y;
            this.soilMoisture = soilMoisture;
            this.lastWatered  = lastWatered;
            this.plantType    = plantType;
            this.needsWater   = -1;
        }

        public String getTypeName() {
            return plantType == 0 ? "Cactus" : plantType == 1 ? "Flower" : "Herb";
        }

        public static double euclideanDistance(Plant a, Plant b) {
            int dx = a.x - b.x, dy = a.y - b.y;
            return Math.sqrt(dx * dx + dy * dy);
        }

        @Override
        public String toString() {
            String pos   = (x == -1) ? "No pos" : "(" + x + "," + y + ")";
            String water = needsWater == -1 ? "Unknown"
                         : needsWater ==  1 ? "Yes" : "No";
            return String.format(
                "Moisture=%-4.0f | LastWatered=%-3.0fh | Type=%-6s | NeedsWater=%-7s | Pos=%s",
                soilMoisture, lastWatered, getTypeName(), water, pos);
        }
    }

   
    public static List<Plant> loadFromCSV(String filename) throws IOException {
        List<Plant> dataset = new ArrayList<>();
        BufferedReader br   = new BufferedReader(new FileReader(filename));
        String  line;
        boolean firstLine = true;

        while ((line = br.readLine()) != null) {
            if (firstLine) { firstLine = false; continue; }   
            line = line.trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split(",");
            if (parts.length < 4) continue;

            try {
                double soilMoisture = Double.parseDouble(parts[0].trim());
                double lastWatered  = Double.parseDouble(parts[1].trim());
                int    plantType    = Integer.parseInt(parts[2].trim());
                int    needsWater   = Integer.parseInt(parts[3].trim());
                dataset.add(new Plant(soilMoisture, lastWatered, plantType, needsWater));
            } catch (NumberFormatException e) {
                System.out.println("[PlantDataLoader] Skipping invalid row: " + line);
            }
        }
        br.close();
        return dataset;
    }


    public static double[][] normalizeFeatures(List<Plant> dataset) {
        int n = dataset.size();
        double[][] X = new double[n][3];
        for (int i = 0; i < n; i++) {
            X[i][0] = dataset.get(i).soilMoisture / 100.0;
            X[i][1] = dataset.get(i).lastWatered  / 48.0;
            X[i][2] = dataset.get(i).plantType    / 2.0;
        }
        return X;
    }

    public static int[] extractLabels(List<Plant> dataset) {
        int[] y = new int[dataset.size()];
        for (int i = 0; i < dataset.size(); i++) {
            y[i] = dataset.get(i).needsWater;
        }
        return y;
    }


    public static void printSummary(List<Plant> dataset) {
        int needs = 0, cactus = 0, flower = 0, herb = 0, placed = 0;
        for (Plant p : dataset) {
            if (p.needsWater == 1) needs++;
            if (p.plantType  == 0) cactus++;
            else if (p.plantType == 1) flower++;
            else herb++;
            if (p.x != -1) placed++;
        }
        System.out.println("========================================");
        System.out.println("        PLANT DATASET SUMMARY           ");
        System.out.println("========================================");
        System.out.println("Total Plants     : " + dataset.size());
        System.out.println("Needs Water      : " + needs);
        System.out.println("Doesn't Need     : " + (dataset.size() - needs));
        System.out.println("Placed in Garden : " + placed);
        System.out.println("----------------------------------------");
        System.out.println("Cactus  (type 0) : " + cactus);
        System.out.println("Flower  (type 1) : " + flower);
        System.out.println("Herb    (type 2) : " + herb);
        System.out.println("========================================");
    }


  /*  public static void main(String[] args) throws IOException {
        List<Plant> dataset = loadFromCSV("Data.csv");
        printSummary(dataset);
        double[][] X = normalizeFeatures(dataset);
        int[]      y = extractLabels(dataset);
        System.out.println("X[0] = [" + X[0][0] + ", " + X[0][1] + ", " + X[0][2] + "]");
        System.out.println("y[0] = " + y[0]);
    }*/
}