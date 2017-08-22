package entropy;

import java.util.Comparator;
import java.util.Map;

public class ValueComparator implements Comparator<Double> {
 
    Map<String, Double> map;
 
    public ValueComparator(Map<String, Double> base) {
        this.map = base;
    }
 
    public int compare(Double a, Double b) {
        if (map.get(a) >= map.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys 
    }

}

