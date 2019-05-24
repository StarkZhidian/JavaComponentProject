import java.io.*;
import java.util.Map;
import java.util.Set;

/**
 * Create by StarkZhidian on 2019-5-24
 * <p>
 * String->String 类型的属性-值处理工具
 * 属性规则：
 * key1=value1
 * key2=value2
 * ...
 * 每一行代表一个键值对属性，example see: ../properties.txt
 */
public class Properties {
    private static boolean DEBUG = true;
    /* 属性之间 key 和 value 的分隔符 */
    private static final String KEY_VALUE_SEPARATOR = "=";

    /* 属性名-值之间的映射 map */
    private PropertyMap propertyMap = new PropertyMap();

    /**
     * 从文件中加载属性键值对
     *
     * @param sourceFile
     * @throws IOException
     */
    public void load(File sourceFile) throws IOException {
        if (sourceFile == null || !sourceFile.exists()) {
            if (DEBUG) {
                throw new IllegalArgumentException();
            }
            return;
        }
        load0(new BufferedReader(new FileReader(sourceFile)));
    }

    /**
     * 从代表文件绝对路径中加载键值对
     *
     * @param sourceFilePath
     * @throws IOException
     */
    public void load(String sourceFilePath) throws IOException {
        load(new File(sourceFilePath));
    }

    /**
     * 从输入流中加载键值对
     *
     * @param sourceStream
     * @throws IOException
     */
    public void load(InputStream sourceStream) throws IOException {
        if (sourceStream == null) {
            if (DEBUG) {
                throw new IllegalArgumentException();
            }
            return;
        }
        load0(new BufferedReader(new InputStreamReader(sourceStream)));
    }

    /**
     * 从键值对数组中加载键值对，数组长度需要为偶数，奇数下标作为键，下一个偶数下标作为其对应值
     * {"1", "2", "2", "3"}: "1"->"2", "2"->"3"
     *
     * @param keyValue
     */
    public void load(String[] keyValue) {
        if (keyValue == null) {
            if (DEBUG) {
                throw new IllegalArgumentException();
            }
            return;
        }
        for (int i = 0; i < keyValue.length - 1; i += 2) {
            put(keyValue[i], keyValue[i + 1]);
        }
    }

    public String get(String key) {
        return propertyMap.get(key);
    }

    public String[] getValues(String key, String separator) {
        String value = get(key);
        return (value == null || separator == null) ? null : value.split(separator);
    }

    public void put(String key, String value) {
        propertyMap.put(key, value);
    }

    /**
     * 将当前储存的键值对信息存入参数所代表的文件中，如果文件已存在，则其数据将被覆盖，文件不存在则会新建文件
     *
     * @param destFile
     * @throws IOException
     */
    public void save(File destFile) throws IOException {
        if (destFile == null) {
            if (DEBUG) {
                throw new IllegalArgumentException();
            }
            return;
        }
        if (destFile.exists() && !destFile.delete()) {
            System.out.println("output file exists and delete failed");
        }
        if (!destFile.createNewFile()) {
            throw new IOException("output file create failed!");
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(destFile));
        Set<Map.Entry<String, String>> entrySet = propertyMap.entrySet();
        for (Map.Entry<String, String> entry : entrySet) {
            writer.write(entry.getKey() + KEY_VALUE_SEPARATOR + entry.getValue() + '\n');
        }
        writer.close();
    }

    /**
     * 将储存的键值对属性存入参数所代表的文件的绝对路径中，如果对应文件已存在，则其数据将被覆盖，文件不存在则会新建文件
     *
     * @param destFilePath
     * @throws IOException
     */
    public void save(String destFilePath) throws IOException {
        save(new File(destFilePath));
    }

    /**
     * 将存储的键值对属性写入参数所代表的输出流中
     *
     * @param destStream
     * @throws IOException
     */
    public void save(OutputStream destStream) throws IOException {
        if (destStream == null) {
            if (DEBUG) {
                throw new IllegalArgumentException();
            }
            return;
        }
        Set<Map.Entry<String, String>> entrySet = propertyMap.entrySet();
        for (Map.Entry<String, String> entry : entrySet) {
            destStream.write((entry.getKey() + KEY_VALUE_SEPARATOR + entry.getValue() + '\n').getBytes());
        }
        destStream.close();
    }

    public void clear() {
        propertyMap.clear();
    }

    private void load0(BufferedReader reader) throws IOException {
        if (reader == null) {
            return;
        }
        String currentLine;
        String[] keyValue;
        while ((currentLine = reader.readLine()) != null) {
            keyValue = currentLine.split(KEY_VALUE_SEPARATOR);
            if (keyValue.length < 2) {
                continue;
            }
            propertyMap.put(keyValue[0], keyValue[keyValue.length - 1]);
        }
        reader.close();
    }

    public static void main(String[] args) {
        String[] keyValue = new String[1000];
        int size = keyValue.length / 2;
        for (int i = 0; i < size; i++) {
            keyValue[2 * i] = "" + i;
            keyValue[2 * i + 1] = "" + (i + 1);
        }
        keyValue[1] = "1, 2, 3, 4, 5";
        Properties properties = new Properties();
        properties.load(keyValue);
//        for (int t = 0; t < 10; t++) {
//            new Thread(){
//                @Override
//                public void run() {
//                    for (int i = 0; i < size; i++) {
//                        System.out.println(Thread.currentThread() + properties.get(i + ""));
//                    }
//                }
//            }.start();
//        }
        try {
            properties.save(
                    new FileOutputStream("D:\\Working\\JAVA\\PersonalProject\\JavaComponentProject\\PropertiesUtils\\properties.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < size; i++) {
            System.out.println(Thread.currentThread() + properties.get(i + ""));
        }
    }
}
