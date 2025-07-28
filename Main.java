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
        String[] testFiles = {"testcase1.json", "testcase2.json"};

        for (int i = 0; i < testFiles.length; i++) {
            BigInteger secret = solve(testFiles[i]);
            System.out.println("Secret for test case " + (i + 1) + ": " + secret);
        }
    }

    static BigInteger solve(String filename) throws Exception {
        // Read JSON as plain string
        String json = new String(Files.readAllBytes(Paths.get(filename)));
        json = json.replaceAll("[\\n\\r\\t ]", ""); // remove whitespace

        // Validate basic structure
        if (!json.startsWith("{") || !json.endsWith("}"))
            throw new Exception("Invalid JSON format");

        json = json.substring(1, json.length() - 1); // remove outer braces

        // Parse top-level entries
        Map<String, String> map = parseTopLevel(json);

        // Extract 'n' and 'k'
        if (!map.containsKey("keys"))
            throw new Exception("Missing keys object");

        Map<String, String> keys = parseTopLevel(map.get("keys").substring(1, map.get("keys").length() - 1));
        int n = Integer.parseInt(keys.get("n"));
        int k = Integer.parseInt(keys.get("k"));

        // Extract points
        List<Point> points = new ArrayList<>();
        for (String key : map.keySet()) {
            if (key.equals("keys")) continue;

            int x = Integer.parseInt(key);
            String val = map.get(key);
            if (!val.startsWith("{") || !val.endsWith("}"))
                continue;

            Map<String, String> inner = parseTopLevel(val.substring(1, val.length() - 1));
            int base = Integer.parseInt(inner.get("base"));
            String value = inner.get("value");

            BigInteger y = new BigInteger(value, base);
            points.add(new Point(x, y));
        }

        // Sort and select first k points
        points.sort(Comparator.comparingInt(p -> p.x));
        List<Point> selected = points.subList(0, k);

        return lagrangeInterpolation(selected);
    }

    static Map<String, String> parseTopLevel(String input) {
        Map<String, String> map = new LinkedHashMap<>();
        int depth = 0;
        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();
        boolean inKey = true;
        boolean inQuotes = false;
        boolean escape = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (escape) {
                (inKey ? key : value).append(c);
                escape = false;
                continue;
            }

            if (c == '\\') {
                escape = true;
                continue;
            }

            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }

            if (!inQuotes && c == ':' && inKey) {
                inKey = false;
                continue;
            }

            if (!inQuotes && c == ',' && depth == 0) {
                map.put(key.toString().replaceAll("^\"|\"$", ""), value.toString());
                key.setLength(0);
                value.setLength(0);
                inKey = true;
                continue;
            }

            if (!inQuotes && (c == '{' || c == '[')) depth++;
            if (!inQuotes && (c == '}' || c == ']')) depth--;

            (inKey ? key : value).append(c);
        }

        // Add last key-value pair
        if (key.length() > 0 && value.length() > 0)
            map.put(key.toString().replaceAll("^\"|\"$", ""), value.toString());

        return map;
    }

    static BigInteger lagrangeInterpolation(List<Point> points) {
        BigInteger result = BigInteger.ZERO;
        int k = points.size();

        for (int i = 0; i < k; i++) {
            BigInteger xi = BigInteger.valueOf(points.get(i).x);
            BigInteger yi = points.get(i).y;

            BigInteger num = BigInteger.ONE;
            BigInteger den = BigInteger.ONE;

            for (int j = 0; j < k; j++) {
                if (i == j) continue;

                BigInteger xj = BigInteger.valueOf(points.get(j).x);
                num = num.multiply(xj.negate());
                den = den.multiply(xi.subtract(xj));
            }

            BigInteger inv = den.modInverse(BigInteger.valueOf(Long.MAX_VALUE)); // Use a large prime mod if needed
            BigInteger li = num.multiply(inv);

            result = result.add(yi.multiply(li));
        }

        return result;
    }
}
