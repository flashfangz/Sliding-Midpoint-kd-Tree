package cmsc420_s23;

import java.security.cert.PolicyNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.w3c.dom.css.Rect;

import java.util.Comparator;
public class SMkdTree<LPoint extends LabeledPoint2D> {

	private final int rebuildOffset;
    private Node root;
    private int size;
    private int deleteCount;
    private Rectangle2D rootCell;
	//private final Comparator<Node> nodeComparator;
	
	private abstract class Node { // abstract node type
        abstract LPoint find(Point2D q); // helpers and utilities
        abstract Node insert(LPoint pt, Rectangle2D cell);
        abstract void delete(Point2D q);
		abstract int getSize();
		abstract Node rebuildAfterInsert(int rebuildOffset, LPoint pt); 
		abstract void buildKeyList(List<LPoint> pts);
		abstract Node rebuild();
        abstract void list(ArrayList<String> out); // list labels
		abstract Point2D getCord();
		abstract LPoint findNearest(Point2D q, Node p, Rectangle2D cell, LPoint best);
		abstract LPoint buildNearest(Point2D q, Node p, Rectangle2D cell, LPoint best,ArrayList<LPoint> lst);
		LinkedList<LPoint> contenders;
		abstract ArrayList<String> listKdWithCenters();
		abstract boolean addCenter(LPoint center, Rectangle2D cell);
		abstract public String toStringWithCenters();
		abstract ArrayList<AssignedPair> listAssignment();
    }

	private class InternalNode extends Node {
		public int cutDim;
        public double cutVal;
        public Node left, right;
		private int size;
		private int insertCounter;
        public Rectangle2D cell;
		InternalNode(int cutDim, double cutVal, Rectangle2D cell, Node left, Node right) {
            this.cutDim = cutDim; //0 is vertical 1 is horizantal
            this.cutVal = cutVal;
            this.left = left;
            this.right = right;
			this.size = left.getSize() + right.getSize();
			this.insertCounter = 0;
			this.cell = cell;
			this.contenders = new LinkedList<LPoint>();
        }
		boolean onLeft(Point2D pt) { // in the left subtree? (for Point2D)
			return pt.get(cutDim) < cutVal;
		}

		boolean onLeft(LPoint pt) { // in the left subtree? (for LPoint)
			return pt.get(cutDim) < cutVal;
		}

		Rectangle2D leftPart(Rectangle2D cell) { // left part of cell
			return cell.leftPart(cutDim, cutVal);
		}

		Rectangle2D rightPart(Rectangle2D cell) { // right part of cell
			return cell.rightPart(cutDim, cutVal);
		}

		boolean addCenter(LPoint center, Rectangle2D cell){
			double R = Double.MAX_VALUE; 
			for(Iterator i = this.contenders.iterator(); i.hasNext();){
				R = Math.min(R, cell.maxDistanceSq(((LPoint)i.next()).getPoint2D()));
			}
			R = Math.min(R, cell.maxDistanceSq(center.getPoint2D()));
			for(Iterator i = this.contenders.iterator(); i.hasNext();){
				double r = cell.distanceSq(((LPoint)i.next()).getPoint2D());
				if( r > R){
					i.remove();
				}

			}
			double rNew = cell.distanceSq(center.getPoint2D());
			
			boolean leftAdded = false;
			if(this.left != null){
				leftAdded = this.left.addCenter(center, leftPart(cell));
			}
			
			boolean rightAdded = false;
			if(this.right != null){
				rightAdded = this.right.addCenter(center, rightPart(cell));
			}

			if (rNew <= R || leftAdded || rightAdded){
				if(!contenders.contains(center))
					contenders.add(center);
				return true;
			}
			return false;
		}
		public ArrayList<String> listKdWithCenters() {
			ArrayList<String> list = new ArrayList<String>();
			list.add(toStringWithCenters()); // add this node
			list.addAll(right.listKdWithCenters()); // add right
			list.addAll(left.listKdWithCenters()); // add left			
			return list;
		}
		ArrayList<AssignedPair> listAssignment(){
			ArrayList<AssignedPair> list = new ArrayList<AssignedPair>();
			list.addAll(right.listAssignment());
			list.addAll(left.listAssignment());
			return list;
		}
		public String toStringWithCenters() {
			String cutIndic = (cutDim == 0 ? "x=" : "y=");
			Collections.sort(this.contenders, new ByLPointLabel());
			boolean first = true;
			String contenders = "{";
			for(Iterator i = this.contenders.iterator(); i.hasNext();){
				if(!first)
					contenders += " ";
				contenders += ((LPoint) i.next()).getLabel();
				first = false;
			}
			contenders += "}";			
			return "(" + cutIndic + cutVal + ") " + size + ":" + insertCounter + " => " + contenders;			
		}
		Point2D getCord(){
			if(this.cutDim == 0){
				return new Point2D(cutVal, 0);
			} else {
				return new Point2D(0, cutVal);
			}
		}
		LPoint find(Point2D q) {
			if(cutDim == 0){
				if(q.getX() < cutVal){
					return left.find(q);
				}
				else{
					return right.find(q);
				}
			}
			else{
				if(q.getY() < cutVal){
					return left.find(q);
				}
				else{
					return right.find(q);
				}
			}
           
        }

