package ai;

import java.util.*;


public class Perceptron {

    // Weights + bias  
    private double[] weights;
    private double   bias;
    private final int numInputs;

    // Hyper-parameters  — set by user via GUI before clicking Train
    public double learningRate   = 0.05;
    public int    epochs         = 100;
    public double errorThreshold = 0.01;  

    public ArrayList<Double>  trainAccHistory = new ArrayList<>();
    public ArrayList<Double>  testAccHistory  = new ArrayList<>();
    public ArrayList<Integer> trainErrHistory = new ArrayList<>(); 


    public int TP = 0, TN = 0, FP = 0, FN = 0;

    public int TP_tr = 0, TN_tr = 0, FP_tr = 0, FN_tr = 0;

    public Perceptron(int numInputs) {
        this.numInputs = numInputs;
        this.weights   = new double[numInputs];
    }
          
    //  Net input  =  w · x  +  bias
    private double netInput(double[] x) {
        double sum = bias;
        for (int j = 0; j < numInputs; j++) {
            sum += weights[j] * x[j];
        }
        return sum;
        
    }
    private int stepFunction(double net) {
        return net >= 0.0 ? 1 : 0;
    }

      //  Predict a single 
    public int predict(double[] x) {
        return stepFunction(netInput(x));
    }


    public void train(double[][] trainX, int[] trainY,
                      double[][] testX,  int[] testY) {

        Random rng = new Random(42);
        for (int j = 0; j < numInputs; j++) {
            weights[j] = (rng.nextDouble() - 0.5) * 0.1;  // small random: [-0.05, +0.05]
        }
        bias = (rng.nextDouble() - 0.5) * 0.1;

        trainAccHistory.clear();
        testAccHistory.clear();
        trainErrHistory.clear();
        TP = TN = FP = FN = 0;
        TP_tr = TN_tr = FP_tr = FN_tr = 0;

        int n = trainX.length;

        for (int ep = 0; ep < epochs; ep++) {

            int errors = 0;

            for (int i = 0; i < n; i++) {
                int pred  = predict(trainX[i]);
                int error = trainY[i] - pred;   

                if (error != 0) {
                    errors++;
                    // Perceptron learning rule
                    for (int j = 0; j < numInputs; j++) {
                        weights[j] += learningRate * error * trainX[i][j];
                    }
                    bias += learningRate * error;
                }
            }

            double trAcc = (double)(n - errors) / n * 100.0;
            trainAccHistory.add(trAcc);
            trainErrHistory.add(errors);

            double tsAcc = evaluateAccuracy(testX, testY);
            testAccHistory.add(tsAcc);

            // early stopping
            double errorRate = (double) errors / n;
            if (errorRate <= errorThreshold) {
                System.out.printf("[Perceptron] Early stop at epoch %d (error rate = %.4f)%n",
                        ep + 1, errorRate);
                break;
            }
        }

        buildConfusionMatrix(trainX, trainY, true);
        buildConfusionMatrix(testX,  testY,  false);
    }

    public double evaluateAccuracy(double[][] X, int[] y) {
        int correct = 0;
        for (int i = 0; i < X.length; i++) {
            if (predict(X[i]) == y[i]) correct++;
        }
        return (double) correct / X.length * 100.0;
    }

    private void buildConfusionMatrix(double[][] X, int[] y, boolean isTrain) {
        int tp = 0, tn = 0, fp = 0, fn = 0;
        for (int i = 0; i < X.length; i++) {
            int actual    = y[i];
            int predicted = predict(X[i]);
            if      (actual == 1 && predicted == 1) tp++;
            else if (actual == 0 && predicted == 0) tn++;
            else if (actual == 0 && predicted == 1) fp++;
            else                                    fn++;   // actual=1, pred=0
        }
        if (isTrain) { TP_tr = tp; TN_tr = tn; FP_tr = fp; FN_tr = fn; }
        else         { TP    = tp; TN    = tn; FP    = fp; FN    = fn; }
    }

    //  Getters
    public double[] getWeights()   { return weights;   }
    public double   getBias()      { return bias;       }
    public int      getNumInputs() { return numInputs;  }

    public double getTrainAccuracy() {
        return trainAccHistory.isEmpty() ? 0.0
               : trainAccHistory.get(trainAccHistory.size() - 1);
    }

    public double getTestAccuracy() {
        return testAccHistory.isEmpty() ? 0.0
               : testAccHistory.get(testAccHistory.size() - 1);
    }


    public String getConfusionMatrixHTML(boolean isTrain) {
        int tp = isTrain ? TP_tr : TP;
        int tn = isTrain ? TN_tr : TN;
        int fp = isTrain ? FP_tr : FP;
        int fn = isTrain ? FN_tr : FN;

        int    total     = tp + tn + fp + fn;
        double acc       = total > 0 ? (double)(tp + tn) / total * 100.0 : 0.0;
        double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) * 100.0 : 0.0;
        double recall    = (tp + fn) > 0 ? (double) tp / (tp + fn) * 100.0 : 0.0;
        double f1        = (precision + recall) > 0
                           ? 2.0 * precision * recall / (precision + recall) : 0.0;

        String label = isTrain ? "TRAINING SET" : "TEST SET";

        return String.format(
            "<html>" +
            "<b style='color:#333'>%s Confusion Matrix (2×2)</b><br><br>" +
            "<table border='1' cellpadding='5' cellspacing='0' style='border-collapse:collapse'>" +
            "<tr bgcolor='#eeeeee'>" +
            "  <th style='padding:5px'>&nbsp;</th>" +
            "  <th style='padding:5px'>Pred: 0<br><small>(OK)</small></th>" +
            "  <th style='padding:5px'>Pred: 1<br><small>(Needs Water)</small></th>" +
            "</tr>" +
            "<tr>" +
            "  <td bgcolor='#eeeeee'><b>Actual: 0<br><small>(OK)</small></b></td>" +
            "  <td bgcolor='#d4edda' align='center'><b>TN = %d</b><br><small>Correct ✓</small></td>" +
            "  <td bgcolor='#f8d7da' align='center'>FP = %d<br><small>False Alarm</small></td>" +
            "</tr>" +
            "<tr>" +
            "  <td bgcolor='#eeeeee'><b>Actual: 1<br><small>(Needs Water)</small></b></td>" +
            "  <td bgcolor='#f8d7da' align='center'>FN = %d<br><small>Missed!</small></td>" +
            "  <td bgcolor='#d4edda' align='center'><b>TP = %d</b><br><small>Correct ✓</small></td>" +
            "</tr>" +
            "</table><br>" +
            "<table cellpadding='3'>" +
            "<tr><td><b>Accuracy :</b></td><td><b style='color:%s'>%.1f%%</b></td></tr>" +
            "<tr><td>Precision :</td><td>%.1f%%</td></tr>" +
            "<tr><td>Recall    :</td><td>%.1f%%</td></tr>" +
            "<tr><td>F1 Score  :</td><td>%.1f%%</td></tr>" +
            "</table>" +
            "</html>",
            label,
            tn, fp, fn, tp,
            acc >= 70 ? "#1a8a6a" : "#cc3333",
            acc, precision, recall, f1);
    }


    public void printWeights() {
        System.out.printf("[Perceptron] bias=%.4f | ", bias);
        for (int j = 0; j < numInputs; j++) {
            System.out.printf("w[%d]=%.4f ", j, weights[j]);
        }
        System.out.println();
    }
}