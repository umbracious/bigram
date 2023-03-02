import java.io.File;
import java.util.*;

public class FileMap<K,V> implements Map<K,V> {

    //number of entries
    private int size;
    //capacity
    private int capacity;
    private Map<K, V> map;

    public FileMap(){
        this.capacity = 1;
        this.map = new HashMap<>(this.capacity);
    }

    @Override
    public int size() { return this.size; }

    @Override
    public boolean isEmpty() {
        if (this.size == 0){
            return true;
        }
        else return false;
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return this.map.get(key);
    }

    @Override
    public V put(K key, V value) {

        this.size += 1;

        if (this.size >= 0.75 * this.capacity){
                this.capacity = 2 * this.capacity + 1;
                Map<K, V> tempMap = this.map;
                this.map = new HashMap<>(this.capacity);
                this.map.putAll(tempMap);
        }

        this.map.put(key, value);
        return this.map.get(key);
    }

    @Override
    public V remove(Object key) {
        V value = this.map.get(key);
        this.map.remove(key);
        this.size -= 1;
        return value;
    }

    @Override
    public void putAll(Map<? extends K,? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()){
            this.size += 1;

            if (this.size >= 0.75 * this.capacity){
                this.capacity = 2 * this.capacity + 1;
                Map<K, V> tempMap = this.map;
                this.map = new HashMap<>(this.capacity);
                this.map.putAll(tempMap);
            }

            this.map.put(entry.getKey(), entry.getValue());

        }

    }

    @Override
    public void clear() {
        this.map.clear();
        this.size = 0;
    }

    @Override
    public Set<K> keySet() {
        Set<K> set = new HashSet<K>();

        for (Map.Entry<K, V> entry : this.map.entrySet()){
            set.add(entry.getKey());
        }

        return set;
    }

    @Override
    public Collection<V> values() {
        Collection<V> collection = new HashSet<V>();

        for (Map.Entry<K, V> entry : this.map.entrySet()){
            collection.add(entry.getValue());
        }

        return collection;
    }

    @Override
    public Set<Map.Entry<K,V>> entrySet() {
        return this.map.entrySet();
    }

    @Override
    public String toString(){
        String out = "";
        for (K key : map.keySet()) {
            out += key + "=" + map.get(key) + " + ";
        }
        return out;
    }


}
