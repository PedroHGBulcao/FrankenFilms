import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

import com.opencsv.CSVReader;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.List; 

public class GUI{

    static Map<String,String> name2id;
    static Map<String,String> id2link;
    static Map<String,String> id2name;
    JFrame frame = new JFrame("Movie Recommender");
    AutoCompleteDecorator decorator;
    JComboBox combobox;
    private DefaultListModel<String> listModel;
    private JList<String> list;
    private Map<String, Integer> movieRatings;

    public GUI(Vector<String> names) {

        names.sort(null);
        combobox = new JComboBox(names);
        AutoCompleteDecorator.decorate(combobox);
        // Create a list model and a JList to display the selected items
        listModel = new DefaultListModel<>();
        list = new JList<>(listModel);
        movieRatings = new HashMap<>();

        combobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String prefix = ((JTextField) combobox.getEditor().getEditorComponent()).getText().toLowerCase();

                for (String item : names) {
                    if (item.toLowerCase().startsWith(prefix)) {
                        combobox.setSelectedItem(item);
                        return;
                    }
                }
            }
        });

        // Create a button to add the typed item to the list
        JButton addButton = new JButton("Add to List");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedItem = (String) combobox.getSelectedItem();
                if (movieRatings.containsKey(selectedItem)) {
                    JOptionPane.showMessageDialog(frame, "Movie already selected.", "Error", JOptionPane.ERROR_MESSAGE);
                }
                else if (selectedItem != null && !selectedItem.isEmpty()) {
                    int rating = askForRating(selectedItem);
                    if (rating >=0 && rating <=10){
                        listModel.addElement(selectedItem + " (Rating: " + rating + ")");
                        movieRatings.put(selectedItem, rating);
                    }
                }
            }
        });

        JButton removeButton = new JButton("Remove from List");
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = list.getSelectedIndex();
                if (selectedIndex != -1) {
                    String selectedMovie = listModel.getElementAt(selectedIndex);
                    int index = selectedMovie.indexOf(" (Rating:");
                    if (index != -1) {
                        String movie = selectedMovie.substring(0, index);
                        listModel.removeElementAt(selectedIndex);
                        movieRatings.remove(movie);
                    }
                }
            }
        });

        JButton recommendButton = new JButton("Recommend");
        recommendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (movieRatings.size() < 3) {
                    JOptionPane.showMessageDialog(frame, "Please select at least 3 movies!", "Error", JOptionPane.ERROR_MESSAGE);
                }
                else showRecommendations(movieRatings);
            }
        });

        frame.setSize(1000,400);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        combobox.setEditable(true);
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.PAGE_AXIS));
        topPanel.add(combobox);
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonsPanel.add(addButton);
        buttonsPanel.add(removeButton);

        topPanel.add(buttonsPanel);

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(recommendButton);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(new JScrollPane(list), BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private int askForRating(String movie) {
        JFrame ratingFrame = new JFrame("Select a Rating");
        JPanel panel = new JPanel(new GridLayout(1, 10));
        JButton[] ratingButtons = new JButton[10];

        for (int i = 0; i < 10; i++) {
            ratingButtons[i] = new JButton(Integer.toString(i + 1));
            final int rating = i + 1;
            ratingButtons[i].addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // Add the selected item and its rating to the list
                    listModel.addElement(movie + " (Rating: " + rating + ")");
                    movieRatings.put(movie, rating);
                    ratingFrame.dispose();
                }
            });
            panel.add(ratingButtons[i]);
        }

        ratingFrame.add(panel);
        ratingFrame.setSize(600, 100);
        ratingFrame.setLocationRelativeTo(frame);
        ratingFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        ratingFrame.setVisible(true);

        return -1; // Return -1 temporarily, actual rating will be set when a button is clicked
    }

    private void showRecommendations(Map<String, Integer> movieRatings) {
        Map<String, Integer> movieRatingsReviewed = new HashMap<>();
        for (Map.Entry<String, Integer> set:movieRatings.entrySet()){
            movieRatingsReviewed.put(name2id.get(set.getKey()), set.getValue());
        }
        Recommender rec = new Recommender(movieRatingsReviewed);
        Vector<String> recommendedMovies = rec.getRecommendations();

        JPopupMenu popupMenu = new JPopupMenu();
        for (String movie : recommendedMovies) {
            if (movie != ""){
                JMenuItem menuItem = new JMenuItem(new MovieAction(id2name.get(movie), id2link.get(movie)));
                popupMenu.add(menuItem);
            }
        }

        JMenuItem closeMenuItem = new JMenuItem("Close");
        closeMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                popupMenu.setVisible(false);
            }
        });
        popupMenu.addSeparator();
        popupMenu.add(closeMenuItem);

        popupMenu.setPreferredSize(new Dimension(300, 200));

        popupMenu.show(frame, frame.getX(), frame.getY());
    }

    private class MovieAction extends AbstractAction {
        private String movie;
        private String url;

        public MovieAction(String movie, String url) {
            this.movie = movie;
            this.url = url;
            putValue(Action.NAME, movie);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Open the URL in the default web browser
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                try {
                    Desktop.getDesktop().browse(new java.net.URI(url));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        CSVReader reader = null;
        id2name=new HashMap<String,String>();
        name2id=new HashMap<String,String>();
        id2link=new HashMap<String,String>();
        Vector<String> lherme = new Vector<String>();
        System.out.println(new File(".").getAbsolutePath());
        try{
            reader = new CSVReader(new FileReader("movie_database.csv"));
            String[] nextLine = new String[20];
            reader.readNext();
            while ((nextLine = reader.readNext()) != null){
                try{
                    id2name.put(nextLine[0], nextLine[1]);
                    name2id.put(nextLine[1], nextLine[0]);
                    id2link.put(nextLine[0], nextLine[2]);
                    lherme.add(nextLine[1]);
                } catch (Exception e){
                    System.out.print(e);
                }
            } 
            System.out.println(lherme.size());
        } catch (Exception e){
            System.out.print(e);
        }
        GUI g = new GUI(lherme);
    }
}