		Node insert(LPoint pt, Rectangle2D cell) {
            size++;
			insertCounter++;
			Rectangle2D[] splitCells = SplitCells(cell, cutDim, cutVal);
			if(cutDim == 0){
				if(pt.getX() < cutVal){
					left = left.insert(pt, splitCells[0]);
				}
				else{
					right = right.insert(pt, splitCells[1]);
				}
			}
			else{
				if(pt.getY() < cutVal){
					left = left.insert(pt, splitCells[0]);
				}
				else{
					right = right.insert(pt, splitCells[1]);
				}
			}
            return this;
        }

		void delete(Point2D q){
			size--;
			if(cutDim == 0){
				if(q.getX() < cutVal){
					 left.delete(q);
				}
				else{
					 right.delete(q);
				}
			}
			else{
				if(q.getY() < cutVal){
					 left.delete(q);
				}
				else{
					 right.delete(q);
				}
			}
		}
		
		int getSize() {
			return this.size;
		}
		
		int getInsertCounter() {
			return this.insertCounter;
		}
		
		Node rebuildAfterInsert(int rebuildOffset, LPoint pt) {
			if (getInsertCounter() > (getSize() + rebuildOffset) / 2){
				ArrayList<LPoint> ref = new ArrayList<LPoint>();
				ref.addAll(this.contenders);
				Node n = rebuild();
				for(Iterator i = ref.iterator(); i.hasNext();){
					n.addCenter((LPoint) i.next(), cell);
				}				
				return n;
			}
			else if(cutDim == 0){
				if(pt.getX() < cutVal){
					left = left.rebuildAfterInsert(rebuildOffset, pt);
					for(Iterator i = left.contenders.iterator(); i.hasNext();){
						this.addCenter((LPoint) i.next(), cell);
					}
				}
				else{
					right = right.rebuildAfterInsert(rebuildOffset, pt);
					for(Iterator i = right.contenders.iterator(); i.hasNext();){	
						this.addCenter((LPoint) i.next(), cell);
					}
				}
			}
			else{
				if(pt.getY() < cutVal){
					left = left.rebuildAfterInsert(rebuildOffset, pt);
					for(Iterator i = left.contenders.iterator(); i.hasNext();){
						this.addCenter((LPoint) i.next(), cell);
					}
				}
				else{
					right = right.rebuildAfterInsert(rebuildOffset, pt);
					for(Iterator i = right.contenders.iterator(); i.hasNext();){	
						this.addCenter((LPoint) i.next(), cell);
					}
				}
			}
			return this;
		}
		
		public Node rebuild(){
			List<LPoint> pts = new ArrayList<LPoint>();
			buildKeyList(pts);
			pts.sort( new ByXThenY() );
			Node n = bulkCreate(pts, cell);
			for(Iterator i = this.contenders.iterator(); i.hasNext();){	
				n.addCenter((LPoint) i.next(), cell);
			}
			return n;
		}
		
		void buildKeyList(List<LPoint> pts) {
			right.buildKeyList(pts); // add right
			left.buildKeyList(pts); // add left
		}

