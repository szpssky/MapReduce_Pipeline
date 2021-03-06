package com.mapreduce;

import akka.actor.ActorSelection;
import akka.actor.UntypedActor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.util.*;

/**
 * Created by szp on 16/5/27.
 */
public class SpillActor extends UntypedActor {
    private List<KeyValue<String, Integer>> mappedKeyValue = new LinkedList<KeyValue<String, Integer>>();
    private int count = 0;
    private static Logger loger = LogManager.getLogger(SpillActor.class.getName());
    private ActorSelection spillMergeActor;
    private volatile int thread_count = 0;

    @Override
    public void preStart() throws Exception {
//        spillMergeActoy = getContext().actorOf(Props.create(SpillMergeActor.class), "SpillMergeActor");
        spillMergeActor = getContext().actorSelection("../SpillMergeActor");
//        Files.createFile(Paths.get("testData/spill_out/out.txt"));
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof List) {
            for (KeyValue<String, Integer> item : (List<KeyValue<String, Integer>>) message) {
                mappedKeyValue.add(item);
            }
            ((List<KeyValue<String, Integer>>) message).clear();
            if (mappedKeyValue.size() > 5000000) {
                List<KeyValue<String, Integer>> tmp = mappedKeyValue;
                mappedKeyValue = null;
                mappedKeyValue = new LinkedList<>();
                new Thread(() -> {
                    registThread();
                    int tmpcount = count++;
//                    Collections.sort(tmp);
                    KeyValue[] tmps = tmp.toArray(new KeyValue[0]);
                    QuickSort<KeyValue<String, Integer>> quickSort = new QuickSort<KeyValue<String, Integer>>(tmps);
                    quickSort.sort();
                    ListIterator<KeyValue<String, Integer>> it = tmp.listIterator();
                    for (KeyValue<String, Integer> e : tmps) {
                        it.next();
                        it.set(e);
                    }
                    loger.debug("正在写入文件" + tmpcount);
                    File srcFile = new File("testData/spill_out/" + tmpcount + ".txt");
                    RandomAccessFile raf = null;
                    try {
                        raf = new RandomAccessFile(srcFile, "rw");
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    FileChannel fileChannel = raf.getChannel();
                    ByteBuffer rBuffer = ByteBuffer.allocateDirect(128 * 1024 * 1024);
                    try {
                        int size = tmp.size();
                        for (int i = 0; i < size; i++) {
                            KeyValue<String, Integer> keyValue = tmp.remove(0);
                            rBuffer.put((keyValue.getKey().toString() + " " + keyValue.getValue().toString() + "\n").getBytes());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    rBuffer.flip();
                    try {
                        fileChannel.write(rBuffer);
                        fileChannel.close();
                        raf.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    unregistThread();
                    loger.debug("文件" + tmpcount + "写入结束");
                }).start();

            }

        }
        if (message instanceof String) {
            if ("END".equals(message)) {
                if (mappedKeyValue.size() > 0) {
                    registThread();
                    new Thread(() -> {
//                    Collections.sort(mappedKeyValue);
                        KeyValue[] tmps = mappedKeyValue.toArray(new KeyValue[0]);
                        QuickSort<KeyValue<String, Integer>> quickSort = new QuickSort<KeyValue<String, Integer>>(tmps);
                        quickSort.sort();
                        ListIterator<KeyValue<String, Integer>> it = mappedKeyValue.listIterator();
                        for (KeyValue<String, Integer> e : tmps) {
                            it.next();
                            it.set(e);
                        }
                        loger.debug("正在写入文件--" + count);
                        File srcFile = new File("testData/spill_out/" + count + ".txt");
                        RandomAccessFile raf = null;
                        try {
                            raf = new RandomAccessFile(srcFile, "rw");
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        FileChannel fileChannel = raf.getChannel();
                        ByteBuffer rBuffer = ByteBuffer.allocateDirect(128 * 1024 * 1024);
                        try {
                            int size = mappedKeyValue.size();
                            for (int i = 0; i < size; i++) {
                                KeyValue<String, Integer> keyValue = mappedKeyValue.remove(0);
                                rBuffer.put((keyValue.getKey().toString() + " " + keyValue.getValue().toString() + "\n").getBytes());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        rBuffer.flip();
                        try {
                            fileChannel.write(rBuffer);
                            fileChannel.close();
                            raf.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mappedKeyValue = null;
                        loger.debug("文件" + count + "写入结束");
                        count++;
                        unregistThread();

                    }).start();
                }
                while (thread_count != 0) ;
                loger.info("溢写完成");
                spillMergeActor.tell("StartMerge", getSelf());
                context().stop(getSelf());
            }
        }
    }

    public void registThread() {
        synchronized (this) {
            this.thread_count++;
        }
    }

    public void unregistThread() {
        synchronized (this) {
            this.thread_count--;
        }
    }

}
