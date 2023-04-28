//******************************************************************************
// Copyright (C) 2016-2023 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Fri Mar 10 18:48:57 2023 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20160209 [weaver]:	Original file.
// 20190203 [weaver]:	Updated to JOGL 2.3.2 and cleaned up.
// 20190227 [weaver]:	Updated to use model and asynchronous event handling.
// 20190318 [weaver]:	Modified for homework04.
// 20210320 [weaver]:	Added basic keyboard hints to drawMode().
// 20220311 [weaver]:	Improved hint wording in updatePointWithReflection().
// 20230310 [weaver]:	Improved TODO guidance especially for members to add.
//
//******************************************************************************
// Notes:
//
//******************************************************************************

package edu.ou.cs.cg.assignment.spotifyWidget;

//import java.lang.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.lang.ProcessBuilder.Redirect;
import java.text.DecimalFormat;
import java.util.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.*;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;

import edu.ou.cs.cg.assignment.spotifyWidget.Model.screenState;
import edu.ou.cs.cg.utilities.Utilities;

//******************************************************************************

/**
 * The <CODE>View</CODE> class.
 * <P>
 *
 * @author Chris Weaver
 * @version %I%, %G%
 */
public final class View
		implements GLEventListener {
	// **********************************************************************
	// Private Class Members
	// **********************************************************************

	private static final boolean DEBUG = true;
	private static final int DEFAULT_FRAMES_PER_SECOND = 60;
	private static final DecimalFormat FORMAT = new DecimalFormat("0.000");
		
	public static final Random			RANDOM = new Random();
	// **********************************************************************
	// Public Class Members
	// **********************************************************************

	public static final int MIN_SIDES = 3;
	public static final int MAX_SIDES = 12;

	// **********************************************************************
	// Private Members
	// **********************************************************************

	// State (internal) variables
	private final GLJPanel canvas;
	private int w; // Canvas width
	private int h; // Canvas height

	private TextRenderer renderer;

	private final FPSAnimator animator;
	private int counter; // Frame counter

	private final Model model;

	private final KeyHandler keyHandler;
	private final MouseHandler mouseHandler;

	private int screenWidth = 880;
	private int screenHeight = 300;

	private int[] barHeights;

	// **********************************************************************
	// Constructors and Finalizer
	// **********************************************************************

	public View(GLJPanel canvas) {
		this.canvas = canvas;

		// Initialize rendering
		counter = 0;
		canvas.addGLEventListener(this);

		// Initialize model (scene data and parameter manager)
		model = new Model(this);

		// Initialize controller (interaction handlers)
		keyHandler = new KeyHandler(this, model);
		mouseHandler = new MouseHandler(this, model);

		// Initialize animation
		animator = new FPSAnimator(canvas, DEFAULT_FRAMES_PER_SECOND);
		animator.start();
	}

	// **********************************************************************
	// Getters and Setters
	// **********************************************************************

	public GLJPanel getCanvas() {
		return canvas;
	}

	public int getWidth() {
		return w;
	}

	public int getHeight() {
		return h;
	}

	// **********************************************************************
	// Public Methods
	// **********************************************************************

	

	// **********************************************************************
	// Override Methods (GLEventListener)
	// **********************************************************************

	public void init(GLAutoDrawable drawable) {
		w = drawable.getSurfaceWidth();
		h = drawable.getSurfaceHeight();

		renderer = new TextRenderer(new Font("Monospaced", Font.PLAIN, 12),
				true, true);

		initPipeline(drawable);
		barHeights =new int[Model.NUM_BARS];
		for(int i = 0; i < Model.NUM_BARS; i++)
			barHeights[i] = 0;
	}

	public void dispose(GLAutoDrawable drawable) {
		renderer = null;
	}

	public void display(GLAutoDrawable drawable) {
		updatePipeline(drawable);

		update(drawable);
		render(drawable);
	}

	public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
		this.w = w;
		this.h = h;
	}

	// **********************************************************************
	// Private Methods (Rendering)
	// **********************************************************************

	private void update(GLAutoDrawable drawable) {
		counter++; 

		if(model.isPlaying && counter % 60 == 0)
			model.progress();
		
		updateBars();

		int bps = model.getBPM() / 60;
		int freq = 60/(3 * bps); 

		if(counter % freq == 0 && model.isPlaying)
			addBeat(model.nextNote(), model.nextFreq());

	}

	private void render(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();

		gl.glClear(GL.GL_COLOR_BUFFER_BIT); // Clear the buffer

		// Draw the scene
		drawMain(gl); // Draw main content
		drawMode(drawable); // Draw mode text

		gl.glFlush(); // Finish and display
	}

	// **********************************************************************
	// Private Methods (Pipeline)
	// **********************************************************************

	private void initPipeline(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();

		//Background color
		gl.glClearColor((209.0f/255.0f), (186.0f/255.0f), (152.0f/255.0f), 0.0f); // Black background
		
		// Make points easier to see on Hi-DPI displays
		gl.glEnable(GL2.GL_POINT_SMOOTH); // Turn on point anti-aliasing
	}

	private void updatePipeline(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		GLU glu = GLU.createGLU();

		gl.glMatrixMode(GL2.GL_PROJECTION); // Prepare for matrix xform
		gl.glLoadIdentity(); // Set to identity matrix
		glu.gluOrtho2D(0.0f, 1080f, 0.0f, 720.0f); // 2D translate and scale
	}

	// **********************************************************************
	// Private Methods (Scene)
	// **********************************************************************

	private void drawMode(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();

		renderer.beginRendering(w, h);

		// Draw all text in light gray
		renderer.setColor(0.75f, 0.75f, 0.75f, 1.0f);

		Point2D.Double cursor = model.getCursor();

		//Draw menu options
		//Draw either playbackview or a list view
		switch(model.getCurrentView()){
			case player :
				drawPlayback(screenWidth, screenHeight, 365, 500); 
				break;
			
			default:
				break;
			
		}
		
		renderer.endRendering();
	}

	private void drawMain(GL2 gl) {
		drawControls(gl);
		drawScreen(gl);
	}


	private void drawScreen(GL2 gl){
		
		//Draw black background
		//Width 880
		setColor(gl, 0,0,0);
		fillRect(gl, 100, 400, screenWidth, screenHeight);

		setColor(gl, new Color(255,255,255));
		fillRect(gl,  100 + (screenWidth/2),400,2, screenHeight);

		drawVisualizer(gl, 130, 450);
		//Draw progress bar if playback is occuring
		if(model.getCurrentView() == screenState.player)
			drawProgressBar(gl);
		
	}
	
	private void drawVisualizer(GL2 gl, int ix, int iy){
		int x = ix;
		int minHeight = 10;
		setColor(gl, new Color(255,255,255));
		for(int i = 0; i < model.NUM_BARS; i++){
			int barHeight = minHeight + barHeights[i];
			fillRect(gl, x, iy , 20, barHeight);
			x += 25;
		}
	}

	private void addBeat(int barNum, int mag){
		int max_height = (50);
		mag = (int) (mag * model.getVolume());
		barNum = Math.min(model.NUM_BARS, barNum);

		barHeights[barNum] = Math.min(max_height, barHeights[barNum] + mag);
		int tmpMag = mag/2;
		int i = barNum + 1;
		int locMag;

		//Spread freq forward
		while(tmpMag > 10 && i < model.NUM_BARS){
			barHeights[i] = Math.min(max_height, barHeights[i] + tmpMag);
			tmpMag -= tmpMag/5;
			i++;
		}

		//Spread freq backward
		tmpMag = mag/2;
		i = barNum - 1;
		while(tmpMag > 10 && i >= 0){
			barHeights[i] += Math.min(max_height, barHeights[i] + tmpMag + RANDOM.nextInt(tmpMag/2));
			tmpMag -= tmpMag/5;
			i--;
		}

	}
	private void updateBars(){
		for(int i = 0; i < Model.NUM_BARS; i++)
			if(barHeights[i] > 0)
				barHeights[i] -= 2;
			else
				barHeights[i] = 0;
	}
	private void drawProgressBar(GL2 gl){
		int barStart = 600;
		int barHeight = 450;
		int barWidth = 300;
		
		//Draw Bar
		setColor(gl, new Color(255,255,255));
		fillRect(gl, barStart, barHeight, barWidth, 1);

		//Draw endcaps
		fillRect(gl, barStart, barHeight-8, 1, 16);
		fillRect(gl, barStart+barWidth, barHeight-8,1.5,16);

		//Draw point
		drawCircle(gl, barStart + (int)(barWidth * model.getProgress()), barHeight, 4, 10, new Color(255,255,255));

	}
	//Draw Playback information on right half of screen
	//x,y is the upper right hand point of screen
	//Height is region of information and doesn't include the footer options
	public void drawPlayback(int w, int h, int x, int y){
		TextRenderer rend = new TextRenderer(new Font("Monospaced", Font.PLAIN, 14),
				false, true);
		int ix = x + ((2/4) * w);

		rend.beginRendering(this.w, this.h);
		rend.setColor(166.0f/255.0f, 163.0f/255.0f, 162.0f/255.0f, 1.0f);
		rend.draw(model.getTitle(), ix, this.h-75);
		rend.endRendering();
		
		rend = new TextRenderer(new Font("Monospaced", Font.PLAIN, 10),
				false, true);

		rend.beginRendering(this.w, this.h);
		rend.draw(model.getArtist(), ix + 20, this.h-95);
		rend.setColor(166.0f/255.0f, 163.0f/255.0f, 162.0f/255.0f, 1.0f);
		rend.endRendering();

		
	}
	//Outline of play button
	public static final Point[] PLAY_BUTTON = new Point[] {
		new Point(0,50),
		new Point(0,-50),
		new Point(75,0) 
	};

	public static final Point[] FWD_BUTTON = new Point[] {
		new Point(0,50),
		new Point(0,-50),
		new Point(50,0) 
	};

	//Outline of bar used for pause, rwwd, and fwwd
	public static final Point[] BUTTON_BAR = new Point[] {
		new Point(-12,50),
		new Point(12,50),
		new Point(12,-50),
		new Point(-12,-50)
	};

	//Reverse play button for rewind
	public static final Point[] RWD_BUTTON = new Point[] {
		new Point(0,50),
		new Point(0,-50),
		new Point(-50,0) 
	};

	private void drawControls(GL2 gl){
		int controlHeight = 260;
		//Draw Media Controls
		setColor(gl, new Color(150, 148, 143));
		
		//If playing draw the play button
		if(model.isPlaying){
			fillPoly(gl, 1080/2 - 25, controlHeight, PLAY_BUTTON);
			setColor(gl, new Color(0,0,0));
			edgePoly(gl, 1080/2 - 25, controlHeight, PLAY_BUTTON);
		}
		//Draw Pause Button
		else{
			fillPoly(gl, (1080/2) - 25, controlHeight, BUTTON_BAR);
			fillPoly(gl, (1080/2) + 25, controlHeight, BUTTON_BAR);
			setColor(gl, new Color(0,0,0));
			edgePoly(gl, (1080/2) - 25, controlHeight, BUTTON_BAR);
			edgePoly(gl, (1080/2) + 25, controlHeight, BUTTON_BAR);
		}

		setColor(gl, new Color(150, 148, 143));
		//Draw rewind
		fillPoly(gl, 1080/4 + 14, controlHeight, BUTTON_BAR);
		fillPoly(gl, 1080/4 - 14, controlHeight, RWD_BUTTON);

		//Draw ffwd
		fillPoly(gl, (3 * 1080/4) - 14, controlHeight, BUTTON_BAR);
		fillPoly(gl, (3 * 1080/4) + 14, controlHeight, FWD_BUTTON);

		setColor(gl, new Color(0,0,0));
		//Draw rewind
		edgePoly(gl, 1080/4 + 14, controlHeight, BUTTON_BAR);
		edgePoly(gl, 1080/4 - 14, controlHeight, RWD_BUTTON);

		//Draw ffwd
		edgePoly(gl, (3 * 1080/4) - 14, controlHeight, BUTTON_BAR);
		edgePoly(gl, (3 * 1080/4) + 14, controlHeight, FWD_BUTTON);
	}

	// **********************************************************************
	// Private Methods (Polygons)
	// **********************************************************************

	
	

	// Creates a regular N-gon with points stored in counterclockwise order.
	// The polygon is centered at the origin with first vertex at (1.0, 0.0).
	private Deque<Point2D.Double> createPolygon(int sides) {
		Deque<Point2D.Double> polygon = new ArrayDeque<Point2D.Double>(sides);
		// Random Comments
		double dt = 1.0 / (double) sides;

		for (double t = 0.0; t < 1; t += dt) { // We alwasy need full shape, so vary t 0->1
			// Radius is always 1, so we just need
			double dx = Math.cos(2 * Math.PI * t);
			double dy = Math.sin(2 * Math.PI * t);
			polygon.add(new Point2D.Double(dx, dy));

		}

		return polygon;
	}

	// Draws the sides of the specified polygon.
	private void edgePolygon(GL2 gl, Deque<Point2D.Double> polygon) {
		Point2D.Double p;
		Iterator<Point2D.Double> polyIter = polygon.iterator();
		if (polygon.isEmpty())
			System.out.println("Its the deque issue :(");
		gl.glBegin(GL.GL_LINE_LOOP);
		while (polyIter.hasNext()) {
			p = polyIter.next();
			gl.glVertex2d(p.x, p.y);
		}
		gl.glEnd();
	}

	// Draws the interior of the specified polygon.
	private void fillPolygon(GL2 gl, Deque<Point2D.Double> polygon) {
		Point2D.Double p;
		Iterator<Point2D.Double> polyIter = polygon.iterator();
		gl.glLineWidth(1.5f);
		gl.glBegin(GL2.GL_POLYGON);
		while (polyIter.hasNext()) {
			p = polyIter.next();
			gl.glVertex2d(p.x, p.y);
		}
		gl.glEnd();
	}

	private void	fillRect(GL2 gl, int x, int y, int w, int h)
	{
		

		gl.glBegin(GL2.GL_POLYGON);

		gl.glVertex2i(x+0, y+0);
		gl.glVertex2i(x+0, y+h);
		gl.glVertex2i(x+w, y+h);
		gl.glVertex2i(x+w, y+0);

		gl.glEnd();
	}

	private void	edgeRect(GL2 gl, int x, int y, int w, int h)
	{
		

		gl.glBegin(GL.GL_LINE_LOOP);

		gl.glVertex2i(x+0, y+0);
		gl.glVertex2i(x+0, y+h);
		gl.glVertex2i(x+w, y+h);
		gl.glVertex2i(x+w, y+0);

		gl.glEnd();

		gl.glLineWidth(1.0f);
	}

	// Fills a polygon defined by a starting point and a sequence of offsets.
	private void	fillPoly(GL2 gl, int startx, int starty, Point[] offsets)
	{
		

		gl.glBegin(GL2.GL_POLYGON);

		for (int i=0; i<offsets.length; i++)
			gl.glVertex2i(startx + offsets[i].x, starty + offsets[i].y);

		gl.glEnd();
	}

	// Edges a polygon defined by a starting point and a sequence of offsets.
	private void	edgePoly(GL2 gl, int startx, int starty, Point[] offsets)
	{
		

		gl.glBegin(GL2.GL_LINE_LOOP);

		for (int i=0; i<offsets.length; i++)
			gl.glVertex2i(startx + offsets[i].x, starty + offsets[i].y);

		gl.glEnd();

		gl.glLineWidth(1.0f);
	}
	

	// **********************************************************************
	// Private Methods (Reflection)
	// **********************************************************************

	

	// **********************************************************************
	// Private Methods (Vectors)
	// **********************************************************************

	// This might be a method to calculate a dot product. Sure seems like it.
	private double dot(double vx, double vy,
			double wx, double wy) {

		return vx * wx + wy * vy;
	}

	// Helper for dot product
	private double dot(Point2D.Double p1, Point2D.Double p2) {

		return dot(p1.x, p1.y, p2.x, p2.y);
	}

	// Determines if point q is to the left of line p1->p2. If strict is false,
	// points exactly on the line are considered to be left of it.
	private boolean isLeft(Point2D.Double p1, Point2D.Double p2,
			Point2D.Double q, boolean strict) {
		// Generate normal vector and vector from p1->q
		Point2D.Double v1 = new Point2D.Double(-(p2.y - p1.y), p2.x - p1.x);
		Point2D.Double v2 = new Point2D.Double(q.x - p1.x, q.y - p1.y);

		// If dot product is greater than 0, then it is left of it
		// if it equals 0, then it is normal to it and on the line
		double res = dot(v1, v2);
		return (res > 0) || (res == 0 && !strict);
	}

	// Determines if point q is inside a polygon. The polygon must be convex
	// with points stored in counterclockwise order. Points exactly on any side
	// of the polygon are considered to be outside of it.
	private boolean contains(Deque<Point2D.Double> polygon,
			Point2D.Double q) {
		Iterator<Point2D.Double> polyIter = polygon.iterator();
		Point2D.Double p1, p2, first = polyIter.next();

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

	// **********************************************************************
	// Private Methods (Helper Functions from previous assignments)
	// **********************************************************************
	private void drawCircle(GL2 gl, int x, int y, double r, int sides, Color c) {
		drawCircle(gl, x, y, r, sides, 0.0, 1.0, c);
	}

	private void drawCircle(GL2 gl, int x, int y, double radius, int sides, double startTheta, double endTheta,
			Color color) {
		double dt = (endTheta - startTheta) / sides;

		gl.glBegin(GL2.GL_POLYGON);
		setColor(gl, color);
		for (double t = startTheta; t < endTheta; t += dt) {
			double dx = radius * Math.cos(2 * Math.PI * t);
			double dy = radius * Math.sin(2 * Math.PI * t);

			gl.glVertex2d(x + dx, y + dy);

		}
		gl.glEnd();
	}

	private void fillRect(GL2 gl, double x, double y, double w, double h) {
		gl.glBegin(GL2.GL_POLYGON);

		gl.glVertex2d(x + 0.0, y + 0.0);
		gl.glVertex2d(x + 0.0, y + h);
		gl.glVertex2d(x + w, y + h);
		gl.glVertex2d(x + w, y + 0.0);

		gl.glEnd();
	}

	// Edges a rectangle having lower left corner at (x,y) and dimensions (w,h).
	private void edgeRect(GL2 gl, double x, double y, double w, double h) {
		gl.glLineWidth(1.0f);
		gl.glBegin(GL.GL_LINE_LOOP);

		gl.glVertex2d(x + 0.0, y + 0.0);
		gl.glVertex2d(x + 0.0, y + h);
		gl.glVertex2d(x + w, y + h);
		gl.glVertex2d(x + w, y + 0.0);

		gl.glEnd();
	}

	// Sets color, normalizing r, g, b, a values from max 255 to 1.0.
	private void setColor(GL2 gl, int r, int g, int b, int a) {
		gl.glColor4f(r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f);
	}

	// Sets fully opaque color, normalizing r, g, b values from max 255 to 1.0.
	private void setColor(GL2 gl, int r, int g, int b) {
		setColor(gl, r, g, b, 255);
	}

	// Wrapper for setting color with Java color object
	private void setColor(GL2 gl, Color c) {
		setColor(gl, c.getRed(), c.getGreen(), c.getBlue());
	}
}

// ******************************************************************************
