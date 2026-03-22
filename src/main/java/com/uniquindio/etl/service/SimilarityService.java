package com.uniquindio.etl.service;

import java.util.List;

public interface SimilarityService {

    double euclidean(List<Double> a, List<Double> b);

    double pearson(List<Double> a, List<Double> b);

    double cosine(List<Double> a, List<Double> b);

    double dtw(List<Double> a, List<Double> b);
}