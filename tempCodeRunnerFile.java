import java.io.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.util.*;

public class Main {
    static class Point {
        int x;
        BigInteger y;
        Point(int x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
    }

    public static void main(String[] args) throws Exception {
        String testCase1 = "testcase1.json";
        String testCase2 = "testcase2.json";

        BigInteger secret1 = solve(testCase1);
        BigInteger secret2 = solve(testCase2);

        System.out.println("Secret for test case 1: " + secret1);
        System.out.println("Secret for test case 2: " + secret2);
    }

    static BigInteger solve(String filename) throws Exception {
        String jsonContent = new String(Files.readAllBytes(Paths.get(filename)));
        jsonContent = jsonContent.replaceAll("\\s", "");

        if (!jsonContent.startsWith("{") || !jsonContent.endsWith("}")) {
            throw new Exception("Invalid JSON: missing braces");
        }
        String content = jsonContent.substring(1, jsonContent.length() - 1);

        List<String> topEntries = splitTopLevel(content);
        Map<String, String> topMap = new HashMap<>();
        for (String entry : topEntries) {
            int idx = entry.indexOf(':');
            if (idx == -1) continue;
            String key = entry.substring(0, idx);
            String value = entry.substring(idx + 1);
            if (key.startsWith("\"") && key.endsWith("\"") && key.length() >= 2) {
                key = key.substring(1, key.length() - 1);
            }
            topMap.put(key, value);
        }

        String keysObj = topMap.get("keys");
        if (keysObj == null) throw new Exception("Missing 'keys' object");
        if (!keysObj.startsWith("{") || !keysObj.endsWith("}")) {
            throw new Exception("Invalid inner object for keys");
        }
        keysObj = keysObj.substring(1, keysObj.length() - 1);
        Map<String, String> keysMap = parseSimpleObject(keysObj);
        String nStr = keysMap.get("n");
        String kStr = keysMap.get("k");
        if (nStr == null || kStr == null) {
            throw new Exception("Missing n or k in keys");
        }
        int n = Integer.parseInt(nStr);
        int k = Integer.parseInt(kStr);

        List<Point> points = new ArrayList<>();
        for (Map.Entry<String, String> entry : topMap.entrySet()) {
            String key = entry.getKey();
            if (key.equals("keys")) continue;
            String pointValue = entry.getValue();
            if (!pointValue.startsWith("{") || !pointValue.endsWith("}")) {
                throw new Exception("Invalid point object for key: " + key);
            }
            pointValue = pointValue.substring(1, pointValue.length() - 1);
            Map<String, String> pointMap = parseSimpleObject(pointValue);
            String baseStr = pointMap.get("base");
            String valueStr = pointMap.get("value");
            if (baseStr == null || valueStr == null) {
                throw new Exception("Missing base or value in point object");
            }
            int x = Integer.parseInt(key);
            int base = Integer.parseInt(baseStr);
            BigInteger y = new BigInteger(valueStr, base);
            points.add(new Point(x, y));
        }

        if (points.size() < k) {
            throw new Exception("Not enough points to reconstruct the polynomial");
        }

        Collections.sort(points, Comparator.comparingInt(p -> p.x));
        List<Point> selectedPoints = points.subList(0, k);

        BigInteger constant = computeConstantTerm(selectedPoints, k);
        return constant;
    }

    private static List<String> splitTopLevel(String s) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            } else if (c == ',' && depth == 0) {
                parts.add(s.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(s.substring(start));
        return parts;
    }

    private static Map<String, String> parseSimpleObject(String s) {
        Map<String, String> map = new HashMap<>();
        List<String> parts = splitTopLevel(s);
        for (String part : parts) {
            int idx = part.indexOf(':');
            if (idx == -1) continue;
            String key = part.substring(0, idx);
            String value = part.substring(idx + 1);
            if (key.startsWith("\"") && key.endsWith("\"") && key.length() >= 2) {
                key = key.substring(1, key.length() - 1);
            }
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                value = value.substring(1, value.length() - 1);
            }
            map.put(key, value);
        }
        return map;
    }

    private static BigInteger computeConstantTerm(List<Point> points, int k) {
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < k; i++) {
            BigInteger xi = BigInteger.valueOf(points.get(i).x);
            BigInteger yi = points.get(i).y;

            BigInteger num = BigInteger.ONE;
            BigInteger den = BigInteger.ONE;

            for (int j = 0; j < k; j++) {
                if (i == j) continue;
                BigInteger xj = BigInteger.valueOf(points.get(j).x);
                num = num.multiply(xj.negate()); // -xj
                den = den.multiply(xi.subtract(xj));
            }

            BigInteger li0 = num.multiply(den.modInverse(BigInteger.valueOf(1L << 64))); // Use a large modulus if needed
            result = result.add(yi.multiply(li0));
        }

        return result;
    }
}
