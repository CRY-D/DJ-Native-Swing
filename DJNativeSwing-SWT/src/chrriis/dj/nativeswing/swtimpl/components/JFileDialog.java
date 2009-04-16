/*
 * Christopher Deckers (chrriis@nextencia.net)
 * http://www.nextencia.net
 *
 * See the file "readme.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package chrriis.dj.nativeswing.swtimpl.components;

import java.awt.Component;
import java.io.Serializable;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import chrriis.dj.nativeswing.swtimpl.ControlCommandMessage;
import chrriis.dj.nativeswing.swtimpl.components.ModalDialogUtils.NativeModalComponent;

/**
 * A native file selection dialog.
 * @author Christopher Deckers
 */
public class JFileDialog {

  private static class NativeFileDialogContainer extends NativeModalComponent {

    private static class CMN_openFileDialog extends ControlCommandMessage {
      @Override
      public Object run(Object[] args) {
        Data data = (Data)args[0];
        Control control = getControl();
        int style = 0;
        if(data.isSave) {
          style |= SWT.SAVE;
        }
        if(data.isMulti) {
          style |= SWT.MULTI;
        }
        FileDialog fileDialog = new FileDialog(control.getShell(), style);
        if(data.title != null) {
          fileDialog.setText(data.title);
        }
        if(data.parentDirectory != null) {
          fileDialog.setFilterPath(data.parentDirectory);
        }
        if(data.selectedFileNames != null && data.selectedFileNames.length == 1) {
          fileDialog.setFileName(data.selectedFileNames[0]);
        }
        if(data.extensionFilters != null) {
          fileDialog.setFilterExtensions(data.extensionFilters);
          fileDialog.setFilterNames(data.extensionFiltersNames);
          fileDialog.setFilterIndex(data.selectedExtensionFilterIndex);
        }
        fileDialog.open();
        data.selectedFileNames = fileDialog.getFileNames();
        data.selectedExtensionFilterIndex = fileDialog.getFilterIndex();
        data.parentDirectory = fileDialog.getFilterPath();
        if(data.parentDirectory.length() == 0) {
          data.parentDirectory = null;
        }
        return data;
      }
    }

    protected static Control createControl(Shell shell, Object[] parameters) {
      return new Composite(shell, SWT.NONE);
    }

    @Override
    protected Component createEmbeddableComponent() {
      return super.createEmbeddableComponent(new HashMap<Object, Object>());
    }

    public Data open(Data data) {
      return (Data)new CMN_openFileDialog().syncExec(this, data);
    }

  }

  private static class Data implements Serializable {
    public String title;
    public boolean isSave;
    public boolean isMulti;
    public String[] selectedFileNames;
    public String[] extensionFiltersNames;
    public String[] extensionFilters;
    public int selectedExtensionFilterIndex;
    public String parentDirectory;
  }

  private Data data = new Data();

  /**
   * Show the file selection dialog, which is a blocking call until the user has made a choice.
   * @param component The parent component.
   */
  public void show(Component component) {
    final NativeFileDialogContainer nativeComponent = new NativeFileDialogContainer();
    ModalDialogUtils.showModalDialog(component, nativeComponent, new Runnable() {
      public void run() {
        data = nativeComponent.open(data);
      }
    });
  }

  /**
   * Get the file name of the first file that was selected relative to the parent directory.
   * @return the file name or null if no file was selected.
   */
  public String getSelectedFileName() {
    String[] selectedFileNames = getSelectedFileNames();
    return selectedFileNames.length >= 1? selectedFileNames[0]: null;
  }

  /**
   * Get the file names of the files that was selected relative to the parent directory.
   * @return the file names or null if no file was selected.
   */
  public String[] getSelectedFileNames() {
    if(data.selectedFileNames == null) {
      return new String[0];
    }
    String[] selectedFileNames = new String[data.selectedFileNames.length];
    System.arraycopy(data.selectedFileNames, 0, selectedFileNames, 0, selectedFileNames.length);
    return selectedFileNames;
  }

