/***********************************************************************************
 * 
 * Copyright (c) 2015 Kamil Baczkowicz
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 
 *    Kamil Baczkowicz - initial API and implementation and/or initial documentation
 *    
 */

package pl.baczkowicz.mqttspy.ui.controls;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.Tooltip;

/**
 * A common interface for accessing text areas (standard and styled). 
 */
public interface TextAreaInterface
{
	void setEditable(boolean editable);
	
	void setWrapText(boolean wrapText);
	
	ObservableValue<String> selectedTextProperty();
	
	void setTooltip(Tooltip tooltip);
	
	void clear();
	
	void appendText(String text);
	
	void positionCaret(int position);
	
	String getSelectedText();

	String getText();
}