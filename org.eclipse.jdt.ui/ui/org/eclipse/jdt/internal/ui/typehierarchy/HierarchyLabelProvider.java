package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

public class HierarchyLabelProvider extends LabelProvider {

	private TypeHierarchyViewPart fViewPart;
	private JavaElementImageProvider fImageLabelProvider;

	/**
	 * Constructor for HierarchyLabelProvider.
	 * @param flags
	 */
	public HierarchyLabelProvider(TypeHierarchyViewPart viewPart) {
		super();
		fViewPart= viewPart;
		fImageLabelProvider= new JavaElementImageProvider();
	}

	/*
	 * @see ILabelProvider#getImage(Object)
	 */
	public Image getImage(Object obj) {
		if (obj instanceof IJavaElement) {
			IJavaElement element= (IJavaElement) obj;
			int flags= JavaElementImageProvider.OVERLAY_ICONS | JavaElementImageProvider.ERROR_TICKS;

			IJavaElement input= fViewPart.getInputElement();
			if (input != null && input.getElementType() != IJavaElement.TYPE && element.getElementType() == IJavaElement.TYPE) {
				IJavaElement parent= JavaModelUtil.findElementOfKind((IType) element, input.getElementType());
				if (!input.equals(parent)) {
					flags |= JavaElementImageProvider.LIGHT_TYPE_ICONS;
				}
			}
			return fImageLabelProvider.getImageLabel(element, flags); 
		}
		return super.getImage(obj);
	}
	
	/*
	 * @see ILabelProvider#getText(Object)
	 */
	public String getText(Object obj) {
		if (obj instanceof IJavaElement) {
			return JavaElementLabels.getElementLabel((IJavaElement) obj, JavaElementLabels.M_PARAMETER_TYPES);
		}
		return super.getText(obj);
	}

	/*
	 * @see IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		fImageLabelProvider.dispose();
		super.dispose();
	}

}

