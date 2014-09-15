package gui;

import wiki_parser.Article;
import wiki_parser.Parser;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.Border;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;

import edu.uci.ics.jung.algorithms.layout.PolarPoint;
import edu.uci.ics.jung.algorithms.layout.RadialTreeLayout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Tree;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalLensGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.EllipseVertexShapeTransformer;
import edu.uci.ics.jung.visualization.decorators.PickableEdgePaintTransformer;
import edu.uci.ics.jung.visualization.decorators.PickableVertexPaintTransformer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.layout.LayoutTransition;
import edu.uci.ics.jung.visualization.picking.PickedState;
import edu.uci.ics.jung.visualization.renderers.DefaultVertexLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.GradientVertexRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
import edu.uci.ics.jung.visualization.subLayout.TreeCollapser;
import edu.uci.ics.jung.visualization.transform.LensSupport;
import edu.uci.ics.jung.visualization.transform.shape.HyperbolicShapeTransformer;
import edu.uci.ics.jung.visualization.transform.shape.ViewLensSupport;
import edu.uci.ics.jung.visualization.util.Animator;

@SuppressWarnings("serial")
public class UI extends JApplet {
	final int windowSizeX = 800;
	final int windowSizeY = 600;
	
	static final String instructions = 
            "<html><body style=\"padding:20px;\">"+
            "<h2><center>Instructions for Graphiti</center></h2>"+
            "<br><ul><li>Hypyerbolic View</li>"+
            "<li>Parse Tree in Realtime</li>"+
            "<li>...</li></ul><br>"+
            "<p>More Information...</p><br><hr><br>"+
            "<p>This program was developed by:<br>Andre Karge and Sebastian Gottschlich<br>Bauhaus University Weimar - Summerterm 2014"
            + "</body></html>";
    JDialog instDialog;

    Forest<String,Integer> graph;
    Factory<DirectedGraph<String,Integer>> graphFactory = 
        new Factory<DirectedGraph<String,Integer>>() {
             public DirectedGraph<String, Integer> create() {
                return new DirectedSparseMultigraph<String,Integer>();
            }
        };
    Factory<Tree<String,Integer>> treeFactory =
        new Factory<Tree<String,Integer>> () {
         public Tree<String, Integer> create() {
            return new DelegateTree<String,Integer>(graphFactory);
        }
    };
    Factory<Integer> edgeFactory = new Factory<Integer>() {
        int i=0;
        public Integer create() {
            return i++;
        }};
    Factory<String> vertexFactory = new Factory<String>() {
        int i=0;
        public String create() {
            return "V"+i++;
        }};

    VisualizationViewer<String,Integer> vv;
    VisualizationServer.Paintable rings;
    TreeLayout<String,Integer> treeLayout;
    RadialTreeLayout<String,Integer> radialLayout;
    
    ArrayList<String> otherNodes = new ArrayList<String>();
    ArrayList<Integer> edgeList = new ArrayList<Integer>();
    ArrayList<Article> parsedGraph = new ArrayList<Article>();
    
    JProgressBar progressBar;
    String root, startUrl;
    boolean expanded = false;
    boolean hyperbola = false;
    LensSupport hyperbolicViewSupport;

    @SuppressWarnings({ "unchecked", "rawtypes" })
	public UI(final JFrame frame) {
        graph = new DelegateForest<String,Integer>();
        treeLayout = new TreeLayout<String,Integer>(graph);
        radialLayout = new RadialTreeLayout<String,Integer>(graph);
        final TreeCollapser collapser = new TreeCollapser();
        radialLayout.setSize(new Dimension(windowSizeX, windowSizeY));
        vv =  new VisualizationViewer<String,Integer>(radialLayout, new Dimension(windowSizeX,windowSizeY));
        
        vv.getRenderContext().setVertexLabelRenderer(new DefaultVertexLabelRenderer(Color.blue));
        vv.getRenderContext().setVertexStrokeTransformer(new ConstantTransformer(new BasicStroke(0.5f)));
        vv.getRenderContext().setEdgeStrokeTransformer(new ConstantTransformer(new BasicStroke(1.5f)));
        vv.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);

