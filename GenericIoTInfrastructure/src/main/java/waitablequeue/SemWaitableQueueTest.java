package waitablequeue;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

class SemWaitableQueueTest {
    /*public static SemWaitableQueue<Integer> queue = new SemWaitableQueue<>(100);*/
    public static CondWaitableQueue<Integer> queue = new CondWaitableQueue<>(100);
    static AtomicInteger atomicInteger = new AtomicInteger();

    private static class Producer implements Runnable {

        @Override
        public void run() {
            queue.enqueue(1);
        }
    }

    private static class Consumer implements Runnable {

        @Override
        public void run() {
            atomicInteger.addAndGet((Integer) queue.dequeue());
        }
    }

    public static void main(String[] args) {
        //create array of threads
        Thread[] producerThreads = new Thread[100];
        Thread[] consumerThreads = new Thread[100];

        for (int i = 0; i < 100; ++i) {
            producerThreads[i] = new Thread(new Producer());
            consumerThreads[i] = new Thread(new Consumer());
        }

        //run threads
        for (int i = 0; i < 100; ++i) {
            consumerThreads[i].start();
            producerThreads[i].start();
        }

        //wait for all threads
        for (int i = 0; i < 100; ++i) {
            try {
                consumerThreads[i].join();
                producerThreads[i].join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }

        assertEquals(100, atomicInteger.get());

        assertEquals(0, queue.size());

        assertTrue(queue.isEmpty());

        queue.enqueue(5);
        queue.enqueue(10);

        boolean res = queue.remove(5);
        assertTrue(res);

        int peekRes = queue.peek();
        assertEquals(10, peekRes);
    }


}