		LPoint findNearest(Point2D q, Node p, Rectangle2D cell, LPoint best) {
			if(p == null){
				return best;
			}

			InternalNode node = this;
			double distToCut = (node.cutDim == 0 ? q.getX() : q.getY()) - node.cutVal;

			Rectangle2D leftCell = cell.leftPart(cutDim, cutVal);
			Rectangle2D rightCell = cell.rightPart(cutDim, cutVal);
			if(distToCut < 0) {
				best = p.findNearest(q, node.left, leftCell, best);
				if(rightCell.distanceSq(q) < q.distanceSq(best!=null?best.getPoint2D():null)) {
					best = p.findNearest(q, node.right, rightCell, best);
				}
			} else {
				best = p.findNearest(q, node.right, rightCell, best);
				if(leftCell.distanceSq(q) < q.distanceSq(best!=null?best.getPoint2D():null)) {
					best = p.findNearest(q, node.left, leftCell, best);
				}
			}
			
			return best;

		}
		LPoint buildNearest(Point2D q, Node p, Rectangle2D cell, LPoint best,ArrayList<LPoint> lst){
			
			if(p == null){
				return best;
			}
			
			InternalNode node = this;
			double distToCut = (this.cutDim == 0 ? q.getX() : q.getY()) - node.cutVal;
			
			
			
			Rectangle2D leftCell = cell.leftPart(cutDim, cutVal);
			Rectangle2D rightCell = cell.rightPart(cutDim, cutVal);
			Point2D dist = cutDim == 0? new Point2D(cutVal, q.getY()) : new Point2D(q.getX(), cutVal);

			if(distToCut < 0) {
				best = p.buildNearest(q, left, leftCell, best, lst);
				if(dist.distanceSq(q) < q.distanceSq(best!=null?best.getPoint2D():null)) {
					best = p.buildNearest(q, right, rightCell, best, lst);
				}
			} else {
				best = p.buildNearest(q, right, rightCell, best, lst);
				if(dist.distanceSq(q) < q.distanceSq(best!=null?best.getPoint2D():null)) {
					best = p.buildNearest(q, left, leftCell, best, lst);
				}
			}
			return best;
		}
		@Override
		void list(ArrayList<String> out) {
			if(cutDim ==0){
				out.add("(x=" + cutVal + ") " + size + ":" + insertCounter );
			}else{
				out.add("(y=" + cutVal + ") " + size + ":" + insertCounter );
			}
			this.right.list(out);
			this.left.list(out);

		}
	}
	
	private class ExternalNode extends Node{
		LPoint point;

		ExternalNode(LPoint point){
			this.point = point;
			this.contenders = new LinkedList<LPoint>();
		}
		boolean addCenter(LPoint center, Rectangle2D cell){
			double R = Double.MAX_VALUE; 
			for(Iterator i = this.contenders.iterator(); i.hasNext();){
				R = Math.min(R, cell.maxDistanceSq(((LPoint)i.next()).getPoint2D()));
			}
			R = Math.min(R, cell.maxDistanceSq(center.getPoint2D()));
			for(Iterator i = this.contenders.iterator(); i.hasNext();){
				double r = cell.distanceSq(((LPoint)i.next()).getPoint2D());
				if( r > R){
					i.remove();
				}

			}
			double rNew = cell.distanceSq(center.getPoint2D());
			if (rNew <= R){
				if(!contenders.contains(center))
					contenders.add(center);
				return true;
			}
			return false;
		}
		public String toStringWithCenters() {
			String contents = new String();
			contents += "[" + (point == null ? "null" : point.toString()) + "]";
			Collections.sort(this.contenders, new ByLPointLabel());
			boolean first = true;
			String contenders = "{";
			for(Iterator i = this.contenders.iterator(); i.hasNext();){
				if(!first)
					contenders += " ";
				contenders += ((LPoint) i.next()).getLabel();
				first = false;
			}
			contenders += "}";
			return contents + " => " + contenders;			
		}
		public ArrayList<String> listKdWithCenters() {
			ArrayList<String> list = new ArrayList<String>();
			list.add(toStringWithCenters()); // add this node
			return list;
		}
		
