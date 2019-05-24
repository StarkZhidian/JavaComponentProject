import java.io.*;
import java.util.Map;
import java.util.Set;

/**
 * String->String 类型的属性-值处理工具
 * 属性规则：
 * key1=value1
 * key2=value2
 * ...
 */
public class Properties {
    private static boolean DEBUG = true;
    /* 属性之间 key 和 value 的分隔符 */
    private static final String KEY_VALUE_SEPARATOR = "=";

    /* 属性名-值之间的映射 map */
    private PropertyMap propertyMap = new PropertyMap();

    public void load(File sourceFile) throws IOException {
        if (sourceFile == null || !sourceFile.exists()) {
            if (DEBUG) {
                throw new IllegalArgumentException();
            }
            return;
        }
        load0(new BufferedReader(new FileReader(sourceFile)));
    }

    public void load(String sourceFilePath) throws IOException {
        load(new File(sourceFilePath));
    }

    public void load(InputStream sourceStream) throws IOException {
        if (sourceStream == null) {
            if (DEBUG) {
                throw new IllegalArgumentException();
            }
            return;
        }
        load0(new BufferedReader(new InputStreamReader(sourceStream)));
    }

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

    public void put(String key, String value) {
        propertyMap.put(key, value);
    }

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

    public void save(String destFilePath) throws IOException {
        save(new File(destFilePath));
    }

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