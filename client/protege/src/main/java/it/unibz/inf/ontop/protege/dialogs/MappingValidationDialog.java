package it.unibz.inf.ontop.protege.dialogs;

/*
 * #%L
 * ontop-protege
 * %%
 * Copyright (C) 2009 - 2013 KRDB Research Centre. Free University of Bozen Bolzano.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import it.unibz.inf.ontop.protege.utils.DialogUtils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

public class MappingValidationDialog extends JDialog {

	private static final long serialVersionUID = -3099215805478663834L;
	
	private final DefaultStyledDocument	doc;
	private int	index = 0;

	public final Style VALID;
	public final Style CRITICAL_ERROR;
	public final Style NORMAL;

	public boolean closed = false;

	public MappingValidationDialog() {
		DialogUtils.installEscapeCloseOperation(this);
		doc = new DefaultStyledDocument();

		StyleContext context = new StyleContext();
		VALID = context.getStyle(StyleContext.DEFAULT_STYLE);
		StyleConstants.setFontFamily(VALID, "Arial");
		StyleConstants.setFontSize(VALID, 12);
		StyleConstants.setForeground(VALID, Color.GREEN.darker());

		StyleContext context1 = new StyleContext();
		CRITICAL_ERROR = context1.getStyle(StyleContext.DEFAULT_STYLE);
		StyleConstants.setFontFamily(CRITICAL_ERROR, "Arial");
		StyleConstants.setFontSize(CRITICAL_ERROR, 12);
		StyleConstants.setForeground(CRITICAL_ERROR, Color.RED);

		StyleContext context3 = new StyleContext();
		NORMAL = context3.getStyle(StyleContext.DEFAULT_STYLE);
		StyleConstants.setFontFamily(NORMAL, "Arial");
		StyleConstants.setFontSize(NORMAL, 11);
		StyleConstants.setForeground(NORMAL, Color.BLACK);


		setTitle("Validate Mapping...");
		setSize(new Dimension(500, 360));
		Container panel = this.getContentPane();
		panel.setLayout(new BorderLayout());
		JTextPane area = new JTextPane();
		area.setBounds(0, 0, 298, 273);
		area.setEditable(false);
		area.setBackground(Color.WHITE);
		area.setDocument(doc);
		JScrollPane areaScrollPane = new JScrollPane(area);
		areaScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		areaScrollPane.setBounds(0, 0, 300, 275);

		JButton button = new JButton();
		button.setText("OK");
		button.setBounds(120, 290, 60, 25);
		button.addActionListener(a -> {
			closed = true;
			this.dispose();
		});
		button.requestFocus();

		panel.add(areaScrollPane, BorderLayout.CENTER);
		panel.add(button, BorderLayout.SOUTH);
		setResizable(true);
		setModal(true);
	}

	/***
	 * Adds the text synchronously.
	 * Do not call from the Event thread. Use a working thread.
	 *
	 * @param text
	 * @param style
	 */
	public void addText(String text, Style style) {
		try {
			SwingUtilities.invokeAndWait(() -> {
				try {
					doc.insertString(index, text, style);
					index += text.length();
					invalidate();
					repaint();
				}
				catch (BadLocationException e) {
					e.printStackTrace();
				}
			});
		}
		catch (InterruptedException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}
}
