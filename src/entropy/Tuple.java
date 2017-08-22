package entropy;

// Class for GeneValue and classname properties
public class Tuple implements Comparable<Tuple> {
    double geneValue;
    String className;

    public Tuple(double geneValue, String className) {
        this.geneValue =geneValue;
        this.className = className;
    }

	@Override
	public int compareTo(Tuple o) {
		// TODO Auto-generated method stub
		return geneValue < o.geneValue ? -1 : geneValue > o.geneValue ? 1 : 0;
	}
}
