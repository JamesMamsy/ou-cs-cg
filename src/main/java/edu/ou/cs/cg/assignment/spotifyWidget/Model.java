//******************************************************************************
// Copyright (C) 2019 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Tue Mar 19 00:38:27 2019 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20190227 [weaver]:	Original file.
// 20190318 [weaver]:	Modified for homework04.
//
//******************************************************************************
//
// The model manages all of the user-adjustable variables utilized in the scene.
// (You can store non-user-adjustable scene data here too, if you want.)
//
// For each variable that you want to make interactive:
//
//   1. Add a member of the right type
//   2. Initialize it to a reasonable default value in the constructor.
//   3. Add a method to access (getFoo) a copy of the variable's current value.
//   4. Add a method to modify (setFoo) the variable.
//
// Concurrency management is important because the JOGL and the Java AWT run on
// different threads. The modify methods use the GLAutoDrawable.invoke() method
// so that all changes to variables take place on the JOGL thread. Because this
// happens at the END of GLEventListener.display(), all changes will be visible
// to the View.update() and render() methods in the next animation cycle.
//
//******************************************************************************

package edu.ou.cs.cg.assignment.spotifyWidget;

//import java.lang.*;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.*;
import com.jogamp.opengl.*;
import edu.ou.cs.cg.utilities.Utilities;

//******************************************************************************

/**
 * The <CODE>Model</CODE> class.
 *
 * @author Chris Weaver
 * @version %I%, %G%
 */
public final class Model {
	// **********************************************************************
	// Private Members
	// **********************************************************************

	// State (internal) variables
	private final View view;
	public final static int NUM_BARS = 15;
	public static final Random			RANDOM = new Random();

	// Model variables
	private Point2D.Double cursor; // Current cursor coords

	//State
	public boolean isPlaying;
	private screenState curState;
	private double volume;
	private boolean isCondesnsed;
	private int bpm; //Beats per minute

	//Song information
	private String songTitle;
	private String songArtist;
	private double songProgress;
	private String songDuration;
	private ArrayDeque<Integer> songNotes;
	private ArrayDeque<Integer> songFreqs;

	enum screenState{
		player,
		playlistView,
		queueView,
		allPlaylists
	};
	// **********************************************************************
	// Constructors and Finalizer
	// **********************************************************************

	public Model(View view) {
		this.view = view;
		curState = screenState.player;
		isPlaying = false;
		songTitle = "Title of Song";
		songArtist = "Example Name";
		songProgress = 0.6;
		songDuration = "1:35";
		volume = 1.0;
		bpm = 60;
		
		
		songNotes = new ArrayDeque<>();
		songFreqs = new ArrayDeque<>();

		for(int i = 0; i < 20; i++){
			songNotes.add(RANDOM.nextInt(15));
			songFreqs.add(RANDOM.nextInt(100));
		}
	}

	// **********************************************************************
	// Public Methods (Access Variables)
	// **********************************************************************

	public Point2D.Double getCursor() {
		if (cursor == null)
			return null;
		else
			return new Point2D.Double(cursor.x, cursor.y);
	}

	public screenState getCurrentView(){
		return curState;
	}

	public String getTitle() {
		return songTitle;
	}

	public String getArtist(){
		return songArtist;
	}

	public Double getProgress(){
		return songProgress;
	}

	public int getBPM(){
		return bpm;
	}

	public double getVolume(){
		return volume;
	}

	public int nextNote(){
		int res = songNotes.peekFirst();
		songNotes.addLast(songNotes.pollFirst());
		return res;
	}

	public int nextFreq(){
		int res = songFreqs.peekFirst();
		songFreqs.addLast(songFreqs.pollFirst());
		return res;
	}
	// **********************************************************************
	// Public Methods (Modify Variables)
	// **********************************************************************

	public void togglePlayback(){
		isPlaying = !isPlaying;
	}

	public void swtichScreen(screenState s){
		curState = s;
	}

	public void rewind(){
		songProgress = 0.0;
	}

	public void forward(){
		songProgress = 0.0;
	}
	
	public void progress(){
		if(songProgress < 1)
			songProgress += 0.02;
		else
			songProgress = 0.0;
	}

	public void handleClick(){
		
	}

	public void incVolume(){
		if (volume < 100)
			volume++;
	}

	public void decVolume(){
		if (volume > 0)
			volume--;
	}

	public void incBPM(){
			bpm++;
	}

	public void decBPM(){
		if (bpm > 60)
			bpm--;
	}
	public void setCursorInViewCoordinates(Point q) {
		if (q == null) {
			view.getCanvas().invoke(false, new BasicUpdater() {
				public void update(GL2 gl) {
					cursor = null;
				}
			});
			;
		} else {
			view.getCanvas().invoke(false, new ViewPointUpdater(q) {
				public void update(double[] p) {
					cursor = new Point2D.Double(p[0], p[1]);
				}
			});
			;
		}
	}

	

	// Special method for privileged use by the View class ONLY. This version
	// avoids scheduling a Runnable on the OpenGL thread. Doing it this way is
	// necessary to allow setObjectInViewCoordinates calls to work from other
	// threads, specifically the MouseHandler running on the Swing event thread.
	

	
	// **********************************************************************
	// Inner Classes
	// **********************************************************************
	private static Point[] pointsFromOffset(Point[] pts, int x, int y){
		Point[] res = new Point[pts.length];

		for(int i = 0; i < pts.length; i++)
			res[i] = new Point(pts[i].x + x, pts[i].y + y);

		return res;
	}

	private static int dot(Point a, Point b){
		return a.x * b.x + a.y * b.y;
	}

	private static boolean isLeft(Point p1, Point p2, Point q, boolean strict){
		Point v1 = new Point(-(p2.y - p1.y), p2.x-p1.x);
		Point v2 = new Point(q.x - p1.x, q.y - p1.y);
		double res = dot(v1,v2);
		return (res > 0) || (res == 0 && !strict);
	}

	private boolean contains(Deque<Point> polygon,
			Point q) {
				
		Iterator<Point> polyIter = polygon.iterator();
		Point p1, p2, first = polyIter.next();

		p1 = first;
		p2 = polyIter.next();

		// Cycle through points on a polygon, passing each side to isLeft
		while (polyIter.hasNext()) {
			if (!isLeft(p1, p2, q, true))
				return false; // If it is not left if a side, it is not contained in the polygon
			p1 = p2;
			p2 = polyIter.next();
		}

		// Compare last point with first point
		if (!isLeft(p2, first, q, true))
			return false;

		return true;
	}
	// Convenience class to simplify the implementation of most updaters.
	private abstract class BasicUpdater implements GLRunnable {
		public final boolean run(GLAutoDrawable drawable) {
			GL2 gl = drawable.getGL().getGL2();

			update(gl);

			return true; // Let animator take care of updating the display
		}

		public abstract void update(GL2 gl);
	}

	// Convenience class to simplify updates in cases in which the input is a
	// single point in view coordinates (integers/pixels).
	private abstract class ViewPointUpdater extends BasicUpdater {
		private final Point q;

		public ViewPointUpdater(Point q) {
			this.q = q;
		}

		public final void update(GL2 gl) {
			int h = view.getHeight();
			double[] p = Utilities.mapViewToScene(gl, q.x, h - q.y, 0.0);

			update(p);
		}

		public abstract void update(double[] p);
	}

	
}

// ******************************************************************************
