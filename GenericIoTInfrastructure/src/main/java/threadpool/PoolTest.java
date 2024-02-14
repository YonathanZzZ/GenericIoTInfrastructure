package threadpool;

import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

class PoolTest {

    ThreadPool pool;

    private static class someTask implements Callable<Integer> {

        int num;

        public someTask(int num) {
            this.num = num;
        }

        @Override
        public Integer call() throws Exception {
            System.out.println("task no. " + num + " in thread " +
                    Thread.currentThread().getName());
            Thread.sleep(1000);
            return 5;
        }
    }


    @BeforeEach
    void initPool() {
        pool = new ThreadPool(4);
    }

    @org.junit.jupiter.api.Test
    void submit() throws InterruptedException {

        for (int j = 1; j <= 20; ++j) {
            pool.submit(new someTask(j));
        }

        Thread.sleep(3000);

    }

    @org.junit.jupiter.api.Test
    void execute() {
        pool.execute(() -> {
            for (int i = 0; i < 10; ++i) {
                System.out.println("Nice.");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        });

        System.out.println(System.lineSeparator() + "after execute");
    }

    @org.junit.jupiter.api.Test
    void setNumOfThreads() throws InterruptedException {
        for (int j = 1; j <= 100; ++j) {
            pool.submit(new someTask(j));
        }

        Thread.sleep(3000);

        pool.setNumOfThreads(10);
        System.out.println(System.lineSeparator() + "After setting " +
                "numOfThreads to 10:" + System.lineSeparator());

        Thread.sleep(3000);

        pool.setNumOfThreads(2);
        System.out.println(System.lineSeparator() + "After setting " +
                "numOfThreads to 2:" + System.lineSeparator());

        TimeUnit.SECONDS.sleep(5);
    }

    @org.junit.jupiter.api.Test
    void pauseAndResume() throws InterruptedException {
        for (int j = 1; j <= 200; ++j) {
            pool.submit(new someTask(j));
        }

        System.out.println("pausing");
        pool.pause();

        System.out.println("resuming");
        pool.resume();
        TimeUnit.SECONDS.sleep(10);
    }

    @org.junit.jupiter.api.Test
    void shutDown() {
        for (int j = 1; j <= 100; ++j) {
            pool.submit(new someTask(j));
        }

        System.out.println("shutting down");
        pool.shutDown();
    }

    @org.junit.jupiter.api.Test
    void awaitTermination() {
        for (int j = 1; j <= 100; ++j) {
            pool.submit(new someTask(j));
        }

        System.out.println("shutting down");
        pool.shutDown();
        System.out.println("waiting for termination of all tasks");
        pool.awaitTermination();
    }

    @org.junit.jupiter.api.Test
    void timedAwaitTermination() {

    }

    /*@org.junit.jupiter.api.Test
    void cancelTest() throws InterruptedException {
        Callable<Integer> calculateFactorial = () -> {
            int n = 5; // Calculate factorial for 5
            int result = 1;

            for (int i = 1; i <= n; i++) {
                result *= i;
            }
            return result;
        };

        Future<Integer> future = pool.submit(calculateFactorial);
        System.out.println("im stuck");
        Thread.sleep(100);
        System.out.println("im stuck2");
        boolean canceled = future.cancel(true);
        System.out.println("im stuck3");
        System.out.println(canceled);
        //Assertions.assertTrue(canceled);
        System.out.println("im stuck4");
        pool.shutDown();
        pool.awaitTermination();

    }*/
}