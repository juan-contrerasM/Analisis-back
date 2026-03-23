package com.uniquindio.etl.sorting;

import com.uniquindio.etl.model.SortingResultData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;

@Service
@Slf4j
public class SortingBenchmarkService {

    public List<SortingResultData> ejecutarBenchmark(int size) {

        // Aseguramos potencia de 2 para Bitonic
        size = ajustarAPotenciaDe2(size);

        int[] original = new Random().ints(size, 0, 100000).toArray();

        List<SortingResultData> resultados = new ArrayList<>();

        log.info("===== INICIO BENCHMARK =====");

        resultados.add(probar("TimSort", original, SortingAlgorithms::timSort));
        resultados.add(probar("CombSort", original, SortingAlgorithms::combSort));
        resultados.add(probar("SelectionSort", original, SortingAlgorithms::selectionSort));
        resultados.add(probar("TreeSort", original, SortingAlgorithms::treeSort));
        resultados.add(probar("PigeonholeSort", original, SortingAlgorithms::pigeonholeSort));
        resultados.add(probar("BucketSort", original, SortingAlgorithms::bucketSort));
        resultados.add(probar("QuickSort", original, arr -> SortingAlgorithms.quickSort(arr, 0, arr.length - 1)));
        resultados.add(probar("HeapSort", original, SortingAlgorithms::heapSort));
        resultados.add(probar("BitonicSort", original, arr -> SortingAlgorithms.bitonicSort(arr, 0, arr.length, true)));
        resultados.add(probar("GnomeSort", original, SortingAlgorithms::gnomeSort));
        resultados.add(probar("BinaryInsertionSort", original, SortingAlgorithms::binaryInsertionSort));
        resultados.add(probar("RadixSort", original, SortingAlgorithms::radixSort));

        log.info("===== FIN BENCHMARK =====");

        return resultados;
    }

    private SortingResultData probar(String nombre, int[] original, Consumer<int[]> algoritmo) {

        int[] copia = Arrays.copyOf(original, original.length);

        long inicio = System.nanoTime();
        algoritmo.accept(copia);
        long fin = System.nanoTime();

        long tiempo = fin - inicio;

        log.info("{} | Tamaño: {} | Tiempo: {}", nombre, copia.length, tiempo);

        return new SortingResultData(nombre, copia.length, tiempo);
    }

    private int ajustarAPotenciaDe2(int n) {
        int potencia = 1;
        while (potencia < n) {
            potencia *= 2;
        }
        return potencia;
    }
}