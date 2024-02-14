package threadpool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class ThreadPoolTest {
    private ThreadPool pool;

    @BeforeEach
    void setUp() {
        pool = new ThreadPool(3);
    }

    Callable<String> task = () -> {
        System.out.println("DEFAULT");
        TimeUnit.SECONDS.sleep(1);
        return null;
    };
    Runnable task2 = () -> {
        System.out.println("HIGH");
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    };
    Callable<String> task3 = () -> {
        System.out.println("LOW");
        TimeUnit.SECONDS.sleep(1);
        return null;
    };

    @Test
    void submit() {
        for (int i = 0; i < 50; ++i) {
            pool.submit(task);
            pool.submit(task2, ThreadPool.Priority.HIGH);
            pool.submit(task3, ThreadPool.Priority.LOW);
        }
    }

    @Test
    void execute() {
        for (int i = 0; i < 50; ++i) {
            pool.submit(task);
            pool.submit(task2, ThreadPool.Priority.HIGH);
            pool.submit(task3, ThreadPool.Priority.LOW);
        }

        Runnable runnable = () -> {
            while (true) {
            }
        };
        pool.execute(runnable);
        System.out.println("execute not working");
    }

    @Test
    void setNumOfThreads() throws InterruptedException {
        for (int i = 0; i < 50; ++i) {
            pool.submit(task);
            pool.submit(task2, ThreadPool.Priority.HIGH);
            pool.submit(task3, ThreadPool.Priority.LOW);
        }
        TimeUnit.SECONDS.sleep(5);
        pool.setNumOfThreads(2);
        TimeUnit.SECONDS.sleep(5);
        pool.setNumOfThreads(10);
        TimeUnit.SECONDS.sleep(5);
    }

    @Test
    void pause() throws InterruptedException {
        for (int i = 0; i < 50; ++i) {
            pool.submit(task);
            pool.submit(task2, ThreadPool.Priority.HIGH);
            pool.submit(task3, ThreadPool.Priority.LOW);
        }
        TimeUnit.SECONDS.sleep(3);
        pool.pause();
        TimeUnit.SECONDS.sleep(3);
    }

    @Test
    void resume() throws InterruptedException {
        for (int i = 0; i < 50; ++i) {
            pool.submit(task);
            pool.submit(task2, ThreadPool.Priority.HIGH);
            pool.submit(task3, ThreadPool.Priority.LOW);
        }
        TimeUnit.SECONDS.sleep(3);
        pool.pause();
        TimeUnit.SECONDS.sleep(3);
        pool.resume();
        TimeUnit.SECONDS.sleep(3);
    }

    @Test
    void shutDown() throws InterruptedException {
        for (int i = 0; i < 20; ++i) {
            pool.submit(task);
            pool.submit(task2, ThreadPool.Priority.HIGH);

        }

        pool.shutDown();

        for (int i = 0; i < 20; ++i) {

            pool.submit(task3, ThreadPool.Priority.LOW);
        }
        TimeUnit.SECONDS.sleep(10);
    }

    @Test
    void awaitTermination() {
        for (int i = 0; i < 20; ++i) {
            pool.submit(task);
            pool.submit(task2, ThreadPool.Priority.HIGH);
            pool.submit(task3, ThreadPool.Priority.LOW);
        }

        pool.shutDown();
        pool.awaitTermination();
    }

    @Test
    void Cancel() throws InterruptedException {
        Callable<String> task4 = () -> {
            while (true) {
                System.out.println("PLEASE DON'T CANCEL ME");
                TimeUnit.SECONDS.sleep(1);
            }
        };

        Future<String> future = pool.submit(task4);

        for (int i = 0; i < 10; ++i) {
            pool.submit(task);
            pool.submit(task2, ThreadPool.Priority.HIGH);
            pool.submit(task3, ThreadPool.Priority.LOW);
        }

        TimeUnit.SECONDS.sleep(5);
        future.cancel(true);
        pool.shutDown();
        pool.awaitTermination();
        System.out.println("is cancled??" + future.isCancelled());
    }
}