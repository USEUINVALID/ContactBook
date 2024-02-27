import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.util.Vector;
import java.util.regex.Pattern;

public class ContactBook extends JFrame {
    private Connection conn;

    private JTextField nameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JButton addButton;
    private JButton deleteButton;
    private JButton editButton;
    private JTable table;

    public ContactBook() {
        setTitle("Address Book");
        setSize(600, 400);
        setPreferredSize(new Dimension(800, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Создание текстовых полей для ввода данных
        nameField = new JTextField(20);
        emailField = new JTextField(20);
        phoneField = new JTextField(20);

        // Создание кнопок для добавления, удаления и редактирования данных
        addButton = new JButton("Добавить");
        deleteButton = new JButton("Удалить");
        editButton = new JButton("Редактировать");

        // Создание таблицы для отображения данных
        table = new JTable();

        // Создание панели для размещения компонентов
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(3, 2));
        inputPanel.add(new JLabel("Имя:"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("Email:"));
        inputPanel.add(emailField);
        inputPanel.add(new JLabel("Телефон:"));
        inputPanel.add(phoneField);
        table.setBackground(Color.WHITE);
        table.setForeground(Color.BLACK);
        table.setFont(new Font("Arial", Font.PLAIN, 12));
        addButton.setBackground(Color.BLUE);
        deleteButton.setBackground(Color.RED);
        editButton.setBackground(Color.ORANGE);
        nameField.setBackground(Color.WHITE);
        nameField.setForeground(Color.BLACK);
        emailField.setBackground(Color.WHITE);
        emailField.setForeground(Color.BLACK);
        phoneField.setBackground(Color.WHITE);
        phoneField.setForeground(Color.BLACK);

        // Создание панели для размещения кнопок
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(editButton);

        // Размещение компонентов на форме
        setLayout(new BorderLayout());
        add(inputPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Установка видимости формы
        setVisible(true);

        // Подключение к базе данных SQLite
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:addressbook.db");
            System.out.println("Подключение к базе данных SQLite успешно установлено.");

            // Создание таблицы адресной книги, если она не существует
            Statement stmt = conn.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS contacts (\n"
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                    + " name TEXT NOT NULL,\n"
                    + " email TEXT,\n"
                    + " phone TEXT\n"
                    + ");";
            stmt.execute(sql);

            // Обновление таблицы при запуске приложения
            updateTable();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Обработчики событий для кнопок
        addButton.addActionListener(event -> {
            String name = nameField.getText();
            String email = emailField.getText();
            String phone = phoneField.getText();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Имя не может быть пустым");
                return;
            }

            String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
            Pattern pattern = Pattern.compile(emailRegex);
            if (email.isEmpty() || !pattern.matcher(email).matches()) {
                JOptionPane.showMessageDialog(null, "Введите действительный адрес электронной почты");
                return;
            }

            String phoneRegex = "^\\+?[0-9. ()-]{10,25}$";
            pattern = Pattern.compile(phoneRegex);
            if (phone.isEmpty() || !pattern.matcher(phone).matches()) {
                JOptionPane.showMessageDialog(null, "Введите действительный номер телефона");
                return;
            }

            try {
                insertData(name, email, phone);
                updateTable();
                nameField.setText("");
                emailField.setText("");
                phoneField.setText("");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        deleteButton.addActionListener(event -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                int id = (int) table.getValueAt(selectedRow, 0);
                try {
                    deleteData(id);
                    updateTable();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        editButton.addActionListener(event -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                int id = (int) table.getValueAt(selectedRow, 0);
                try {
                    updateData(id, nameField.getText(), emailField.getText(), phoneField.getText());
                    updateTable();
                    nameField.setText("");
                    emailField.setText("");
                    phoneField.setText("");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // Метод для вставки данных в таблицу
    private void insertData(String name, String email, String phone) throws SQLException {
        String sql = "INSERT INTO contacts(name, email, phone) VALUES(?,?,?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, name);
        pstmt.setString(2, email);
        pstmt.setString(3, phone);
        pstmt.executeUpdate();
    }

    private ResultSet getData() throws SQLException {
        String sql = "SELECT id, name, email, phone FROM contacts";
        Statement stmt = conn.createStatement();
        return stmt.executeQuery(sql);
    }

    private void deleteData(int id) throws SQLException {
        String sql = "DELETE FROM contacts WHERE id = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, id);
        pstmt.executeUpdate();
    }

    private void updateData(int id, String name, String phone, String email) throws SQLException {
        String sql = "UPDATE contacts SET name = ?, email = ?, phone = ? WHERE id = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, name);
        pstmt.setString(2, email);
        pstmt.setString(3, phone);
        pstmt.setInt(4, id);
        pstmt.executeUpdate();
    }

    // Метод для обновления таблицы
    private void updateTable() throws SQLException {
        ResultSet rs = getData();
        table.setModel(buildTableModel(rs));
    }

    public static TableModel buildTableModel(ResultSet rs)
            throws SQLException {

        ResultSetMetaData metaData = rs.getMetaData();

        // Названия столбцов
        Vector<String> columnNames = new Vector<>();
        int columnCount = metaData.getColumnCount();
        for (int column = 1; column <= columnCount; column++) {
            columnNames.add(metaData.getColumnName(column));
        }

        // Данные таблицы
        Vector<Vector<Object>> data = new Vector<>();
        while (rs.next()) {
            Vector<Object> vector = new Vector<>();
            for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                vector.add(rs.getObject(columnIndex));
            }
            data.add(vector);
        }

        return new DefaultTableModel(data, columnNames);
    }

    public static void main(String... args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Error UI");
        }
        SwingUtilities.invokeLater(ContactBook::new);
    }
}