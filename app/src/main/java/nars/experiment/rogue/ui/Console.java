package nars.experiment.rogue.ui;

import nars.experiment.rogue.combat.PtrlConstants;

import java.awt.*;

public class Console
{
	public Console(Graphics g, Font f, int w, int h)
	{
		graph=g;
		font=f;
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		font_w=fm.charWidth('w');
		font_h=fm.getHeight();//+fm.getLeading();
		width=w;
		height=h;
		colors=new Color[]
		           {Color.BLACK,
					new Color(0, 0, 127),
					new Color(0, 127, 127),
					new Color(0, 127, 0),
					new Color(127, 0, 127),
					new Color(127, 0, 0),
					new Color(127, 64, 0),
					Color.GRAY,
					Color.DARK_GRAY,
					Color.BLUE,
					Color.CYAN,
					Color.GREEN,
					Color.MAGENTA,
					Color.RED,
					Color.YELLOW,
					Color.WHITE};
	}
	
	public Graphics getGraphics()
	{
		return graph;
	}

	public Font getFont()
	{
		return font;
	}

	/**
	 * Shows write colored character to console in the specified position.
	 * 
	 * @param x x coordinate.
	 * @param y y coordinate.
	 * @param c c char to display.
	 * @param col color of the char.
	 */
	public void showChar(int x, int y, char c, short col)
	{
		graph.setFont(font);
		graph.setColor(colors[col]);
		graph.drawChars(new char[]{c}, 0, 1, x*font_w, (y+1)*font_h);
	}
	
	public void printString(String s, int x, int y, short col)
	{
		for (int i=0; i<s.length(); i++)
		{
			//showBlackChar(x+i, y);
			showChar(x+i, y, s.charAt(i), col);	
		}
	}
	
	public void showRectChar(int x, int y, short col)
	{
		graph.setColor(colors[col]);
		graph.fillRect(x*font_w, y*(font_h)+2, font_w, font_h);

	}
	
	public void showBlackChar(int x, int y)
	{
		showRectChar(x,y,PtrlConstants.BLACK);
		//		graph.setColor(Color.BLACK);
//		graph.fillRect(x*font_w+1, y*font_h, font_w-1, font_h);
	}
	
	public void clear()
	{
		graph.setColor(Color.BLACK);
        graph.fillRect(0,0, width, height);
	}
	
	/**
	 * Returns console width in symbols.
	 * 
	 * @return console width in symbols.
	 */
	public int getSymWidth()
	{
		int rw;
		if (font_w!=0) {
            rw= width /font_w;
        }
		else  rw=0;
		return rw;
	}

	/**
	 * Returns console height in symbols.
	 * 
	 * @return console height in symbols.
	 */
	public int getSymHeight()
	{
		int rh;
		if (font_h!=0) {
            rh= height /font_h;
        }
		else  rh=0;
		return rh;
	}
	
	public int getHeight()
	{
		return height;
	}

	public int getWidth()
	{
		return width;
	}	
	
	private final Color[] colors;
	
	private final int font_w;
	private final int font_h;
	private final int width;
	private final int height;
	private final Graphics graph;
	private final Font font;

}
