/*******************************************************************************
 * openthinclient.org ThinClient suite
 * 
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 * 
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *******************************************************************************/
package org.openthinclient.console.wizards.registerdirectory;

import java.awt.Component;
import java.util.NoSuchElementException;

import javax.swing.JComponent;
import javax.swing.event.ChangeListener;

import org.openide.WizardDescriptor;
import org.openthinclient.console.Messages;
import org.openthinclient.console.wizards.ConnectionSettingsWizardPanel;


public final class RegisterDirectoryWizardIterator
    implements
      WizardDescriptor.Iterator {
  private int index;

  private WizardDescriptor.Panel[] panels;

  /**
   * Initialize panels representing individual wizard's steps and sets various
   * properties for them influencing wizard appearance.
   */
  private WizardDescriptor.Panel[] getPanels() {
    if (panels == null) {
      panels = new WizardDescriptor.Panel[]{
          new ConnectionSettingsWizardPanel()};
      String[] steps = new String[panels.length];
      for (int i = 0; i < panels.length; i++) {
        Component c = panels[i].getComponent();
        // Default step name to component name of panel.
        steps[i] = c.getName();
        if (c instanceof JComponent) { // assume Swing components
          JComponent jc = (JComponent) c;
          // Sets step number of a component
          jc.putClientProperty("WizardPanel_contentSelectedIndex", new Integer( //$NON-NLS-1$
              i));
          // Sets steps names for a panel
          jc.putClientProperty("WizardPanel_contentData", steps); //$NON-NLS-1$
          // Turn on subtitle creation on each step
          jc.putClientProperty("WizardPanel_autoWizardStyle", Boolean.TRUE); //$NON-NLS-1$
          // Show steps on the left side with the image on the background
          jc.putClientProperty("WizardPanel_contentDisplayed", Boolean.TRUE); //$NON-NLS-1$
          // Turn on numbering of all steps
          jc.putClientProperty("WizardPanel_contentNumbered", Boolean.TRUE); //$NON-NLS-1$

          jc.putClientProperty("WizardPanel_errorMessage", Messages //$NON-NLS-1$
              .getString("RegisterRealm_baseDN_host_error")); //$NON-NLS-1$
        }
      }
    }
    return panels;
  }

  public WizardDescriptor.Panel current() {
    return getPanels()[index];
  }

  public String name() {
    return Messages.getString("Wizards.xofy", index + 1, getPanels().length); //$NON-NLS-1$
  }

  public boolean hasNext() {
    return index < getPanels().length - 1;
  }

  public boolean hasPrevious() {
    return index > 0;
  }

  public void nextPanel() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    index++;
  }

  public void previousPanel() {
    if (!hasPrevious()) {
      throw new NoSuchElementException();
    }
    index--;
  }

  // If nothing unusual changes in the middle of the wizard, simply:
  public void addChangeListener(ChangeListener l) {
  }

  public void removeChangeListener(ChangeListener l) {
  }

  // If something changes dynamically (besides moving between panels), e.g.
  // the number of panels changes in response to user input, then uncomment
  // the following and call when needed: fireChangeEvent();
  /*
   * private transient Set<ChangeListener> listeners = new HashSet<ChangeListener>(1);
   * public final void addChangeListener(ChangeListener l) { synchronized
   * (listeners) { listeners.add(l); } } public final void
   * removeChangeListener(ChangeListener l) { synchronized (listeners) {
   * listeners.remove(l); } } protected final void fireChangeEvent() { Iterator<ChangeListener>
   * it; synchronized (listeners) { it = new HashSet<ChangeListener>(listeners).iterator(); }
   * ChangeEvent ev = new ChangeEvent(this); while (it.hasNext()) {
   * it.next().stateChanged(ev); } } private void readObject(ObjectInputStream
   * in) throws IOException, ClassNotFoundException { in.defaultReadObject();
   * listeners = new HashSet<ChangeListener>(1); }
   */

}
