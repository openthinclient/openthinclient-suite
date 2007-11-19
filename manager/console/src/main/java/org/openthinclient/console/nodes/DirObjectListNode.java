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
package org.openthinclient.console.nodes;

import java.awt.EventQueue;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;

import org.openide.ErrorManager;
import org.openide.actions.DeleteAction;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Node;
import org.openide.nodes.NodeEvent;
import org.openide.nodes.NodeListener;
import org.openide.nodes.NodeMemberEvent;
import org.openide.nodes.NodeReorderEvent;
import org.openide.util.Lookup;
import org.openide.util.WeakListeners;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openide.windows.TopComponent;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.AbstractDetailView;
import org.openthinclient.console.DetailViewProvider;
import org.openthinclient.console.MainTreeTopComponent;
import org.openthinclient.console.Messages;
import org.openthinclient.console.NewAction;
import org.openthinclient.console.RefreshAction;
import org.openthinclient.console.Refreshable;
import org.openthinclient.console.SysLogAction;
import org.openthinclient.console.util.GenericDirectoryObjectComparator;
import org.openthinclient.console.util.StringFilterTableModel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.levigo.util.swing.IconManager;
import com.levigo.util.swing.table.SunTableSorter;

/** Getting the feed node and wrapping it in a FilterNode */
public class DirObjectListNode extends MyAbstractNode
		implements
			DetailViewProvider,
			Refreshable {
	static class Children extends AbstractAsyncArrayChildren {
		protected Collection asyncInitChildren() {
			try {
				Realm realm = (Realm) getNode().getLookup().lookup(Realm.class);
				Class typeClass = (Class) getNode().getLookup().lookup(Class.class);

				List<? extends DirectoryObject> list = new ArrayList<DirectoryObject>(
						realm.getDirectory().list(typeClass));

				// sort the list
				Collections.sort(list, GenericDirectoryObjectComparator.getInstance());

				return list;
			} catch (Exception e) {
				ErrorManager.getDefault().notify(e);
				add(new Node[]{new ErrorNode(Messages
						.getString("DirObjectListNode.cantDisplay"), e)}); //$NON-NLS-1$

				return Collections.EMPTY_LIST;
			}
		}

		@Override
		protected Node[] createNodes(Object key) {
			return new Node[]{new DirObjectNode(getNode(), (DirectoryObject) key)};
		}
	}

	static class DetailView extends AbstractDetailView {
		private static DetailView detailView;

		public static DetailView getInstance() {
			if (null == detailView)
				detailView = new DetailView();
			return detailView;
		}

		private JTextField queryField;

		private JTable objectsTable;

		private MouseAdapter listener;

		private JComponent mainComponent;

		private StringFilterTableModel tableModel;

		private SunTableSorter sts;

		private boolean readonly;

		private Class objectClass;

		private static List<DirObjectListNode> plns = new ArrayList<DirObjectListNode>();

		/*
		 * @see org.openthinclient.console.AbstractDetailView#getHeaderComponent()
		 */
		@Override
		public JComponent getHeaderComponent() {
			// make sure that the main component has been initialized
			getMainComponent();

			DefaultFormBuilder dfb = new DefaultFormBuilder(new FormLayout(
					"p, 10dlu, r:p, 3dlu, f:p:g")); //$NON-NLS-1$
			dfb.setDefaultDialogBorder();
			dfb.setLeadingColumnOffset(2);
			dfb.setColumn(3);

			queryField = new JTextField();
			dfb.append(Messages.getString("DirObjectListNode.filter"), queryField); //$NON-NLS-1$
			dfb.nextLine();

			queryField.getDocument().addDocumentListener(new DocumentListener() {
				public void changedUpdate(DocumentEvent e) {
					tableModel.setFilter(queryField.getText());
				}

				public void removeUpdate(DocumentEvent e) {
					tableModel.setFilter(queryField.getText());
				}

				public void insertUpdate(DocumentEvent e) {
					tableModel.setFilter(queryField.getText());
				}
			});

			dfb.add(new JLabel(IconManager.getInstance(DetailViewProvider.class,
					"icons").getIcon("tree." + objectClass.getSimpleName())), //$NON-NLS-1$ //$NON-NLS-2$
					new CellConstraints(1, 1, 1, dfb.getRowCount(),
							CellConstraints.CENTER, CellConstraints.TOP));

			return dfb.getPanel();
		}

		/*
		 * @see org.openthinclient.console.DetailView#getRepresentation()
		 */
		public JComponent getMainComponent() {
			if (null == mainComponent) {
				objectsTable = new JTable();

				tableModel = new StringFilterTableModel();
				sts = new SunTableSorter(tableModel);
				sts.setTableHeader(objectsTable.getTableHeader());

				objectsTable.setModel(sts);

				mainComponent = new JScrollPane(objectsTable);
				mainComponent.setBackground(UIManager.getColor("TextField.background")); //$NON-NLS-1$
				mainComponent.setBorder(BorderFactory.createEmptyBorder());
			}
			return mainComponent;
		}

		/*
		 * @see org.openthinclient.console.DetailView#init(org.openide.nodes.Node[])
		 */
		public void init(Node[] selection, TopComponent tc) {

			// This is for the reload of the Query every time when the Node is
			// called... FT
			if (null != queryField)
				queryField.setText("");
			// find the realm node
			if (null != selection)
				for (Node node : selection) {
					if (node instanceof DirObjectListNode) {
						setDirObjectList((DirObjectListNode) node, tc);
						break;
					}
				}
		}

		private static class DirObjectTableModel extends AbstractTableModel
				implements
					NodeListener {
			private static final Pattern SPLIT_PATTERN = Pattern.compile("\\s*,\\s*"); //$NON-NLS-1$

			private final Class objectClass;

			private final Map<String, Method> getters = new HashMap<String, Method>();

			private final String columnNames[];

			private final String getterNames[];

			private final DirObjectListNode dol;

			public DirObjectTableModel(DirObjectListNode dol, Class objectClass) {
				this.dol = dol;
				this.objectClass = objectClass;

				String tableDesc = Messages.getString("table." //$NON-NLS-1$
						+ objectClass.getSimpleName());

				String splitDesc[] = SPLIT_PATTERN.split(tableDesc);

				if (splitDesc.length % 2 != 0)
					ErrorManager.getDefault().log(ErrorManager.WARNING,
							"Table description for " + objectClass + " is malformed"); //$NON-NLS-1$ //$NON-NLS-2$

				columnNames = new String[splitDesc.length / 2];
				getterNames = new String[splitDesc.length / 2];

				int i = 0, j = 0;
				while (i < splitDesc.length - 1) {
					columnNames[j] = splitDesc[i++];
					getterNames[j++] = splitDesc[i++];
				}

				// attach listener
				dol.addNodeListener((NodeListener) WeakListeners.create(
						NodeListener.class, this, dol));
			}

			/*
			 * @see javax.swing.table.TableModel#getValueAt(int, int)
			 */
			public Object getValueAt(int row, int column) {
				Node[] nodes = dol.getChildren().getNodes();
				if (nodes.length <= row)
					return ""; //$NON-NLS-1$

				DirectoryObject o = (DirectoryObject) nodes[row].getLookup().lookup(
						DirectoryObject.class);

				// HACK: lets me get the node without making it visible as a
				// row.
				if (column == -1)
					return nodes[row];

				if (null == o)
					return "..."; //$NON-NLS-1$

				String getterName = getterNames[column];

				Method getter = getters.get(getterName);
				if (null == getter) {
					try {
						getter = objectClass.getMethod(getterName, new Class[]{});
						getters.put(getterName, getter);
					} catch (Exception e) {
						ErrorManager.getDefault().notify(e);
						return "<" + e.getLocalizedMessage() + ">"; //$NON-NLS-1$ //$NON-NLS-2$
					}
				}

				try {
					return getter.invoke(o, new Object[]{});
				} catch (Exception e) {
					ErrorManager.getDefault().notify(e);
					return "<" + e.getLocalizedMessage() + ">"; //$NON-NLS-1$ //$NON-NLS-2$
				}
			}

			Node getNodeAtRow(int row) {
				Node[] nodes = dol.getChildren().getNodes();
				if (nodes.length <= row)
					return null;
				return nodes[row];
			}

			/*
			 * @see javax.swing.table.TableModel#getColumnCount()
			 */
			public int getColumnCount() {
				return columnNames.length;
			}

			/*
			 * @see javax.swing.table.TableModel#getColumnName(int)
			 */
			public String getColumnName(int columnIndex) {
				return columnNames[columnIndex];
			}

			/*
			 * @see javax.swing.table.TableModel#getRowCount()
			 */
			public int getRowCount() {
				return dol.getChildren().getNodes().length;
			}

			/*
			 * @see org.openide.nodes.NodeListener#childrenAdded(org.openide.nodes.NodeMemberEvent)
			 */
			public void childrenAdded(NodeMemberEvent ev) {
				propagateChangeOnEDT();
			}

			/*
			 * @see org.openide.nodes.NodeListener#childrenRemoved(org.openide.nodes.NodeMemberEvent)
			 */
			public void childrenRemoved(NodeMemberEvent ev) {
				propagateChangeOnEDT();
			}

			/*
			 * @see org.openide.nodes.NodeListener#childrenReordered(org.openide.nodes.NodeReorderEvent)
			 */
			public void childrenReordered(NodeReorderEvent ev) {
				propagateChangeOnEDT();
			}

			/*
			 * @see org.openide.nodes.NodeListener#nodeDestroyed(org.openide.nodes.NodeEvent)
			 */
			public void nodeDestroyed(NodeEvent ev) {
				propagateChangeOnEDT();
			}

			/*
			 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
			 */
			public void propertyChange(PropertyChangeEvent evt) {
				propagateChangeOnEDT();
			}

			/**
			 * 
			 */
			private void propagateChangeOnEDT() {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						fireTableDataChanged();
					}
				});
			}
		}

		/**
		 * @param dol
		 * @param tc
		 */
		private void setDirObjectList(final DirObjectListNode dol,
				final TopComponent tc) {
			getMainComponent();

			objectClass = (Class) dol.getLookup().lookup(Class.class);
			tableModel.setTableModel(new DirObjectTableModel(dol, objectClass));
			sts.setSortingStatus(0, SunTableSorter.ASCENDING);
			boolean isIn = false;
			for (DirObjectListNode ref : plns)
				if (ref.getName().equalsIgnoreCase(dol.getName()))
					isIn = true;
			if (!isIn) {
				if (plns.size() > 0) {
					objectsTable
							.removeMouseListener(objectsTable.getMouseListeners()[(objectsTable
									.getMouseListeners().length) - 1]);
					plns.remove(plns.size() - 1);
				}
				if (null != tc && tc instanceof ExplorerManager.Provider) {
					listener = new MouseAdapter() {
						@Override
						public void mouseClicked(MouseEvent e) {
							if (e.getClickCount() > 1) {
								int selectedRow = objectsTable.getSelectedRow();
								if (selectedRow < 0)
									return;
								final Node nodeAtRow = (Node) (objectsTable.getModel())
										.getValueAt(selectedRow, -1);

								// navigate explorer to node and, if it was a
								// double-click,
								// execute the default action
								if (null != nodeAtRow)
									try {
										((ExplorerManager.Provider) tc).getExplorerManager()
												.setSelectedNodes(new Node[]{nodeAtRow});

										if (e.getClickCount() > 1)
											SwingUtilities.invokeLater(new Runnable() {
												public void run() {
													nodeAtRow.getPreferredAction().actionPerformed(
															new ActionEvent(nodeAtRow, 1, "open")); //$NON-NLS-1$
												}
											});
									} catch (PropertyVetoException e1) {
										ErrorManager.getDefault().notify(e1);
									}
							}
						}
					};
					plns.add(dol);
					objectsTable.addMouseListener((MouseListener) WeakListeners.create(
							MouseListener.class, listener, objectsTable));
				}
			}
		}

		/*
		 * @see org.openthinclient.console.DetailView#setReadonly(boolean)
		 */
		public void setReadonly(boolean readonly) {
			this.readonly = readonly;
		}

		/*
		 * @see org.openthinclient.console.DetailView#isReadonly()
		 */
		public boolean isReadonly() {
			return readonly;
		}

		/*
		 * @see org.openthinclient.console.ObjectEditorPart#getTitle()
		 */
		public String getTitle() {
			return null;
		}
	}

	/**
	 * @param node
	 * @param keys
	 */
	public DirObjectListNode(Node node, Class key) {
		super(new Children(), new ProxyLookup(new Lookup[]{
				Lookups.fixed(new Object[]{key}), node.getLookup()}));
	}

	public Action[] getActions(boolean context) {
		if (getName().equalsIgnoreCase(Messages.getString("types.plural.Client")))
			return new Action[]{SystemAction.get(NewAction.class),
					SystemAction.get(RefreshAction.class),
					SystemAction.get(DeleteAction.class),
					SystemAction.get(SysLogAction.class)};
		else
			return new Action[]{SystemAction.get(NewAction.class),
					SystemAction.get(RefreshAction.class),
					SystemAction.get(DeleteAction.class)};

	}

	/*
	 * @see org.openide.nodes.FilterNode#canCopy()
	 */
	@Override
	public boolean canCopy() {
		return false;
	}

	/*
	 * @see org.openide.nodes.FilterNode#canDestroy()
	 */
	@Override
	public boolean canDestroy() {
		return false;
	}

	/*
	 * @see org.openide.nodes.FilterNode#canRename()
	 */
	@Override
	public boolean canRename() {
		return false;
	}

	// /*
	// * @see org.openthinclient.console.nodes.MyAbstractNode#getIcon(int)
	// */
	// @Override
	// public Image getIcon(int type) {
	// Class typeClass = (Class) getLookup().lookup(Class.class);
	// return IconManager.getInstance(DetailViewProvider.class,
	// "icons").getImage(
	// "tree.list." + typeClass.getSimpleName(),
	// IconManager.EFFECT_MORECONTRAST);
	// }

	/*
	 * @see org.openthinclient.console.nodes.MyAbstractNode#getIcon(int)
	 */
	@Override
	public Image getOpenedIcon(int type) {
		Class typeClass = (Class) getLookup().lookup(Class.class);
		return IconManager.getInstance(DetailViewProvider.class, "icons").getImage( //$NON-NLS-1$
				"tree.list." + typeClass.getSimpleName()); //$NON-NLS-1$
	}

	public String getName() {
		Class typeClass = (Class) getLookup().lookup(Class.class);
		return Messages.getString("types.plural." + typeClass.getSimpleName()); //$NON-NLS-1$
	}

	/*
	 * @see org.openthinclient.console.DetailViewProvider#getDetailView()
	 */
	public DetailView getDetailView() {
		return DetailView.getInstance();
	}

	/*
	 * @see org.openthinclient.console.Refreshable#refresh()
	 */
	public void refresh(String s) {
		MainTreeTopComponent.expandThisNode(this);
		((AbstractAsyncArrayChildren) getChildren()).refreshChildren();
	}

	public void refresh() {
		MainTreeTopComponent.expandThisNode(this);
		((AbstractAsyncArrayChildren) getChildren()).refreshChildren();

	}
}