package cmsc420_s23;

// ------------------------------------------------------------------------
// The following class is not required, but you may find it helpful. It
// represents the triple (site, center, squared-distance). Feel free to
// delete or modify.
// ------------------------------------------------------------------------
 public class AssignedPair<LPoint extends LabeledPoint2D> implements Comparable<AssignedPair> {
    public AssignedPair(LPoint site, LPoint center, double distanceSq) {
        this.site = site;
        this.center = center;
        this.distanceSq = distanceSq;
    }
    public LPoint site; // a site
    public LPoint center; // its assigned center
    public double distanceSq; // the squared distance between them
    public int compareTo(AssignedPair p1) { 
        if(this.distanceSq > p1.distanceSq){
            return 1;
        }
        else if(this.distanceSq < p1.distanceSq){
            return -1;
        }
        else{
            double x1 = this.site.getX();
			double x2 = p1.site.getX();
			if (x1 < x2)
				return -1;
			else if (x1 > x2)
				return +1;
			else {
				double y1 = this.site.getY();
				double y2 = p1.site.getY();
				if (y1 < y2)
					return -1;
				else if (y1 > y2)
					return +1;
				else {
					return 0;
				}
			}
        }
    } // for sorting
}