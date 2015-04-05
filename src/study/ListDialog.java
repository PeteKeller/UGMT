import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/*
 * ListDialog.java is a 1.4 class meant to be used by programs such as
 * ListDialogRunner.  It requires no additional files.
 */

/**
 * Use this modal dialog to let the user choose one string from a long
 * list.  See ListDialogRunner.java for an example of using ListDialog.
 * The basics:
 * <pre>
    String[] choices = {"A", "long", "array", "of", "strings"};
    String selectedName = ListDialog.showDialog(
                                componentInControllingFrame,
                                locatorComponent,
                                "A description of the list:",
                                "Dialog Title",
                                choices,
                                choices[0]);
 * </pre>
 */
public class ListDialog extends JDialog
                        implements ActionListener {
    private static ListDialog dialog;
    private static String value = "";
    private JList list;

    /**
     * Set up and show the dialog.  The first Component argument
     * determines which frame the dialog depends on; it should be
     * a component in the dialog's controlling frame. The second
     * Component argument should be null if you want the dialog
     * to come up with its left corner in the center of the screen;
     * otherwise, it should be the component on top of which the
     * dialog should appear.
     */
    public static String showDialog(Component frameComp,
                                    Component locationComp,
                                    String labelText,
                                    String title,
                                    String[] possibleValues,
                                    String initialValue,
                                    String longValue) {
        Frame frame = JOptionPane.getFrameForComponent(frameComp);
System.out.println("a");
        dialog = new ListDialog(frame,
                                locationComp,
                                labelText,
                                title,
                                possibleValues,
                                initialValue,
                                longValue);
System.out.println("b");
        dialog.setVisible(true);
System.out.println("c");
        return value;
    }

    private void setValue(String newValue) {
        value = newValue;
        list.setSelectedValue(value, true);
    }

    private ListDialog(Frame frame,
                       Component locationComp,
                       String labelText,
                       String title,
                       Object[] data,
                       String initialValue,
                       String longValue) {
        super(frame, title, true);

        //Create and initialize the buttons.
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);

        Container contentPane = getContentPane();
        contentPane.add(cancelButton, BorderLayout.PAGE_END);
        pack();
    }

    //Handle clicks on the Set and Cancel buttons.
    public void actionPerformed(ActionEvent e) {
        if ("Set".equals(e.getActionCommand())) {
            ListDialog.value = (String)(list.getSelectedValue());
        }
        ListDialog.dialog.setVisible(false);
    }
}
