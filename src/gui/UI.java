package gui;

import wiki_parser.Article;
import wiki_parser.Parser;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.functors.ConstantTransformer;

import edu.uci.ics.jung.algorithms.layout.PolarPoint;
import edu.uci.ics.jung.algorithms.layout.RadialTreeLayout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.Tree;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.layout.LayoutTransition;
import edu.uci.ics.jung.visualization.util.Animator;
 
@SuppressWarnings("serial")
public class UI extends JApplet {

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
    String root;
    String startUrl = "https://de.wikipedia.org/wiki/Weimar";
    TreeLayout<String,Integer> treeLayout;
    RadialTreeLayout<String,Integer> radialLayout;
 
    public UI(JFrame frame) {
        graph = new DelegateForest<String,Integer>();
        
        createTree(startUrl);
        treeLayout = new TreeLayout<String,Integer>(graph);
        radialLayout = new RadialTreeLayout<String,Integer>(graph);
        radialLayout.setSize(new Dimension(400,400));
        vv =  new VisualizationViewer<String,Integer>(radialLayout, new Dimension(600,400));
        vv.setBackground(Color.white);
       
        vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line());
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
        // add a listener for ToolTips
        vv.setVertexToolTipTransformer(new ToStringLabeller());
        vv.getRenderContext().setArrowFillPaintTransformer(new ConstantTransformer(Color.lightGray));
        rings = new Rings();
 
        Container content = getContentPane();
        final GraphZoomScrollPane panel = new GraphZoomScrollPane(vv);
        content.add(panel);
         
        final DefaultModalGraphMouse graphMouse = new DefaultModalGraphMouse();
 
        vv.setGraphMouse(graphMouse);
         
        JComboBox modeBox = graphMouse.getModeComboBox();
        modeBox.addItemListener(graphMouse.getModeListener());
        graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);
 
        final ScalingControl scaler = new CrossoverScalingControl();
 
        JButton plus = new JButton("+");
        plus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scaler.scale(vv, 1.1f, vv.getCenter());
            }
        });
        JButton minus = new JButton("-");
        minus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scaler.scale(vv, 1/1.1f, vv.getCenter());
            }
        });
 
        JButton vor = new JButton("Vor");
        plus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //scaler.scale(vv, 1.1f, vv.getCenter());
            }
        });
        JButton zurueck = new JButton("Zurueck");
        minus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //scaler.scale(vv, 1/1.1f, vv.getCenter());
            }
        });
    	final JTextField tf  = new JTextField("", 20);
    	JButton button = new JButton("GO!");
    	
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	if(tf.getText() != null) {
            		startUrl = tf.getText().toString();
            		System.out.println(startUrl);
            		//createTree(startUrl);
                    content.add(new UI(frame));
                    frame.pack();
            	} else
            		System.out.println("Keine URL eingegeben.");
            }
        });
    	            
        JToggleButton radial = new JToggleButton("Radial");
        radial.addItemListener(new ItemListener() {
 
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    LayoutTransition<String,Integer> lt =
                        new LayoutTransition<String,Integer>(vv, treeLayout, radialLayout);
                    Animator animator = new Animator(lt);
                    animator.start();
                    vv.getRenderContext().getMultiLayerTransformer().setToIdentity();
                    vv.addPreRenderPaintable(rings);
                } else {
                    LayoutTransition<String,Integer> lt =
                        new LayoutTransition<String,Integer>(vv, radialLayout, treeLayout);
                    Animator animator = new Animator(lt);
                    animator.start();
                    vv.getRenderContext().getMultiLayerTransformer().setToIdentity();
                    vv.removePreRenderPaintable(rings);
                }
                vv.repaint();
            }});
 
        JPanel scaleGrid = new JPanel(new GridLayout(1,0));
        scaleGrid.setBorder(BorderFactory.createTitledBorder("Zoom"));
        JPanel historyGrid = new JPanel(new GridLayout(1,0));
        historyGrid.setBorder(BorderFactory.createTitledBorder("Verlauf"));
 
        JPanel controls = new JPanel();
        //scaleGrid.add(plus);
        //scaleGrid.add(minus);
        historyGrid.add(vor);
        historyGrid.add(zurueck);
        controls.add(tf);
        controls.add(button);
        //controls.add(radial);
        //controls.add(scaleGrid);
        controls.add(historyGrid);
        //controls.add(modeBox);
 
        content.add(controls, BorderLayout.SOUTH);
    }
     
    class Rings implements VisualizationServer.Paintable {
         
        Collection<Double> depths;
         
        public Rings() {
            depths = getDepths();
        }
         
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
            g.setColor(Color.lightGray);
            Graphics2D g2d = (Graphics2D)g;
            Point2D center = radialLayout.getCenter();
             Ellipse2D ellipse = new Ellipse2D.Double();
            for(double d : depths) {
                ellipse.setFrameFromDiagonal(center.getX()-d, center.getY()-d, center.getX()+d, center.getY()+d);
                Shape shape = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).transform(ellipse);
                g2d.draw(shape);
            }
        }
 
        public boolean useTransform() {
            return true;
        }
    }
    
    /*private void drawChildren(String url, int actualdepth) {
    	int depth = 4;
    	if(depth >= actualdepth) {
			Parser pars = new Parser();
			ArrayList<Pair> l = pars.getList(url);
			String title = pars.getTitle(url);
			graph.addVertex(title);
			for(int x=0; x < 5; ++x) {
				Pair t = l.get(x);
				graph.addVertex(t.titel);
				graph.addEdge(edgeFactory.create(), title, t.titel);
				drawChildren(t.url, ++actualdepth);
			}
    	}
    }*/
     
    private void createTree(String url) {
		Parser parser = new Parser(url);
		ArrayList<Article> l = parser.getList();
		String title = parser.getTitle();
		graph.addVertex(title);
		parser.printList();
		for(int x=0; x < 5; ++x) {
			Article t = l.get(x);
			//graph.addEdge(edgeFactory.create(), title, t.titel);
//			Parser pars2 = new Parser(t.url);
//			ArrayList<Article> l2 = pars2.getList();
//			String title2 = pars2.getTitle();
//			for(int y=0; y < 5; ++y) {
//				Article t2 = l2.get(y);
//				graph.addEdge(edgeFactory.create(), title2, t2.titel);
//				//Parser pars3 = new Parser(t2.url);
//				//ArrayList<Article> l3 = pars3.getList();
//				//String title3 = pars3.getTitle();
//				//for(int z=0; z < 5; ++z) {
//				//	Pair t3 = l.get(z);
//				//	graph.addEdge(edgeFactory.create(), title, t3.titel);
//				//}
//			}
		}
    }
    
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        Container content = frame.getContentPane();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        content.add(new UI(frame));
        frame.pack();
        frame.setVisible(true);
    }
}