  /**
   * Set the file name that the dialog will select by default.
   * @param selectedFileName the name of the file to select or null if no file is to be selected.
   */
  public void setSelectedFileName(String selectedFileName) {
    data.selectedFileNames = new String[] {selectedFileName};
  }

  /**
   * A representation of the selection modes that a file dialog supports.
   * @author Christopher Deckers
   */
  public static enum SelectionMode {
    SINGLE_SELECTION,
    MULTIPLE_SELECTION,
  }

  /**
   * Get the selection mode.
   * @return the selection mode.
   */
  public SelectionMode getSelectionMode() {
    return data.isMulti? SelectionMode.MULTIPLE_SELECTION: SelectionMode.SINGLE_SELECTION;
  }

  /**
   * Set the selection mode.
   * @param selectionMode the selection mode to set.
   */
  public void setSelectionMode(SelectionMode selectionMode) {
    data.isMulti = selectionMode == SelectionMode.MULTIPLE_SELECTION;
  }

  /**
   * The types that a file dialog supports.
   * @author Christopher Deckers
   */
  public static enum DialogType {
    OPEN_DIALOG_TYPE,
    SAVE_DIALOG_TYPE,
  }

  /**
   * Get the dialog type.
   * @return the dialog type.
   */
  public DialogType getDialogType() {
    return data.isSave? DialogType.SAVE_DIALOG_TYPE: DialogType.OPEN_DIALOG_TYPE;
  }

  /**
   * Set the dialog type.
   * @param dialogType the type of dialog.
   */
  public void setDialogType(DialogType dialogType) {
    data.isSave = dialogType == DialogType.SAVE_DIALOG_TYPE;
  }

  /**
   * Get the parent directory of the files that will be/were shown.
   * @return The parent directory or null if it is not set.
   */
  public String getParentDirectory() {
    return data.parentDirectory;
  }

  /**
   * Set the parent directory of the files that will be shown.
   * @param parentDirectory The parent directory or null to use the default.
   */
  public void setParentDirectory(String parentDirectory) {
    data.parentDirectory = parentDirectory;
  }

  /**
   * Set the extension filters, with optional descriptions, and set which index is selected by default.
   * To clear the list, set both arrays to null.
   * @param extensionFilters an array of extensions typically in the form "*.extension" or null. A filter with multiple extension uses semicolon as a separator, e.g. "*.jpg;*.png".
   * @param extensionFiltersNames the array of names corresponding to each extension filter, or null.
   * @param selectedExtensionFilterIndex the index that is selected by default.
   */
  public void setExtensionFilters(String[] extensionFilters, String[] extensionFiltersNames, int selectedExtensionFilterIndex) {
    if(extensionFiltersNames != null && (extensionFilters == null || extensionFilters.length != extensionFiltersNames.length)) {
      throw new IllegalArgumentException("Filter descriptions can only be defined when filter extensions are defined, and the two arrays must have the same size!");
    }
    data.extensionFilters = extensionFilters;
    data.extensionFiltersNames = extensionFiltersNames;
    data.selectedExtensionFilterIndex = selectedExtensionFilterIndex;
  }

  /**
   * Get the extension filters.
   * @return the extension filters, or null if not set.
   */
  public String[] getExtensionFilters() {
    return data.extensionFilters;
  }

  /**
   * Get the names corresponding to the extension filters.
   * @return the extension filters names, or null if not set.
   */
  public String[] getExtensionFiltersNames() {
    return data.extensionFiltersNames;
  }

  /**
   * Get the index of the selected extension filter.
   * @return the selected extension filter index.
   */
  public int getSelectedExtensionFilterIndex() {
    return data.selectedExtensionFilterIndex;
  }

  /**
   * Set the title of the file dialog.
   * @param title the title to set or null to use the default title.
   */
  public void setTitle(String title) {
    data.title = title;
  }

  /**
   * Get the title of the file dialog.
   * @return the title or null if it is not set.
   */
  public String getTitle() {
    return data.title;
  }

}