package com.words.model.mysqlmodel;

import com.words.controller.futurewords.FutureWord;
import com.words.controller.utils.DateTimeUtils;
import com.words.controller.words.Word;
import com.words.controller.words.wordkinds.WordComplexity;
import com.words.main.EnglishWords;
import com.words.model.Model;
import com.words.model.filemodel.FileModel;
import com.words.model.filemodel.IterationLog;
import com.words.model.filemodel.RepeatWords;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumMap;
import java.util.Properties;
import java.util.stream.Collectors;

class FileModelToMysqlModel {
    
    private final Connection connection;
    private final Model model;
    
    public FileModelToMysqlModel(Connection connection)
        throws IOException {
        this.connection = connection;
        this.model = new FileModel(EnglishWords.PROJECT_DIRECTORY);
    }
    
    private void insertBundles() throws SQLException {
        String bundlesStr = model.allBundlesSorted().stream()
            .map(s -> "('" + s + "')").collect(Collectors.joining(", "));
        
        try (Statement st = connection.createStatement()) {
            int inserted = st.executeUpdate(
                "INSERT INTO bundles (bundle_date) VALUES " + bundlesStr);
            
            System.out.println(
                "Inserted " + inserted + " bundles");
        }
    }
    
    private void insertFutureWords() throws SQLException {
        String query = "INSERT INTO future_words " +
            "(future_word, priority, date_added, date_changed) VALUES " +
            "(?, ?, STR_TO_DATE(?, '%d.%m.%Y'), STR_TO_DATE(?, '%d.%m.%Y'))";
        
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            int inserted = 0;
            for (FutureWord fw : model.getFutureWords().values()) {
                ps.setString(1, fw.getWord());
                ps.setInt(2, fw.getOriginalPriority());
                ps.setString(3, fw.getDateAdded());
                ps.setString(4, fw.getDateChanged());
                inserted += ps.executeUpdate();
            }
            
            System.out.println("Inserted " + inserted + " future words");
        }
    }
    
    private void insertWords() throws SQLException {
        EnumMap<WordComplexity, Integer> complexityMap =
            new EnumMap<>(WordComplexity.class);
        try (Statement statement = connection.createStatement()) {
            ResultSet rs = statement.executeQuery(
                "SELECT complexity_id, complexity_name FROM complexities");
            while (rs.next()) {
                complexityMap.put(WordComplexity.valueOf(
                    rs.getString("complexity_name")),
                    rs.getInt("complexity_id"));
            }
        }
        
        String query = "INSERT INTO words (word, translation, synonyms, " +
            "bundle_id, times_picked, last_picked_timestamp, complexity_id, definition)" +
            "SELECT ?, ?, ?, bundle_id, ?, ?, ?, ? FROM bundles WHERE bundle_date = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            int inserted = 0;
            for (Word word : model.getAllWords().values()) {
                ps.setString(1, word.getWord());
                ps.setString(2, word.getTranslation());
                ps.setString(3, word.getSynonyms());
                ps.setInt(4, word.getTimesPicked());
                ps.setLong(5, word.getLastPickedTimestamp());
                ps.setInt(6, complexityMap.get(word.getComplexity()));
                ps.setString(7, model.getDefinition(word.getWord()));
                ps.setString(8, word.getBundle().toString());
                inserted += ps.executeUpdate();
            }
            
            System.out.println("Inserted " + inserted + " words into the model");
        }
    }
    
    private void insertIterations() {
        String query = "INSERT INTO daily_iterations (local_date, iterations) " +
            "VALUES (?, ?)";
        
        try {
            Properties props = new Properties();
            props.load(Files.newBufferedReader(
                EnglishWords.PROJECT_DIRECTORY.resolve(IterationLog.LOG_FILE_NAME),
                StandardCharsets.UTF_8));
            
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                int i = 0;
                for (String date : props.stringPropertyNames()) {
                    try {
                        if (!DateTimeUtils.isValidDate(date)) continue;
                        ps.setString(1, DateTimeUtils.parseDate(date).toString());
                        ps.setInt(2, Integer.parseInt(props.getProperty(date)));
                        i += ps.executeUpdate();
                    } catch (NumberFormatException nfe) { }
                }
                
                System.out.println("Inserted " + i + " days of iterations");
            } catch (SQLException ignore) {
                ignore.printStackTrace();
            }
        } catch (IOException ignore) {
            ignore.printStackTrace();
        }
    }
    
    private void insertRepeatWords() {
        String query = "INSERT INTO repeat_words (word_id, date_added) " +
            "SELECT words.word_id, ? FROM words WHERE word = ?";
        
        Properties props = new Properties();
        try {
            props.load(Files.newBufferedReader(
                EnglishWords.PROJECT_DIRECTORY.resolve(RepeatWords.FILE_NAME),
                StandardCharsets.UTF_8));
            
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                int i = 0;
                for (String date : props.stringPropertyNames()) {
                    String value = props.getProperty(date);
                    String[] tokens = value.split(";");
                    
                    for (String word : tokens) {
                        ps.setString(1, DateTimeUtils.parseDate(date).toString());
                        ps.setString(2, word);
                        i += ps.executeUpdate();
                    }
                }
                System.out.println("Inserted " + i + " repeat words");
            } catch (SQLException ignore) { }
        } catch (IOException ignore) { }
    }
    
    public void insert() throws SQLException {
        insertBundles();
        insertFutureWords();
        insertWords();
        insertIterations();
        insertRepeatWords();
        
        connection.commit();
    }
}
