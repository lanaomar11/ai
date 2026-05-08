package ai;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class GardenGUI extends JFrame {
    //  Dark botanical palette
    private static final Color C_BG      = new Color(0x0f, 0x1a, 0x14);
    private static final Color C_SURFACE = new Color(0x16, 0x20, 0x13);
    private static final Color C_SURF2   = new Color(0x1c, 0x2b, 0x1e);
    private static final Color C_BORDER  = new Color(0x2a, 0x3d, 0x2e);
    private static final Color C_ACCENT  = new Color(0x4a, 0xde, 0x80);
    private static final Color C_ACCENT2 = new Color(0x22, 0xc5, 0x5e);
    private static final Color C_ORANGE  = new Color(0xfb, 0x92, 0x3c);
    private static final Color C_RED     = new Color(0xf8, 0x71, 0x71);
    private static final Color C_MUTED   = new Color(0x6b, 0x8f, 0x71);
    private static final Color C_TEXT    = new Color(0xe8, 0xf5, 0xe9);
    private static final Color C_TEXT2   = new Color(0xa7, 0xc4, 0xac);

    private static final Font F_BIG   = new Font("SansSerif", Font.BOLD,  26);
    private static final Font F_TITLE = new Font("SansSerif", Font.BOLD,  13);
    private static final Font F_BODY  = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font F_SMALL = new Font("SansSerif", Font.PLAIN, 11);
    private static final Font F_MED   = new Font("SansSerif", Font.BOLD,  14);
    private static final Font F_MONO  = new Font("Monospaced", Font.PLAIN, 11);

    //  State
    private Perceptron perceptron = null;
    private List<PlantDataLoader.Plant>          trainingData = new ArrayList<>();
    private List<SimulatedAnnealing.GardenPlant> gardenPlants = new ArrayList<>();
    private List<SimulatedAnnealing.GardenPlant> saResult     = null;
    private int nextId = 1;

    private double[][] trainX, testX;
    private int[]      trainY, testY;

    private List<Double>  trainAccH = new ArrayList<>();
    private List<Double>  testAccH  = new ArrayList<>();
    private List<Integer> errH      = new ArrayList<>();
    private List<Double>  costH     = new ArrayList<>();

    //  UI refs
    private GardenCanvas  gardenCanvas, resultCanvas;
    private JPanel        plantListPanel, orderListPanel;
    private JLabel        lblTotal, lblNeeds, lblOk, lblDataStatus;
    private JComboBox<String> cbType;
    private JSlider       slMoisture, slWatered, slNumPlants;
    private JLabel        lblMV, lblWV, lblNPV, lblClickHint;
    private Point         pendingPos = null;

    private JSpinner spEpochs, spLR, spErr, spSplit, spNeurons;

    private LearningChart learningChart;
    private JLabel        lblTrainAcc, lblTestAcc, lblTrainCM, lblTestCM;
    private JLabel        lblWeights, lblTestResult;

    private CostChart costChart;
    private JLabel    lblSACost, lblSAIter, lblCostBD;
    private JLabel    lblRC, lblRD, lblRM, lblRE;

    public GardenGUI() {
        setTitle("🌿  Smart Plant Watering Scheduler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1340, 820);
        setLocationRelativeTo(null);
        getContentPane().setBackground(C_BG);
        applyGlobalDefaults();
     //   seedSamplePlants();
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(F_TITLE);
        tabs.setBackground(C_BG);
        tabs.setForeground(C_TEXT2);
        tabs.addTab("   Garden  ",              buildGardenTab());
        tabs.addTab("   Perceptron  ",          buildPerceptronTab());
        tabs.addTab("   Simulated Annealing  ", buildSATab());
        tabs.addTab("   Optimal Route  ",        buildResultTab());
        add(tabs);
        refreshGarden();
    }

    private void applyGlobalDefaults() {
        UIManager.put("Panel.background",              C_BG);
        UIManager.put("TabbedPane.background",         C_BG);
        UIManager.put("TabbedPane.foreground",         C_TEXT);
        UIManager.put("TabbedPane.selected",           C_SURF2);
        UIManager.put("TabbedPane.tabAreaBackground",  C_BG);
        UIManager.put("ScrollPane.background",         C_SURFACE);
        UIManager.put("Viewport.background",           C_SURFACE);
        UIManager.put("ComboBox.background",           C_SURF2);
        UIManager.put("ComboBox.foreground",           C_TEXT);
        UIManager.put("ComboBox.selectionBackground",  C_ACCENT);
        UIManager.put("ComboBox.selectionForeground",  C_BG);
        UIManager.put("Spinner.background",            C_SURF2);
        UIManager.put("FormattedTextField.background", C_SURF2);
        UIManager.put("FormattedTextField.foreground", C_TEXT);
        UIManager.put("TextField.background",          C_SURF2);
        UIManager.put("TextField.foreground",          C_TEXT);
        UIManager.put("TextField.caretForeground",     C_ACCENT);
        UIManager.put("Slider.background",             C_SURFACE);
    }

    //  TAB 1 — GARDEN
    private JPanel buildGardenTab() {
        JPanel root = dp(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        // top bar
        JPanel top = dp(new BorderLayout(12, 0));
        JPanel metrics = dp(new GridLayout(1, 3, 10, 0));
        lblTotal = bigLbl("0"); lblNeeds = bigLbl("0"); lblOk = bigLbl("0");
        metrics.add(mCard("Total Plants", lblTotal, C_MUTED));
        metrics.add(mCard("Need Water",   lblNeeds, C_ACCENT));
        metrics.add(mCard("OK",           lblOk,    C_ORANGE));

        JPanel loadCard = sCard();
        loadCard.setLayout(new BorderLayout(6, 6));
        loadCard.setPreferredSize(new Dimension(260, 0));
        JButton btnLoad = accentBtn("📂  Choose CSV File");
        btnLoad.addActionListener(e -> loadCSV());
        lblDataStatus = lbl("No data loaded", F_SMALL, C_MUTED);
        loadCard.add(lbl("Load Training Data", F_TITLE, C_TEXT), BorderLayout.NORTH);
        loadCard.add(btnLoad,       BorderLayout.CENTER);
        loadCard.add(lblDataStatus, BorderLayout.SOUTH);

        top.add(metrics,  BorderLayout.CENTER);
        top.add(loadCard, BorderLayout.EAST);
        root.add(top, BorderLayout.NORTH);

        // canvas
        gardenCanvas = new GardenCanvas(false);
        gardenCanvas.setBorder(BorderFactory.createLineBorder(C_BORDER));
        gardenCanvas.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                pendingPos = e.getPoint();
                lblClickHint.setText("✅ (" + pendingPos.x + ", " + pendingPos.y + ")");
                lblClickHint.setForeground(C_ACCENT);
            }
        });
        root.add(gardenCanvas, BorderLayout.CENTER);

        // sidebar
        JPanel sb = dp(null);
        sb.setLayout(new BoxLayout(sb, BoxLayout.Y_AXIS));
        sb.setPreferredSize(new Dimension(228, 0));

        JPanel addCard = sCard();
        addCard.add(secTitle("Add New Plant")); addCard.add(vg(6));
        cbType = new JComboBox<>(new String[]{"🌵 Cactus","🌸 Flower","🌿 Herb"});
        cbType.setFont(F_BODY); styleCombo(cbType);
        slMoisture = dSlider(0,100,40); slWatered = dSlider(0,48,12);
        lblMV = lbl("40",F_MED,C_ACCENT); lblWV = lbl("12",F_MED,C_ACCENT);
        slMoisture.addChangeListener(e -> lblMV.setText(String.valueOf(slMoisture.getValue())));
        slWatered .addChangeListener(e -> lblWV.setText(String.valueOf(slWatered .getValue())));
        lblClickHint = lbl("👆 Click garden to place", F_SMALL, C_MUTED);
        JButton btnAdd = secBtn("＋  Add Plant");
        btnAdd.addActionListener(e -> addPlant());

        addCard.add(ml("Plant Type"));    addCard.add(vg(3)); addCard.add(cbType); addCard.add(vg(8));
        addCard.add(ml("Soil Moisture")); addCard.add(vg(3)); addCard.add(slRow(slMoisture, lblMV)); addCard.add(vg(8));
        addCard.add(ml("Last Watered (h)")); addCard.add(vg(3)); addCard.add(slRow(slWatered, lblWV));
        addCard.add(vg(10)); addCard.add(lblClickHint); addCard.add(vg(10)); addCard.add(btnAdd);
        sb.add(addCard); sb.add(vg(10));

        JPanel listCard = sCard();
        listCard.add(secTitle("Plants in Garden")); listCard.add(vg(6));
        plantListPanel = dp(null);
        plantListPanel.setLayout(new BoxLayout(plantListPanel, BoxLayout.Y_AXIS));
        listCard.add(dScroll(plantListPanel, 210, 260));
        sb.add(listCard);
        root.add(sb, BorderLayout.EAST);
        return root;
    }

    //  TAB 2 — PERCEPTRON
    private JPanel buildPerceptronTab() {
        JPanel root = dp(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel paramCard = sCard();
        paramCard.setLayout(new GridLayout(2, 6, 14, 8));

        spEpochs = dSpinner(new SpinnerNumberModel(100,   1,    1000, 10));
        spLR     = dSpinner(new SpinnerNumberModel(0.05,  0.001, 1.0, 0.01));
        spErr    = dSpinner(new SpinnerNumberModel(0.010, 0.001, 0.5, 0.001));
        spSplit  = dSpinner(new SpinnerNumberModel(80,    50,    95,   5));
        spNeurons= dSpinner(new SpinnerNumberModel(4,     1,     10,   1));
        ((JSpinner.DefaultEditor)spLR .getEditor()).getTextField().setColumns(5);
        ((JSpinner.DefaultEditor)spErr.getEditor()).getTextField().setColumns(5);

        paramCard.add(pl("Epochs"));        paramCard.add(pl("Learning Rate"));
        paramCard.add(pl("Error Thresh")); paramCard.add(pl("Train Split %"));
        paramCard.add(pl("Neurons"));      paramCard.add(new JLabel());
        paramCard.add(spEpochs); paramCard.add(spLR); paramCard.add(spErr);
        paramCard.add(spSplit);  paramCard.add(spNeurons);
        JButton btnTrain = accentBtn("🚀  Train Perceptron");
        btnTrain.addActionListener(e -> runTraining());
        paramCard.add(btnTrain);
        root.add(paramCard, BorderLayout.NORTH);

        JPanel mid = dp(new GridLayout(1, 3, 10, 0));

        // chart
        learningChart = new LearningChart();
        JPanel cc = sCard();
        cc.add(secTitle("Learning Curve")); cc.add(vg(4)); cc.add(learningChart);
        mid.add(cc);

        // confusion matrices
        JPanel cmCard = sCard();
        cmCard.add(secTitle("Confusion Matrices (2×2)")); cmCard.add(vg(6));
        lblTrainAcc = lbl("—", F_BIG, C_ACCENT);
        lblTestAcc  = lbl("—", F_BIG, C_ORANGE);
        lblTrainCM  = new JLabel("<html><i style='color:#6b8f71'>Train first…</i></html>");
        lblTestCM   = new JLabel("<html><i style='color:#6b8f71'>Train first…</i></html>");
        JPanel accRow = dp(new GridLayout(1,2,8,0));
        accRow.add(miniM("Train Acc", lblTrainAcc));
        accRow.add(miniM("Test Acc",  lblTestAcc));
        cmCard.add(accRow); cmCard.add(vg(10));
        JPanel cmIn = dp(null);
        cmIn.setLayout(new BoxLayout(cmIn, BoxLayout.Y_AXIS));
        cmIn.add(lbl("Training Set:", F_SMALL, C_MUTED)); cmIn.add(vg(4)); cmIn.add(lblTrainCM);
        cmIn.add(vg(10)); cmIn.add(dSep()); cmIn.add(vg(10));
        cmIn.add(lbl("Test Set:", F_SMALL, C_MUTED)); cmIn.add(vg(4)); cmIn.add(lblTestCM);
        cmCard.add(dScroll(cmIn, 0, 0));
        mid.add(cmCard);

        // live test
        JPanel tc = sCard();
        tc.add(secTitle("Live Prediction")); tc.add(vg(8));
        JSlider tM = dSlider(0,100,30); JSlider tW = dSlider(0,48,20);
        JLabel tMv = lbl("30",F_MED,C_ACCENT), tWv = lbl("20",F_MED,C_ACCENT);
        JComboBox<String> tType = new JComboBox<>(new String[]{"🌵 Cactus","🌸 Flower","🌿 Herb"});
        styleCombo(tType);
        lblTestResult = lbl("Train first", F_MED, C_MUTED);
        Runnable upd = () -> {
            if (perceptron == null) return;
            int n2 = ((Number)spNeurons.getValue()).intValue();
            int pred = perceptron.predict(fv(tM.getValue(), tW.getValue(), tType.getSelectedIndex(), n2));
            lblTestResult.setText(pred==1 ? "💧 Needs Water" : "✅ Does NOT need water");
            lblTestResult.setForeground(pred==1 ? C_ACCENT : C_MUTED);
        };
        tM.addChangeListener(e -> { tMv.setText(String.valueOf(tM.getValue())); upd.run(); });
        tW.addChangeListener(e -> { tWv.setText(String.valueOf(tW.getValue())); upd.run(); });
        tType.addActionListener(e -> upd.run());
        tc.add(ml("Soil moisture"));    tc.add(vg(3)); tc.add(slRow(tM,tMv)); tc.add(vg(8));
        tc.add(ml("Last watered (h)")); tc.add(vg(3)); tc.add(slRow(tW,tWv)); tc.add(vg(8));
        tc.add(ml("Plant type"));       tc.add(vg(3)); tc.add(tType);
        tc.add(vg(14)); tc.add(dSep()); tc.add(vg(10));
        tc.add(lbl("Prediction:", F_SMALL, C_MUTED)); tc.add(vg(6)); tc.add(lblTestResult);
        mid.add(tc);
        root.add(mid, BorderLayout.CENTER);

        // weights
        JPanel wCard = sCard(); wCard.setLayout(new BorderLayout());
        wCard.add(secTitle("Final Weights"), BorderLayout.NORTH);
        lblWeights = new JLabel("<html><i style='color:#6b8f71'>Run training first</i></html>");
        lblWeights.setFont(F_MONO);
        wCard.add(dScroll(lblWeights, 0, 70), BorderLayout.CENTER);
        root.add(wCard, BorderLayout.SOUTH);
        return root;
    }

    //  TAB 3 — SA
    private JPanel buildSATab() {
        JPanel root = dp(new BorderLayout(10,10));
        root.setBorder(new EmptyBorder(14,14,14,14));

        JPanel metrics = dp(new GridLayout(1,4,10,0));
        lblSACost = bigLbl("—"); lblSAIter = bigLbl("—");
        metrics.add(mCard("Initial Temp",  bigLbl("1000"),  C_MUTED));
        metrics.add(mCard("Cooling Rate",  bigLbl("0.995"), C_MUTED));
        metrics.add(mCard("Iterations",    lblSAIter,       C_ORANGE));
        metrics.add(mCard("Best Cost",     lblSACost,       C_ACCENT));
        root.add(metrics, BorderLayout.NORTH);

        JPanel mid = dp(new GridLayout(1,2,10,0));
        costChart = new CostChart();
        JPanel cCard = sCard();
        cCard.add(secTitle("Cost Convergence")); cCard.add(vg(4)); cCard.add(costChart);
        mid.add(cCard);

        JPanel stCard = sCard();
        stCard.add(secTitle("SA Algorithm Steps")); stCard.add(vg(10));
        String[] steps = {
            "Start with a random candidate order",
            "Calculate initial cost",
            "Swap two plants → new candidate",
            "Calculate new cost",
            "Accept if better — or with P = e^(−ΔC/T)",
            "Cool temperature × 0.995",
            "Repeat until T < 0.001 (converged)"
        };
        for (int i = 0; i < steps.length; i++) {
            JPanel row = dp(new BorderLayout(8,0));
            row.setBorder(new EmptyBorder(4,0,4,0));
            JLabel num = lbl(String.valueOf(i+1), F_TITLE, C_ACCENT);
            num.setPreferredSize(new Dimension(16,16));
            row.add(num, BorderLayout.WEST);
            row.add(lbl(steps[i], F_BODY, C_TEXT2), BorderLayout.CENTER);
            stCard.add(row);
        }
        mid.add(stCard);
        root.add(mid, BorderLayout.CENTER);

        lblCostBD = new JLabel("<html><i style='color:#6b8f71'>Run SA first</i></html>");
        lblCostBD.setFont(F_BODY);
        JPanel bCard = sCard();
        bCard.add(secTitle("Cost Breakdown")); bCard.add(vg(6)); bCard.add(lblCostBD);

        slNumPlants = dSlider(1,20,4);
        lblNPV = lbl("4",F_MED,C_ACCENT);
        slNumPlants.addChangeListener(e -> lblNPV.setText(String.valueOf(slNumPlants.getValue())));

        JPanel ctrl = dp(new FlowLayout(FlowLayout.CENTER,12,6));
        ctrl.add(lbl("Plants to water:", F_BODY, C_TEXT2));
        ctrl.add(slNumPlants); ctrl.add(lblNPV);
        JButton btnSA = accentBtn("🔥  Run Simulated Annealing");
        btnSA.addActionListener(e -> runSA());
        ctrl.add(btnSA);

        JPanel south = dp(new BorderLayout(0,8));
        south.add(bCard, BorderLayout.CENTER);
        south.add(ctrl,  BorderLayout.SOUTH);
        root.add(south, BorderLayout.SOUTH);
        return root;
    }

    //  TAB 4 — OPTIMAL ROUTE
    private JPanel buildResultTab() {
        JPanel root = dp(new BorderLayout(10,10));
        root.setBorder(new EmptyBorder(14,14,14,14));

        JPanel metrics = dp(new GridLayout(1,4,10,0));
        lblRC = bigLbl("—"); lblRD = bigLbl("—"); lblRM = bigLbl("—"); lblRE = bigLbl("—");
        metrics.add(mCard("Plants in Route", lblRC, C_ACCENT));
        metrics.add(mCard("Total Distance",  lblRD, C_MUTED));
        metrics.add(mCard("Missed",          lblRM, C_RED));
        metrics.add(mCard("Extra Watered",   lblRE, C_ORANGE));
        root.add(metrics, BorderLayout.NORTH);

        JPanel mid = dp(new BorderLayout(10,0));
        resultCanvas = new GardenCanvas(true);
        resultCanvas.setBorder(BorderFactory.createLineBorder(C_BORDER));
        mid.add(resultCanvas, BorderLayout.CENTER);

        JPanel oCard = sCard();
        oCard.setPreferredSize(new Dimension(235,0));
        oCard.add(secTitle("Watering Order")); oCard.add(vg(6));
        orderListPanel = dp(null);
        orderListPanel.setLayout(new BoxLayout(orderListPanel, BoxLayout.Y_AXIS));
        oCard.add(dScroll(orderListPanel, 215, 0));
        mid.add(oCard, BorderLayout.EAST);
        root.add(mid, BorderLayout.CENTER);
        return root;
    }

    //  LOGIC
    private void loadCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)","csv"));
        fc.setCurrentDirectory(new File(System.getProperty("user.home")));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try {
            trainingData = PlantDataLoader.loadFromCSV(f.getAbsolutePath());
            if (trainingData.isEmpty()) { JOptionPane.showMessageDialog(this,"File empty!","Error",JOptionPane.WARNING_MESSAGE); return; }
            PlantDataLoader.printSummary(trainingData);
            trainX=null; testX=null; trainY=null; testY=null;
            lblDataStatus.setText("✅ "+trainingData.size()+" records: "+f.getName());
            lblDataStatus.setForeground(C_ACCENT);
            JOptionPane.showMessageDialog(this,
                "Loaded "+trainingData.size()+" plants from:\n"+f.getName()+
                "\n\n→ Go to Perceptron tab → Set params → Train","Data Loaded ✅",JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            lblDataStatus.setText("❌ "+ex.getMessage()); lblDataStatus.setForeground(C_RED);
            JOptionPane.showMessageDialog(this,"Could not read: "+f.getName(),"Load Error",JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runTraining() {
        if (trainingData.isEmpty()) {
            int c = JOptionPane.showConfirmDialog(this,"No CSV! Use 100 synthetic samples?","No Data",JOptionPane.YES_NO_OPTION);
            if (c != JOptionPane.YES_OPTION) return;
            genSynth();
        }
        int    ep  = ((Number)spEpochs .getValue()).intValue();
        double lr  = ((Number)spLR     .getValue()).doubleValue();
        double et  = ((Number)spErr    .getValue()).doubleValue();
        int    sp  = ((Number)spSplit  .getValue()).intValue();
        int    n   = ((Number)spNeurons.getValue()).intValue();

        int    sz  = trainingData.size();
        double[][] allX = fm(trainingData, n);
        int[]      allY = PlantDataLoader.extractLabels(trainingData);
        List<Integer> idx = new ArrayList<>(); for (int i=0;i<sz;i++) idx.add(i);
        Collections.shuffle(idx, new Random(42));
        int trSz = (int)Math.round(sz*sp/100.0), tsSz = sz-trSz;
        if (trSz<2||tsSz<2) { JOptionPane.showMessageDialog(this,"Not enough data!","Error",JOptionPane.ERROR_MESSAGE); return; }
        trainX=new double[trSz][n]; trainY=new int[trSz];
        testX =new double[tsSz][n]; testY =new int[tsSz];
        for (int i=0;i<trSz;i++){trainX[i]=allX[idx.get(i)];      trainY[i]=allY[idx.get(i)];}
        for (int i=0;i<tsSz;i++){testX[i] =allX[idx.get(trSz+i)]; testY[i] =allY[idx.get(trSz+i)];}

        perceptron = new Perceptron(n);
        perceptron.learningRate=lr; perceptron.epochs=ep; perceptron.errorThreshold=et;
        perceptron.train(trainX,trainY,testX,testY);

        trainAccH = new ArrayList<>(perceptron.trainAccHistory);
        testAccH  = new ArrayList<>(perceptron.testAccHistory);
        errH      = new ArrayList<>(perceptron.trainErrHistory);

        double trAcc = perceptron.getTrainAccuracy();
        double tsAcc = perceptron.getTestAccuracy();
        lblTrainAcc.setText(String.format("%.1f%%",trAcc)); lblTrainAcc.setForeground(trAcc>=70?C_ACCENT:C_RED);
        lblTestAcc .setText(String.format("%.1f%%",tsAcc)); lblTestAcc .setForeground(tsAcc>=70?C_ORANGE:C_RED);
        lblTrainCM.setText(cmHTML(true));
        lblTestCM .setText(cmHTML(false));

        double[] w = perceptron.getWeights();
        String[] fn={"moisture","last_watered","plant_type","moisture²","lastWatered²","m×w","type×m","type×w","m×w×t","1-m"};
        StringBuilder sb2 = new StringBuilder("<html><span style='color:#a7c4ac'>");
        for (int j=0;j<n;j++) {
            String nm = j<fn.length?fn[j]:"feat"+(j+1);
            sb2.append(String.format("w[%d] <span style='color:#6b8f71'>(%s)</span> = <b style='color:#4ade80'>%.4f</b><br>",j+1,nm,w[j]));
        }
        sb2.append(String.format("bias = <b style='color:#fb923c'>%.4f</b></span></html>",perceptron.getBias()));
        lblWeights.setText(sb2.toString());

        for (SimulatedAnnealing.GardenPlant p : gardenPlants)
            p.needsWater = perceptron.predict(fv((int)p.soilMoisture,(int)p.lastWatered,p.plantType,n));
        learningChart.repaint(); refreshGarden();

        JOptionPane.showMessageDialog(this,
            String.format("✅ Training Complete!\n\nTrain: %.1f%%\nTest:  %.1f%%\n\nSamples: %d / %d\nEpochs: %d | Neurons: %d\n\nGarden re-predicted ✓",
            trAcc,tsAcc,trSz,tsSz,perceptron.trainAccHistory.size(),n),"Results",JOptionPane.INFORMATION_MESSAGE);
    }

    private String cmHTML(boolean isTrain) {
        int tp=isTrain?perceptron.TP_tr:perceptron.TP, tn=isTrain?perceptron.TN_tr:perceptron.TN;
        int fp=isTrain?perceptron.FP_tr:perceptron.FP, fn=isTrain?perceptron.FN_tr:perceptron.FN;
        int tot=tp+tn+fp+fn;
        double acc=(tot>0?(tp+tn)*100.0/tot:0), prec=((tp+fp)>0?tp*100.0/(tp+fp):0);
        double rec=((tp+fn)>0?tp*100.0/(tp+fn):0), f1=((prec+rec)>0?2*prec*rec/(prec+rec):0);
        String ac=acc>=70?"#4ade80":"#f87171";
        return String.format(
            "<html><table border='1' cellpadding='4' cellspacing='0' style='border-color:#2a3d2e;color:#e8f5e9'>"+
            "<tr style='background:#1c2b1e'><th>&nbsp;</th><th>Pred:0 OK</th><th>Pred:1 Water</th></tr>"+
            "<tr><td style='background:#1c2b1e'><b>Act:0 OK</b></td>"+
            "<td style='background:#163421'><b style='color:#4ade80'>TN=%d</b></td>"+
            "<td style='background:#2d1616'>FP=%d</td></tr>"+
            "<tr><td style='background:#1c2b1e'><b>Act:1 Water</b></td>"+
            "<td style='background:#2d1616'>FN=%d</td>"+
            "<td style='background:#163421'><b style='color:#4ade80'>TP=%d</b></td></tr>"+
            "</table><br><span style='color:#a7c4ac'>"+
            "<b style='color:%s'>Acc:%.1f%%</b> &nbsp; Prec:%.1f%% &nbsp; Rec:%.1f%% &nbsp; F1:%.1f%%"+
            "</span></html>",tn,fp,fn,tp,ac,acc,prec,rec,f1);
    }

    private void runSA() {
        if (gardenPlants.isEmpty()){JOptionPane.showMessageDialog(this,"Add plants first!","",JOptionPane.WARNING_MESSAGE);return;}
        if (perceptron==null){JOptionPane.showMessageDialog(this,"Train Perceptron first!","",JOptionPane.WARNING_MESSAGE);return;}
        costH.clear();
        int numSel = Math.min(slNumPlants.getValue(), gardenPlants.size());
        List<SimulatedAnnealing.GardenPlant> nl=new ArrayList<>(), ol=new ArrayList<>();
        for (SimulatedAnnealing.GardenPlant p:gardenPlants){if(p.needsWater==1)nl.add(p);else ol.add(p);}
        List<SimulatedAnnealing.GardenPlant> sel=new ArrayList<>();
        for (int i=0;i<nl.size()&&sel.size()<numSel;i++) sel.add(nl.get(i));
        for (int i=0;i<ol.size()&&sel.size()<numSel;i++) sel.add(ol.get(i));

        SimulatedAnnealing sa=new SimulatedAnnealing(1000.0,0.995,3000);
        Random rand=new Random(42);
        List<SimulatedAnnealing.GardenPlant> cur=new ArrayList<>(sel);
        Collections.shuffle(cur,rand);
        double cc=sa.calculateCost(cur,gardenPlants);
        List<SimulatedAnnealing.GardenPlant> best=new ArrayList<>(cur); double bc=cc, T=1000.0; int iters=0;

        for (int i=1;i<=3000;i++){
            List<SimulatedAnnealing.GardenPlant> cand=new ArrayList<>(cur);
            int a=rand.nextInt(cand.size()),b2; do{b2=rand.nextInt(cand.size());}while(b2==a);
            SimulatedAnnealing.GardenPlant tmp=cand.get(a);cand.set(a,cand.get(b2));cand.set(b2,tmp);
            double nc=sa.calculateCost(cand,gardenPlants);
            if(nc<cc||rand.nextDouble()<Math.exp(-(nc-cc)/T)){cur=cand;cc=nc;}
            if(cc<bc){best=new ArrayList<>(cur);bc=cc;}
            T*=0.995;iters=i;
            if(i%100==0)costH.add(bc);
            if(T<0.001)break;
        }
        saResult=best;
        lblSACost.setText(String.format("%.0f",bc)); lblSAIter.setText(String.valueOf(iters));

        Set<Integer> ids=new HashSet<>(); for(var p:best)ids.add(p.id);
        int missed=0; for(var p:gardenPlants)if(p.needsWater==1&&!ids.contains(p.id))missed++;
        int extra=0;  for(var p:best)if(p.needsWater==0)extra++;
        double dist=0; for(int i=0;i<best.size()-1;i++)dist+=eucl(best.get(i),best.get(i+1));

        lblCostBD.setText(String.format(
            "<html><span style='color:#a7c4ac'>"+
            "Missed &nbsp;&nbsp;: <b style='color:#f87171'>%d × 100 = %.0f</b><br>"+
            "Distance : <b style='color:#fb923c'>%.1f × 1 = %.1f</b><br>"+
            "Extra &nbsp;&nbsp;&nbsp;: <b style='color:#fbbf24'>%d × 50 = %.0f</b><br>"+
            "<hr style='border-color:#2a3d2e'>"+
            "Total &nbsp;&nbsp;&nbsp;: <b style='color:#4ade80'>%.1f</b></span></html>",
            missed,missed*100.0,dist,dist,extra,extra*50.0,bc));

        lblRC.setText(String.valueOf(best.size())); lblRD.setText(String.format("%.0fpx",dist));
        lblRM.setText(String.valueOf(missed)); lblRM.setForeground(missed>0?C_RED:C_ACCENT);
        lblRE.setText(String.valueOf(extra));  lblRE.setForeground(extra>0?C_ORANGE:C_ACCENT);

        orderListPanel.removeAll();
        for (int i=0;i<best.size();i++){
            SimulatedAnnealing.GardenPlant p=best.get(i);
            JPanel row=dp(new BorderLayout(8,0)); row.setBorder(new EmptyBorder(6,4,6,4));
            JLabel badge=new JLabel(String.valueOf(i+1),SwingConstants.CENTER);
            badge.setFont(F_TITLE); badge.setForeground(C_BG); badge.setOpaque(true);
            badge.setBackground(C_ACCENT); badge.setPreferredSize(new Dimension(24,24));
            JLabel info=new JLabel(String.format(
                "<html><b style='color:#e8f5e9'>#%d %s</b><br><small style='color:#6b8f71'>M:%d  W:%dh</small></html>",
                p.id,p.getTypeName(),(int)p.soilMoisture,(int)p.lastWatered));
            JLabel tag=new JLabel(p.needsWater==1?"💧":"✓");
            tag.setFont(F_TITLE); tag.setForeground(p.needsWater==1?C_ACCENT:C_MUTED);
            row.add(badge,BorderLayout.WEST); row.add(info,BorderLayout.CENTER); row.add(tag,BorderLayout.EAST);
            orderListPanel.add(row);
            if(i<best.size()-1){
                JLabel arr=new JLabel(String.format("   ↓  %.0f px",eucl(best.get(i),best.get(i+1))));
                arr.setFont(F_SMALL); arr.setForeground(C_BORDER); orderListPanel.add(arr);
            }
        }
        orderListPanel.revalidate(); orderListPanel.repaint();
        costChart.repaint(); resultCanvas.repaint();
    }

    // helpers
    private void addPlant(){
        if(pendingPos==null){JOptionPane.showMessageDialog(this,"Click garden first!","",JOptionPane.WARNING_MESSAGE);return;}
        int t=cbType.getSelectedIndex(), m=slMoisture.getValue(), w=slWatered.getValue();
        int nw=perceptron==null?((m<35&&w>20)?1:0):perceptron.predict(fv(m,w,t,((Number)spNeurons.getValue()).intValue()));
        gardenPlants.add(new SimulatedAnnealing.GardenPlant(nextId++,pendingPos.x,pendingPos.y,m,w,t,nw));
        pendingPos=null; lblClickHint.setText("👆 Click garden to place"); lblClickHint.setForeground(C_MUTED);
        refreshGarden();
    }

   /* private void seedSamplePlants(){
        gardenPlants.add(new SimulatedAnnealing.GardenPlant(nextId++,120,180,15,40,0,1));
        gardenPlants.add(new SimulatedAnnealing.GardenPlant(nextId++,300,130,60, 5,1,0));
        gardenPlants.add(new SimulatedAnnealing.GardenPlant(nextId++,440,280,30,28,2,1));
        gardenPlants.add(new SimulatedAnnealing.GardenPlant(nextId++,190,290,20,35,1,1));
        gardenPlants.add(new SimulatedAnnealing.GardenPlant(nextId++,370, 80,75, 3,0,0));
        gardenPlants.add(new SimulatedAnnealing.GardenPlant(nextId++, 90,120,25,42,2,1));
    }*/

    private void refreshGarden(){
        if(gardenCanvas!=null)gardenCanvas.repaint();
        if(resultCanvas!=null)resultCanvas.repaint();
        int needs=(int)gardenPlants.stream().filter(p->p.needsWater==1).count();
        lblTotal.setText(String.valueOf(gardenPlants.size()));
        lblNeeds.setText(String.valueOf(needs));
        lblOk   .setText(String.valueOf(gardenPlants.size()-needs));
        // list
        plantListPanel.removeAll();
        for(SimulatedAnnealing.GardenPlant p:gardenPlants){
            JPanel row=dp(new BorderLayout(6,0)); row.setBorder(new EmptyBorder(3,2,3,2));
            JLabel dot=lbl("●",F_SMALL,p.needsWater==1?C_ACCENT:C_MUTED);
            JLabel info=new JLabel(String.format(
                "<html><b style='color:#e8f5e9'>#%d %s</b> <small style='color:#6b8f71'>M:%d W:%dh</small></html>",
                p.id,p.getTypeName(),(int)p.soilMoisture,(int)p.lastWatered));
            info.setFont(F_SMALL);
            row.add(dot,BorderLayout.WEST); row.add(info,BorderLayout.CENTER);
            plantListPanel.add(row);
        }
        plantListPanel.revalidate(); plantListPanel.repaint();
    }

    private void genSynth(){
        trainingData.clear(); Random r=new Random(42);
        for(int i=0;i<100;i++){
            double m=5+r.nextInt(90),w=r.nextInt(49);int t=r.nextInt(3),nw=(m<40||w>24)?1:0;
            trainingData.add(new PlantDataLoader.Plant(m,w,t,nw));
        }
        lblDataStatus.setText("⚠ 100 synthetic samples"); lblDataStatus.setForeground(C_ORANGE);
    }

    private double eucl(SimulatedAnnealing.GardenPlant a,SimulatedAnnealing.GardenPlant b){
        return Math.sqrt(Math.pow(a.x-b.x,2)+Math.pow(a.y-b.y,2));
    }
    private double[] fv(int m,int w,int t,int n){
        double md=m/100.0,wd=w/48.0,td=t/2.0;
        double[]all={md,wd,td,md*md,wd*wd,md*wd,td*md,td*wd,md*wd*td,1-md};
        double[]x=new double[n]; for(int j=0;j<n;j++)x[j]=j<all.length?all[j]:0; return x;
    }
    private double[][] fm(List<PlantDataLoader.Plant> data,int n){
        double[][]X=new double[data.size()][n];
        for(int i=0;i<data.size();i++){PlantDataLoader.Plant p=data.get(i);X[i]=fv((int)p.soilMoisture,(int)p.lastWatered,p.plantType,n);}
        return X;
    }

    //  INNER — Garden Canvas
    class GardenCanvas extends JPanel {
        final boolean isResult;
        GardenCanvas(boolean r){this.isResult=r;setBackground(new Color(0x0d,0x17,0x10));}
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            // dot grid
            g2.setColor(C_SURF2);
            for(int x=0;x<getWidth();x+=40) for(int y=0;y<getHeight();y+=40) g2.fillOval(x-1,y-1,3,3);

            List<SimulatedAnnealing.GardenPlant> route=isResult?saResult:null;
            if(route!=null&&route.size()>1){
                g2.setColor(new Color(0xfb,0x92,0x3c,110));
                g2.setStroke(new BasicStroke(2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,0,new float[]{8,5},0));
                for(int i=0;i<route.size()-1;i++)
                    g2.drawLine(route.get(i).x,route.get(i).y,route.get(i+1).x,route.get(i+1).y);
                g2.setFont(F_SMALL); g2.setColor(C_ORANGE);
                for(int i=0;i<route.size()-1;i++){
                    int mx=(route.get(i).x+route.get(i+1).x)/2, my=(route.get(i).y+route.get(i+1).y)/2;
                    g2.drawString(String.format("%.0fpx",eucl(route.get(i),route.get(i+1))),mx+4,my-4);
                }
            }
            for(SimulatedAnnealing.GardenPlant p:gardenPlants){
                boolean inR=route!=null&&route.stream().anyMatch(r->r.id==p.id);
                Color fill,ring;
                if(isResult&&inR){fill=C_ORANGE;ring=new Color(0xff,0xb3,0x70);}
                else if(p.needsWater==1){fill=C_ACCENT2;ring=C_ACCENT;}
                else{fill=new Color(0x3a,0x52,0x3e);ring=new Color(0x55,0x73,0x59);}
                if(p.needsWater==1||( isResult&&inR)){
                    g2.setColor(isResult&&inR?new Color(0xfb,0x92,0x3c,35):new Color(0x4a,0xde,0x80,28));
                    g2.fillOval(p.x-22,p.y-22,44,44);
                }
                g2.setColor(fill); g2.fillOval(p.x-14,p.y-14,28,28);
                g2.setColor(ring); g2.setStroke(new BasicStroke(1.5f)); g2.drawOval(p.x-14,p.y-14,28,28);
                String lbl2;
                if(isResult&&route!=null){int idx=-1;for(int i=0;i<route.size();i++)if(route.get(i).id==p.id){idx=i+1;break;}lbl2=idx>0?String.valueOf(idx):String.valueOf(p.id);}
                else lbl2=String.valueOf(p.id);
                g2.setFont(new Font("SansSerif",Font.BOLD,10)); g2.setColor(C_BG);
                FontMetrics fm2=g2.getFontMetrics();
                g2.drawString(lbl2,p.x-fm2.stringWidth(lbl2)/2,p.y+fm2.getAscent()/2-1);
                g2.setColor(C_MUTED); g2.setFont(F_SMALL);
                String nm=new String[]{"Cactus","Flower","Herb"}[p.plantType];
                FontMetrics fm3=g2.getFontMetrics();
                g2.drawString(nm,p.x-fm3.stringWidth(nm)/2,p.y+26);
            }
            int lx=10,ly=getHeight()-14; g2.setFont(F_SMALL);
            leg(g2,lx,ly,C_ACCENT2,"Needs water"); leg(g2,lx+115,ly,new Color(0x3a,0x52,0x3e),"OK");
            if(isResult)leg(g2,lx+165,ly,C_ORANGE,"In route");
        }
        private void leg(Graphics2D g2,int x,int y,Color c,String l){
            g2.setColor(c);g2.fillOval(x,y-8,10,10);g2.setColor(C_MUTED);g2.drawString(l,x+14,y);
        }
    }

    //  INNER — Learning Chart
    class LearningChart extends JPanel {
        LearningChart(){setBackground(C_SURFACE);setPreferredSize(new Dimension(0,220));}
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            if(trainAccH.isEmpty()){g2.setColor(C_MUTED);g2.setFont(F_SMALL);g2.drawString("Train perceptron to see curve",20,getHeight()/2);return;}
            int pad=44,w=getWidth()-pad*2,h=getHeight()-pad*2;
            for(int i=0;i<=4;i++){
                int y=pad+h-i*h/4; g2.setColor(C_BORDER);g2.setStroke(new BasicStroke(0.5f));g2.drawLine(pad,y,pad+w,y);
                g2.setColor(C_MUTED);g2.setFont(F_SMALL);g2.drawString(i*25+"%",4,y+4);
            }
            int n=trainAccH.size();
            if(n>1){g2.setColor(C_ACCENT);g2.setStroke(new BasicStroke(2f));
                for(int i=1;i<n;i++){int x1=pad+(i-1)*w/(n-1),y1=pad+h-(int)(trainAccH.get(i-1)*h/100),x2=pad+i*w/(n-1),y2=pad+h-(int)(trainAccH.get(i)*h/100);g2.drawLine(x1,y1,x2,y2);}}
            int nt=testAccH.size();
            if(nt>1){g2.setColor(C_ORANGE);g2.setStroke(new BasicStroke(2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,0,new float[]{6,4},0));
                for(int i=1;i<nt;i++){int x1=pad+(i-1)*w/(nt-1),y1=pad+h-(int)(testAccH.get(i-1)*h/100),x2=pad+i*w/(nt-1),y2=pad+h-(int)(testAccH.get(i)*h/100);g2.drawLine(x1,y1,x2,y2);}}
            if(!errH.isEmpty()&&n>0){int mxE=errH.stream().mapToInt(Integer::intValue).max().orElse(1);
                if(mxE>0){g2.setColor(new Color(0xfb,0x92,0x3c,45));for(int i=0;i<n;i++){int x=pad+(n>1?i*w/(n-1):w/2),bh=(int)(errH.get(i)*h/mxE/3);g2.fillRect(x-2,pad+h-bh,4,bh);}}}
            g2.setFont(F_SMALL);g2.setColor(C_ACCENT);g2.drawString("— Train",pad+4,pad-6);
            g2.setColor(C_ORANGE);g2.drawString("--- Test",pad+60,pad-6);
            g2.setColor(C_MUTED);g2.drawString("Epochs",pad+w/2-20,getHeight()-4);
        }
    }

    //  INNER — Cost Chart
    class CostChart extends JPanel {
        CostChart(){setBackground(C_SURFACE);setPreferredSize(new Dimension(0,200));}
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            if(costH.isEmpty()){g2.setColor(C_MUTED);g2.setFont(F_SMALL);g2.drawString("Run SA to see convergence",14,getHeight()/2);return;}
            int pad=44,w=getWidth()-pad*2,h=getHeight()-pad*2;
            double maxC=costH.stream().mapToDouble(Double::doubleValue).max().orElse(1);
            double minC=costH.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            double rng=Math.max(maxC-minC,1);
            for(int i=0;i<=4;i++){int y=pad+i*h/4;g2.setColor(C_BORDER);g2.setStroke(new BasicStroke(0.5f));g2.drawLine(pad,y,pad+w,y);g2.setColor(C_MUTED);g2.setFont(F_SMALL);g2.drawString(String.format("%.0f",maxC-i*(maxC-minC)/4),2,y+4);}
            int n=costH.size();
            if(n>1){
                int[]xs=new int[n+2],ys=new int[n+2];
                for(int i=0;i<n;i++){xs[i]=pad+i*w/(n-1);ys[i]=pad+(int)((maxC-costH.get(i))*h/rng);}
                xs[n]=pad+w;ys[n]=pad+h;xs[n+1]=pad;ys[n+1]=pad+h;
                g2.setPaint(new GradientPaint(0,pad,new Color(0xfb,0x92,0x3c,55),0,pad+h,new Color(0xfb,0x92,0x3c,5)));
                g2.fillPolygon(xs,ys,n+2);
                g2.setColor(C_ORANGE);g2.setStroke(new BasicStroke(2.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                for(int i=1;i<n;i++)g2.drawLine(xs[i-1],ys[i-1],xs[i],ys[i]);
            }
            g2.setColor(C_MUTED);g2.setFont(F_SMALL);g2.drawString("Iterations (×100)",pad+w/2-35,getHeight()-4);
        }
    }

    //  UI factory helpers
    private JPanel dp(LayoutManager lm){JPanel p=lm!=null?new JPanel(lm):new JPanel();p.setBackground(C_BG);p.setOpaque(true);return p;}
    private JPanel sCard(){
        JPanel p=new JPanel();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.setBackground(C_SURFACE);p.setOpaque(true);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(C_BORDER),new EmptyBorder(12,14,12,14)));
        return p;
    }
    private JPanel mCard(String l,JLabel v,Color vc){
        JPanel p=new JPanel();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setBackground(C_SURFACE);p.setOpaque(true);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(C_BORDER),new EmptyBorder(12,16,12,16)));
        JLabel ll=lbl(l,F_SMALL,C_MUTED);v.setFont(F_BIG);v.setForeground(vc);p.add(ll);p.add(vg(4));p.add(v);return p;
    }
    private JPanel miniM(String l,JLabel v){
        JPanel p=new JPanel();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.setBackground(C_SURF2);p.setBorder(new EmptyBorder(8,10,8,10));
        p.add(lbl(l,F_SMALL,C_MUTED));p.add(vg(2));p.add(v);return p;
    }
    private JLabel bigLbl(String t){JLabel l=new JLabel(t);l.setFont(F_BIG);l.setForeground(C_TEXT);return l;}
    private JLabel lbl(String t,Font f,Color c){JLabel l=new JLabel(t);l.setFont(f);l.setForeground(c);return l;}
    private JLabel secTitle(String t){JLabel l=new JLabel(t.toUpperCase());l.setFont(new Font("SansSerif",Font.BOLD,10));l.setForeground(C_MUTED);return l;}
    private JLabel ml(String t){JLabel l=new JLabel(t);l.setFont(F_SMALL);l.setForeground(C_TEXT2);return l;}
    private JLabel pl(String t){JLabel l=new JLabel(t);l.setFont(F_TITLE);l.setForeground(C_TEXT);return l;}
    private Component vg(int h){return Box.createVerticalStrut(h);}
    private JSeparator dSep(){JSeparator s=new JSeparator();s.setForeground(C_BORDER);s.setBackground(C_SURFACE);return s;}
    private JPanel slRow(JSlider sl,JLabel v){
        JPanel p=dp(new BorderLayout(6,0));v.setFont(F_MED);v.setForeground(C_ACCENT);p.add(sl,BorderLayout.CENTER);p.add(v,BorderLayout.EAST);return p;
    }
    private JSlider dSlider(int mn,int mx,int v){
        JSlider s=new JSlider(mn,mx,v);s.setBackground(C_SURFACE);s.setForeground(C_ACCENT);s.setBorder(null);return s;
    }
    private JSpinner dSpinner(SpinnerModel m){
        JSpinner sp=new JSpinner(m);sp.setBackground(C_SURF2);sp.setForeground(C_TEXT);
        sp.setBorder(BorderFactory.createLineBorder(C_BORDER));
        JComponent ed=sp.getEditor();
        if(ed instanceof JSpinner.DefaultEditor){
            JTextField tf=((JSpinner.DefaultEditor)ed).getTextField();
            tf.setBackground(C_SURF2);tf.setForeground(C_TEXT);tf.setCaretColor(C_ACCENT);tf.setBorder(new EmptyBorder(2,4,2,4));
        }
        return sp;
    }
    private void styleCombo(JComboBox<?> cb){cb.setBackground(C_SURF2);cb.setForeground(C_TEXT);cb.setFont(F_BODY);cb.setBorder(BorderFactory.createLineBorder(C_BORDER));}
    private JScrollPane dScroll(Component v,int w,int h){
        JScrollPane sc=new JScrollPane(v);sc.setBackground(C_SURFACE);sc.getViewport().setBackground(C_SURFACE);
        sc.setBorder(null);sc.getVerticalScrollBar().setBackground(C_SURFACE);
        if(w>0||h>0)sc.setPreferredSize(new Dimension(w,h));return sc;
    }
    private JButton accentBtn(String t){
        JButton b=new JButton(t){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g;g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed()?C_ACCENT2:getModel().isRollover()?new Color(0x6e,0xf0,0x9a):C_ACCENT);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                g2.setColor(C_BG);g2.setFont(getFont());FontMetrics fm=g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        b.setFont(F_TITLE);b.setContentAreaFilled(false);b.setBorderPainted(false);b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));b.setPreferredSize(new Dimension(200,36));return b;
    }
    private JButton secBtn(String t){
        JButton b=new JButton(t){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g;g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed()?C_SURF2:getModel().isRollover()?new Color(0x28,0x3d,0x2c):C_SURFACE);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                g2.setColor(C_BORDER);g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,8,8);
                g2.setColor(C_TEXT);g2.setFont(getFont());FontMetrics fm=g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        b.setFont(F_TITLE);b.setContentAreaFilled(false);b.setBorderPainted(false);b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));b.setMaximumSize(new Dimension(Integer.MAX_VALUE,34));return b;
    }
}