package com.aplos.common.utils;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.swing.JEditorPane;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import javax.swing.text.View;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.apache.log4j.Logger;

public class DocumentRenderer implements Printable {
	private static Logger logger = Logger.getLogger( DocumentRenderer.class );
	protected int currentPage = -1; // Used to keep track of when
	// the page to print changes.

	protected JEditorPane jeditorPane; // Container to hold the
	// Document. This object will
	// be used to lay out the
	// Document for printing.

	protected double pageEndY = 0; // Location of the current page
	// end.

	protected double pageStartY = 0; // Location of the current page
	// start.

	protected boolean scaleWidthToFit = true; // boolean to allow control over

	protected PageFormat pFormat;
	protected PrinterJob pJob;

	public DocumentRenderer() {
		pFormat = new PageFormat();
		pJob = PrinterJob.getPrinterJob();
	}

	public Document getDocument() {
		if (jeditorPane != null) {
			return jeditorPane.getDocument();
		} else {
			return null;
		}
	}

	public static void printHTML(String content) {
		logger.info("printing...");
		DocumentRenderer DocumentRenderer1 = new DocumentRenderer();

		HTMLDocument htmlDocument = new HTMLDocument();
		try{
			//htmlDocument.setParser(new HTMLEditorKit().createDefaultDocument());;
			HTMLEditorKit htmlEditorKit = new HTMLEditorKit();
			htmlDocument = (HTMLDocument)htmlEditorKit.createDefaultDocument( );
			htmlDocument.setOuterHTML(htmlDocument.getCharacterElement (htmlDocument.getLength()), content);
			DocumentRenderer1.print(htmlDocument);
		}
		catch (Exception e) {
			logger.info("oops: " + e.getMessage());
		}
	}

	public boolean getScaleWidthToFit() {
		return scaleWidthToFit;
	}

	public void pageDialog() {
		pFormat = pJob.pageDialog(pFormat);
	}

	@Override
	public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
		double scale = 1.0;
		Graphics2D graphics2D;
		View rootView;
		// I
		graphics2D = (Graphics2D) graphics;
		// II
		jeditorPane.setSize((int) pageFormat.getImageableWidth(),
				Integer.MAX_VALUE);
		jeditorPane.validate();
		// III
		rootView = jeditorPane.getUI().getRootView(jeditorPane);
		// IV
		if ((scaleWidthToFit)
				&& (jeditorPane.getMinimumSize().getWidth() > pageFormat
						.getImageableWidth())) {
			scale = pageFormat.getImageableWidth()
					/ jeditorPane.getMinimumSize().getWidth();
			graphics2D.scale(scale, scale);
		}
		// V
		graphics2D.setClip((int) (pageFormat.getImageableX() / scale),
				(int) (pageFormat.getImageableY() / scale), (int) (pageFormat
						.getImageableWidth() / scale), (int) (pageFormat
						.getImageableHeight() / scale));
		// VI
		if (pageIndex > currentPage) {
			currentPage = pageIndex;
			pageStartY += pageEndY;
			pageEndY = graphics2D.getClipBounds().getHeight();
		}
		// VII
		graphics2D.translate(graphics2D.getClipBounds().getX(), graphics2D
				.getClipBounds().getY());
		// VIII
		Rectangle allocation = new Rectangle(0, (int) -pageStartY,
				(int) (jeditorPane.getMinimumSize().getWidth()),
				(int) (jeditorPane.getPreferredSize().getHeight()));
		// X
		if (printView(graphics2D, allocation, rootView)) {
			return Printable.PAGE_EXISTS;
		} else {
			pageStartY = 0;
			pageEndY = 0;
			currentPage = -1;
			return Printable.NO_SUCH_PAGE;
		}
	}

	/*
	 * print(HTMLDocument) is called to set an HTMLDocument for printing.
	 */
	public void print(HTMLDocument htmlDocument) {
		setDocument(htmlDocument);
		printDialog();
	}

	public void print(JEditorPane jedPane) {
		setDocument(jedPane);
		printDialog();
	}

	public void print(PlainDocument plainDocument) {
		setDocument(plainDocument);
		printDialog();
	}

	protected void printDialog() {
		if (pJob.printDialog()) {
			pJob.setPrintable(this, pFormat);
			try {
				pJob.print();
			} catch (PrinterException printerException) {
				pageStartY = 0;
				pageEndY = 0;
				currentPage = -1;
				System.out.println("Error Printing Document");
			}
		}
	}

	protected boolean printView(Graphics2D graphics2D, Shape allocation,
			View view) {
		boolean pageExists = false;
		Rectangle clipRectangle = graphics2D.getClipBounds();
		Shape childAllocation;
		View childView;

		if (view.getViewCount() > 0) {
			for (int i = 0; i < view.getViewCount(); i++) {
				childAllocation = view.getChildAllocation(i, allocation);
				if (childAllocation != null) {
					childView = view.getView(i);
					if (printView(graphics2D, childAllocation, childView)) {
						pageExists = true;
					}
				}
			}
		} else {
			// I
			if (allocation.getBounds().getMaxY() >= clipRectangle.getY()) {
				pageExists = true;
				// II
				if ((allocation.getBounds().getHeight() > clipRectangle
						.getHeight())
						&& (allocation.intersects(clipRectangle))) {
					view.paint(graphics2D, allocation);
				} else {
					// III
					if (allocation.getBounds().getY() >= clipRectangle.getY()) {
						if (allocation.getBounds().getMaxY() <= clipRectangle
								.getMaxY()) {
							view.paint(graphics2D, allocation);
						} else {
							// IV
							if (allocation.getBounds().getY() < pageEndY) {
								pageEndY = allocation.getBounds().getY();
							}
						}
					}
				}
			}
		}
		return pageExists;
	}

	/*
	 * Method to set the content type the JEditorPane.
	 */
	protected void setContentType(String type) {
		jeditorPane.setContentType(type);
	}

	/*
	 * Method to set an HTMLDocument as the Document to print.
	 */
	public void setDocument(HTMLDocument htmlDocument) {
		jeditorPane = new JEditorPane();
		setDocument("text/html", htmlDocument);
	}

	public void setDocument(JEditorPane jedPane) {
		jeditorPane = new JEditorPane();
		setDocument(jedPane.getContentType(), jedPane.getDocument());
	}

	public void setDocument(PlainDocument plainDocument) {
		jeditorPane = new JEditorPane();
		setDocument("text/plain", plainDocument);
	}

	/*
	 * Method to set the content type and document of the JEditorPane.
	 */
	protected void setDocument(String type, Document document) {
		setContentType(type);
		jeditorPane.setDocument(document);
	}

	/*
	 * Method to set the current choice of the width scaling option.
	 */
	public void setScaleWidthToFit(boolean scaleWidth) {
		scaleWidthToFit = scaleWidth;
	}

}