package base;

/**
 * 获取缓存中元素的结果（命中、未命中）的回调方法
 */
public interface GetElementListener<K, V> {

    void onHit(K key, V value);

    void onMiss(K key);

}
