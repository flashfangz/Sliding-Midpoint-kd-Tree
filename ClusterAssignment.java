package cmsc420_s23;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public class ClusterAssignment<LPoint extends LabeledPoint2D> {
	private SMkdTree<LPoint> kdTree; // storage for the sites
	private ArrayList<LPoint> centers; // storage for the centers
	LPoint start;
	// ------------------------------------------------------------------------
	// The following class is not required, but you may find it helpful. It
	// represents the triple (site, center, squared-distance). Feel free to
	// delete or modify.
	// ------------------------------------------------------------------------
	//
	//class AssignedPair implements Comparable<AssignedPair> {
	//	private LPoint site; // a site
	//	private LPoint center; // its assigned center
	//	private double distanceSq; // the squared distance between them
	//	public int compareTo(AssignedPair o) { /* ... */ return 0; } // for sorting
	//}
    //
	private class ByLPointLabel implements Comparator<LPoint> {
		public int compare(LPoint n1, LPoint n2) {
			return n1.getLabel().compareTo(n2.getLabel());
		}
	}
	public ClusterAssignment(int rebuildOffset, Rectangle2D bbox, LPoint startCenter) {
		kdTree = new SMkdTree<LPoint>(rebuildOffset, bbox, startCenter);
		centers = new ArrayList<LPoint>();
		centers.add(startCenter);
	}
	public void addSite(LPoint site) throws Exception {
		kdTree.insert(site);

	}
	public void deleteSite(LPoint site) throws Exception { 
		kdTree.delete(site.getPoint2D());
	 }
	public void addCenter(LPoint center) throws Exception {
		centers.add(center);
		kdTree.addCenter(center);
	}
	public int sitesSize() { 
		return kdTree.size(); 
	}
	public int centersSize() {
		return centers.size();
	 }
	public void clear() {
		kdTree.clear();
		centers.clear();
		centers.add(kdTree.getStartCenter());

	}
	public ArrayList<String> listKdWithCenters() {
		return kdTree.listKdWithCenters();

	}
	public ArrayList<String> listCenters() {
		centers.sort(new ByLPointLabel());
		ArrayList<String> res = new ArrayList<String> ();
		for(LPoint point: centers){
			res.add(point.getLabel() + ": " + point.getPoint2D().toString());
		}
		return res;
	}
	public ArrayList<String> listAssignments() { 
		 ArrayList<String> list = new ArrayList<>();
		 ArrayList<AssignedPair> results = kdTree.listAssignment();
		 Collections.sort(results);
		 for(AssignedPair p: results) {
			list.add("[" + p.site.getLabel() + "->" + p.center.getLabel() +  "] distSq = " + p.distanceSq );
		 }
		 return list;
	}
	public void deleteCenter(LPoint center) throws Exception { /* ... */ } // For extra credit only


}

