/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.workingsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.text.Collator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetNewWizard;
import org.eclipse.ui.dialogs.SelectionDialog;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.internal.ui.viewsupport.BasicElementLabels;

public final class ConfigureWorkingSetAssignementAction extends SelectionDispatchAction {

	/**
	 * A set of GrayedCheckedModelElements
	 */
	private static final class GrayedCheckedModel {

		private ArrayList fElements;

		public GrayedCheckedModel(GrayedCheckedModelElement[] elements) {
			fElements= new ArrayList(Arrays.asList(elements));
		}

		public void addElement(GrayedCheckedModelElement element) {
			fElements.add(element);
		}

		public GrayedCheckedModelElement[] getElements() {
			return (GrayedCheckedModelElement[]) fElements.toArray(new GrayedCheckedModelElement[fElements.size()]);
		}

		public GrayedCheckedModelElement[] getChecked() {
			ArrayList result= new ArrayList();
			for (int i= 0; i < fElements.size(); i++) {
				if (((GrayedCheckedModelElement)fElements.get(i)).isChecked())
					result.add(fElements.get(i));
			}
			return (GrayedCheckedModelElement[])result.toArray(new GrayedCheckedModelElement[result.size()]);
		}

		public GrayedCheckedModelElement[] getGrayed() {
			ArrayList result= new ArrayList();
			for (int i= 0; i < fElements.size(); i++) {
				if (((GrayedCheckedModelElement)fElements.get(i)).isGrayed())
					result.add(fElements.get(i));
			}
			return (GrayedCheckedModelElement[])result.toArray(new GrayedCheckedModelElement[result.size()]);
		}

		public void selectAll() {
			for (int i= 0; i < fElements.size(); i++) {
				((GrayedCheckedModelElement) fElements.get(i)).select();
			}
		}

		public void deselectAll() {
			for (int i= 0; i < fElements.size(); i++) {
				((GrayedCheckedModelElement) fElements.get(i)).deselect();
			}
		}

	}

	/**
	 * Connects a IWorkingSet with its grayed-checked state
	 */
	private final static class GrayedCheckedModelElement {

		private final IWorkingSet fWorkingSet;
		private final int fElementCount;
		private int fCheckCount;

		public GrayedCheckedModelElement(IWorkingSet workingSet, int checkCount, int elementCount) {
			fWorkingSet= workingSet;
			fCheckCount= checkCount;
			fElementCount= elementCount;
		}

		public IWorkingSet getWorkingSet() {
			return fWorkingSet;
		}

		public int getCheckCount() {
			return fCheckCount;
		}

		public boolean isGrayed() {
			return isChecked() && fCheckCount < fElementCount;
		}

		public boolean isChecked() {
			return fCheckCount > 0;
		}

		public void deselect() {
			fCheckCount= 0;
		}

		public void select() {
			fCheckCount= fElementCount;
		}

		public int getElementCount() {
			return fElementCount;
		}

	}

	/**
	 * Content provider for a GrayedCheckedModel input
	 */
	private static final class GrayedCheckedModelContentProvider implements IStructuredContentProvider {
		private GrayedCheckedModelElement[] fElements;

		public Object[] getElements(Object element) {
			return fElements;
		}

