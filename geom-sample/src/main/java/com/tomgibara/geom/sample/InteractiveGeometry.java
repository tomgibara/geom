package com.tomgibara.geom.sample;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.beans.Transient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;

import com.tomgibara.geom.awt.AWTUtil;
import com.tomgibara.geom.contour.Contour;
import com.tomgibara.geom.contour.EllipseContour;
import com.tomgibara.geom.contour.PathContour;
import com.tomgibara.geom.contour.RectContour;
import com.tomgibara.geom.core.Angles;
import com.tomgibara.geom.core.Context;
import com.tomgibara.geom.core.GeomUtil;
import com.tomgibara.geom.core.LineSegment;
import com.tomgibara.geom.core.Norm;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.core.Tolerances;
import com.tomgibara.geom.core.Vector;
import com.tomgibara.geom.curve.BezierCurve;
import com.tomgibara.geom.curve.CurvePath;
import com.tomgibara.geom.curve.Ellipse;
import com.tomgibara.geom.curve.EllipticalArc;
import com.tomgibara.geom.curve.OffsetCurve;
import com.tomgibara.geom.curve.Spiral;
import com.tomgibara.geom.floats.FloatMapping;
import com.tomgibara.geom.floats.FloatRange;
import com.tomgibara.geom.helper.Intersector;
import com.tomgibara.geom.path.CompositePath;
import com.tomgibara.geom.path.Parameterization;
import com.tomgibara.geom.path.Path;
import com.tomgibara.geom.path.Path.Corner;
import com.tomgibara.geom.path.Path.Location;
import com.tomgibara.geom.path.PolygonalPath;
import com.tomgibara.geom.path.RectPath;
import com.tomgibara.geom.path.SequencePath;
import com.tomgibara.geom.path.SequencePath.Builder.Policy;
import com.tomgibara.geom.path.SplitPath;
import com.tomgibara.geom.shape.Shape;
import com.tomgibara.geom.shape.WindingRule;
import com.tomgibara.geom.stroke.Cap;
import com.tomgibara.geom.stroke.Dash;
import com.tomgibara.geom.stroke.Join;
import com.tomgibara.geom.stroke.Outline;
import com.tomgibara.geom.stroke.PatternDash;
import com.tomgibara.geom.transform.Transform;

public class InteractiveGeometry extends JFrame {

	static final String[] WIDTH_MAPPINGS = new String[] {"MEDIUM", "THIN", "TAPER", "POINT", "BULGE"};
	static final String[] CAPS = new String[] {"BUTT", "SQUARE", "ROUND"};
	static final String[] JOINS = new String[] {"ROUND", "BEVEL"};
	static final String[] DASHES = new String[] {"EVERYWHERE", "DASHED", "DOT DASH"};

	static final FloatMapping BULGE = new FloatMapping() {

		private final float scale = 60f;
		private final FloatRange domain = FloatRange.UNIT_CLOSED;
		private final FloatRange range = new FloatRange(0, scale);

		public FloatRange getDomain() {
			return domain;
		}

		public FloatRange getRange() {
			return range;
		}

		public FloatMapping inverse() {
			throw new UnsupportedOperationException();
		}

		public float map(float x) {
			return scale * (float) Math.sin(x * Angles.PI);
		}

		@Override
		public boolean isLinear() {
			return false;
		}

		@Override
		public boolean isConstant() {
			return false;
		}

		@Override
		public boolean isIdentity() {
			return false;
		}
	};

	public static void main(String[] args) {
		new InteractiveGeometry();
	}

	List<MutableShape> shapes = new ArrayList<>();
	int selectedShape = -1;
	int selectedPt = -1;

	private float gridSize = 10f;
	private boolean applyGrid = false;
	private boolean showStart = false;
	private boolean showBounds = false;
	private boolean showNearest = false;
	private boolean showTangents = false;
	private boolean showCorners = false;
	private boolean showHalfPoint = false;
	private boolean showLengths = false;
	private boolean showStroke = false;
	private boolean showShapes = false;
	private String widthMapping = "MEDIUM";
	private String capName = "BUTT";
	private String joinName = "BEVEL";
	private String dashName = "EVERYWHERE";