		public ArrayList<AssignedPair> listAssignment(){
			ArrayList<AssignedPair> list = new ArrayList<AssignedPair>();
			if(this.point != null) {
				LPoint closest = point;
				double dist = Double.MAX_VALUE;
				for(Iterator i = this.contenders.iterator(); i.hasNext();){
					LPoint contender = (LPoint) i.next();

					double localDist = point.getPoint2D().distanceSq(contender.getPoint2D());
					if(localDist < dist) {
						dist = localDist;
						closest = contender;
					} else if(localDist == dist) {
						if(new ByXThenY().compare(closest, contender) >= 0){
							closest = contender;
						}
					}
				}
				AssignedPair ap = new AssignedPair(point, closest, dist);
				list.add(ap);
			}
			return list;
		}
		Point2D getCord(){
			if(point == null){
				return null;
			}
			return point.getPoint2D();
		}
		LPoint find(Point2D q){
			if(point != null && point.getPoint2D().equals(q)){
				return point;
			}
			return null;

		}
		
		Node rebuildAfterInsert(int rebuildOffset, LPoint pt){
			return this;
		}
		int getSize(){
			return point==null? 0: 1;
		}

		Node insert(LPoint pt, Rectangle2D cell){
			if(point == null){
				point = pt;
				return this;
			}
			else {	
				List<LPoint> pts = new ArrayList<LPoint>();
				pts.add(point);
				pts.add(pt);
				Node newNode =  bulkCreate(pts, cell);
				for(Iterator i = this.contenders.iterator(); i.hasNext();){
					newNode.addCenter(((LPoint)i.next()), cell);
				}
				return newNode;
			}
			
		}
		void delete(Point2D q){
			this.point = null;
		}
		void buildKeyList(List<LPoint> pts){
			if(point!=null) pts.add(point);
		}
		public Node rebuild(){
			return this;
		}
		LPoint findNearest(Point2D q, Node p, Rectangle2D cell, LPoint best) {
			if (p == null) {
				return best;
			}
			LPoint current = this.point;
			if (current != null && (q.distanceSq(current.getPoint2D()) < q.distanceSq(best!=null?best.getPoint2D():null))) {
				best = current;
			} 
			else if (current != null && q.distanceSq(current.getPoint2D()) == q.distanceSq(best!= null ?best.getPoint2D() : null)) {
				if (new ByXThenY().compare(current, best) < 0) {
					best = current;
				}
			}
			return best;
		}
		LPoint buildNearest(Point2D q, Node p, Rectangle2D cell, LPoint best,ArrayList<LPoint> lst){
			if (p == null) {
				return best;
			}
			LPoint current = this.point;
			if(current != null && lst != null && !lst.contains(current)){
				lst.add(current);
			}
			if (current != null && (q.distanceSq(current.getPoint2D()) < q.distanceSq(best!=null?best.getPoint2D():null))) {
				best = current;
			} else if (current != null && q.distanceSq(current.getPoint2D()) == q.distanceSq(best!= null ?best.getPoint2D() : null)) {
				if (new ByXThenY().compare(current, best) < 0) {
					best = current;
				}
			}
			return best;
		}
		@Override
		void list(ArrayList<String> out) {
			if(point!= null){
				out.add("[" + point.toString() + "]");
			}
			else{
				out.add("[null]");
			}
		}
		
	}
	
	private LPoint startCenter;
	public SMkdTree(int rebuildOffset, Rectangle2D rootCell,LPoint startCenter) {
		this.rebuildOffset = rebuildOffset;
		this.rootCell = rootCell;
		this.root = null;
		this.size = 0;
		this.deleteCount = 0;
		this.startCenter = startCenter;
		clear();

    }
	public LPoint getStartCenter() {
		return startCenter;
	}
	public void addCenter(LPoint center){
		root.addCenter(center, rootCell);
	}
	public ArrayList<String> listKdWithCenters() {
		return root.listKdWithCenters();
	}
	public ArrayList<AssignedPair> listAssignment(){
		return root.listAssignment();
	}
	public void clear() { /* ... */ 
		root = new ExternalNode(null);
		this.root.contenders.clear();
		this.root.contenders.add(startCenter);
		size=0;
	}

	public int size() { 
		return size;
	}
	public int deleteCount() {
		return deleteCount;
	}
	public LPoint find(Point2D q) {
		LPoint lastExt = root.find(q);
		if(lastExt == null)
			return null;
		return lastExt;
	}
	
	public void insert(LPoint pt) throws Exception {
		if(!rootCell.contains(pt.getPoint2D())){
			throw new Exception("Attempt to insert a point outside bounding box");
		}
		LPoint lastExt = root.find(pt.getPoint2D());
		if(lastExt != null /*&& !lastExt.getPoint2D().equals(pt.getPoint2D())*/) {
			throw new Exception("Insertion of duplicate point");
		}

		root = root.insert(pt, rootCell);
		root = root.rebuildAfterInsert(rebuildOffset, pt);
		size++;
	
	}

