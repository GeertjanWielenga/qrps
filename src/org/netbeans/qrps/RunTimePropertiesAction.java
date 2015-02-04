package org.netbeans.qrps;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.support.ant.AntProjectHelper;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.util.EditableProperties;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;
import org.openide.util.actions.Presenter;

@ActionID(
        category = "Build",
        id = "org.netbeans.qrps.RunTimePropertiesAction"
)
@ActionRegistration(
        lazy = false,
        displayName = "#CTL_RunTimePropertiesAction"
)
@ActionReference(path = "Toolbars/Build", position = 0)
@Messages("CTL_RunTimePropertiesAction=Runtime Properties")
public final class RunTimePropertiesAction extends AbstractAction implements Presenter.Toolbar, LookupListener {

    private JComboBox comboBox;
    private DefaultComboBoxModel model;
    private Lookup.Result<FileObject> result;
    private FileObject fo;

    public RunTimePropertiesAction() {
        comboBox = new JComboBox();
        comboBox.setModel(model = new DefaultComboBoxModel());
        comboBox.setPreferredSize(new Dimension(200, 10));
        comboBox.setEditable(true);
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fo != null) {
                    Project p = FileOwnerQuery.getOwner(fo);
                    FileObject projectprop = p.getProjectDirectory().getFileObject(AntProjectHelper.PRIVATE_PROPERTIES_PATH);
                    EditableProperties ep;
                    try {
                        ep = loadProperties(projectprop);
                        if (ep.containsKey("application.args")) {
                            ep.setProperty("application.args", comboBox.getEditor().getItem().toString());
                            storeProperties(projectprop, ep);
                        } else {
                            ep.put("application.args", comboBox.getEditor().getItem().toString());
                            storeProperties(projectprop, ep);
                        }
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                    Object toAdd = comboBox.getEditor().getItem();
                    if (model.getIndexOf(toAdd) == -1) {
                        model.addElement(toAdd);
                    }
                } else {
                    StatusDisplayer.getDefault().setStatusText("Open the project for which you want to set runtime properties.");
                }
            }
        });
        result = Utilities.actionsGlobalContext().lookupResult(FileObject.class);
        result.addLookupListener(WeakListeners.create(LookupListener.class, this, result));
    }

    @Override
    public Component getToolbarPresenter() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(new JLabel("Runtime Properties: "));
        panel.add(comboBox);
        return panel;
    }

    private static EditableProperties loadProperties(FileObject propsFO) throws IOException {
        InputStream propsIS = propsFO.getInputStream();
        EditableProperties props = new EditableProperties(true);
        try {
            props.load(propsIS);
        } finally {
            propsIS.close();
        }
        return props;
    }

    public static void storeProperties(FileObject propsFO, EditableProperties props) throws IOException {
        FileLock lock = propsFO.lock();
        try {
            OutputStream os = propsFO.getOutputStream(lock);
            try {
                props.store(os);
            } finally {
                os.close();
            }
        } finally {
            lock.releaseLock();
        }
    }

    //not used:
    @Override
    public void actionPerformed(ActionEvent e) {
    }

    @Override
    public void resultChanged(LookupEvent le) {
        if (result.allInstances().iterator().hasNext()) {
            fo = result.allInstances().iterator().next();
            Project p = FileOwnerQuery.getOwner(fo);
            FileObject projectprop = p.getProjectDirectory().getFileObject(AntProjectHelper.PRIVATE_PROPERTIES_PATH);
            EditableProperties ep;
            try {
                ep = loadProperties(projectprop);
                if (ep.containsKey("application.args")) {
                    String args = ep.getProperty("application.args");
                    comboBox.getEditor().setItem(args);
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

}
