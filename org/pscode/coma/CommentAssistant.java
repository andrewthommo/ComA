package org.pscode.coma;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.PatternSyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import javax.swing.text.DefaultEditorKit;
import org.xml.sax.InputSource;

public class CommentAssistant {

    public static final String COMMENTS_ENTRY_NAME = "comments.xml";

    private JMenuBar menuBar;
    private JComponent gui;
    private JTextField searchField;
    private JTable commentTable;
    //private CommentTableModel commentTableModel;
    private DefaultListModel<Comment> listModel;
    private JSplitPane mainSplit;
    private ArrayList<Comment> comments = new ArrayList<Comment>();
    private TableRowSorter tableRowSorter;
    private TextRowFilter tableRowFilter;
    private JList commentList;
    private JTextArea commentsArea;
    private ButtonGroup replyType;
    private ActionListener copyListener;
    private Logger logger = Logger.getAnonymousLogger();

    CommentAssistant() {
        copyListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                copyOutput();
            }
        };
    }

    public JComponent getGui() {
        if (gui == null) {
            gui = new JPanel(new BorderLayout(5, 5));
            gui.setBorder(new EmptyBorder(2, 3, 2, 3));

            JPanel tb = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 5));
            gui.add(tb, BorderLayout.PAGE_START);
            //tb.setFloatable(false);

            JButton clearComments = new JButton("Clear");
            ActionListener clearCommentsListener = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    clearInput();
                }
            };
            clearComments.addActionListener(clearCommentsListener);
            clearComments.setMnemonic('l');
            tb.add(clearComments);

            JLabel searchLabel = new JLabel("Search");
            tb.add(searchLabel);
            searchField = new JTextField(8);
            searchLabel.setLabelFor(searchField);
            searchLabel.setDisplayedMnemonic('s');
            tb.add(searchField);

            ActionListener searchListener = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    updateListOnSingleSelection();
                }
            };
            searchField.addActionListener(searchListener);

            replyType = new ButtonGroup();
            JRadioButton commentRadio = new JRadioButton("Comment", true);
            commentRadio.setActionCommand("Comment");
            replyType.add(commentRadio);
            tb.add(commentRadio);
            JRadioButton answerRadio = new JRadioButton("Answer");
            answerRadio.setActionCommand("Answer");
            replyType.add(answerRadio);
            tb.add(answerRadio);
            CommentTableModel commentTableModel;

            ItemListener typeListener  = new ItemListener() {

                @Override
                public void itemStateChanged(ItemEvent e) {
                    updateOutputFromList();
                }
            };
            commentRadio.addItemListener(typeListener);
            answerRadio.addItemListener(typeListener);

            try {
                loadComments();
                commentTableModel = new CommentTableModel(comments);
            } catch (Exception ex) {
                commentTableModel = new CommentTableModel();
                Logger.getLogger(CommentAssistant.class.getName()).log(Level.SEVERE, null, ex);
            }
            tableRowSorter = new TableRowSorter<CommentTableModel>(commentTableModel);
            tableRowFilter = new TextRowFilter("");
            tableRowSorter.setRowFilter(tableRowFilter);
            commentTable = new JTable(commentTableModel);
            commentTable.setRowSorter(tableRowSorter);
            commentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            MouseListener tableListener = new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent me) {
                    if (me.getClickCount() == 2) {
                        int index = commentTable.getSelectedRow();
                        int i2 = commentTable.convertRowIndexToModel(index);
                        CommentTableModel commentTableModel =
                                (CommentTableModel) commentTable.getModel();
                        Comment c = commentTableModel.getComment(i2);
                        listModel.addElement(c);
                        updateOutputFromList();
                    }
                }
            };
            commentTable.addMouseListener(tableListener);

            DocumentListener dl = new DocumentListener() {

                public void changedUpdate(DocumentEvent e) {
                    filterTable();
                }

                public void insertUpdate(DocumentEvent e) {
                    filterTable();
                }

                public void removeUpdate(DocumentEvent e) {
                    filterTable();
                }
            };
            searchField.getDocument().addDocumentListener(dl);

            TableColumn column = commentTable.getColumnModel().getColumn(0);
            column.setPreferredWidth(60);
            column.setMaxWidth(100);
            listModel = new DefaultListModel<Comment>();
            commentList = new JList(listModel);
            commentList.setCellRenderer(new CommentListCelRenderer());
            commentList.setVisibleRowCount(8);
            mainSplit = new JSplitPane(
                    JSplitPane.HORIZONTAL_SPLIT,
                    new JScrollPane(commentTable),
                    new JScrollPane(
                    commentList,
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));

            gui.add(mainSplit, BorderLayout.CENTER);

            commentsArea = new JTextArea(5, 20);
            JScrollPane commentsScroll = new JScrollPane(
                    commentsArea,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            commentsArea.setWrapStyleWord(true);
            commentsArea.setLineWrap(true);
            commentsArea.setEditable(false);
            gui.add(commentsScroll, BorderLayout.PAGE_END);

            JButton copy = new JButton("Copy");
            copy.setMnemonic('c');
            copy.addActionListener(copyListener);
            tb.add(copy);
        }
        return gui;
    }

    private void updateListOnSingleSelection() {
        int rows = commentTable.getRowCount();
        logger.log(Level.FINER, "Rows: " + rows);
        if (rows == 1) {
            int i2 = commentTable.convertRowIndexToModel(0);
            CommentTableModel commentTableModel =
                    (CommentTableModel) commentTable.getModel();
            Comment c = commentTableModel.getComment(i2);
            listModel.addElement(c);
            updateOutputFromList();
            searchField.setText("");
        }
    }

    public void clearInput() {
        searchField.setText("");
        listModel.clear();
        updateOutputFromList();
    }

    public void copyOutput() {
        String s = commentsArea.getText();
        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable t = new StringSelection(s);
        cb.setContents(t, null);
    }

    public void updateOutputFromList() {
        ListModel lm = commentList.getModel();
        StringBuilder sb = new StringBuilder();
        String cmd = replyType.getSelection().getActionCommand();
        boolean isAnswer = cmd.equals("Answer");
        logger.log(Level.INFO, "isAnswer: " + isAnswer);
        if (lm.getSize() == 1) {
            Comment com = (Comment) lm.getElementAt(0);

            sb.append(com.getComment1());
            if (isAnswer && com.getComment2().trim().length() > 0) {
                sb.append(com.getComment2());
            }
        } else {
            String sep = (isAnswer ? ". " : ") ");
            for (int ii = 0; ii < lm.getSize(); ii++) {
                sb.append("" + (ii + 1));
                sb.append(sep);
                Comment com = (Comment) lm.getElementAt(ii);

                sb.append(com.getComment1());
                sb.append(" ");
                if (isAnswer && com.getComment2().trim().length() > 0) {
                    sb.append("<br><br>");
                    sb.append(com.getComment2());
                    sb.append("<br>");
                }
            }
        }
        commentsArea.setText(sb.toString().trim());
    }

    public void filterTable() {
        tableRowFilter.setText(searchField.getText());
        tableRowSorter.setRowFilter(tableRowFilter);
    }
    
    public void loadComments() throws FileNotFoundException, IOException {
        File f = getPropertiesFile();
        FileInputStream fis = new FileInputStream(f);
        ZipInputStream zis = new ZipInputStream(fis);
        ZipEntry entry = zis.getNextEntry();

        while (!entry.getName().equals(COMMENTS_ENTRY_NAME)) {
            entry = zis.getNextEntry();
        }
        InputSource is = new InputSource(zis);
        XMLDecoder xmld = new XMLDecoder(is);
        comments = (ArrayList<Comment>) xmld.readObject();
        try {
            fis.close();
        } catch (IOException ex) {
            Logger.getLogger(CommentAssistant.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void saveComments() throws FileNotFoundException, IOException {
        CommentComparator commentComparator = new CommentComparator();
        Collections.sort(comments, commentComparator);

        File f = getPropertiesFile();
        System.out.println("Save to: " + f.getAbsolutePath());
        File p = f.getParentFile();
        if (!p.exists() && !p.mkdirs()) {
            throw new UnsupportedOperationException(
                    "Could not create settings directory: "
                    + p.getAbsolutePath());
        }
        FileOutputStream fos = new FileOutputStream(f);
        ZipOutputStream zos = new ZipOutputStream(fos);
        
        ZipEntry entry = new ZipEntry(COMMENTS_ENTRY_NAME);
        zos.putNextEntry(entry);

        XMLEncoder xmld = new XMLEncoder(zos);
        xmld.writeObject(comments);
        xmld.flush();
        xmld.close();
    }

    public final File getPropertiesFile() {
        File f = new File(System.getProperty("user.home"));
        f = new File(f, ".appsettings");
        String[] parts = this.getClass().getCanonicalName().split("\\.");
        for (String part : parts) {
            f = new File(f, part);
        }
        f = new File(f, "props.zip");
        return f;
    }

    public void centerSplitDivider() {
        mainSplit.setDividerLocation(.5d);
    }

    public JMenuBar getMenuBar() {
        if (menuBar == null) {
            menuBar = new JMenuBar();

            JMenu file = new JMenu("File");
            file.setMnemonic('f');
            menuBar.add(file);

            JMenuItem copyMenu = new JMenuItem("Copy");
            copyMenu.setMnemonic('c');
            file.add(copyMenu);
            copyMenu.addActionListener(copyListener);
            file.addSeparator();

            JMenu commentMenu = new JMenu("Comment");
            commentMenu.setMnemonic('o');
            menuBar.add(commentMenu);

            JMenuItem newComment = new JMenuItem("Add");
            newComment.setMnemonic('a');
            commentMenu.add(newComment);

            ActionListener newCommentListener = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    addNewComment();
                }
            };
            newComment.addActionListener(newCommentListener);

            JMenuItem editComment = new JMenuItem("Edit");
            ActionListener editListener = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    editComment();
                }
            };
            editComment.addActionListener(editListener);
            commentMenu.add(editComment);
            editComment.setMnemonic('e');
            JMenuItem deleteComment = new JMenuItem("Delete");
            ActionListener deleteListener = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    deleteComment();
                }
            };
            deleteComment.addActionListener(deleteListener);
            deleteComment.setMnemonic('d');
            commentMenu.add(deleteComment);

            String quitCommand = getQuitCommand();
            JMenuItem quit = new JMenuItem(quitCommand);
            file.add(quit);
            if (quitCommand.equals("Exit")) {
                quit.setMnemonic(quitCommand.charAt(1));
            } else {
                quit.setMnemonic(quitCommand.charAt(0));
            }
            ActionListener quitListener = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    Container c = gui.getTopLevelAncestor();
                    if (c instanceof Frame) {
                        Frame f = (Frame) c;
                        f.dispose();
                    }
                }
            };
            quit.addActionListener(quitListener);
        }
        return menuBar;
    }
    private JTextField commentCategoryField;
    private JTextField comment1Field;
    private JTextField comment2Field;
    private JComponent commentPanel;

    public Comment getSelectedComment() {
        int ii = commentTable.getSelectedRow();
        Comment c = null;
        if (ii > -1) {
            int index = commentTable.convertRowIndexToModel(ii);
            c = comments.get(index);
        }

        return c;
    }

    private void editComment() {
        Comment c = getSelectedComment();
        if (c == null) {
            JOptionPane.showMessageDialog(
                    gui,
                    "Select a comment to edit!",
                    "No comment selected",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            createCommentPanel();
            commentCategoryField.setText(c.getCategory());
            commentCategoryField.setCaretPosition(0);
            comment1Field.setText(c.getComment1());
            comment1Field.setCaretPosition(0);
            comment2Field.setText(c.getComment2());
            comment2Field.setCaretPosition(0);
            int result = JOptionPane.showConfirmDialog(
                    gui,
                    commentPanel,
                    "Add New Comment",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                c.setCategory(commentCategoryField.getText());
                c.setComment1(comment1Field.getText());
                c.setComment2(comment2Field.getText());
                try {
                    saveComments();
                    loadComments();
                    refreshTable();
                } catch (Exception ex) {
                    Logger.getLogger(
                            CommentAssistant.class.getName()).log(
                            Level.SEVERE, null, ex);
                    JOptionPane.showMessageDialog(
                            gui,
                            ex.getMessage(),
                            ex.toString(),
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    public void refreshTable() {
        CommentTableModel commentTableModel = new CommentTableModel(comments);
        tableRowSorter = new TableRowSorter<CommentTableModel>(commentTableModel);
        tableRowFilter = new TextRowFilter("");
        tableRowSorter.setRowFilter(tableRowFilter);
        commentTable.setModel(commentTableModel);
        //commentTable = new JTable(commentTableModel);
        commentTable.setRowSorter(tableRowSorter);
        commentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        commentTable.setModel(commentTableModel);
    }

    private void deleteComment() {
        Comment c = getSelectedComment();

        if (c != null && comments.contains(c)) {
            comments.remove(c);
        }
        try {
            saveComments();
            loadComments();
            refreshTable();
        } catch (Exception ex) {
            Logger.getLogger(CommentAssistant.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(
                    gui, ex.getMessage(),
                    ex.toString(),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private JComponent createCommentPanel() {
        if (commentPanel == null) {
            String[] labels = {
                "Category", "Comment 1", "Comment 2"
            };
            commentCategoryField = new JTextField(4);
            comment1Field = new JTextField(30);
            comment2Field = new JTextField(30);
            JComponent[] fields = {
                commentCategoryField, comment1Field, comment2Field
            };
            commentPanel = getTwoColumnLayout(labels, fields);
        }
        return commentPanel;
    }

    private void addNewComment() {
        createCommentPanel();
        int result = JOptionPane.showConfirmDialog(
                gui,
                commentPanel,
                "Add New Comment",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            Comment comment = new Comment();
            comment.setCategory(commentCategoryField.getText());
            comment.setComment1(comment1Field.getText());
            comment.setComment2(comment2Field.getText());
            comments.add(comment);
            try {
                saveComments();
                loadComments();
                refreshTable();
                System.out.println(" comments.size(): " + comments.size());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        gui,
                        ex.getLocalizedMessage(),
                        "Could not Save",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public String getQuitCommand() {
        String s = "Exit";
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            s = "Quit";
        }
        return s;
    }

    /**
     * Provides a JPanel with two columns (labels & fields) laid out using
     * GroupLayout. The arrays must be of equal size.
     *
     * Typical fields would be single line textual/input components such as
     * JTextField, JPasswordField, JFormattedTextField, JSpinner, JComboBox,
     * JCheckBox.. & the multi-line components wrapped in a JScrollPane -
     * JTextArea or (at a stretch) JList or JTable.
     *
     * @param labels The first column contains labels.
     * @param fields The last column contains fields.
     * @param addMnemonics Add mnemonic by next available letter in label text.
     * @return JComponent A JPanel with two columns of the components provided.
     */
    public static JComponent getTwoColumnLayout(
            JLabel[] labels,
            JComponent[] fields,
            boolean addMnemonics) {
        if (labels.length != fields.length) {
            String s = labels.length + " labels supplied for "
                    + fields.length + " fields!";
            throw new IllegalArgumentException(s);
        }
        JComponent panel = new JPanel();
        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);
        // Turn on automatically adding gaps between components
        layout.setAutoCreateGaps(true);
        // Create a sequential group for the horizontal axis.
        GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();
        GroupLayout.Group yLabelGroup = layout.createParallelGroup(GroupLayout.Alignment.TRAILING);
        hGroup.addGroup(yLabelGroup);
        GroupLayout.Group yFieldGroup = layout.createParallelGroup();
        hGroup.addGroup(yFieldGroup);
        layout.setHorizontalGroup(hGroup);
        // Create a sequential group for the vertical axis.
        GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();
        layout.setVerticalGroup(vGroup);

        int p = GroupLayout.PREFERRED_SIZE;
        // add the components to the groups
        for (JLabel label : labels) {
            yLabelGroup.addComponent(label);
        }
        for (Component field : fields) {
            yFieldGroup.addComponent(field, p, p, p);
        }
        for (int ii = 0; ii < labels.length; ii++) {
            vGroup.addGroup(layout.createParallelGroup().
                    addComponent(labels[ii]).
                    addComponent(fields[ii], p, p, p));
        }

        if (addMnemonics) {
            addMnemonics(labels, fields);
        }

        return panel;
    }

    private final static void addMnemonics(
            JLabel[] labels,
            JComponent[] fields) {
        Map<Character, Object> m = new HashMap<Character, Object>();
        for (int ii = 0; ii < labels.length; ii++) {
            labels[ii].setLabelFor(fields[ii]);
            String lwr = labels[ii].getText().toLowerCase();
            for (int jj = 0; jj < lwr.length(); jj++) {
                char ch = lwr.charAt(jj);
                if (m.get(ch) == null && Character.isLetterOrDigit(ch)) {
                    m.put(ch, ch);
                    labels[ii].setDisplayedMnemonic(ch);
                    break;
                }
            }
        }
    }

    /**
     * Provides a JPanel with two columns (labels & fields) laid out using
     * GroupLayout. The arrays must be of equal size.
     *
     * @param labelStrings Strings that will be used for labels.
     * @param fields The corresponding fields.
     * @return JComponent A JPanel with two columns of the components provided.
     */
    public static JComponent getTwoColumnLayout(
            String[] labelStrings,
            JComponent[] fields) {
        JLabel[] labels = new JLabel[labelStrings.length];
        for (int ii = 0; ii < labels.length; ii++) {
            labels[ii] = new JLabel(labelStrings[ii]);
        }
        return getTwoColumnLayout(labels, fields);
    }

    /**
     * Provides a JPanel with two columns (labels & fields) laid out using
     * GroupLayout. The arrays must be of equal size.
     *
     * @param labels The first column contains labels.
     * @param fields The last column contains fields.
     * @return JComponent A JPanel with two columns of the components provided.
     */
    public static JComponent getTwoColumnLayout(
            JLabel[] labels,
            JComponent[] fields) {
        return getTwoColumnLayout(labels, fields, true);
    }

    public static String getProperty(String name) {
        return name + ": \t"
                + System.getProperty(name)
                + System.getProperty("line.separator");
    }

    class TextRowFilter extends RowFilter {

        private String text;

        TextRowFilter(String text) {
            this.text = text;
        }

        @Override
        public boolean include(RowFilter.Entry entry) {
            String s = entry.getStringValue(1).toLowerCase();
            String comp = text.toLowerCase();
            if (comp.trim().length() == 0) {
                return true;
            }
            String parts[] = comp.split(" ");
            for (String part : parts) {
                if (!s.contains(part)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * @param text the text to set
         */
        public void setText(String text) {
            this.text = text;
        }
    }

    public static void main(String[] args) {
        Runnable r = new Runnable() {

            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception useDefault) {
                }
                JFrame f = new JFrame("ComA");

                CommentAssistant ca = new CommentAssistant();
                f.add(ca.getGui());
                f.setJMenuBar(ca.getMenuBar());
                try {
                    Image i16 = ImageIO.read(ca.getClass().getResource("coma16.png"));
                    Image i32 = ImageIO.read(ca.getClass().getResource("coma32.png"));
                    ArrayList<Image> icons = new ArrayList<Image>();
                    icons.add(i16);
                    icons.add(i32);
                    f.setIconImages(icons);

                } catch (IOException ex) {
                    Logger.getLogger(CommentAssistant.class.getName()).log(Level.SEVERE, null, ex);
                }
                // Ensures JVM closes after frame(s) closed and
                // all non-daemon threads are finished
                f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                // See http://stackoverflow.com/a/7143398/418556 for demo.
                f.setLocationByPlatform(true);

                // ensures the frame is the minimum size it needs to be
                // in order display the components within it
                f.pack();
                // should be done last, to avoid flickering, moving,
                // resizing artifacts.
                f.setVisible(true);
                ca.centerSplitDivider();
            }
        };
        // Swing GUIs should be created and updated on the EDT
        // http://docs.oracle.com/javase/tutorial/uiswing/concurrency/initial.html
        SwingUtilities.invokeLater(r);
    }
}

class CommentTableModel extends DefaultTableModel {

    CommentTableModel() {
    }

    CommentTableModel(ArrayList<Comment> comments) {
        columnIdentifiers = new Vector();
        columnIdentifiers.add("Cat.");
        columnIdentifiers.add("Comment 1/2");
        columnIdentifiers.add("Comment 2/2");
        Vector<Comment> t = new Vector<Comment>();
        for (Comment comment : comments) {
            t.add(comment);
        }
        dataVector = t;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public Comment getComment(int index) {
        return (Comment) dataVector.get(index);
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Comment comment = (Comment) dataVector.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return comment.getCategory();
            case 1:
                return comment.getComment1();
            case 2:
                return comment.getComment2();
            default:
                return "";
        }
    }
}

class CommentListCelRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
        Component c = super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);
        if (c instanceof JLabel && value instanceof Comment) {
            JLabel l = (JLabel) c;
            Comment comment = (Comment) value;
            //String pre = "<html><body style='width:200px'>";
            l.setText(comment.getComment1());
        }
        return c;
    }
}

class CommentComparator implements Comparator {

    @Override
    public int compare(Object o1, Object o2) {
        Comment c1 = (Comment) o1;
        Comment c2 = (Comment) o2;
        int cat = c1.getCategory().compareTo(c2.getCategory());
        if (cat != 0) {
            return cat;
        }
        return c1.getComment1().compareTo(c2.getComment1());
    }
}
