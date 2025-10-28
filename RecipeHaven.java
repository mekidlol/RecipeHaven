import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class RecipeHaven extends JFrame {
    private List<Recipe> recipes;
    private List<Recipe> filteredRecipes;
    private JPanel recipePanel;
    private JTextField searchField;
    private JComboBox<String> categoryFilter;
    private Set<String> favoriteIds;
    private String currentFilter = "All";
    
    // Color scheme - warm kitchen colors
    private final Color CREAM = new Color(255, 250, 240);
    private final Color TERRACOTTA = new Color(210, 105, 80);
    private final Color SAGE = new Color(138, 154, 124);
    private final Color WARM_WHITE = new Color(250, 248, 246);
    private final Color DARK_BROWN = new Color(101, 67, 33);
    private final Color LIGHT_ORANGE = new Color(255, 228, 196);
    private final Color GOLD = new Color(255, 215, 0);
    
    private static final String DATA_FILE = "recipes_data.ser";
    private static final String FAVORITES_FILE = "favorites_data.ser";

    public RecipeHaven() {
        recipes = new ArrayList<>();
        filteredRecipes = new ArrayList<>();
        favoriteIds = new HashSet<>();
        
        loadData();
        initUI();
        refreshRecipeDisplay();
    }

    private void initUI() {
        setTitle("Recipe Haven - Your Personal Cookbook");
        setSize(1200, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Main panel with background
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(CREAM);
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // Header
        mainPanel.add(createHeader(), BorderLayout.NORTH);
        
        // Center - Recipe display area
        mainPanel.add(createRecipeDisplayArea(), BorderLayout.CENTER);
        
        add(mainPanel);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 10));
        header.setBackground(CREAM);
        
        // Title
        JLabel title = new JLabel("Recipe Haven", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(TERRACOTTA);
        header.add(title, BorderLayout.NORTH);
        
        // Search and filter panel
        JPanel controlPanel = new JPanel(new BorderLayout(10, 10));
        controlPanel.setBackground(CREAM);
        
        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.setBackground(CREAM);
        
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField = new JTextField(25);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { filterRecipes(); }
            public void removeUpdate(DocumentEvent e) { filterRecipes(); }
            public void insertUpdate(DocumentEvent e) { filterRecipes(); }
        });
        
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        
        // Filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.setBackground(CREAM);
        
        JLabel filterLabel = new JLabel("Category:");
        filterLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        String[] categories = {"All", "Breakfast", "Lunch", "Dinner", "Dessert", 
                              "Snack", "Beverage", "Appetizer", "Favorites"};
        categoryFilter = new JComboBox<>(categories);
        categoryFilter.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        categoryFilter.addActionListener(e -> {
            currentFilter = (String) categoryFilter.getSelectedItem();
            filterRecipes();
        });
        
        filterPanel.add(filterLabel);
        filterPanel.add(categoryFilter);
        
        controlPanel.add(searchPanel, BorderLayout.WEST);
        controlPanel.add(filterPanel, BorderLayout.CENTER);
        
        // Add button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(CREAM);
        
        JButton addButton = createStyledButton("Add New Recipe", SAGE);
        addButton.addActionListener(e -> showAddRecipeDialog());
        buttonPanel.add(addButton);
        
        controlPanel.add(buttonPanel, BorderLayout.EAST);
        
        header.add(controlPanel, BorderLayout.CENTER);
        
        return header;
    }

    private JPanel createRecipeDisplayArea() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(CREAM);
        
        recipePanel = new JPanel();
        recipePanel.setLayout(new BoxLayout(recipePanel, BoxLayout.Y_AXIS));
        recipePanel.setBackground(CREAM);
        
        JScrollPane scrollPane = new JScrollPane(recipePanel);
        scrollPane.setBackground(CREAM);
        scrollPane.getViewport().setBackground(CREAM);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        container.add(scrollPane, BorderLayout.CENTER);
        
        return container;
    }

    private void refreshRecipeDisplay() {
        recipePanel.removeAll();
        
        if (filteredRecipes.isEmpty()) {
            JLabel emptyLabel = new JLabel("No recipes found. Add your first recipe!", SwingConstants.CENTER);
            emptyLabel.setFont(new Font("Segoe UI", Font.ITALIC, 18));
            emptyLabel.setForeground(DARK_BROWN);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            recipePanel.add(Box.createVerticalGlue());
            recipePanel.add(emptyLabel);
            recipePanel.add(Box.createVerticalGlue());
        } else {
            for (Recipe recipe : filteredRecipes) {
                recipePanel.add(createRecipeCard(recipe));
                recipePanel.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }
        
        recipePanel.revalidate();
        recipePanel.repaint();
    }

    private JPanel createRecipeCard(Recipe recipe) {
        JPanel card = new JPanel(new BorderLayout(15, 10));
        card.setBackground(WARM_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LIGHT_ORANGE, 2),
            new EmptyBorder(15, 15, 15, 15)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        
        // Left - Recipe info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(WARM_WHITE);
        
        // Title with favorite star
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        titlePanel.setBackground(WARM_WHITE);
        
        boolean isFavorite = favoriteIds.contains(recipe.id);
        JLabel favIcon = new JLabel(isFavorite ? "★" : "☆");
        favIcon.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        favIcon.setForeground(isFavorite ? GOLD : new Color(200, 200, 200));
        favIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));
        favIcon.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                toggleFavorite(recipe);
            }
        });
        
        JLabel nameLabel = new JLabel(recipe.name);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        nameLabel.setForeground(TERRACOTTA);
        
        titlePanel.add(favIcon);
        titlePanel.add(nameLabel);
        
        JLabel categoryLabel = new JLabel("Category: " + recipe.category);
        categoryLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        categoryLabel.setForeground(SAGE);
        
        JLabel ingredientsLabel = new JLabel("Ingredients: " + recipe.getShortIngredients());
        ingredientsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        ingredientsLabel.setForeground(DARK_BROWN);
        
        infoPanel.add(titlePanel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(categoryLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(ingredientsLabel);
        
        // Right - Action buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBackground(WARM_WHITE);
        
        JButton viewButton = createSmallButton("View", SAGE);
        viewButton.addActionListener(e -> showRecipeDetails(recipe));
        
        JButton editButton = createSmallButton("Edit", new Color(100, 149, 237));
        editButton.addActionListener(e -> showEditRecipeDialog(recipe));
        
        JButton deleteButton = createSmallButton("Delete", new Color(220, 90, 90));
        deleteButton.addActionListener(e -> deleteRecipe(recipe));
        
        buttonPanel.add(viewButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(editButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(deleteButton);
        
        card.add(infoPanel, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.EAST);
        
        return card;
    }

    private void toggleFavorite(Recipe recipe) {
        if (favoriteIds.contains(recipe.id)) {
            favoriteIds.remove(recipe.id);
        } else {
            favoriteIds.add(recipe.id);
        }
        saveData();
        refreshRecipeDisplay();
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(180, 40));
        return btn;
    }

    private JButton createSmallButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(100, 30));
        btn.setMaximumSize(new Dimension(100, 30));
        return btn;
    }

    private void showAddRecipeDialog() {
        showRecipeDialog(null);
    }

    private void showEditRecipeDialog(Recipe recipe) {
        showRecipeDialog(recipe);
    }

    private void showRecipeDialog(Recipe editRecipe) {
        JDialog dialog = new JDialog(this, editRecipe == null ? "Add New Recipe" : "Edit Recipe", true);
        dialog.setSize(600, 700);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(CREAM);
        
        // Name
        JLabel nameLabel = new JLabel("Recipe Name:");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JTextField nameField = new JTextField(editRecipe != null ? editRecipe.name : "");
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        // Category
        JLabel categoryLabel = new JLabel("Category:");
        categoryLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        String[] cats = {"Breakfast", "Lunch", "Dinner", "Dessert", "Snack", "Beverage", "Appetizer"};
        JComboBox<String> categoryBox = new JComboBox<>(cats);
        categoryBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        if (editRecipe != null) categoryBox.setSelectedItem(editRecipe.category);
        
        // Ingredients
        JLabel ingredientsLabel = new JLabel("Ingredients (one per line):");
        ingredientsLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JTextArea ingredientsArea = new JTextArea(8, 40);
        ingredientsArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        ingredientsArea.setLineWrap(true);
        ingredientsArea.setWrapStyleWord(true);
        if (editRecipe != null) ingredientsArea.setText(String.join("\n", editRecipe.ingredients));
        JScrollPane ingredientsScroll = new JScrollPane(ingredientsArea);
        
        // Steps
        JLabel stepsLabel = new JLabel("Cooking Steps:");
        stepsLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JTextArea stepsArea = new JTextArea(10, 40);
        stepsArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        stepsArea.setLineWrap(true);
        stepsArea.setWrapStyleWord(true);
        if (editRecipe != null) stepsArea.setText(editRecipe.steps);
        JScrollPane stepsScroll = new JScrollPane(stepsArea);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(CREAM);
        
        JButton saveButton = createStyledButton(editRecipe == null ? "Add Recipe" : "Save Changes", SAGE);
        saveButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String category = (String) categoryBox.getSelectedItem();
            String ingredientsText = ingredientsArea.getText().trim();
            String steps = stepsArea.getText().trim();
            
            if (name.isEmpty() || ingredientsText.isEmpty() || steps.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in all fields!", 
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            List<String> ingredients = Arrays.asList(ingredientsText.split("\n"));
            
            if (editRecipe == null) {
                Recipe newRecipe = new Recipe(name, category, ingredients, steps);
                recipes.add(newRecipe);
            } else {
                editRecipe.name = name;
                editRecipe.category = category;
                editRecipe.ingredients = ingredients;
                editRecipe.steps = steps;
            }
            
            saveData();
            filterRecipes();
            dialog.dispose();
        });
        
        JButton cancelButton = createStyledButton("Cancel", new Color(150, 150, 150));
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        // Add components
        panel.add(nameLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(nameField);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        panel.add(categoryLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(categoryBox);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        panel.add(ingredientsLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(ingredientsScroll);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        panel.add(stepsLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(stepsScroll);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(buttonPanel);
        
        JScrollPane dialogScroll = new JScrollPane(panel);
        dialogScroll.setBackground(CREAM);
        dialogScroll.getViewport().setBackground(CREAM);
        dialog.add(dialogScroll);
        
        dialog.setVisible(true);
    }

    private void showRecipeDetails(Recipe recipe) {
        JDialog dialog = new JDialog(this, recipe.name, true);
        dialog.setSize(600, 650);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(WARM_WHITE);
        
        // Title
        JLabel titleLabel = new JLabel(recipe.name);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(TERRACOTTA);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel categoryLabel = new JLabel("Category: " + recipe.category);
        categoryLabel.setFont(new Font("Segoe UI", Font.ITALIC, 16));
        categoryLabel.setForeground(SAGE);
        categoryLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Ingredients
        JLabel ingredientsTitle = new JLabel("Ingredients:");
        ingredientsTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        ingredientsTitle.setForeground(DARK_BROWN);
        
        JTextArea ingredientsArea = new JTextArea();
        ingredientsArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        ingredientsArea.setEditable(false);
        ingredientsArea.setBackground(WARM_WHITE);
        ingredientsArea.setLineWrap(true);
        ingredientsArea.setWrapStyleWord(true);
        StringBuilder ingredientsText = new StringBuilder();
        for (String ing : recipe.ingredients) {
            ingredientsText.append("• ").append(ing).append("\n");
        }
        ingredientsArea.setText(ingredientsText.toString());
        
        // Steps
        JLabel stepsTitle = new JLabel("Cooking Steps:");
        stepsTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        stepsTitle.setForeground(DARK_BROWN);
        
        JTextArea stepsArea = new JTextArea(recipe.steps);
        stepsArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        stepsArea.setEditable(false);
        stepsArea.setBackground(WARM_WHITE);
        stepsArea.setLineWrap(true);
        stepsArea.setWrapStyleWord(true);
        
        JScrollPane stepsScroll = new JScrollPane(stepsArea);
        stepsScroll.setPreferredSize(new Dimension(500, 200));
        
        // Close button
        JButton closeButton = createStyledButton("Close", SAGE);
        closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeButton.addActionListener(e -> dialog.dispose());
        
        panel.add(titleLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(categoryLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(ingredientsTitle);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(ingredientsArea);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(stepsTitle);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(stepsScroll);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(closeButton);
        
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBackground(WARM_WHITE);
        scrollPane.getViewport().setBackground(WARM_WHITE);
        dialog.add(scrollPane);
        
        dialog.setVisible(true);
    }

    private void deleteRecipe(Recipe recipe) {
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete '" + recipe.name + "'?", 
            "Confirm Delete", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            recipes.remove(recipe);
            favoriteIds.remove(recipe.id);
            saveData();
            filterRecipes();
        }
    }

    private void filterRecipes() {
        String searchText = searchField.getText().toLowerCase().trim();
        
        filteredRecipes = new ArrayList<>();
        for (Recipe recipe : recipes) {
            // Category filter
            if (!currentFilter.equals("All")) {
                if (currentFilter.equals("Favorites")) {
                    if (!favoriteIds.contains(recipe.id)) continue;
                } else if (!recipe.category.equals(currentFilter)) {
                    continue;
                }
            }
            
            // Search filter
            if (!searchText.isEmpty()) {
                boolean matches = recipe.name.toLowerCase().contains(searchText) ||
                                recipe.category.toLowerCase().contains(searchText);
                
                if (!matches) {
                    for (String ing : recipe.ingredients) {
                        if (ing.toLowerCase().contains(searchText)) {
                            matches = true;
                            break;
                        }
                    }
                }
                
                if (!matches) continue;
            }
            
            filteredRecipes.add(recipe);
        }
        
        refreshRecipeDisplay();
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        try {
            File file = new File(DATA_FILE);
            if (file.exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
                recipes = (List<Recipe>) ois.readObject();
                ois.close();
            }
            
            File favFile = new File(FAVORITES_FILE);
            if (favFile.exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(favFile));
                favoriteIds = (Set<String>) ois.readObject();
                ois.close();
            }
            
            filteredRecipes = new ArrayList<>(recipes);
        } catch (Exception e) {
            recipes = new ArrayList<>();
            filteredRecipes = new ArrayList<>();
            favoriteIds = new HashSet<>();
        }
    }

    private void saveData() {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE));
            oos.writeObject(recipes);
            oos.close();
            
            ObjectOutputStream oos2 = new ObjectOutputStream(new FileOutputStream(FAVORITES_FILE));
            oos2.writeObject(favoriteIds);
            oos2.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving data: " + e.getMessage(), 
                "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            RecipeHaven app = new RecipeHaven();
            app.setVisible(true);
        });
    }
}

class Recipe implements Serializable {
    private static final long serialVersionUID = 1L;
    
    String id;
    String name;
    String category;
    List<String> ingredients;
    String steps;
    
    public Recipe(String name, String category, List<String> ingredients, String steps) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.category = category;
        this.ingredients = new ArrayList<>(ingredients);
        this.steps = steps;
    }
    
    public String getShortIngredients() {
        if (ingredients.isEmpty()) return "No ingredients";
        
        StringBuilder sb = new StringBuilder();
        int count = Math.min(3, ingredients.size());
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(", ");
            String ing = ingredients.get(i);
            sb.append(ing.length() > 30 ? ing.substring(0, 27) + "..." : ing);
        }
        if (ingredients.size() > 3) {
            sb.append(" (+" + (ingredients.size() - 3) + " more)");
        }
        return sb.toString();
    }
}