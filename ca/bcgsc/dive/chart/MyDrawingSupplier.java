/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.bcgsc.dive.chart;

import java.awt.Color;
import org.jfree.chart.ChartColor;
import org.jfree.chart.plot.DefaultDrawingSupplier;

/**
 *
 * @author kmnip
 */
public final class MyDrawingSupplier extends DefaultDrawingSupplier{

    public final static Color[] PAINT_SEQ_FOR_WHITE_BG = new Color[] {

                    new Color(227, 26, 28), //(red)
                    new Color(31, 120, 180), //(dark blue)
                    new Color(51, 160, 44), //(dark green)                    
                    //new Color(255, 127, 0), //(dark orange)
                    new Color(106, 61, 154), //(dark purple)
                    new Color(251, 154, 153), //(pink)
                    //new Color(166, 206, 227), //(light blue)
                    new Color(178, 223, 138), //(light green)                
                    new Color(253, 191, 11), //(light orange)                    
                    new Color(202, 178, 214), //(light purple)                    
                    //new Color(255, 255, 153), //(yellow)

                    //new Color(8,81,156),
//                    new Color(0xFF, 0x55, 0x55),
//                    new Color(0x55, 0x55, 0xFF),
//                    new Color(0x55, 0xFF, 0x55),
//                    new Color(0xFF, 0x55, 0xFF),
//                    new Color(0x55, 0xFF, 0xFF),
                    Color.PINK,
                    Color.GRAY,
                    ChartColor.DARK_RED,
                    ChartColor.DARK_BLUE,
                    ChartColor.DARK_GREEN,
                    ChartColor.DARK_YELLOW,
                    ChartColor.DARK_MAGENTA,
                    ChartColor.DARK_CYAN,
                    Color.DARK_GRAY,
                    ChartColor.LIGHT_RED,
                    ChartColor.LIGHT_BLUE,
                    ChartColor.LIGHT_GREEN,
                    ChartColor.LIGHT_MAGENTA,
                    ChartColor.LIGHT_CYAN,
                    //Color.lightGray,
                    ChartColor.VERY_DARK_RED,
                    ChartColor.VERY_DARK_BLUE,
                    ChartColor.VERY_DARK_GREEN,
                    ChartColor.VERY_DARK_YELLOW,
                    ChartColor.VERY_DARK_MAGENTA,
                    ChartColor.VERY_DARK_CYAN,
                    Color.BLACK,
                    ChartColor.VERY_LIGHT_RED,
                    ChartColor.VERY_LIGHT_BLUE,
                    ChartColor.VERY_LIGHT_GREEN,
                    ChartColor.VERY_LIGHT_MAGENTA,
                    ChartColor.VERY_LIGHT_CYAN
    };

//    private final static Paint[] PAINT_SEQ_FOR_GREY_BG = new Paint[] {
//                    new Color(0xFF, 0x55, 0x55),
//                    new Color(0x55, 0x55, 0xFF),
//                    new Color(0x55, 0xFF, 0x55),
//                    new Color(0xFF, 0xFF, 0x55),
//                    new Color(0xFF, 0x55, 0xFF),
//                    new Color(0x55, 0xFF, 0xFF),
//                    Color.pink,
//                    //Color.gray,
//                    ChartColor.DARK_RED,
//                    ChartColor.DARK_BLUE,
//                    ChartColor.DARK_GREEN,
//                    ChartColor.DARK_YELLOW,
//                    ChartColor.DARK_MAGENTA,
//                    ChartColor.DARK_CYAN,
//                    Color.darkGray,
//                    ChartColor.LIGHT_RED,
//                    ChartColor.LIGHT_BLUE,
//                    ChartColor.LIGHT_GREEN,
//                    ChartColor.LIGHT_YELLOW,
//                    ChartColor.LIGHT_MAGENTA,
//                    ChartColor.LIGHT_CYAN,
//                    Color.lightGray,
//                    ChartColor.VERY_DARK_RED,
//                    ChartColor.VERY_DARK_BLUE,
//                    ChartColor.VERY_DARK_GREEN,
//                    ChartColor.VERY_DARK_YELLOW,
//                    ChartColor.VERY_DARK_MAGENTA,
//                    ChartColor.VERY_DARK_CYAN,
//                    ChartColor.VERY_LIGHT_RED,
//                    ChartColor.VERY_LIGHT_BLUE,
//                    ChartColor.VERY_LIGHT_GREEN,
//                    ChartColor.VERY_LIGHT_YELLOW,
//                    ChartColor.VERY_LIGHT_MAGENTA,
//                    ChartColor.VERY_LIGHT_CYAN
//    };


    public MyDrawingSupplier()
    {
        super(PAINT_SEQ_FOR_WHITE_BG,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE);
    }
}
