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
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;

import edu.uci.ics.jung.algorithms.layout.Layout;
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
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.EllipseVertexShapeTransformer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.layout.LayoutTransition;
import edu.uci.ics.jung.visualization.picking.PickedState;
import edu.uci.ics.jung.visualization.subLayout.TreeCollapser;
import edu.uci.ics.jung.visualization.transform.shape.GraphicsDecorator;
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
    String startUrl = null;
    TreeLayout<String,Integer> treeLayout;
    RadialTreeLayout<String,Integer> radialLayout;
    JProgressBar progressBar;
    
    ArrayList<String> otherNodes = new ArrayList<String>();
    ArrayList<Integer> edgeList = new ArrayList<Integer>();
    boolean expanded = false;
 
    public UI(JFrame frame) {
        graph = new DelegateForest<String,Integer>();
        progressBar = new JProgressBar();

        treeLayout = new TreeLayout<String,Integer>(graph);
        radialLayout = new RadialTreeLayout<String,Integer>(graph);
        final TreeCollapser collapser = new TreeCollapser();
        final Toolkit tk = Toolkit.getDefaultToolkit();  
        int xSize = ((int) tk.getScreenSize().getWidth());  
        int ySize = ((int) tk.getScreenSize().getHeight());  
        radialLayout.setSize(new Dimension(xSize-500,ySize-500));
        vv =  new VisualizationViewer<String,Integer>(radialLayout, new Dimension(xSize-200,ySize-200));
        vv.setBackground(Color.lightGray);
        vv.setAutoscrolls(true);
        Border border = BorderFactory.createLineBorder(Color.black);
        border = BorderFactory.createLoweredBevelBorder();
        vv.setBorder(border);
        
        Transformer<String, String> transformer = new Transformer<String, String>() {
            @Override public String transform(String arg0) { return arg0; }
          };
          vv.getRenderContext().setVertexLabelTransformer(transformer);
       
        vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line<String, Integer>());
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<String>());
        vv.setVertexToolTipTransformer(new ToStringLabeller<String>());
        vv.getRenderContext().setArrowFillPaintTransformer(new ConstantTransformer(Color.lightGray));
        rings = new Rings();

        Container content = getContentPane();
        final GraphZoomScrollPane panel = new GraphZoomScrollPane(vv);
        content.add(panel);
         
        final DefaultModalGraphMouse<String, Integer> graphMouse = new DefaultModalGraphMouse<String, Integer>();
 
        vv.setGraphMouse(graphMouse);
         
        JComboBox<String> modeBox = graphMouse.getModeComboBox();
        modeBox.addItemListener(graphMouse.getModeListener());
        graphMouse.setMode(ModalGraphMouse.Mode.PICKING);
 
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
    	final JTextField tf  = new JTextField("", 30);
    	
    	JButton button = new JButton("URL visualisieren");
    	
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	// TODO: Brauchen URL-Checker (Ist es ein Wikipedia-link?)
            	if(!tf.getText().equals("")) {
            		startUrl = tf.getText().toString();
            		deleteTree();
            		setProgress(1);
            		vv.removeAll();
            		createTree(startUrl);
            		radialLayout = new RadialTreeLayout<String,Integer>(graph);
                    int ySize = ((int) tk.getScreenSize().getHeight());  
                    radialLayout.setSize(new Dimension(ySize-50,ySize-100));
                    vv.setGraphLayout(radialLayout);
            		vv.repaint();
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
        JButton collapse = new JButton("Collapse");
        collapse.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
            	if(expanded) {
            		for(String s : otherNodes) {
            			for(int edge : edgeList) {
            				graph.removeEdge(edge);
            			}
            			for(String v : otherNodes) {
            				graph.removeVertex(v);
            			}
            			vv.removeAll();
                		radialLayout = new RadialTreeLayout<String,Integer>(graph);
                        int ySize = ((int) tk.getScreenSize().getHeight());  
                        radialLayout.setSize(new Dimension(ySize-50,ySize-100));
                        vv.setGraphLayout(radialLayout);
                		vv.repaint();
            		}
            		expanded = false;
            	}
//                Collection picked =new HashSet(vv.getPickedVertexState().getPicked());
//                if(picked.size() == 1) {
//                	Object root = picked.iterator().next();
//                    Forest inGraph = (Forest)treeLayout.getGraph();
//                    try {
//						collapser.collapse(vv.getGraphLayout(), inGraph, root);
//					} catch (InstantiationException e1) {
//						e1.printStackTrace();
//					} catch (IllegalAccessException e1) {
//						e1.printStackTrace();
//					}
//                    vv.getPickedVertexState().clear();
//                    vv.repaint();
//                }
            }});
        JButton expand = new JButton("Expand");
        expand.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
            	if(!expanded) {
            		for(String s : otherNodes) {
            			graph.addVertex(s);
            			int num = edgeFactory.create();
            			edgeList.add(num);
            			graph.addEdge(num,"other", s);
            		}
            		vv.removeAll();
            		radialLayout = new RadialTreeLayout<String,Integer>(graph);
                    int ySize = ((int) tk.getScreenSize().getHeight());  
                    radialLayout.setSize(new Dimension(ySize-50,ySize-100));
                    vv.setGraphLayout(radialLayout);
            		vv.repaint();
            		expanded = true;
            	}
            	
            	
                /*Collection picked = vv.getPickedVertexState().getPicked();
                for(Object v : picked) {
                    if(v instanceof Forest) {
                        Forest inGraph = (Forest)treeLayout.getGraph();
            			collapser.expand(inGraph, (Forest)v);
                    }
                    vv.getPickedVertexState().clear();
                   vv.repaint();
                }*/
            }});
 
        JPanel scaleGrid = new JPanel(new GridLayout(1,0));
        scaleGrid.setBorder(BorderFactory.createTitledBorder("Zoom"));
        JPanel historyGrid = new JPanel(new GridLayout(1,0));
        historyGrid.setBorder(BorderFactory.createTitledBorder("Verlauf"));
        JPanel searchGrid = new JPanel(new GridLayout(1,1));
        searchGrid.setBorder(BorderFactory.createTitledBorder("Wikipedia-URL"));

        progressBar.setMaximum(100);
        progressBar.setMinimum(0);
        progressBar.setStringPainted(true);
 
        JPanel controls = new JPanel();
        //scaleGrid.add(plus);
        //scaleGrid.add(minus);
        historyGrid.add(vor);
        historyGrid.add(zurueck);
        searchGrid.add(tf);
        searchGrid.add(button);
        controls.add(searchGrid);
        //controls.add(radial);
        //controls.add(scaleGrid);
        controls.add(historyGrid);
        //controls.add(modeBox);
        controls.add(collapse);
        controls.add(expand);
        content.add(controls, BorderLayout.SOUTH);
        content.add(progressBar, BorderLayout.NORTH);
        
        
        
        
        
    }
    
    public void paintVertex(RenderContext<String, String> rc, Layout<String, String> layout, String vertex) {
          GraphicsDecorator graphicsContext = rc.getGraphicsContext();
          Point2D center = layout.transform(vertex);
          Shape shape = null;
          Color color = null;
          if(vertex.equals("Square")) {
            shape = new Rectangle((int)center.getX()-10, (int)center.getY()-10, 20, 20);
            color = new Color(127, 127, 0);
          } else if(vertex.equals("Rectangle")) {
            shape = new Rectangle((int)center.getX()-10, (int)center.getY()-20, 20, 40);
            color = new Color(127, 0, 127);
          } else if(vertex.equals("Weimar")) {
            shape = new Ellipse2D.Double(center.getX()-10, center.getY()-10, 20, 20);
            color = new Color(0, 20, 255);
          }
          graphicsContext.setPaint(color);
          graphicsContext.fill(shape);
        }
 
    private void createTree(String url) {
		Parser parser = new Parser(url);
		Parser pars2 = null;
		Parser pars3 = null;
		ArrayList<Article> l = parser.getList();
		String title = parser.getTitle();
		graph.addVertex(title);
		for(int x=0; x < 10; ++x) {
			pars2 = new Parser();
			if(!graph.containsVertex(l.get(x).titel)) {
				graph.addVertex(l.get(x).titel);
			}
			graph.addEdge(edgeFactory.create(), title, l.get(x).titel);
			pars2.setUrl(l.get(x).url);
			pars2.parse();
			ArrayList<Article> l1 = pars2.getList();
			for(int z=0; z < 4; ++z) {
				progressBar.setValue(x*z);
				pars3 = new Parser();
				pars3.setUrl(l.get(x).url);
				pars3.parse();
				if(!graph.containsVertex(l1.get(z).titel)) {
					graph.addVertex(l1.get(z).titel);
					graph.addEdge(edgeFactory.create(), l.get(x).titel, l1.get(z).titel);
				}
				pars3 = null;
			}
			pars2 = null;
			setProgress((x+1)*10);
		}
		graph.addVertex("other");
		graph.addEdge(edgeFactory.create(),title, "other");
		for(int i=10; i<l.size(); ++i) {
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
    
    public static void main(String[] args) {
        JFrame frame = new JFrame("Graphiti - Visualize Wikipedia");
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
 
        public boolean useTransform() {  return true;  }
    }
    class ClusterVertexShapeFunction<V> extends EllipseVertexShapeTransformer<V>
    {

            ClusterVertexShapeFunction() {
                setSizeTransformer(new ClusterVertexSizeFunction<V>(20));
            }
            @SuppressWarnings("unchecked")
    		@Override
            public Shape transform(V v) {
                if(v instanceof Graph) {
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
}