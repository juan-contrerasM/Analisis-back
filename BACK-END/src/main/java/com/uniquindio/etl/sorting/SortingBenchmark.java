package com.uniquindio.etl.sorting;

import java.util.*;
import java.util.function.Consumer;

public class SortingBenchmark {

    public static void main(String[] args) {

        int size = 8192; //potencia de 2 (por Bitonic)
        int[] original = new Random().ints(size, 0, 100000).toArray();

        System.out.println("===== BENCHMARK DE ALGORITMOS =====");

        probar("TimSort", original, SortingAlgorithms::timSort);
        probar("CombSort", original, SortingAlgorithms::combSort);
        probar("SelectionSort", original, SortingAlgorithms::selectionSort);
        probar("TreeSort", original, SortingAlgorithms::treeSort);
        probar("PigeonholeSort", original, SortingAlgorithms::pigeonholeSort);
        probar("BucketSort", original, SortingAlgorithms::bucketSort);
        probar("QuickSort", original, arr -> SortingAlgorithms.quickSort(arr, 0, arr.length - 1));
        probar("HeapSort", original, SortingAlgorithms::heapSort);
        probar("BitonicSort", original, arr -> SortingAlgorithms.bitonicSort(arr, 0, arr.length, true));
        probar("GnomeSort", original, SortingAlgorithms::gnomeSort);
        probar("BinaryInsertionSort", original, SortingAlgorithms::binaryInsertionSort);
        probar("RadixSort", original, SortingAlgorithms::radixSort);

        System.out.println("===== FIN =====");
    }

    // METODO GENÉRICO DE PRUEBA
    public static void probar(String nombre, int[] original, Consumer<int[]> algoritmo) {

        int[] copia = Arrays.copyOf(original, original.length);

        long inicio = System.nanoTime();
        algoritmo.accept(copia);
        long fin = System.nanoTime();

        long tiempo = fin - inicio;

        System.out.println(nombre + " | Tamaño: " + copia.length + " | Tiempo: " + tiempo);
    }
}