		public void dispose() {}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput instanceof GrayedCheckedModel) {
				fElements= ((GrayedCheckedModel)newInput).getElements();
			} else {
				fElements= new GrayedCheckedModelElement[0];
			}
		}
	}

	/**
	 * Label provider for GrayedCheckedModelElements
	 */
	private class GrayedCheckedModelLabelProvider extends LabelProvider {

		private Map fIcons;

		public GrayedCheckedModelLabelProvider() {
			fIcons= new Hashtable();
		}

		public void dispose() {
			Iterator iterator= fIcons.values().iterator();
			while (iterator.hasNext()) {
				Image icon= (Image)iterator.next();
				icon.dispose();
			}
			super.dispose();
		}

		public Image getImage(Object object) {
			IWorkingSet workingSet= ((GrayedCheckedModelElement)object).getWorkingSet();
			ImageDescriptor imageDescriptor= workingSet.getImageDescriptor();
			if (imageDescriptor == null)
				return null;

			Image icon= (Image)fIcons.get(imageDescriptor);
			if (icon == null) {
				icon= imageDescriptor.createImage();
				fIcons.put(imageDescriptor, icon);
			}

			return icon;
		}

		public String getText(Object object) {
			GrayedCheckedModelElement modelElement= (GrayedCheckedModelElement)object;
			IWorkingSet workingSet= modelElement.getWorkingSet();
			if (!modelElement.isGrayed()) {
				return BasicElementLabels.getWorkingSetLabel(workingSet);
			} else {
				return Messages.format(WorkingSetMessages.ConfigureWorkingSetAssignementAction_XofY_label, new Object[] { BasicElementLabels.getWorkingSetLabel(workingSet), new Integer(modelElement.getCheckCount()), new Integer(modelElement.getElementCount())});
			}

		}

	}

	private final class WorkingSetModelAwareSelectionDialog extends SelectionDialog {

		/**
		 * Section ID for the WorkingSetModelAwareSelectionDialog class.
		 * 
		 * @since 3.5
		 */
		private static final String DIALOG_SETTINGS_SECTION= "WorkingSetModelAwareSelectionDialog"; //$NON-NLS-1$

		/**
		 * Key associated with the 'Show Only PE Visible Working Sets' check box.
		 * 
		 * @since 3.5
		 */
		private static final String SETTINGS_SHOW_VISIBLE_ONLY= "ShowVisibleOnly"; //$NON-NLS-1$


		private final class GrayedCheckModelElementSorter extends ViewerSorter {
			public int compare(Viewer viewer, Object e1, Object e2) {
				GrayedCheckedModelElement w1= (GrayedCheckedModelElement)e1;
				GrayedCheckedModelElement w2= (GrayedCheckedModelElement)e2;
				return Collator.getInstance().compare(w1.getWorkingSet().getLabel(), w2.getWorkingSet().getLabel());
			}
		}

		private class Filter extends ViewerFilter {

			public boolean select(Viewer viewer, Object parentElement, Object element) {
				GrayedCheckedModelElement model= (GrayedCheckedModelElement) element;
				IWorkingSet set= model.getWorkingSet();
				return accept(set);
			}

			private boolean accept(IWorkingSet set) {
				if (!isValidWorkingSet(set))
					return false;

				if (fWorkingSetModel == null)
					return true;

				if (fShowVisibleOnly && !fWorkingSetModel.isActiveWorkingSet(set))
					return false;

				return true;
			}
		}

		private CheckboxTableViewer fTableViewer;
		private boolean fShowVisibleOnly;
		private GrayedCheckedModel fModel;
		private final IAdaptable[] fElements;
		private final ArrayList fCreatedWorkingSets;

		/**
		 * @since 3.5
		 */
		private IDialogSettings fSettings;

		private WorkingSetModelAwareSelectionDialog(Shell shell, GrayedCheckedModel model, IAdaptable[] elements) {
			super(shell);

			setTitle(WorkingSetMessages.ConfigureWorkingSetAssignementAction_WorkingSetAssignments_title);
			setHelpAvailable(false);

			fModel= model;
			fElements= elements;
			fCreatedWorkingSets= new ArrayList();
			fSettings= JavaPlugin.getDefault().getDialogSettingsSection(DIALOG_SETTINGS_SECTION);
			if (fSettings.get(SETTINGS_SHOW_VISIBLE_ONLY) == null) {
				fSettings.put(SETTINGS_SHOW_VISIBLE_ONLY, true);
			}
			fShowVisibleOnly= fSettings.getBoolean(SETTINGS_SHOW_VISIBLE_ONLY);
		}

		public IWorkingSet[] getGrayed() {
			GrayedCheckedModelElement[] grayed= fModel.getGrayed();
			IWorkingSet[] result= new IWorkingSet[grayed.length];
			for (int i= 0; i < grayed.length; i++) {
				result[i]= grayed[i].getWorkingSet();
			}
			return result;
		}

		public IWorkingSet[] getSelection() {
			GrayedCheckedModelElement[] checked= fModel.getChecked();
			IWorkingSet[] result= new IWorkingSet[checked.length];
			for (int i= 0; i < checked.length; i++) {
				result[i]= checked[i].getWorkingSet();
			}
			return result;
		}

		protected final Control createDialogArea(Composite parent) {
			Composite composite= (Composite)super.createDialogArea(parent);

			createMessageArea(composite);
			Composite inner= new Composite(composite, SWT.NONE);
			inner.setLayoutData(new GridData(GridData.FILL_BOTH));
			GridLayout layout= new GridLayout();
			layout.numColumns= 2;
			layout.marginHeight= 0;
			layout.marginWidth= 0;
			inner.setLayout(layout);

			Composite tableComposite= new Composite(inner, SWT.NONE);
			tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			layout= new GridLayout();
			layout.marginHeight= 0;
			layout.marginWidth= 0;
			tableComposite.setLayout(layout);

			fTableViewer= createTableViewer(tableComposite);
			createShowVisibleOnly(tableComposite);

			createRightButtonBar(inner);

			Dialog.applyDialogFont(composite);
			return composite;
		}

		protected void createRightButtonBar(Composite parent) {
			Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
			GridLayout layout= new GridLayout();
			layout.marginHeight= 0;
			layout.marginWidth= 0;
			composite.setLayout(layout);

			Button selectAll= new Button(composite, SWT.PUSH);
			selectAll.setText(WorkingSetMessages.ConfigureWorkingSetAssignementAction_SelectAll_button);
			setButtonLayoutData(selectAll);
			selectAll.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					fTableViewer.setAllChecked(true);

					fModel.selectAll();
					fTableViewer.setGrayedElements(new Object[0]);
					fTableViewer.refresh();
				}
			});

			Button deselectAll= new Button(composite, SWT.PUSH);
			deselectAll.setText(WorkingSetMessages.ConfigureWorkingSetAssignementAction_DeselectAll_button);
			setButtonLayoutData(deselectAll);
			deselectAll.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					fTableViewer.setAllChecked(false);

					fModel.deselectAll();
					fTableViewer.setGrayedElements(new Object[0]);
					fTableViewer.refresh();
				}
			});

			new Label(composite, SWT.NONE);

			Button newWorkingSet= new Button(composite, SWT.PUSH);
			newWorkingSet.setText(WorkingSetMessages.ConfigureWorkingSetAssignementAction_New_button);
			setButtonLayoutData(newWorkingSet);
			newWorkingSet.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					IWorkingSetManager manager= PlatformUI.getWorkbench().getWorkingSetManager();

					//can only allow to create java working sets at the moment, see bug 186762
					IWorkingSetNewWizard wizard= manager.createWorkingSetNewWizard(VALID_WORKING_SET_IDS);
					if (wizard == null)
						return;

					WizardDialog dialog= new WizardDialog(getShell(), wizard);
					dialog.create();
					if (dialog.open() == Window.OK) {
						IWorkingSet workingSet= wizard.getSelection();
						if (isValidWorkingSet(workingSet)) {
							manager.addWorkingSet(workingSet);
							addNewWorkingSet(workingSet);
							fCreatedWorkingSets.add(workingSet);
						}
					}
				}
			});
		}

		protected CheckboxTableViewer createTableViewer(Composite parent) {

			final CheckboxTableViewer result= CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.MULTI);
			result.addCheckStateListener(new ICheckStateListener() {
				public void checkStateChanged(CheckStateChangedEvent event) {
					GrayedCheckedModelElement element= (GrayedCheckedModelElement)event.getElement();
					result.setGrayed(element, false);
					if (event.getChecked()) {
						element.select();
					} else {
						element.deselect();
					}
					result.update(element, null);
				}
			});
			GridData data= new GridData(GridData.FILL_BOTH);
			data.heightHint= convertHeightInCharsToPixels(20);
			data.widthHint= convertWidthInCharsToPixels(50);
			result.getTable().setLayoutData(data);

			result.addFilter(new Filter());
			result.setLabelProvider(new GrayedCheckedModelLabelProvider());
			result.setSorter(new GrayedCheckModelElementSorter());
			result.setContentProvider(new GrayedCheckedModelContentProvider());

			result.setInput(fModel);
			result.setCheckedElements(fModel.getChecked());
			result.setGrayedElements(fModel.getGrayed());

			return result;
		}

		protected void addNewWorkingSet(IWorkingSet workingSet) {
			int checkCount= 0;
			for (int i= 0; i < fElements.length; i++) {
				IAdaptable adapted= adapt(workingSet, fElements[i]);
				if (adapted != null && contains(workingSet, adapted)) {
					checkCount++;
				}
			}

			GrayedCheckedModelElement element= new GrayedCheckedModelElement(workingSet, checkCount, fElements.length);
			fModel.addElement(element);

			fTableViewer.setInput(fModel);
			fTableViewer.refresh();

			fTableViewer.setCheckedElements(fModel.getChecked());
			fTableViewer.setGrayedElements(fModel.getGrayed());

			fTableViewer.setSelection(new StructuredSelection(element));
		}

		private void createShowVisibleOnly(Composite parent) {
			if (fWorkingSetModel == null)
				return;

			final Button showVisibleOnly= new Button(parent, SWT.CHECK);
			showVisibleOnly.setText(WorkingSetMessages.ConfigureWorkingSetAssignementAction_OnlyShowVisible_check);
			showVisibleOnly.setSelection(fShowVisibleOnly);
			showVisibleOnly.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, true, true));
			showVisibleOnly.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					fShowVisibleOnly= showVisibleOnly.getSelection();

					fTableViewer.refresh();

					fTableViewer.setCheckedElements(fModel.getChecked());
					fTableViewer.setGrayedElements(fModel.getGrayed());
				}
			});

			Link ppwsLink= new Link(parent, SWT.NONE);
			ppwsLink.setText(WorkingSetMessages.ConfigureWorkingSetAssignementAction_OnlyShowVisible_link);
			ppwsLink.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, true, true));
			ppwsLink.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {

					List workingSets= new ArrayList(Arrays.asList(fWorkingSetModel.getAllWorkingSets()));
					IWorkingSet[] activeWorkingSets= fWorkingSetModel.getActiveWorkingSets();
					boolean isSortingEnabled= fWorkingSetModel.isSortingEnabled();
					WorkingSetConfigurationDialog dialog= new WorkingSetConfigurationDialog(
						getShell(),
						(IWorkingSet[])workingSets.toArray(new IWorkingSet[workingSets.size()]),
							activeWorkingSets, isSortingEnabled);
					dialog.setSelection(activeWorkingSets);
					if (dialog.open() == IDialogConstants.OK_ID) {
						isSortingEnabled= dialog.isSortingEnabled();
						IWorkingSet[] selection= dialog.getSelection();
						fWorkingSetModel.setActiveWorkingSets(selection, isSortingEnabled);
					}
					
					recalculateCheckedState(dialog.getNewlyAddedWorkingSets());
				}
			});
		}

		/*
		 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
		 * @since 3.5
		 */
		protected void buttonPressed(int buttonId) {
			if (IDialogConstants.OK_ID == buttonId)
				fSettings.put(SETTINGS_SHOW_VISIBLE_ONLY, fShowVisibleOnly);
			super.buttonPressed(buttonId);
		}

		private void recalculateCheckedState(List addedWorkingSets) {
			Set checkedWorkingSets= new HashSet();
			GrayedCheckedModelElement[] elements= fModel.getChecked();
			for (int i= 0; i < elements.length; i++)
				checkedWorkingSets.add(elements[i].getWorkingSet());

			if (addedWorkingSets != null)
				checkedWorkingSets.addAll(addedWorkingSets);

			fModel= createGrayedCheckedModel(fElements, getAllWorkingSets(), checkedWorkingSets);

			fTableViewer.setInput(fModel);
			fTableViewer.refresh();
			fTableViewer.setCheckedElements(fModel.getChecked());
			fTableViewer.setGrayedElements(fModel.getGrayed());
		}

		/**
		 * {@inheritDoc}
		 */
		protected void cancelPressed() {
			IWorkingSetManager manager= PlatformUI.getWorkbench().getWorkingSetManager();
			for (int i= 0; i < fCreatedWorkingSets.size(); i++) {
				manager.removeWorkingSet((IWorkingSet)fCreatedWorkingSets.get(i));
			}

			super.cancelPressed();
		}
	}

	private static final String[] VALID_WORKING_SET_IDS= new String[] {
			JavaWorkingSetUpdater.ID,
			"org.eclipse.ui.resourceWorkingSetPage" //$NON-NLS-1$
	};

	private WorkingSetModel fWorkingSetModel;
	private final IWorkbenchSite fSite;

	public ConfigureWorkingSetAssignementAction(IWorkbenchSite site) {
		super(site);
		fSite= site;
		setText(WorkingSetMessages.ConfigureWorkingSetAssignementAction_WorkingSets_actionLabel);
		setEnabled(false);
	}

	public void setWorkingSetModel(WorkingSetModel workingSetModel) {
		fWorkingSetModel= workingSetModel;
	}

	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(canEnable(selection));
	}

	private boolean canEnable(IStructuredSelection selection) {
		if (selection.isEmpty())
			return false;

		List list= selection.toList();
		for (Iterator iterator= list.iterator(); iterator.hasNext();) {
			Object object= iterator.next();
			if (!(object instanceof IResource) && !(object instanceof IJavaElement))
				return false;
		}

		return true;
	}

	private IAdaptable[] getSelectedElements(IStructuredSelection selection) {
		ArrayList result= new ArrayList();

		List list= selection.toList();
		for (Iterator iterator= list.iterator(); iterator.hasNext();) {
			Object object= iterator.next();
			if (object instanceof IResource || object instanceof IJavaElement) {
				result.add(object);
			}
		}

		return (IAdaptable[])result.toArray(new IAdaptable[result.size()]);
	}

	public void run(IStructuredSelection selection) {
		IAdaptable[] elements= getSelectedElements(selection);
		GrayedCheckedModel model= createGrayedCheckedModel(elements, getAllWorkingSets(), null);
		WorkingSetModelAwareSelectionDialog dialog= new WorkingSetModelAwareSelectionDialog(fSite.getShell(), model, elements);

		if (elements.length == 1) {
			IAdaptable element= elements[0];
			String elementName;
			if (element instanceof IResource) {
				elementName= BasicElementLabels.getResourceName((IResource) element);
			} else {
				elementName= JavaElementLabels.getElementLabel((IJavaElement)element, JavaElementLabels.ALL_DEFAULT);
			}
			dialog.setMessage(Messages.format(WorkingSetMessages.ConfigureWorkingSetAssignementAction_DialogMessage_specific, elementName));
		} else {
			dialog.setMessage(Messages.format(WorkingSetMessages.ConfigureWorkingSetAssignementAction_DialogMessage_multi, new Integer(elements.length)));
		}
		if (dialog.open() == Window.OK) {
			updateWorkingSets(dialog.getSelection(), dialog.getGrayed(), elements);
			selectAndReveal(elements);
		}
	}

	private static GrayedCheckedModel createGrayedCheckedModel(IAdaptable[] elements, IWorkingSet[] workingSets, Set checkedWorkingSets) {
		GrayedCheckedModelElement[] result= new GrayedCheckedModelElement[workingSets.length];

		for (int i= 0; i < workingSets.length; i++) {
			IWorkingSet workingSet= workingSets[i];
			
			int checkCount= 0;
			for (int j= 0; j < elements.length; j++) {
				if (checkedWorkingSets == null) {
					IAdaptable adapted= adapt(workingSet, elements[j]);
					if (adapted != null && contains(workingSet, adapted))
						checkCount++;
				} else {
					if (checkedWorkingSets.contains(workingSet))
						checkCount++;
				}
			}

			result[i]= new GrayedCheckedModelElement(workingSet, checkCount, elements.length);
		}

		return new GrayedCheckedModel(result);
	}

	private void updateWorkingSets(IWorkingSet[] newWorkingSets, IWorkingSet[] grayedWorkingSets, IAdaptable[] elements) {
		HashSet selectedSets= new HashSet(Arrays.asList(newWorkingSets));
		HashSet grayedSets= new HashSet(Arrays.asList(grayedWorkingSets));
		IWorkingSet[] workingSets= getAllWorkingSets();

		for (int i= 0; i < workingSets.length; i++) {
			IWorkingSet workingSet= workingSets[i];
			if (isValidWorkingSet(workingSet) && !selectedSets.contains(workingSet) && !grayedSets.contains(workingSet)) {
				for (int j= 0; j < elements.length; j++) {
					IAdaptable adapted= adapt(workingSet, elements[j]);
					if (adapted != null && contains(workingSet, adapted)) {
						remove(workingSet, adapted);
					}
				}
			}
		}

		for (int i= 0; i < newWorkingSets.length; i++) {
			IWorkingSet set= newWorkingSets[i];
			if (isValidWorkingSet(set) && !grayedSets.contains(set)) {
				boolean checkForYetHiddenWorkingSet= false;
				for (int j= 0; j < elements.length; j++) {
					IAdaptable adapted= adapt(set, elements[j]);
					if (adapted != null && !contains(set, adapted)) {
						add(set, adapted);
						checkForYetHiddenWorkingSet= true;
					}
				}
				if (checkForYetHiddenWorkingSet) {
					IWorkingSet[] activeSets= getActiveWorkingSets();
					if (activeSets != null) {
						List activeWorkingSets= new ArrayList(Arrays.asList(activeSets));
						if (!activeWorkingSets.contains(set))
							activateWorkingSet(set);
					}
				}
			}
		}
	}

	/**
	 * Adds the given working set to the set of currently active working sets.
	 * 
	 * @param workingSet working set to be activated
	 * @since 3.5
	 */
	private void activateWorkingSet(IWorkingSet workingSet) {
		if (fWorkingSetModel != null) {
			fWorkingSetModel.addActiveWorkingSet(workingSet);
		} else {
			PackageExplorerPart activePart= getActivePackageExplorer();
			if (activePart != null) {
				activePart.getWorkingSetModel().addActiveWorkingSet(workingSet);
			}
		}

	}

	/**
	 * Returns an array of currently active WorkingSets.
	 * 
	 * @return array of active working sets or <code>null</code> if none
	 * @since 3.5
	 */
	private IWorkingSet[] getActiveWorkingSets() {
		if (fWorkingSetModel != null) {
			return fWorkingSetModel.getActiveWorkingSets();
		} else {
			WorkingSetModel model= null;
			PackageExplorerPart activePart= getActivePackageExplorer();
			if (activePart != null)
				model= activePart.getWorkingSetModel();
			return model == null ? null : model.getActiveWorkingSets();
		}
	}

	private IWorkingSet[] getAllWorkingSets() {
		if (fWorkingSetModel != null) {
			return fWorkingSetModel.getAllWorkingSets();
		} else {
			return PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSets();
		}
	}

	private static boolean isValidWorkingSet(IWorkingSet set) {
		if (set.isAggregateWorkingSet() || !set.isSelfUpdating())
			return false;

		if (!set.isVisible())
			return false;

		if (!set.isEditable())
			return false;

		for (int i= 0; i < VALID_WORKING_SET_IDS.length; i++) {
			if (VALID_WORKING_SET_IDS[i].equals(set.getId()))
				return true;
		}

		return false;
	}

	private static IAdaptable adapt(IWorkingSet set, IAdaptable element) {
		IAdaptable[] adaptedElements= set.adaptElements(new IAdaptable[] {
			element
		});
		if (adaptedElements.length != 1)
			return null;

		return adaptedElements[0];
	}

	private static boolean contains(IWorkingSet set, IAdaptable adaptedElement) {
		IAdaptable[] elements= set.getElements();
		for (int i= 0; i < elements.length; i++) {
			if (elements[i].equals(adaptedElement))
				return true;
		}

		return false;
	}

	private static void remove(IWorkingSet workingSet, IAdaptable adaptedElement) {
		HashSet set= new HashSet(Arrays.asList(workingSet.getElements()));
		set.remove(adaptedElement);
		workingSet.setElements((IAdaptable[])set.toArray(new IAdaptable[set.size()]));
	}

	private static void add(IWorkingSet workingSet, IAdaptable adaptedElement) {
		IAdaptable[] elements= workingSet.getElements();
		IAdaptable[] newElements= new IAdaptable[elements.length + 1];
		System.arraycopy(elements, 0, newElements, 0, elements.length);
		newElements[elements.length]= adaptedElement;
		workingSet.setElements(newElements);
	}

	private void selectAndReveal(IAdaptable[] elements) {
		PackageExplorerPart explorer= getActivePackageExplorer();
		if (explorer != null)
			explorer.selectReveal(new StructuredSelection(elements));
	}

	private PackageExplorerPart getActivePackageExplorer() {
		IWorkbenchPage page= JavaPlugin.getActivePage();
		if (page != null) {
			IWorkbenchPart activePart= page.getActivePart();
			if (activePart instanceof PackageExplorerPart) {
				return (PackageExplorerPart) activePart;
			}
		}
		return null;
	}

}