	Buttons buttons;
	Editor editor;

	public InteractiveGeometry() {
		super("Interactive Geometry");
		Context.enter(Tolerances.current().builder().setShortestNonLinearCurve(20f).setLeastNonLinearDeviation(0.001f).build(), Context.Policy.LOG_MESSAGE);
		editor = new Editor();
		buttons = new Buttons();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, editor);
		add(BorderLayout.SOUTH, buttons);
		pack();
		setVisible(true);

		addLine();
		addPointLine();
//		addCubicBezier();
//		addSexticBezier();
//		addPolygonal();
//		addSequence();
		addRect();
//		addCircularArc();
//		addSpiral();
//		addEllipticalArc();
//		addBoundedDls();
//		addOffsetCurve();
	}

	void addCubicBezier() {
		shapes.add(new MutableShape(ShapeType.BEZIER, null, new Point(100, 100), new Point(100, 400), new Point(400, 400), new Point(400, 100)));
	}

	void addSexticBezier() {
		shapes.add(new MutableShape(ShapeType.BEZIER, null, new Point(100, 100), new Point(50, 300), new Point(100, 400), new Point(400, 400), new Point(450, 300), new Point(400, 100)));
	}

	void addLine() {
		shapes.add(new MutableShape(ShapeType.LINE, "intersector", new Point(450, 50), new Point(40, 450)));
	}

	void addPolygonal() {
		shapes.add(new MutableShape(ShapeType.POLYGONAL, null, new Point(150, 150), new Point(200, 150), new Point(200, 200), new Point(250, 200), new Point(250, 250), new Point(300, 250)));
	}

	void addPointLine() {
		shapes.add(new MutableShape(ShapeType.LINE, "nearpoint", new Point(200, 250)));
	}

	void addSequence() {
		Point[] pts = GeomUtil.asPoints(0,0, 50,0, 50,50, 50,150, 250,150, 250,50, 250, 0, 300, 0);
		Transform.translation(100, 100).transform(pts);
		shapes.add(new MutableShape(ShapeType.SEQUENCE, null, pts));
	}

	void addRect() {
		shapes.add(new MutableShape(ShapeType.RECT, null, new Point(70, 30), new Point(330, 270)));
	}

	void addSpiral() {
		shapes.add(new MutableShape(ShapeType.SPIRAL, null, new Point(200,200), new Point(270, 170), new Point(150, 100)));
	}

	void addEllipticalArc() {
		shapes.add(new MutableShape(ShapeType.ELLIPTICAL_ARC, null, new Point(300,200), new Point(400, 200), new Point(300, 130)));
	}

	void addEllipse() {
		shapes.add(new MutableShape(ShapeType.ELLIPTICAL, null, new Point(300,200), new Point(400, 200), new Point(300, 130)));
	}

	void addCircularArc() {
		shapes.add(new MutableShape(ShapeType.ARC, null, new Point(300,200), new Point(400, 200), new Point(300, 130)));
	}

	void addBoundedDls() {
		shapes.add(new MutableShape(ShapeType.BOUNDED_DLS, null, new Point(0,10), new Point(400, 390), new Point(100, 100), new Point(200,200)));
	}

	void addOffsetCurve() {
		shapes.add(new MutableShape(ShapeType.OFFSET_BEZIER, null, new Point(100, 130), new Point(100, 100), new Point(100, 400), new Point(400, 400), new Point(400, 100)));
	}

	class Buttons extends JComponent {

		public Buttons() {
			setLayout(new FlowLayout());
			setPreferredSize(new Dimension(1200, 60));
			add(new JCheckBox(new AbstractAction("Grid") {
				@Override
				public void actionPerformed(ActionEvent e) {
					applyGrid = ((JCheckBox) e.getSource()).isSelected();
					editor.repaint();
				}
			}));
			add(new JCheckBox(new AbstractAction("Start") {
				@Override
				public void actionPerformed(ActionEvent e) {
					showStart = ((JCheckBox) e.getSource()).isSelected();
					editor.repaint();
				}
			}));
			add(new JCheckBox(new AbstractAction("Bounds") {
				@Override
				public void actionPerformed(ActionEvent e) {
					showBounds = ((JCheckBox) e.getSource()).isSelected();
					editor.repaint();
				}
			}));
			add(new JCheckBox(new AbstractAction("Tangents") {
				@Override
				public void actionPerformed(ActionEvent e) {
					showTangents = ((JCheckBox) e.getSource()).isSelected();
					editor.repaint();
				}
			}));
			add(new JCheckBox(new AbstractAction("Corners") {
				@Override
				public void actionPerformed(ActionEvent e) {
					showCorners = ((JCheckBox) e.getSource()).isSelected();
					editor.repaint();
				}
			}));
			add(new JCheckBox(new AbstractAction("Nearest") {
				@Override
				public void actionPerformed(ActionEvent e) {
					showNearest = ((JCheckBox) e.getSource()).isSelected();
					editor.repaint();
				}
			}));
			add(new JCheckBox(new AbstractAction("Half-point") {
				@Override
				public void actionPerformed(ActionEvent e) {
					showHalfPoint = ((JCheckBox) e.getSource()).isSelected();
					editor.repaint();
				}
			}));
			add(new JCheckBox(new AbstractAction("Lengths") {
				@Override
				public void actionPerformed(ActionEvent e) {
					showLengths = ((JCheckBox) e.getSource()).isSelected();
					editor.repaint();
				}
			}));
			add(new JCheckBox(new AbstractAction("Stroke") {
				@Override
				public void actionPerformed(ActionEvent e) {
					showStroke = ((JCheckBox) e.getSource()).isSelected();
					editor.repaint();
				}
			}));
			final JComboBox<String> width = new JComboBox<>(WIDTH_MAPPINGS);
			width.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					widthMapping = width.getSelectedItem().toString();
					editor.repaint();
				}
			});
			add(width);
			final JComboBox<String> cap = new JComboBox<>(CAPS);
			cap.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					capName = cap.getSelectedItem().toString();
					editor.repaint();
				}
			});
			add(cap);
			final JComboBox<String> join = new JComboBox<>(JOINS);
			join.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					joinName = join.getSelectedItem().toString();
					editor.repaint();
				}
			});
			add(join);
			final JComboBox<String> dash = new JComboBox<>(DASHES);
			dash.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					dashName = dash.getSelectedItem().toString();
					editor.repaint();
				}
			});
			add(dash);
			add(new JCheckBox(new AbstractAction("Shapes") {
				@Override
				public void actionPerformed(ActionEvent e) {
					showShapes = ((JCheckBox) e.getSource()).isSelected();
					editor.repaint();
				}
			}));
		}

	}

	class Editor extends JComponent implements MouseListener, MouseMotionListener {

		private int width = 1200;
		private int height = 800;

		public Editor() {
			addMouseListener(this);
			addMouseMotionListener(this);
		}

		@Override
		@Transient
		public Dimension getPreferredSize() {
			return new Dimension(width, height);
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setColor(Color.WHITE);
			g2.fillRect(0,0,width, height);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Font font = g2.getFont();
			g2.setFont(font.deriveFont(20f));

			Path intersector = getPath("intersector");
			Path nearpath = getPath("nearpoint");
			Point nearpoint = nearpath == null ? null : nearpath.getStart();
			for (MutableShape shape : shapes) {
				Point np = shape.tag.equals("nearpoint") ? null : nearpoint;
				Path in = shape.tag.equals("intersector") ? null : intersector;
				List<Path> paths = shape.getPaths();
				for (Path pth : paths) {
					plot(pth, g2, np, in);
				}
			}

			for (int i = 0; i < shapes.size(); i++) {
				MutableShape shape = shapes.get(i);
				Point[] points = shape.points;
				boolean number = false;
				for (int j = 0; j < points.length; j++) {
					Point pt = points[j];
					g2.setColor(Color.BLACK);
					g2.fillOval(Math.round(pt.x) - 15, Math.round(pt.y) - 15, 30, 30);
					Color color;
					if (i == selectedShape && j == selectedPt) {
						color = Color.MAGENTA;
					} else if (shape.tag.equals("intersector")) {
						color = Color.RED;
					} else if (shape.tag.equals("nearpoint")) {
						List<Contour> contours = new ArrayList<>();
						for (MutableShape ms : shapes) {
							if (ms == shape) continue;
							contours.addAll(ms.getContours());
						}
						Shape s = new Shape(WindingRule.NON_ZERO, contours);
						Point p = shape.points[0];
						color = s.containsPoint(p) ? Color.PINK : Color.GREEN;
//						for (MutableShape ms : shapes) {
//							if (ms == shape) continue;
//							Contour contour = ms.getContour();
//							if (contour == null) continue;
//							Winder winder = new Winder();
//							com.tomgibara.geom.core.Point.List test = winder.test(contour, point);
//							g2.setColor(Color.MAGENTA);
//							plot(test, g2);
//							int windings = winder.countWindings(contour, point);
//							sum += windings;
//						}
//						color = sum > 0 ? Color.PINK : Color.GREEN;
					} else if (i == selectedShape) {
						color = new Color(0x8080ff);
						number = true;
					} else {
						color = Color.CYAN;
						number = true;
					}
					g2.setColor(color);
					g2.fillOval(Math.round(pt.x) - 10, Math.round(pt.y) - 10, 20, 20);
					g2.setColor(Color.BLACK);
					String label = number ? Integer.toString(j) : "";
					if (!label.isEmpty()) g2.drawString(label, Math.round(pt.x) - 5, Math.round(pt.y) + 8);
				}
			}
			if (showShapes) {
				g2.setColor(new Color(0xffff8000, true));
				Color fill = new Color(0x60ffff00, true);
				Rect rect = Rect.atPoints(20, 30, 200, 130);
				plotSimple(new Shape(WindingRule.EVEN_ODD, new RectContour(RectPath.fromRect(rect, true))), g2, fill);
				plotSimple(new Shape(WindingRule.EVEN_ODD, new EllipseContour(Ellipse.fromRect(rect.translate(200, 0)), true)), g2, fill);
				Rect r = rect.translate(400, 0);
				plotSimple(new Shape(WindingRule.EVEN_ODD, new EllipseContour(Ellipse.fromRect(r).apply(Transform.rotationAbout(r.getCenter(), Angles.PI_BY_SIX)), true)), g2, fill);
			}
		}

		private int findShapeIndex(String tag) {
			for (int i = 0; i < shapes.size(); i++) {
				if (shapes.get(i).tag.equals(tag)) return i;
			}
			return -1;
		}

		private MutableShape findShape(String tag) {
			int i = findShapeIndex(tag);
			return i == -1 ? null : shapes.get(i);
		}

		private Path getPath(String tag) {
			MutableShape shape = findShape(tag);
			return shape == null ? null : shape.getUnderlyingPath();
		}

		private void plot(Collection<Point> pts, Graphics2D g2) {
			Font f = g2.getFont();
			g2.setFont(f.deriveFont(10f));
			int count = 0;
			for (Point pt : pts) {
				g2.drawString(Integer.toString(++count), pt.x + 10, pt.y + 10);
				g2.fillOval(Math.round(pt.x) - 5, Math.round(pt.y) - 5, 10, 10);
			}
			g2.setFont(f);
		}

		private void plot(Path path, Graphics2D g2, Point point, Path inter) {
			try {
				if (showBounds) {
					Rect bounds = path.getBounds();
					g2.setStroke(new BasicStroke(1f));
					g2.setColor(Color.LIGHT_GRAY);
					g2.draw(new Rectangle2D.Float(bounds.minX, bounds.minY, bounds.getWidth(), bounds.getHeight()));
				}
				if (showStart) {
					Point start = path.getStart();
					g2.setColor(Color.GREEN);
					g2.fill(new Ellipse2D.Float(start.x - 4, start.y - 4, 4, 4));
				}
				if (inter != null) {
					Point.List list = new Point.List();
					Intersector intersector = new Intersector(list);
					intersector.intersect(path, inter);
//					System.out.println("bounds checks: " + intersector.getBoundsChecks());
//					System.out.println("rect checks: " + intersector.getRectChecks());
//					System.out.println("line checks: " + intersector.getLineChecks());

					List<Path> paths = new ArrayList<>();
					Path rem = path;
					float gap = 10;
					SplitPath split = null;
					for (Point pt : list) {
						split = rem.byLength().location().moveClosestTo(pt).split();
						Path seg = split.getFirstPath();
						rem = split.getLastPath();
						if (!paths.isEmpty()) seg = seg.byLength().location().moveBy(gap).split().getLastPath();
						seg = seg.byLength().location().moveToFinish().moveBy(-gap).split().getFirstPath();
						paths.add(seg);
					}
					if (split != null) { // ie there was at least one intersection
						paths.add(rem.byLength().location().moveBy(gap).split().getLastPath());
						Color color = Color.BLACK;
						for (Path segment : paths) {
							g2.setColor(color);
							g2.setStroke(new BasicStroke(3f));
							plot(segment, g2);
							color = color == Color.BLACK ? Color.GRAY : Color.BLACK;
						}
						for (Point pt : list) {
							g2.setColor(Color.RED);
							g2.fillOval(Math.round(pt.x) - 5, Math.round(pt.y) - 5, 10, 10);
						}
					} else {
						g2.setColor(Color.BLACK);
						plot(path, g2);
					}
				} else {
					g2.setColor(Color.BLACK);
					plot(path, g2);
				}

				if (showLengths) { // label with length
					g2.setColor(Color.BLUE);
					float length = path.getLength();
					Point pt = new Vector(30,30).translate( path.getFinish() );
					g2.drawString(String.format("%3.3f", length), pt.x, pt.y);
				}

				if (showTangents) {
					g2.setColor(Color.RED);
					g2.setStroke(new BasicStroke(1f));
					for (Location loc = path.byLength().location(); !loc.isAtFinish(); loc.moveBy(40)) {
						LineSegment dls = loc.getPointTangent().getTangentAsSegment().scaleLength(20);
						Point pt = dls.getStart();
						g2.fill(new Ellipse2D.Float(pt.x - 2, pt.y - 2, 4, 4));
						g2.draw(new Line2D.Float(pt.x, pt.y, dls.getFinish().x, dls.getFinish().y));
						Point e = dls.getFinish();
						Path2D.Float tri = new Path2D.Float();
						tri.moveTo(0, -4);
						tri.lineTo(5,0);
						tri.lineTo(0, 4);
						tri.closePath();
						AffineTransform t = AffineTransform.getTranslateInstance(e.x, e.y);
						t.rotate(dls.getTangent().getAngle());
						tri.transform(t);
						g2.fill(tri);
					}
				}

				if (showHalfPoint) {
					g2.setColor(Color.BLUE);
					Parameterization param = path.byIntrinsic();
					Location loc = param.location().moveTo(0.5f);
					LineSegment dls = loc.getPointTangent().getTangentAsSegment().scaleLength(40);
					Point pt = dls.getStart();
					g2.fill(new Ellipse2D.Float(pt.x - 3, pt.y - 3, 6, 6));
					g2.draw(new Line2D.Float(pt.x, pt.y, dls.getFinish().x, dls.getFinish().y));
					float length = loc.getLength();
					g2.drawString(String.format("%3.3f", length), pt.x + 30, pt.y - 30);
				}

				if (showNearest && point != null) {
					Point pt = path.byLength().location().moveClosestTo(point).getPoint();
					g2.setColor(Color.DARK_GRAY);
					g2.fillOval(Math.round(pt.x) - 10, Math.round(pt.y) - 10, 20, 20);
					g2.setColor(Color.GREEN);
					g2.fillOval(Math.round(pt.x) - 7, Math.round(pt.y) - 7, 14, 14);
				}

				if (path instanceof CurvePath) {
					Object obj = ((CurvePath) path).getCurve();
					if (obj instanceof EllipticalArc) {
						g2.setColor(Color.PINK);
						g2.setStroke(new BasicStroke(1f));
						EllipticalArc arc = (EllipticalArc) obj;
						Ellipse geom = arc.getGeom();
						Point center = geom.getCenter();
						Path cen = Ellipse.fromRadius(center, 5).completeArc().getPath();
						plot(cen, g2);
						Vector majA = geom.getSemiMajorAxis();
						if (!majA.isZero()) {
							Path maj = LineSegment.fromVector(center, majA).getPath();
							plot(maj, g2);
						}
						Vector minA = geom.getSemiMinorAxis();
						if (!minA.isZero()) {
							Path min = LineSegment.fromVector(center, minA).getPath();
							plot(min, g2);
						}
					}
				}

			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		}

		private void plot(Path path, Graphics2D g2) {
			Color c = g2.getColor();
			Stroke s = g2.getStroke();
			plotSimple(path, g2);

			Color sc = new Color(0x80c000);
			Color tc = new Color(0x608000);
			Color uc = new Color(0x000000);
			Stroke kc = new BasicStroke(4f);
			if (showCorners) {
				g2.setStroke(kc);
				for (Location location = path.byIntrinsic().location(); !location.isAtFinish(); location.moveToNextCorner()) {
					if (location.isAtCorner()) {
						Point cp = location.getPoint();
						float r = 20f;
						Corner corner = location.getCorner();
						Vector t1 = corner.getStartTangent();
						Vector t2 = corner.getFinishTangent();
						float alpha = t1.getAngle();
						float delta = t1.angleTo(t2);
						float alphaD = Angles.toDegrees( alpha );
						float deltaD = Angles.toDegrees( delta );
						g2.setColor(tc);
						g2.draw(new Line2D.Float(cp.x, cp.y, cp.x + t1.x * r * 1.2f, cp.y + t1.y * r * 1.2f));
						g2.draw(new Line2D.Float(cp.x, cp.y, cp.x + t2.x * r * 1.2f, cp.y + t2.y * r * 1.2f));
						g2.setColor(sc);
						g2.fill( new Arc2D.Float(cp.x - r, cp.y - r, 2 * r, 2 * r, -alphaD, -deltaD, Arc2D.PIE) );
					}
				}
				CompositePath seq = path.splitAtCorners();
				List<? extends Path> paths = seq.getSubpaths();
				for (Path pth : paths) {
					float length = pth.getLength();
					float len = Math.min(50f, length/3f);
					g2.setColor(uc);
					plotSimple(pth.byLength().splitAt(len).getFirstPath(), g2);
					plotSimple(pth.byLength().splitAt(length - len).getLastPath(), g2);
				}
			}
			g2.setColor(c);
			g2.setStroke(s);
		}

		private void plotSimple(Path path, Graphics2D g2) {
			if (path.getLength() == 0f) return;
			plotSimple(AWTUtil.toPath2D(path), path.isClosed(), g2, new Color(0xe0e0e0));
		}

		private void plotSimple(Shape shape, Graphics2D g2, Color fill) {
			plotSimple(AWTUtil.toShape2D(shape), true, g2, fill);
		}

		private void plotSimple(java.awt.Shape shape, boolean closed, Graphics2D g2, Color fill) {
			if (closed) {
				Color c = g2.getColor();
				g2.setColor(fill);
				g2.fill(shape);
				g2.setColor(c);
			}
			g2.draw(shape);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}

		@Override
		public void mousePressed(MouseEvent e) {
			Point pt = pointFromEvent(e);
			int closestShape = -1;
			int closestPt = -1;
			float leastDist = Float.MAX_VALUE;
			for (int i = 0; i < shapes.size(); i++) {
				Point[] points = shapes.get(i).points;
				for (int j = 0; j < points.length; j++) {
					Point point = points[j];
					float dist = Norm.L2.distanceBetween(pt, point);
					if (dist < leastDist) {
						leastDist = dist;
						closestShape = i;
						closestPt = j;
					}
				}
			}
			if (leastDist < 15) {
				selectedShape = closestShape;
				selectedPt = closestPt;
				repaint();
			}
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if (selectedShape == -1 || selectedPt == -1) return;
			Point pt = pointFromEvent(e);
			shapes.get(selectedShape).points[selectedPt] = pt;
			repaint();
		}

		@Override
		public void mouseMoved(MouseEvent e) {
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if (selectedPt == -1) return;
			selectedPt = -1;
			repaint();
		}

		private Point pointFromEvent(MouseEvent e) {
			float x = e.getX();
			float y = e.getY();
			if (applyGrid) {
				x = ((float) Math.floor(x / gridSize)) * gridSize;
				y = ((float) Math.floor(y / gridSize)) * gridSize;
			}
			return new Point(x, y);
		}

	}

	enum ShapeType {
		BEZIER,
		LINE,
		POLYGONAL,
		SEQUENCE,
		RECT,
		SPIRAL,
		ELLIPTICAL_ARC,
		ELLIPTICAL,
		ARC,
		BOUNDED_DLS,
		OFFSET_BEZIER
	}

	private class MutableShape {

		final ShapeType type;
		final String tag;
		final Point[] points;

		public MutableShape(ShapeType type, String tag, Point...points) {
			this.type = type;
			this.tag = tag == null ? "" : tag;
			this.points = points;
		}

		List<Path> getPaths() {
			List<? extends Contour> contours = getContours();
			if (contours.isEmpty()) return Collections.singletonList(getUnderlyingPath());
			List<Path> paths = new ArrayList<>();
			for (Contour contour : contours) {
				paths.add(contour.getPath());
			}
			return paths;
		}

		List<? extends Contour> getContours() {
			switch (type) {
			case RECT: return Collections.singletonList(new RectContour((RectPath) getUnderlyingPath()));
			case OFFSET_BEZIER: {
				BezierCurve curve1 = BezierCurve.fromPoints(Arrays.asList(points).subList(1, points.length));
				BezierCurve curve2 = (BezierCurve) curve1.getPath().getReverse().getCurve();
				float radius = Norm.L2.distanceBetween(points[0], points[1]);
				OffsetCurve offset1 = OffsetCurve.from(curve1.getPath(), FloatMapping.Util.linear(FloatRange.UNIT_CLOSED, radius, 0f));
				OffsetCurve offset2 = OffsetCurve.from(curve2.getPath(), FloatMapping.Util.linear(FloatRange.UNIT_CLOSED, 0f, radius));
				SequencePath path = SequencePath.builder().withPolicy(Policy.JOIN).addPaths(offset1.getPath(), offset2.getPath()).closeAndBuild();
				return Collections.singletonList(new PathContour(path));
			}
			default:
			if (!tag.isEmpty() || !showStroke) return Collections.emptyList();
			final FloatMapping mapping;
			switch (widthMapping) {
			case "MEDIUM" : mapping = FloatMapping.Util.constant(FloatRange.UNIT_CLOSED, 20f); break;
			case "THIN" : mapping = FloatMapping.Util.constant(FloatRange.UNIT_CLOSED, 10f); break;
			case "TAPER" : mapping = FloatMapping.Util.linear(FloatRange.UNIT_CLOSED, 40f, 20f); break;
			case "POINT" : mapping = FloatMapping.Util.linear(FloatRange.UNIT_CLOSED, 40f, 0f); break;
			case "BULGE" : mapping = BULGE; break;
			default: throw new IllegalStateException("Unsupported widthMapping: " + widthMapping);
			}

			final Cap cap;
			switch (capName) {
			case "ROUND": cap = Cap.ROUND_CAP; break;
			case "SQUARE": cap = Cap.SQUARE_CAP; break;
			case "BUTT": cap = Cap.BUTT_CAP; break;
			default: throw new IllegalStateException("Unsupported capName: " + capName);
			}

			final Join join;
			switch (joinName) {
			case "ROUND": join = Join.ROUND_JOIN; break;
			case "BEVEL": join = Join.BEVEL_JOIN; break;
			default: throw new IllegalStateException("Unsupported joinName: " + joinName);
			}

			final Dash dash;
			switch (dashName) {
			case "EVERYWHERE" : dash = Dash.EVERYWHERE_DASH; break;
			case "DASHED" : dash = PatternDash.single(80, 80); break;
			case "DOT DASH": dash = PatternDash.pattern(200, new FloatRange(25, 125), new FloatRange(150, 200)); break;
			default: throw new IllegalStateException("Unsupported dashName: " + dashName);
			}

			com.tomgibara.geom.stroke.Stroke stroke = new com.tomgibara.geom.stroke.Stroke(new Outline(join, mapping), cap, dash);
			Path path = getUnderlyingPath();
			return stroke.stroke(path);
			}
		}

		Path getUnderlyingPath() {
			switch (type) {
			case RECT:
				return RectPath.fromRect(Rect.atPoints(points[0],  points[1]), false);
			case BEZIER:
				return BezierCurve.fromPoints(points).getPath();
			case LINE: return (points.length == 1 ? LineSegment.fromPoint(points[0], Vector.UNIT_X) : LineSegment.fromPoints(points[0], points[1])).getPath();
			case POLYGONAL: {
				return PolygonalPath.builder().addPoints(points).build();
			}
			case SEQUENCE: return SequencePath.builder().addPoints(points).asBezierCurve(2).asLinearPath().asLinearPath().asLinearPath().asBezierCurve(2).closeAndBuild().getReverse();
			case SPIRAL: {
				Point center = points[0];
				Vector v1 = points[1].vectorFrom(center);
				Vector v2 = points[2].vectorFrom(center);
				float theta = v1.getAngle();
				float phi = v2.getAngle();
				float TWO_PI = 2f * (float) Math.PI;
				if (theta < 0f) theta += TWO_PI;
				if (phi < 0f) phi += TWO_PI;
				phi += 3 * TWO_PI;
				float a = v1.getMagnitude();
				float b = v2.getMagnitude();
				return Spiral.from(center, theta, phi, a, b).getPath();
			}
			case ELLIPTICAL_ARC: {
				Point c = points[0];
				Point a = points[1];
				Point b = points[2];
				Vector v1 = a.vectorFrom(c);
				Vector v2 = b.vectorFrom(c);
				Transform t = Transform.components(v1.x, v1.y, v2.x, v2.y, c.x, c.y);
//				Vector eigenValues = t.getEigenValues();
//				Transform diag = Transform.components(eigenValues.x, 0f, 0f, eigenValues.y, 0f, 0f);
//				Transform basis = t.getEigenBasis();
//				Transform inv = basis.getInverse();
//				Transform prod = inv.apply(diag).apply(basis);
//				System.out.println(t);
//				System.out.println(prod);
				Ellipse geom = Ellipse.fromTransform(t);

//				Ellipse geom;
//				try {
//					geom = Ellipse.fromRadii(c, v1.x, v1.y);
//				} catch (IllegalArgumentException e) {
//					geom = Ellipse.fromRadius(c, Norm.L2.magnitude(v1.x, v1.y));
//				}

				//Transform tmp = Transform.rotation((float) (Math.random() * Math.PI * 2.0));
				//float x = (float) Math.random();
				//float y = (float) Math.random();
				//Transform tmp = Transform.scale(x, y);
				//System.out.println(x + " " + y + " -> " + tmp.getEigenValues());
				//System.out.println("MAJ " + geom.getMajorRadius() + " MIN " + geom.getMinorRadius());
//				System.out.println(geom.getMajorRadius() + " " + geom.getMinorRadius());
				Transform basis = geom.getTransform().getEigenBasis();
				geom = Ellipse.fromTransform(basis.apply(geom.getTransform()));
				return geom.arc(0, 0.9f).getPath();
			}
			case ELLIPTICAL: {
				Point c = points[0];
				Point a = points[1];
				Point b = points[2];
				Vector v1 = a.vectorFrom(c);
				Vector v2 = b.vectorFrom(c);
				Transform t = Transform.components(v1.x, v1.y, v2.x, v2.y, c.x, c.y);
				Ellipse geom = Ellipse.fromTransform(t);
				Transform basis = geom.getTransform().getEigenBasis();
				geom = Ellipse.fromTransform( basis.apply(geom.getTransform()) );
				return geom.completeArc().getPath();
				}
			case ARC:
				return EllipticalArc.circularArcThroughThreePoints(points[0], points[1], points[2]).getPath();
			case BOUNDED_DLS: {
				LineSegment dls = LineSegment.fromPoints(points[0], points[1]);
				Rect rect = Rect.atPoints(points[2], points[3]);
				return dls.bounded(rect).getPath();
			}
			default : throw new IllegalStateException("non-contour without underlying path: " + type);
			}
		}

	}

}
