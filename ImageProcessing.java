import ecs100.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.*;
import java.lang.Math.*;
import java.lang.reflect.Array;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Scanner;
import java.awt.image.BufferedImage;
public class ImageProcessing {
    private BufferedImage img = null;
    private int[][] workingArray;
    private int[][] finalArray;
    private int[][] kernelX;
    private int[][] kernelY;
    private int threshold1 = 230;
    private int threshold2 = 90;
    private ArrayList<Integer> xPos = new ArrayList<Integer>();
    private ArrayList<Integer> yPos = new ArrayList<Integer>();
    private ArrayList<Integer> xPosFinal = new ArrayList<Integer>();
    private ArrayList<Integer> yPosFinal = new ArrayList<Integer>();
    private ArrayList<Integer> penPos = new ArrayList<Integer>();
    private PrintWriter output;
    public ImageProcessing(){
        try{
            output = new PrintWriter("imagexy.txt");
        }
        catch(Exception e){}
        process();
    }
    /*
        Creates grey image from given colour image. Uses kernels to find differences in intensity and save to finalArray.
        Calculates edge points and saves xPos and yPos to arraylists.
     */
    public void process(){
        // Sobel Kernels
        int kernelX[][] = {{-1,0,1},
                {-2,0,2},
                {-1,0,1}};
        int kernelY[][] = {{-1,-2,-1},
                {0, 0, 0},
                {1, 2, 1}};
        workingArray = new int[640][480];
        finalArray = new int[640][480];
        //Loads colour image
        try {
            File image = new File("Capture.jpg");
            img = ImageIO.read(image);
        }
        catch(Exception e){
            UI.println(e);
        }
        //Convert to grey
        for(int i = 0; i < 640; i++){
            for(int j = 0; j < 480; j++){
                int rgb = img.getRGB(i,j);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb & 0xFF);
                int gray = (r + g + b) / 3;
                workingArray[i][j] = gray;
            }
        }
        //Apply kernel to find high contrast areas
        for(int x = 1; x < 639; x++){
            for(int y = 1; y < 479; y++){
                int pixelX = (kernelX[0][0] * workingArray[x-1][y-1]) + (kernelX[0][1] * workingArray[x][y-1]) + (kernelX[0][2] * workingArray[x+1][y-1]) +
                        (kernelX[1][0] * workingArray[x-1][y])   + (kernelX[1][1] * workingArray[x][y])   + (kernelX[1][2] * workingArray[x+1][y]) +
                        (kernelX[2][0] * workingArray[x-1][y+1]) + (kernelX[2][1] * workingArray[x][y+1]) + (kernelX[2][2] * workingArray[x+1][y+1]);

                int pixelY = (kernelY[0][0] * workingArray[x-1][y-1]) + (kernelY[0][1] * workingArray[x][y-1]) + (kernelY[0][2] * workingArray[x+1][y-1]) +
                        (kernelY[1][0] * workingArray[x-1][y])   + (kernelY[1][1] * workingArray[x][y])   + (kernelY[1][2] * workingArray[x+1][y]) +
                        (kernelY[2][0] * workingArray[x-1][y+1]) + (kernelY[2][1] * workingArray[x][y+1]) + (kernelY[2][2] * workingArray[x+1][y+1]);

                int value = (int)Math.sqrt(Math.pow(pixelX,2) + Math.pow(pixelY,2)); //Add vertical and horizontal edges
                finalArray[x][y] = value; // Contrast value saved to finalArray
            }
        }
        // Search for edges
        for(int x = 0; x < 638; x++){
            for(int y = 0; y < 478; y++){
                if(finalArray[x][y] > threshold1){
                    recursiveCellSearch(x,y);
                }
            }
        }
        // Display images on UI pane
        BufferedImage edges = new BufferedImage(640,480,BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = edges.createGraphics();
        graphics.setColor(Color.black);
        for(int i = 0; i < xPosFinal.size() -1; i++){
            graphics.drawRect(xPosFinal.get(i),yPosFinal.get(i),1,1);
        }
        UI.drawImage(edges,0,0);

        //Print xPos and yPos co ords
        for(int i = 0; i < xPosFinal.size()-1; i++){
            output.println(xPosFinal.get(i) + " " + yPosFinal.get(i) + " " + penPos.get(i) + " ");
            output.flush();
        }
    }

    /*
        Search surrounding cells of start of edge.
     */
    public void recursiveCellSearch(int x, int y){
        //Add point to arraylist
        xPos.add(x);
        yPos.add(y);
        //Remove value to avoid returning to this pixel
        finalArray[x][y] = 0;
        int nextCellX = -2;
        int nextCellY = -2;
        //Search constraints
        int max = 0;
        int minX = Math.max(x-1,0);
        int maxX = Math.min(x+1,637);
        int minY = Math.max(y-1,0);
        int maxY = Math.min(y+1,476);
        //Indexs of found cells
        int xInd = -2;
        int yInd = -2;
        //Search for maximum neighbour
        for(int x1 = minX; x1 < maxX +1; x1++){
            for(int y1 = minY; y1 < maxY +1; y1++){
                if(x1 == x && y1 == y){continue;}
                if(finalArray[x1][y1] > max){
                    max = finalArray[x1][y1];
                    xInd = x1;
                    yInd = y1;
                }
            }
        }
        if(max < threshold2){
            //Terminate edge
            if(xPos.size() < 20){ //If edge is small, delete it
                xPos.clear();
                yPos.clear();
            }
            else{ //Else save edge found
                int i = 0;
                for(int x1 : xPos){
                    if(i % 6 == 0) { //Reduce number of pixels per line
                        xPosFinal.add(x1);
                        penPos.add(0);
                    }
                    i++;
                }
                xPos.clear();
                i = 0;
                for(int y1 : yPos){
                    if(i % 6 == 0) { //Reduce number of pixels per line
                        yPosFinal.add(y1);
                    }
                    i++;
                }
                yPos.clear();
                penPos.remove(penPos.size()-1); //Remove last 0
                penPos.add(1); //Add 1
            }
        }
        else{
            // Search neighbours of highest found neighbour
            recursiveCellSearch(xInd,yInd);
        }
    }
}
