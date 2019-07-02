package TestClass;

import com.sun.deploy.panel.JavaPanel;

import javax.swing.*;
import java.awt.*;

/**
 * @program: simplePsForModelPartition
 * @description:
 * @author: SongZhen
 * @create: 2019-06-24 12:46
 */
class FrameThread implements Runnable{



    public void run(){
        JFrame f=new JFrame();
        JPanel jPanel=new JavaPanel(){
            @Override
            public void paint(Graphics graphics){
                super.paint(graphics);
                graphics.setColor(Color.GREEN);
                for(int i=0;i<10;i++){
                    graphics.drawLine(30,30,250,250);
                    System.out.println(i);
                }

            }
        };

        f.add(jPanel);
        f.setSize(300,300);
        f.setVisible(true);


    }
}