package c45;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class C45 {

    public static ArrayList<Instance> instances = new ArrayList<>();

    public static void main(String[] args) {
        try {
            readDataSet("iris.data.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Node root = createDecisionTree(instances, -1, -1);

        boolean cleaned = cleanTree(root);
        while (cleaned)
            cleaned = cleanTree(root);

        System.out.println(root.toString(0));

        int trueC = 0;
        int falseC = 0;
        for (Instance instance : instances) {
            if (test(root, instance))
                trueC++;
            else
                falseC++;
        }

        System.out.println("Test: " + (double) trueC / (trueC + falseC));
    }

    private static boolean cleanTree(Node node) {
        if (!node.isLeaf) {
            if (node.leftNode.isLeaf && node.rightNode.isLeaf) {
                if (node.leftNode.className.equals(node.rightNode.className)) {
                    node.isLeaf = true;
                    node.className = node.rightNode.className;
                    return true;
                }
                if (node.leftNode.className.equals("failure")) {
                    node.isLeaf = true;
                    node.className = node.rightNode.className;
                    return true;
                } else if (node.rightNode.className.equals("failure")) {
                    node.isLeaf = true;
                    node.className = node.leftNode.className;
                    return true;
                }
            } else {
                return cleanTree(node.leftNode) || cleanTree(node.rightNode);
            }
        }
        return false;
    }

    private static boolean test(Node node, Instance instance) {
        if (node.isLeaf) {
            return node.className.equals(instance.className);
        } else if (instance.attributes.get(node.attributeNumber) <= node.value)
            return test(node.leftNode, instance);
        else
            return test(node.rightNode, instance);
    }

    private static String findMostFreqClass(ArrayList<Instance> instances) {
        HashMap<String, Integer> names = new HashMap<>();
        String freqName = "";
        int freq = 0;
        for (Instance instance : instances) {
            if (names.get(instance.className) == null) {
                names.put(instance.className, 1);
                if (freq == 0) {
                    freqName = instance.className;
                    freq = 1;
                }
            } else {
                names.put(instance.className, names.get(instance.className) + 1);
                if (freq < names.get(instance.className)) {
                    freqName = instance.className;
                    freq = names.get(instance.className);
                }
            }
        }
        return freqName;
    }

    private static Node createDecisionTree(ArrayList<Instance> instances, int attributeNumberPast, double optionPast) {
        if (checkEmptiness(instances)) {
            return new Node("failure");
        } else if (checkAllSame(instances)) {
            return new Node(instances.get(0).className);
        } else {
            double bestGain[] = findBestGain(instances);
            int attributeNumber = (int) bestGain[0];
            double option = bestGain[1];
            if (attributeNumber == attributeNumberPast && option == optionPast) {
                return new Node(findMostFreqClass(instances));
            } else {
                ArrayList<Instance> i1 = new ArrayList<>();
                ArrayList<Instance> i2 = new ArrayList<>();
                for (Instance instance : instances) {
                    if (instance.attributes.get(attributeNumber) <= option) {
                        i1.add(instance);
                    } else {
                        i2.add(instance);
                    }
                }

                Node ln = createDecisionTree(i1, attributeNumber, option);
                Node rn = createDecisionTree(i2, attributeNumber, option);

                return new Node(attributeNumber, option, ln, rn);
            }
        }
    }

    private static boolean checkAllSame(ArrayList<Instance> instances) {
        String temp = instances.get(0).className;
        for (int i = 1; i < instances.size(); i++) {
            if (!instances.get(i).className.equals(temp))
                return false;
        }
        return true;
    }

    private static boolean checkEmptiness(ArrayList<Instance> instances) {
        return instances.size() <= 0;
    }

    private static double[] findBestGain(ArrayList<Instance> instances) {
        double bestGain = 0;
        int bestGainAttribute = -1;
        double bestGainOption = 0;
        double info = findEntropy(instances);
        for (int i = 0; i < instances.get(0).attributes.size(); i++) {
            double temp[] = findGain(instances, i, info);
            double tempGain = temp[0];
            double tempBestOption = temp[1];
            if (bestGain <= tempGain) {
                bestGain = tempGain;
                bestGainAttribute = i;
                bestGainOption = tempBestOption;
            }
        }
        return new double[]{bestGainAttribute, bestGainOption};
    }

    private static double[] findGain(ArrayList<Instance> instances, int attributeNumber, double info) {
        double values[] = new double[instances.size()];
        for (int j = 0; j < values.length; j++) {
            values[j] = instances.get(j).attributes.get(attributeNumber);
        }
        Arrays.sort(values);

        double bestOption = values[0];
        double bestGain = info - findGainForLessThan(instances, values[0], 0, attributeNumber);
        for (int i = 1; i < values.length - 1; i++) {
            double tempGain = findGainForLessThan(instances, values[i], i, attributeNumber);
            if (bestGain <= info - tempGain) {
                bestGain = info - tempGain;
                bestOption = values[i];
            }
        }
        return new double[]{bestGain, bestOption};
    }

    private static double findGainForLessThan(ArrayList<Instance> instances, double value, int place, int attributeNumber) {
        HashMap<String, Frequencies> map = hashMapOfClassNames(instances, attributeNumber, value);
        double infoThis;
        double temp1 = 0;
        for (String key : map.keySet()) {
            int temp = map.get(key).freqLess;
            if (place != 0 && temp != 0)
                temp1 -= (double) temp / place * log((double) temp / place);
        }
        double temp2 = 0;
        for (String key : map.keySet()) {
            int temp = map.get(key).freqGreater;
            if (instances.size() != place && temp != 0)
                temp2 -= ((double) temp / (instances.size() - place)) * log(((double) temp / (instances.size() - place)));

        }
        infoThis = (double) place / instances.size() * temp1 + (double) (instances.size() - place) / instances.size() * temp2;
        return infoThis;
    }


    private static double findEntropy(ArrayList<Instance> instances) {
        HashMap<String, Integer> classes = hashMapOfClassNames(instances);
        int frequencies[] = new int[classes.keySet().size()];
        int i = 0;
        int total = 0;
        for (String key : classes.keySet()) {
            int temp = classes.get(key);
            frequencies[i++] = temp;
            total += temp;
        }
        double info = 0;
        for (int frequency : frequencies) {
            info -= (double) frequency / total * log((double) frequency / total);
        }
        return info;
    }

    private static double log(double v) {
        return Math.log(v) / Math.log(2);
    }

    private static HashMap<String, Integer> hashMapOfClassNames(ArrayList<Instance> instances) {
        HashMap<String, Integer> names = new HashMap<>();
        for (Instance instance : instances) {
            if (names.get(instance.className) == null) {
                names.put(instance.className, 1);
            } else {
                names.put(instance.className, names.get(instance.className) + 1);
            }
        }
        return names;
    }

    private static HashMap<String, Frequencies> hashMapOfClassNames(ArrayList<Instance> instances, int attributeNumber, double lessThan) {
        HashMap<String, Frequencies> names = new HashMap<>();
        for (Instance instance : instances) {
            if (instance.attributes.get(attributeNumber) <= lessThan) {
                if (names.get(instance.className) == null) {
                    names.put(instance.className, new Frequencies(1, 0));
                } else {
                    names.put(instance.className, new Frequencies(names.get(instance.className).freqLess + 1, names.get(instance.className).freqGreater));
                }
            } else {
                if (names.get(instance.className) == null) {
                    names.put(instance.className, new Frequencies(0, 1));
                } else {
                    names.put(instance.className, new Frequencies(names.get(instance.className).freqLess, names.get(instance.className).freqGreater + 1));
                }
            }
        }
        return names;
    }

    public static class Frequencies {
        int freqLess = 0;
        int freqGreater = 0;

        public Frequencies(int x, int y) {
            freqLess = x;
            freqGreater = y;
        }
    }

    private static void readDataSet(String s) throws IOException {
        FileInputStream fstream = new FileInputStream(s);
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

        String strLine;

        while ((strLine = br.readLine()) != null) {
            String[] parts = strLine.split(",");
            Instance instance = new Instance(parts[parts.length - 1]);
            for (int i = 0; i < parts.length - 1; i++) {
                instance.attributes.add(Double.parseDouble(parts[i]));
            }
            instances.add(instance);
        }

        br.close();
    }

    public static class Instance {
        public ArrayList<Double> attributes;
        public String className;

        public Instance(String name) {
            attributes = new ArrayList<>();
            className = name;
        }
    }
}