	public void delete(Point2D pt) throws Exception {
		if (root.find(pt) == null) { // If the point is not in the symbol table, throw an exception
			throw new Exception("Deletion of nonexistent point");
		}
		root.delete(pt); // Remove the point from the set of points
		size--;
		deleteCount++;
		if(deleteCount > size){
			root = root.rebuild();
			deleteCount= 0;
		}
	}



	public ArrayList<String> list() {
		ArrayList<String> res = new ArrayList<String>();
		root.list(res);
		return res;
	}
	public LPoint nearestNeighbor(Point2D center) {
		if (root == null) {
			return null;
		}
		return nearPointHelper(center, root, rootCell, null);	
	}
	
	private LPoint nearPointHelper(Point2D q, Node p, Rectangle2D cell, LPoint best){
		//return p.findNearest(q, p, cell, best);
		return p.buildNearest(q, p, cell, best, null);
   }
	public ArrayList<LPoint> nearestNeighborVisit(Point2D center) { 
		ArrayList<LPoint> result = new ArrayList<LPoint>();
		if(root == null || root.getCord() == null ){
			return result;
		}
		nearPointVisitHelper(center, root, rootCell, null, result);
		result.sort(new ByXThenY());
		return result;
	}

	private LPoint nearPointVisitHelper(Point2D q, Node p, Rectangle2D cell, LPoint best,ArrayList<LPoint> lst){
		
		return p.buildNearest(q, p, cell, best, lst);
   }
   
	// The following is needed only for the Challenge Problem

	private class LPointIterator implements Iterator<LPoint> {
		public LPointIterator() { /* ... */ }
		public boolean hasNext() { /* ... */ return false; }
		public LPoint next() throws NoSuchElementException { /* ... */ return null; }

	}
	public LPointIterator iterator() { /* ... */ return new LPointIterator(); }

	
	public Node bulkCreate(List<LPoint> pts, Rectangle2D cell){
		if(pts.size() == 0){
			return new ExternalNode(null);
		}
		else if(pts.size() == 1){
			return new ExternalNode(pts.get(0));
		}
		else{
			int cutDim = 0;
			double cutVal = 0.0;

			if(cell.getWidth(0) >= cell.getWidth(1)){
				cutDim = 0;
				cutVal = cell.getCenter().getX();
			}
			else{
				cutDim = 1;
				cutVal = cell.getCenter().getY();
			}

			Rectangle2D[] splitCells;
			boolean doneChecking = true;
			do {
				pts.sort(cutDim ==0? new ByXThenY() : new ByYThenX());
				splitCells = SplitCells(cell, cutDim, cutVal);
				doneChecking = true;
			if(isAllInsideCell(pts, splitCells[1])) {  //if no pt in cell[0]			
				//slide to the closest point 
				if(cutDim == 0) {
					cutVal = pts.get(0).getX();  // slide to lowest x
					if(pts.get(0).getX() == pts.get(pts.size()-1).getX()){ //flip cutDim
						cutDim =1;
						cutVal = cell.getCenter().getY();
						doneChecking = false;
					}
				} 
				else {
					cutVal = pts.get(0).getY();   // slide to lowest Y
					if(pts.get(0).getY() == pts.get(pts.size()-1).getY()){ //flip cutDim
						cutDim =0;
						cutVal = cell.getCenter().getX();
						doneChecking = false;
					}
				}
				pts.sort(cutDim ==0? new ByXThenY() : new ByYThenX());
				splitCells = SplitCells(cell, cutDim, cutVal);		
			}
			else if(isAllInsideCell(pts, splitCells[0])) {  //if no pt in cell[1]
				//slide to the closest point
				if(cutDim == 0) {
					cutVal = pts.get(pts.size()-1).getX(); // slide to highest X
					// all X are in the same line as cutVar
					if(pts.get(0).getX() == pts.get(pts.size()-1).getX()){ //flip cutDim
						cutDim =1;
						cutVal = cell.getCenter().getY();
						doneChecking = false;
					}
				} else {
					cutVal = pts.get(pts.size()-1).getY(); // slide to highest Y
					// all Y are in the same line as cutVar
					if(pts.get(0).getY() == pts.get(pts.size()-1).getY()){ //flip cutDim
						cutDim = 0;
						cutVal = cell.getCenter().getX();
						doneChecking = false;
					}
				}
				pts.sort(cutDim ==0? new ByXThenY() : new ByYThenX());
				splitCells = SplitCells(cell, cutDim, cutVal);
			}	
				
			} while(!doneChecking);

			List<List<LPoint>> splitpoint = splitPoints(pts, cutDim, cutVal);

			Node left = bulkCreate(splitpoint.get(0), splitCells[0]);
			Node right = bulkCreate(splitpoint.get(1), splitCells[1]);

			return new InternalNode(cutDim, cutVal, cell, left,right);
		}
		
		

	}
	private List<List<LPoint>> splitPoints(List<LPoint> pts, int cutDim, double cutVar){
		ArrayList<LPoint> lst1 = new ArrayList <LPoint>(); //lower or left
		ArrayList<LPoint> lst2 = new ArrayList <LPoint>(); //higher or right
		if(cutDim == 0){
			for(LPoint pt:pts){
				if(pt.getPoint2D().getX() < cutVar){
					lst1.add(pt);
				}
				else{
					lst2.add(pt);
				}
			}
		}
		else{
			for(LPoint pt:pts){
				if(pt.getPoint2D().getY() < cutVar){
					lst1.add(pt);
				}
				else{
					lst2.add(pt);
				}
			}
		}
		List<List<LPoint>> res = new ArrayList<List<LPoint>>();
		res.add(lst1);
		res.add(lst2);
		return res;
	}
	private Rectangle2D[] SplitCells(Rectangle2D cell, int cutDim, double cutVar) {
		Rectangle2D[] splits = new Rectangle2D[2]; 
		if(cutDim ==0){
			splits[0] = new Rectangle2D(cell.low, new Point2D(cutVar, cell.high.getY()));
			splits[1] = new Rectangle2D(new Point2D(cutVar, cell.getLow().getY()), cell.getHigh());
		}
		else{
			splits[0] = new Rectangle2D(cell.getLow(),new Point2D(cell.getHigh().getX(), cutVar));
			splits[1] = new Rectangle2D(new Point2D(cell.getLow().getX(), cutVar), cell.getHigh()); 
		}
		return splits;
	}

