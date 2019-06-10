import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    private static final int THREADS_COUNT = 5;

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(THREADS_COUNT);
        try {
            File file = new File("src/input.txt");
            RandomAccessFile accessFile = new RandomAccessFile(file, "r");
            int rangeSize = Math.toIntExact(accessFile.length() / THREADS_COUNT) + 1;
            final Map<String, Long> dict = new HashMap<>();
            List<DictCallable> list = new ArrayList<>();
            List<byte[]> listBytes = new ArrayList<>();
            for (int i = 0; i < THREADS_COUNT; i++) {
                byte[] bytes = new byte[rangeSize];
                accessFile.read(bytes, 0, rangeSize);
                list.add(new DictCallable(bytes));
                listBytes.add(bytes);
            }
            long start = System.nanoTime();
            Map<String, Long> dict2 = new HashMap<>();
            for (int i = 0; i < THREADS_COUNT; i++) {
                Map<String, Long> map = buildDict(listBytes.get(i));
                map.forEach((k, v) -> dict2.merge(k, v, Long::sum));
            }
            System.out.println((System.nanoTime() - start) / Math.pow(10, 9));
            System.out.println((System.nanoTime() - start));
            System.out.println(dict2);
            start = System.nanoTime();
            List<Future<Map<String, Long>>> futures = executorService.invokeAll(list);
            while (!futures.isEmpty()) {
                List<Future<Map<String, Long>>> removedFutures = new ArrayList<>();
                for (Future<Map<String, Long>> future : futures) {
                    if (future.isDone()) {
                        Map<String, Long> map = future.get();
                        map.forEach((k, v) -> dict.merge(k, v, Long::sum));
                        removedFutures.add(future);
                    }
                }
                futures.removeAll(removedFutures);
            }
            System.out.println((System.nanoTime() - start) / Math.pow(10, 9));
            System.out.println((System.nanoTime() - start));
            System.out.println(dict);
        }finally {
            executorService.shutdownNow();
        }
    }

    private static Map<String, Long> buildDict(byte[] bytes) {
        Map<String, Long> res = new HashMap<>();
        String text = new String(bytes, Charset.availableCharsets().get("windows-1251"));
        for (String el : text.split("\\s+")) {
            res.compute(el, (k, v) -> (v == null) ? 1 : v + 1);
        }
        return res;
    }
}
    class DictCallable implements Callable<Map<String, Long>> {
        private byte[] bytes;

        DictCallable(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public Map<String, Long> call() throws Exception {
            Map<String, Long> map = buildDict(bytes);
            return map;
        }

        private static Map<String, Long> buildDict(byte[] bytes) {
            Map<String, Long> res = new HashMap<>();
            String text = new String(bytes, Charset.availableCharsets().get("windows-1251"));
            res = Stream.of(text.split("\\s+"))
                    //.parallel()
                    .collect(Collectors.groupingBy(s->s, Collectors.counting()));
/*
            for (String el : text.split("\\s+")) {
                res.compute(el, (k, v) -> (v == null) ? 1 : v + 1);
            }*/
            return res;
        }
    }