        PickedState<String> ps = vv.getPickedVertexState();
        PickedState<Integer> pes = vv.getPickedEdgeState();
        vv.getRenderContext().setVertexFillPaintTransformer(new PickableVertexPaintTransformer<String>(ps, Color.red, Color.yellow));
        vv.getRenderContext().setEdgeDrawPaintTransformer(new PickableEdgePaintTransformer<Integer>(pes, Color.black, Color.cyan));
        vv.setBackground(Color.white);
        vv.setAutoscrolls(true);
        Border border = BorderFactory.createLineBorder(Color.gray);
        border = BorderFactory.createLoweredBevelBorder();
        vv.setBorder(border);
        
        vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line<String,Integer>());
        vv.getRenderer().setVertexRenderer(new GradientVertexRenderer<String,Integer>(Color.lightGray, Color.gray, Color.white, Color.blue, vv.getPickedVertexState(), false));

        Transformer<String, String> transformtip = new Transformer<String, String>() {
            @Override public String transform(String arg0) {
            	int size = parsedGraph.size();
            	Article node = new Article();
        		for(int x=0; x < size; ++x) {
        			if(parsedGraph.get(x).titel.equals(arg0)) {
        				node = parsedGraph.get(x);
        				return "<html><p><b>"+node.titel+"</b> (in '"+ graph.getParent(arg0).toString() +"': "+node.count+")<br>"+node.teaser+"<br><br>"+node.url+"</p></html>";
        			}
        		}
        		return "";
            }
        };

        vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line<String, Integer>());
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<String>());
        vv.setVertexToolTipTransformer(transformtip);
        // Wenn auf Vertex geklickt wurde, dann wird die Funktion ausgefuehrt.
        final PickedState<String> pickedState = vv.getPickedVertexState();
        pickedState.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                Object subject = e.getItem();
                if (subject instanceof String) {
                    String vertex = (String) subject;
                    if (pickedState.isPicked(vertex)) {
                    	int size = parsedGraph.size();
                    	for(int x=0; x < size; ++x) {
                    		if(parsedGraph.get(x).titel.equals(vertex) && graph.getChildCount(vertex)==0) {
                    			parseChildrens(parsedGraph.get(x));
                    		}
                    	}
                    }
                }
            }
        });

        hyperbolicViewSupport = 
                new ViewLensSupport<String,Integer>(vv, new HyperbolicShapeTransformer(vv, 
                		vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW)), 
                        new ModalLensGraphMouse());

        Container content = getContentPane();
        final GraphZoomScrollPane panel = new GraphZoomScrollPane(vv);
        content.add(panel);
         
        final DefaultModalGraphMouse<String, Integer> graphMouse = new DefaultModalGraphMouse<String, Integer>();
        vv.setGraphMouse(graphMouse);
        vv.addKeyListener(graphMouse.getModeKeyListener());
        rings = new Rings();
		//vv.addPreRenderPaintable(rings);
         
        JComboBox<String> modeBox = graphMouse.getModeComboBox();
        modeBox.addItemListener(graphMouse.getModeListener());
    	modeBox.setBackground(new Color(180,180,180));
        graphMouse.setMode(ModalGraphMouse.Mode.PICKING);
 
        final ScalingControl scaler = new CrossoverScalingControl();
 
        JButton plus = new JButton("+");
    	plus.setBackground(new Color(180,180,180));
        plus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scaler.scale(vv, 1.1f, vv.getCenter());
            }
        });
        JButton minus = new JButton("-");
    	minus.setBackground(new Color(180,180,180));
        minus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scaler.scale(vv, 1/1.1f, vv.getCenter());
            }
        });

    	final JTextField tf  = new JTextField("", 30);
    	JButton button = new JButton("visualize URL");
    	button.setBackground(new Color(150,0,0));
    	button.setForeground(Color.white);
    	
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	// TODO: Brauchen URL-Checker (Ist es ein Wikipedia-link?)
            	if(!tf.getText().equals("") && tf.getText().contains("wikipedia")) {
            		startUrl = tf.getText().toString();
            		deleteTree();
            		setProgress(1);
            		vv.removeAll();
            		createTree(startUrl);
            		newPaint();
            	} else
            		JOptionPane.showMessageDialog(frame, "Invalid Wikipedia-URL.");
            }
        });
    	            
        JToggleButton radial = new JToggleButton("Radial");
    	radial.setBackground(new Color(180,180,180));
        radial.addItemListener(new ItemListener() {
             public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    LayoutTransition<String,Integer> lt = new LayoutTransition<String,Integer>(vv, treeLayout, radialLayout);
                    Animator animator = new Animator(lt);
                    animator.start();
                    vv.getRenderContext().getMultiLayerTransformer().setToIdentity();
                    vv.addPreRenderPaintable(rings);
                } else {
                    LayoutTransition<String,Integer> lt = new LayoutTransition<String,Integer>(vv, radialLayout, treeLayout);
                    Animator animator = new Animator(lt);
                    animator.start();
                    vv.getRenderContext().getMultiLayerTransformer().setToIdentity();
                    vv.removePreRenderPaintable(rings);
                }
                vv.repaint();
            }});
        JButton toggleOther = new JButton("Toggle Others");
    	toggleOther.setBackground(new Color(180,180,180));
        toggleOther.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
            	if(expanded) {
        			for(int edge : edgeList) {
        				graph.removeEdge(edge);
        			}
        			for(String v : otherNodes) {
        				graph.removeVertex(v);
        			}
        			vv.removeAll();
            		newPaint();
            		expanded = false;
            	}
            	else {
            		for(String s : otherNodes) {
            			if(!graph.containsVertex(s)) {
	            			graph.addVertex(s);
	            			int num = edgeFactory.create();
	            			edgeList.add(num);
	            			graph.addEdge(num,"other", s);
            			}
            			else {
            				
            			}
            		}
            		vv.removeAll();
            		newPaint();
            		expanded = true;
            	}
            }});
        
        //TODO: nullpointer exception bei collapstem node, wenn man toggled
        JButton collapse = new JButton("Collapse");
    	collapse.setBackground(new Color(180,180,180));
        collapse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	Collection<String> picked = new HashSet(vv.getPickedVertexState().getPicked());
                if(picked.size() == 1) {
                	Object root = picked.iterator().next();
                	Forest<String,Integer> inGraph = (Forest<String,Integer>)treeLayout.getGraph();
                    try {
						collapser.collapse(vv.getGraphLayout(), inGraph, root);
					} catch (InstantiationException e1) {
						e1.printStackTrace();
					} catch (IllegalAccessException e1) {
						e1.printStackTrace();
					}
                    vv.getPickedVertexState().clear();
                    vv.repaint();
                }
            }});
        
        JButton expand = new JButton("Expand");
    	expand.setBackground(new Color(180,180,180));
        expand.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
                Collection<String> picked = vv.getPickedVertexState().getPicked();
                for(Object v : picked) {
                    if(v instanceof Forest) {
                        Forest<String,Integer> inGraph = (Forest<String,Integer>) treeLayout.getGraph();
            			collapser.expand(inGraph, (Forest<String,Integer>) v);
                    }
                    vv.getPickedVertexState().clear();
                   vv.repaint();
                }
            }});
        

        final JToggleButton hyperView = new JToggleButton("Hyperbolic-View");
    	hyperView.setBackground(new Color(180,180,180));
        hyperView.addItemListener(new ItemListener(){
            public void itemStateChanged(ItemEvent e) {
            	if(!hyperbola) {
            		hyperbolicViewSupport.activate(e.getStateChange() == ItemEvent.SELECTED);
            		hyperbola = true;
            	}
            	else{
            		hyperbolicViewSupport.deactivate();
            		hyperbola = false;
            		final DefaultModalGraphMouse<String, Integer> graphMouse = new DefaultModalGraphMouse<String, Integer>();
                    vv.setGraphMouse(graphMouse);
                    graphMouse.setMode(ModalGraphMouse.Mode.PICKING);
            	}
            }
        });
        
        graphMouse.addItemListener(hyperbolicViewSupport.getGraphMouse().getModeListener());
 
        progressBar = new JProgressBar();
        progressBar.setMaximum(100);
        progressBar.setMinimum(0);
        progressBar.setStringPainted(true);
        
        instDialog = new JDialog();
        instDialog.getContentPane().add(new JLabel(instructions));
        JButton instruction = new JButton("Instructions");
    	instruction.setBackground(new Color(180,180,180));
        instruction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                instDialog.pack();
                instDialog.setVisible(true);
            }
        });
        
        JPanel controls = new JPanel(new GridLayout(2,2));
		controls.setBorder(BorderFactory.createLineBorder(Color.gray, 3));
        JPanel view = new JPanel();
        view.setBorder(BorderFactory.createTitledBorder("Instructions"));
        // Search URL
	        JPanel searchGrid = new JPanel();
	        searchGrid.setBorder(BorderFactory.createTitledBorder("Wikipedia-URL"));
	    	searchGrid.add(tf);
	    	searchGrid.add(button);
	        controls.add(searchGrid);
        // Instructions
        	view.add(instruction);
        	controls.add(view);
        // Scaling
	        JPanel scaleGrid = new JPanel();
	        scaleGrid.setBorder(BorderFactory.createTitledBorder("Zoom"));
        	scaleGrid.add(plus);
        	scaleGrid.add(minus);
            scaleGrid.add(radial);
        	scaleGrid.add(hyperView);
        	controls.add(scaleGrid);
        // Control Vertices
            JPanel controlVertices = new JPanel();
            controlVertices.setBorder(BorderFactory.createTitledBorder("Control Vertices"));
            //controlVertices.add(toggleOther);
        	controlVertices.add(collapse);
        	controlVertices.add(expand);
        	controlVertices.add(modeBox);
        	controls.add(controlVertices);
       	
        content.add(controls, BorderLayout.SOUTH);
        content.add(progressBar, BorderLayout.NORTH);
    }

    private void createTree(String url) {
		Parser parser = new Parser(url);
		Parser pars2 = null;
		Parser pars3 = null;
		ArrayList<Article> l = parser.getList();
		String title = parser.getTitle();
		graph.addVertex(title);
		int size = (l.size()>=10 ? 10 : l.size()); 
		for(int x=0; x < size; ++x) {
			pars2 = new Parser();
			pars2.setUrl(l.get(x).url);
			pars2.parse();
			String titel2 = l.get(x).titel;
			if(!graph.containsVertex(titel2)) {
				graph.addVertex(titel2);
				l.get(x).teaser = pars2.getTeaser();
				parsedGraph.add(l.get(x));
				graph.addEdge(edgeFactory.create(), title, titel2);
			}
			ArrayList<Article> l1 = pars2.getList();
			int size2 = (l1.size()>=5 ? 5 : l1.size()); 
			for(int z=0; z < size2; ++z) {
				pars3 = new Parser();
				pars3.setUrl(l.get(x).url);
				pars3.parse();
				String titel3 = l1.get(z).titel;
				if(!graph.containsVertex(titel3)) {
					graph.addVertex(titel3);
					l1.get(z).teaser = pars3.getTeaser();
					parsedGraph.add(l1.get(z));
					graph.addEdge(edgeFactory.create(), titel2, titel3);
				}
				pars3 = null;
			}
			pars2 = null;
			setProgress((x+1)*10);
		}
		//graph.addVertex("other");
		//graph.addEdge(edgeFactory.create(),title, "other");
		int sizeOther = (l.size()>10 ? l.size() : 10); 
		for(int i=10; i<sizeOther; ++i) {
			if(!graph.containsVertex(l.get(i).titel)) {
				otherNodes.add(l.get(i).titel);
//				graph.addVertex(l.get(i).titel);
//				graph.addEdge(edgeFactory.create(), "other", l.get(i).titel);
			}
			
		}
		
		//TODO: auto collapse
//		PickedState st
//		vv.setPickedVertexState();
    }

    private void parseChildrens(Article root) {
    	Parser parser = new Parser(root.url);
		ArrayList<Article> l = parser.getList();
		int size = (l.size()>=5 ? 5 : l.size()); 
		for(int x=0; x < size; ++x) {
			if(!graph.containsVertex(l.get(x).titel)) {
				graph.addVertex(l.get(x).titel);
				l.get(x).teaser = parser.getTeaser();
				parsedGraph.add(l.get(x));
				graph.addEdge(edgeFactory.create(), root.titel, l.get(x).titel);
			}
		}
		newPaint();
    }
    
    public void newPaint() {
		radialLayout = new RadialTreeLayout<String,Integer>(graph);
        radialLayout.setSize(new Dimension(windowSizeX,windowSizeY));
		treeLayout = new RadialTreeLayout<String,Integer>(graph);
        treeLayout.setSize(new Dimension(windowSizeX,windowSizeY));
        vv.setGraphLayout(radialLayout);
		vv.repaint();
    }
    
    private void setProgress(int value) {
		progressBar.setValue(value);
		java.awt.Rectangle progressRect = progressBar.getBounds();
		progressBar.paintImmediately(progressRect);
    }
    
    private boolean deleteTree() {
    	ArrayList<Integer> l = new ArrayList<Integer>();
    	ArrayList<String> l2 = new ArrayList<String>();
    	for(Integer e : graph.getEdges())
    		l.add(e);
    	for(int i : l)
    		graph.removeEdge(i);
    	for(String v : graph.getVertices())
    		l2.add(v);
    	for(String s : l2)
    		graph.removeVertex(s);
    	return true;
    }
    
    class Rings implements VisualizationServer.Paintable {
        
        Collection<Double> depths;
         
        public Rings() { depths = getDepths(); }
         
        private Collection<Double> getDepths() {
            Set<Double> depths = new HashSet<Double>();
            Map<String,PolarPoint> polarLocations = radialLayout.getPolarLocations();
            for(String v : graph.getVertices()) {
                PolarPoint pp = polarLocations.get(v);
                depths.add(pp.getRadius());
            }
            return depths;
        }
 
        public void paint(Graphics g) {
            g.setColor(Color.black);
            Graphics2D g2d = (Graphics2D)g;
            Point2D center = radialLayout.getCenter();
            Ellipse2D ellipse = new Ellipse2D.Double();
            for(double d : depths) {
                ellipse.setFrameFromDiagonal(center.getX()-d, center.getY()-d, center.getX()+d, center.getY()+d);
                Shape shape = vv.getRenderContext().getMultiLayerTransformer().transform(ellipse);
                g2d.draw(shape);
            }
        }
        
        public boolean useTransform() {  return true;  }
    }
    
    class ClusterVertexShapeFunction<V> extends EllipseVertexShapeTransformer<V>
    {
        ClusterVertexShapeFunction() {
            setSizeTransformer(new ClusterVertexSizeFunction<V>(20));
        }
		@Override
        public Shape transform(V v) {
            if(v instanceof Graph) {
                @SuppressWarnings("rawtypes")
				int size = ((Graph)v).getVertexCount();
                if (size < 8) {   
                    int sides = Math.max(size, 3);
                    return factory.getRegularPolygon(v, sides);
                }
                else {
                    return factory.getRegularStar(v, 8);
                }
            }
            return super.transform(v);
        }
    }
    class ClusterVertexSizeFunction<V> implements Transformer<V,Integer> {
    	int size;
        public ClusterVertexSizeFunction(Integer size) {
            this.size = size;
        }
        public Integer transform(V v) {
            if(v instanceof Graph) {
                return 30;
            }
            return size;
        }
    }
    
    public static void main(String[] args) {
        JFrame frame = new JFrame("GRAPHITI - Visualize Wikipedia Now");
        Container content = frame.getContentPane();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        content.add(new UI(frame));
        try {
			frame.setIconImage(ImageIO.read(new File("icon.png")));
		} catch (IOException e) {
			e.printStackTrace();
		}
        frame.pack();
        frame.setVisible(true);
    }
}