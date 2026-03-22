package com.uniquindio.etl.service.impl;

import com.uniquindio.etl.service.SimilarityService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SimilarityServiceImpl implements SimilarityService {

    // NORMALIZACIÓN
    private List<Double> normalize(List<Double> data) {

        double mean = data.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        double sum = 0;
        for (double d : data) {
            sum += Math.pow(d - mean, 2);
        }

        double std = Math.sqrt(sum / data.size());

        List<Double> normalized = new ArrayList<>();

        for (double d : data) {
            if (std == 0) normalized.add(0.0);
            else normalized.add((d - mean) / std);
        }

        return normalized;
    }

    private void validar(List<Double> a, List<Double> b) {
        if (a.size() != b.size()) {
            throw new RuntimeException("Las series deben tener el mismo tamaño");
        }
    }

    // EUCLIDIANA
    @Override
    public double euclidean(List<Double> a, List<Double> b) {

        validar(a, b);

        a = normalize(a);
        b = normalize(b);

        double sum = 0;

        for (int i = 0; i < a.size(); i++) {
            double diff = a.get(i) - b.get(i);
            sum += diff * diff;
        }

        return Math.sqrt(sum);
    }

    // PEARSON
    @Override
    public double pearson(List<Double> a, List<Double> b) {

        validar(a, b);

        int n = a.size();

        double sumA = 0, sumB = 0;
        double sumA2 = 0, sumB2 = 0;
        double sumAB = 0;

        for (int i = 0; i < n; i++) {

            double x = a.get(i);
            double y = b.get(i);

            sumA += x;
            sumB += y;
            sumA2 += x * x;
            sumB2 += y * y;
            sumAB += x * y;
        }

        double numerator = n * sumAB - (sumA * sumB);

        double denominator = Math.sqrt(
                (n * sumA2 - sumA * sumA) *
                        (n * sumB2 - sumB * sumB)
        );

        if (denominator == 0) return 0;

        return numerator / denominator;
    }

    // COSENO
    @Override
    public double cosine(List<Double> a, List<Double> b) {

        validar(a, b);

        double dot = 0;
        double normA = 0;
        double normB = 0;

        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }

        if (normA == 0 || normB == 0) return 0;

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // DTW
    @Override
    public double dtw(List<Double> a, List<Double> b) {

        int n = a.size();
        int m = b.size();

        double[][] dp = new double[n][m];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                dp[i][j] = Double.POSITIVE_INFINITY;
            }
        }

        dp[0][0] = Math.abs(a.get(0) - b.get(0));

        for (int i = 1; i < n; i++) {
            dp[i][0] = dp[i - 1][0] + Math.abs(a.get(i) - b.get(0));
        }

        for (int j = 1; j < m; j++) {
            dp[0][j] = dp[0][j - 1] + Math.abs(a.get(0) - b.get(j));
        }

        for (int i = 1; i < n; i++) {
            for (int j = 1; j < m; j++) {

                double cost = Math.abs(a.get(i) - b.get(j));

                dp[i][j] = cost + Math.min(
                        dp[i - 1][j],
                        Math.min(dp[i][j - 1], dp[i - 1][j - 1])
                );
            }
        }

        return dp[n - 1][m - 1];
    }
}