package WordCount;

import com.mapreduce.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class Main {
    MapReduce<Integer, String, String, Integer, String, Integer> wcMR =
            new MapReduce<Integer, String, String, Integer, String, Integer>(MapWC.class, ReduceWC.class, "MAP_REDUCE");
    private static Logger logger = LogManager.getLogger(Main.class.getName());

    public void init(String filename) {
        wcMR.setParallelThreadNum(1);
        System.out.println(filename);
        try {
            readFiles(filename);

            wcMR.startShuffle();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public File[] getFiles(String path) {
        File file = new File(path);
        File[] fileList = file.listFiles();
        return fileList;
    }

    public void readFiles(String filename) throws IOException {
        int count = 0;
        logger.info("开始读取文件==================");
        File file = new File(filename);
        if(!file.exists()){
            throw new IOException("file not exists");
        }
        if (!file.getName().split("\\.")[0].equals("") && file.getName().split("\\.")[0].substring(0, 5).equals("input")) {
            RandomAccessFile raf = new RandomAccessFile(new File(file.toString()), "r");
            FileChannel fc = raf.getChannel();
            MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            StringBuffer sbf = new StringBuffer();
            while (mbb.remaining() > 0) {
                char data = (char) mbb.get();
                if (data != '\n') {
                    sbf.append(data);
                } else {
                    wcMR.addKeyValue(0, sbf.toString());
                    sbf.setLength(0);
                }
            }
            fc.close();
            raf.close();
        }
        logger.info("文件全部读取完成");
        wcMR.startMap();

    }

    public void run() {
        wcMR.run();
    }

    public static void main(String[] args) {
        StaticCount.count = Integer.parseInt(args[1]);
        Main main = new Main();
        main.init(args[0]);}


}
