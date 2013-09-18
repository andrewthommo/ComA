
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.*;

class CommentAssistantIcons {
    
    private int s = 64;
    Shape shape;
    Shape border;
    
    public BufferedImage getStrokedImage(int size, boolean fill) {
        double scale = s/size;
        double strokeWidth = 2*scale;
        double halfStroke = strokeWidth/2d;
        BufferedImage bi = new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        g.scale(1/scale, 1/scale);
        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        if (shape == null) {
            GeneralPath gp = new GeneralPath();
            double off = strokeWidth+1;
            gp.moveTo(0+off, s/2);
            gp.lineTo(4+off, s/2);
            gp.lineTo(12+off, 4*strokeWidth);
            gp.lineTo(28+off, s-(4*strokeWidth));
            gp.lineTo(36+off, s/2);
            gp.lineTo(64-strokeWidth-1, s/2);
            shape = gp;
            
            border = new RoundRectangle2D.Double(
                    halfStroke, 
                    halfStroke, 
                    s-(strokeWidth+1), s-(strokeWidth+1), 
                    12, 12);
        }
        Stroke s = new BasicStroke(
                (int)strokeWidth,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER);
        g.setStroke(s);
        
        Color bg = new Color(2,127,6);
        
        if (fill) {
            g.setColor(bg);
            g.fill(border);
        }
        
        g.setColor(new Color(8,253,16));
        g.draw(shape);

        g.setColor(bg.darker());
        g.draw(border);
        
        g.dispose();
        return bi;
    }

    public static void main(String[] args) {
        Runnable r = new Runnable() {

            @Override
            public void run() {
                JPanel gui = new JPanel(new GridLayout(0,3));
                
                CommentAssistantIcons cai = new CommentAssistantIcons();
                BufferedImage bi;
                
                bi = cai.getStrokedImage(64, false);
                try {
                    ImageIO.write(bi, "png", new File("coma64.png"));
                } catch (IOException ex) {
                    Logger.getLogger(CommentAssistantIcons.class.getName()).log(Level.SEVERE, null, ex);
                }
                gui.add(new JLabel(new ImageIcon(bi)));
                bi = cai.getStrokedImage(32, true);
                gui.add(new JLabel(new ImageIcon(bi)));
                bi= cai.getStrokedImage(16, false);
                gui.add(new JLabel(new ImageIcon(bi)));

                JOptionPane.showMessageDialog(null, gui);
            }
        };
        // Swing GUIs should be created and updated on the EDT
        // http://docs.oracle.com/javase/tutorial/uiswing/concurrency/initial.html
        SwingUtilities.invokeLater(r);
    }
}