	private boolean isPointInsideCell(LPoint pt, Rectangle2D cell) {
		//include low X and low Y, exclude high X and high Y
		Point2D point = pt.getPoint2D();
		if(point.getX() >= cell.getLow().getX() &&
		   point.getX() < cell.getHigh().getX() &&
		   point.getY() >= cell.getLow().getY() &&
		   point.getY() < cell.getHigh().getY()
		)
			return true;
		return false;
	}
	
	private boolean isAllInsideCell(List<LPoint> pts, Rectangle2D cell) {
		for(LPoint pt: pts){
			if(!isPointInsideCell(pt, cell)){
				return false;
			}
		}
		return true;	
	}
	private boolean atLeastOneInSideCell(List<LPoint> pts, Rectangle2D cell){
		for(LPoint pt: pts){
			if(isPointInsideCell(pt, cell)){
				return true;
			}
		}

		return false;
	}
	private class ByXThenY implements Comparator<LPoint> {
		public int compare(LPoint pt1, LPoint pt2) {
			Point2D p1 = pt1.getPoint2D();
			Point2D p2 = pt2.getPoint2D();
			if(p1.getX() == p2.getX()){
				return Double.compare(p1.getY(), p2.getY());
			}
			return Double.compare(p1.getX(), p2.getX());
		}
	}
	private class ByYThenX implements Comparator<LPoint> {
			public int compare(LPoint pt1, LPoint pt2) {
				Point2D p1 = pt1.getPoint2D();
				Point2D p2 = pt2.getPoint2D();
				if(p1.getY() == p2.getY()){
					return Double.compare(p1.getX(), p2.getX());
				}
				return Double.compare(p1.getY(), p2.getY());
			}
		} 
	private class ByLPointLabel implements Comparator<LPoint> {
		public int compare(LPoint n1, LPoint n2) {
			return n1.getLabel().compareTo(n2.getLabel());
		}
	}
}

