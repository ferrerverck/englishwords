package com.words.model.mysqlmodel;

import com.words.controller.futurewords.FutureWord;
import com.words.controller.utils.DateTimeUtils;
import com.words.controller.words.Word;
import com.words.controller.words.WordFactory;
import com.words.controller.words.wordkinds.WordComplexity;
import com.words.controller.words.wordkinds.WordType;
import com.words.main.EnglishWords;
import com.words.model.Model;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MysqlModel implements Model {
    
    private static final int REPEAT_DAYS_TO_EXPIRE = 5;
    
    private static final String GET_WORD_QUERY_WITHOUT_CONDITION =
        "SELECT word, translation, synonyms, bundle_date, " +
        "complexity_id, times_picked, last_picked_timestamp " +
        "FROM words " +
        "JOIN bundles ON words.bundle_id = bundles.bundle_id";
    private static String getWordQuery(String condition) {
        return GET_WORD_QUERY_WITHOUT_CONDITION + " " + condition;
    }
    
    private final LocalDate today;
    private final Map<WordComplexity, Integer> complexityMap =
        new EnumMap<>(WordComplexity.class);
    
    private final String dbName;
    private final Connection con;
    
    private final Map<String, Word> wordMap = new TreeMap<>((s1, s2) ->
        s1.replaceAll("^to ", "").compareTo(s2.replaceAll("^to ", "")));
    
    public static void main(String[] args) throws Exception {
        MysqlModel model = new MysqlModel("EnglishWordsTestDb");
        
        System.out.println(model.getLastBundleName());
        System.out.println(model.getPenultimateBundleName());
        System.out.println(model.getRepeatWords());
        System.out.println(model.getExpiredRepeatWords());
        
        model.addRepeatWord("to abolish");
        model.deleteRepeatWord("to abolish");
        
        System.out.println("1 day iters = " + model.getIterationsForDays(1));
        System.out.println("2 day iters = " + model.getIterationsForDays(2));
        System.out.println("7 day iters = " + model.getIterationsForDays(7));
        
        System.out.println(model.isExistingBundle(LocalDate.of(2015, Month.JANUARY, 1)));
        System.out.println(model.isExistingBundle(LocalDate.of(2015, Month.JANUARY, 2)));
        System.out.println(model.isExistingBundle(LocalDate.of(2015, Month.JANUARY, 3)));
        System.out.println(model.isExistingBundle(LocalDate.of(2015, Month.JANUARY, 4)));
        System.out.println(model.isExistingBundle(LocalDate.of(2015, Month.JANUARY, 5)));
        System.out.println(model.isExistingBundle(LocalDate.of(2015, Month.JANUARY, 6)));
        System.out.println(model.isExistingBundle(LocalDate.of(2015, Month.JANUARY, 7)));
        System.out.println(model.isExistingBundle(LocalDate.of(2015, Month.JANUARY, 8)));
    }
    
    public Connection getConnection() { return con; }
    
    private Word getWordFromResultSet(ResultSet rs) throws SQLException {
        String englishWord = rs.getString("word");
        if (wordMap.containsKey(englishWord)) return wordMap.get(englishWord);
        
        Word word = WordFactory.newWord();
        word.setWord(englishWord);
        word.setTranslation(rs.getString("translation"));
        word.setSynonyms(rs.getString("synonyms"));
        word.setBundle(rs.getDate("bundle_date").toLocalDate());
        word.setComplexity(getComplexityFromId(rs.getInt("complexity_id")));
        word.setTimesPicked(rs.getInt("times_picked"));
        word.setLastPickedTimestamp(rs.getLong("last_picked_timestamp"));
        
        wordMap.put(englishWord, word);
        
        return word;
    }
    
    private void defaultExceptionHandler(Exception ex) {
        ex.printStackTrace();
    }
    
    private void rollback() {
        try {
            con.rollback();
        } catch (SQLException ignore) { }
    }
    
    private int getIdFromComplexity(WordComplexity complexity) {
        return complexityMap.getOrDefault(complexity, 1);
    }
    
    private WordComplexity getComplexityFromId(Integer id) {
        for (Map.Entry<WordComplexity, Integer> entry : complexityMap.entrySet()) {
            if (entry.getValue().equals(id)) return entry.getKey();
        }
        
        return WordComplexity.NORMAL;
    }
    
    /**
     * Gets bundle id. Returns -1 if bundles doesn't exist.
     * @param bundle bundle to search
     * @return bundle id or -1
     * @throws SQLExceptin if something happens with database
     */
    private int getBundleId(LocalDate bundle) throws SQLException {
        String searchQuery =
            "SELECT bundle_id FROM bundles WHERE bundle_date = ?";
        
        try (PreparedStatement ps = con.prepareCall(searchQuery)) {
            ps.setString(1, bundle.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("bundle_id");
            return -1;
        }
    }
    
    /**
     * Registers new bundle and return generated id.
     * @param bundle bundle to insert
     * @return newly generated bundle id
     * @throws SQLException if something wrong with database or bundle exists
     */
    private int registerNewBundle(LocalDate bundle) throws SQLException {
        String insertQuery = "INSERT INTO bundles (bundle_date) VALUES (?)";
        try (PreparedStatement ps = con.prepareStatement(
            insertQuery, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, bundle.toString());
            ps.executeUpdate();
            
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) return generatedKeys.getInt(1);
                else throw new SQLException("Failed to add new bundle");
            }
        }
    }
    
    public MysqlModel(String dbName) throws Exception {
        this.dbName = dbName;
        
        Properties props = new Properties();
        props.load(getClass().getResource("/resources/mysql/db.properties").openStream());
//            Files.newBufferedReader(PROJECT_DIRECTORY.resolve("db.properties"),
//            Charset.defaultCharset()));
        String user = props.getProperty("db.user");
        String password = props.getProperty("db.password");
        boolean recreate =
            Boolean.parseBoolean(props.getProperty("db.recreate"));
        boolean importDb =
            Boolean.parseBoolean(props.getProperty("db.import"));
        
        Class.forName("com.mysql.jdbc.Driver");
        con = DriverManager.getConnection(
            "jdbc:mysql://localhost/?allowMultiQueries=true&useUnicode=true",
            user, password);
        con.setAutoCommit(false);
        
        // find if database already exists
        boolean dbExists = false;
        try (ResultSet resultSet = con.getMetaData().getCatalogs()) {
            while (resultSet.next()) {
                if (resultSet.getString(1).equals(dbName)) {
                    dbExists = true;
                    break;
                }
            }
        }
        
        if (!dbExists || recreate) createDatabase(dbName, importDb);
        
        useDatabase(dbName);
        
        // hash complexity ids
        try (Statement statement = con.createStatement()) {
            ResultSet rs = statement.executeQuery(
                "SELECT complexity_id, complexity_name FROM complexities");
            while (rs.next()) {
                complexityMap.put(WordComplexity.valueOf(
                    rs.getString("complexity_name")),
                    rs.getInt("complexity_id"));
            }
        }
        
        getAllWords();
        
        today = DateTimeUtils.getCurrentLocalDate();
        getRepeatWords();
    }
    
    // creates database from scratch using predefined script
    // deletes all table and data if exists
    // throws Exception if something goes wrong
    private void createDatabase(String dbName, boolean importDb)
        throws Exception {
        // drop database if exists
        try (Statement statement = con.createStatement()) {
            statement.executeUpdate("DROP DATABASE IF EXISTS " + dbName);
            
            int result = statement.executeUpdate("CREATE DATABASE " + dbName);
            if (result != 1) throw new SQLException("Error creating database");
            
            useDatabase(dbName);
            
            Path sqlScriptPath = Paths.get(getClass()
                .getResource("/resources/mysql/create_tables.sql").toURI());
            String sqlScript =  new String(Files.readAllBytes(sqlScriptPath),
                StandardCharsets.UTF_8);
            
            statement.executeUpdate(sqlScript);
        }

        // init complexity table
        try (PreparedStatement insertComplexities = con.prepareStatement(
            "INSERT INTO complexities (complexity_name, weight, privileged) " +
                "VALUES (?, ?, ?)")) {
            for (WordComplexity wc : WordComplexity.sortedValues()) {
                insertComplexities.setString(1, wc.name());
                insertComplexities.setInt(2, wc.getWeight());
                insertComplexities.setBoolean(3, wc.isPrivileged());
                insertComplexities.executeUpdate();
            }
        }
        
        con.commit();
        
        if (importDb) importFileModel();
        
        System.out.println("Created db");
    }
    
    @Override
    public void backup() {
        try {
            new MysqlModelToFileModel(EnglishWords.PROJECT_DIRECTORY, con)
                .backup();
        } catch (Exception ex) {
            System.err.println("Error while buckuping file model");
        }
    }
    
    private void importFileModel() throws IOException, SQLException {
        FileModelToMysqlModel importer = new FileModelToMysqlModel(con);
        importer.insert();
    }
    
    private void fillDatabaseWithDefaults() throws Exception {
        LocalDate firstBundle = DateTimeUtils.getCurrentLocalDate();
        
        int normalComplexityId;
        int firstBundleId;
        
        try (Statement statement = con.createStatement()) {
            ResultSet rs = statement.executeQuery(
                "SELECT complexity_id FROM complexities WHERE complexity_name='" +
                    WordComplexity.NORMAL.name() + "'");
            rs.next();
            normalComplexityId = rs.getInt("complexity_id");
            
            firstBundleId = statement.executeUpdate(
                "INSERT INTO bundles (bundle_name) VALUES ('" + firstBundle + "')",
                Statement.RETURN_GENERATED_KEYS);
        }
        
        try (PreparedStatement insertWords = con.prepareStatement(
            "INSERT INTO words (word, translation, synonyms, bundle_id, complexity_id) " +
                "VALUES (?, ?, ?, ?, ?)")) {
            for (String line : Files.readAllLines(Paths.get(getClass()
                .getResource("/resources/irregular_verbs").toURI()),
                StandardCharsets.UTF_8)) {
                String[] tokens = line.split("\\t+");
                insertWords.setString(1, tokens[0]);
                insertWords.setString(2, tokens[1]);
                insertWords.setString(3, tokens[2]);
                insertWords.setInt(4, firstBundleId);
                insertWords.setInt(5, normalComplexityId);
                insertWords.executeUpdate();
            }
        }
        
        con.commit();
    }
    
    private void useDatabase(String dbName) throws SQLException {
        try (Statement useStatement = con.createStatement()) {
            useStatement.executeUpdate("USE " + dbName);
        }
    }
    
    @Override
    public Word getWordInstance(String wordToSearch) {
        Objects.requireNonNull(wordToSearch);
        
        Word word = wordMap.get(wordToSearch);
        if (word != null) return word;
        
        String query = getWordQuery("WHERE word = ?");
        
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wordToSearch);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) return getWordFromResultSet(rs);
            else return null;
        } catch (SQLException sqle) {
            defaultExceptionHandler(sqle);
            return null;
        }
    }
    
    @Override
    public boolean wordExists(String word) {
        Objects.requireNonNull(word);
        
        String query = "SELECT 1 FROM words WHERE word IN (?, ?)";
        word = word.replaceAll("^to ", "");
        
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, word);
            ps.setString(2, "to " + word);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException sqle) {
            defaultExceptionHandler(sqle);
            return false;
        }
    }
    
    @Override
    public final Map<String, Word> getAllWords() {
        if (!wordMap.isEmpty()) return wordMap;
        
        String query = GET_WORD_QUERY_WITHOUT_CONDITION;
        
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Word word = getWordFromResultSet(rs);
                wordMap.put(word.getWord(), word);
            }
            
            return Collections.unmodifiableMap(wordMap);
        } catch (SQLException sqle) {
            defaultExceptionHandler(sqle);
            return Collections.emptyMap();
        }
    }
    
    @Override
    public int getTodayIterations() {
        String query =
            "SELECT iterations FROM daily_iterations WHERE local_date = ?";
        
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setDate(1, Date.valueOf(today));
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) return rs.getInt("iterations");
            else return 0;
        } catch (SQLException sqle) {
            defaultExceptionHandler(sqle);
            return 0;
        }
    }
    
    @Override
    public long getTotalIterations() {
        String query = "SELECT SUM(times_picked) AS sum FROM words";
        
        try (Statement statement = con.createStatement()) {
            ResultSet rs = statement.executeQuery(query);
            rs.next();
            return rs.getLong("sum");
        } catch (SQLException sqle) {
            defaultExceptionHandler(sqle);
            return 0;
        }
    }
    
    @Override
    public int getThisWeekIterations() {
        return getIterationsForDays(today.getDayOfWeek().getValue());
    }
    
    @Override
    public int getIterationsForDays(int n) {
        if (n <= 1) return 0;
        
        LocalDate startDate = today.minusDays(n - 1);
        LocalDate endDate = today.minusDays(1);
        
        String query = "SELECT SUM(iterations) FROM daily_iterations " +
            "WHERE local_date BETWEEN ? AND ?";
        
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setDate(1, Date.valueOf(startDate));
            ps.setDate(2, Date.valueOf(endDate));
            
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
            else return 0;
        } catch (SQLException sqle) {
            defaultExceptionHandler(sqle);
            return 0;
        }
    }
    
    @Override
    public void setTodayIterations(int iter) {
        String query = "INSERT INTO daily_iterations (local_date, iterations) " +
            "VALUES (?, ?) ON DUPLICATE KEY UPDATE iterations = VALUES(iterations)";
        
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setDate(1, Date.valueOf(today));
            ps.setInt(2, iter);
            ps.executeUpdate();
            
            con.commit();
        } catch (SQLException sqle) {
            rollback();
            defaultExceptionHandler(sqle);
        }
    }
    
    @Override
    public Collection<Word> getBundle(LocalDate bundle) {
        if (bundle == null) return Collections.emptyList();
        
        String query = getWordQuery("WHERE bundle_date = ?");
        Collection<Word> words = new ArrayList<>(40);
        
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setDate(1, Date.valueOf(bundle));
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Word word = getWordFromResultSet(rs);
                words.add(word);
            }
            
            return words;
        } catch (SQLException sqle) {
            defaultExceptionHandler(sqle);
            return Collections.emptyList();
        }
    }
    
    @Override
    public boolean deleteWord(String wordToDelete) {
        String query = "DELETE FROM words WHERE word = ?";
        
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wordToDelete);
            int result = ps.executeUpdate();
            
            if (result != 0) {
                con.commit();
                wordMap.remove(wordToDelete);
                return true;
            }
            
            return false;
        } catch (SQLException sqle) {
            rollback();
            defaultExceptionHandler(sqle);
            return false;
        }
    }
    
    @Override
    public Map<String, FutureWord> getFutureWords() {
        Map<String, FutureWord> map = new TreeMap<>();
        String query = "SELECT future_word, priority, " +
            "DATE_FORMAT(date_added, '%d.%m.%Y') AS 'date_added', " +
            "DATE_FORMAT(date_changed, '%d.%m.%Y') AS 'date_created' " +
            "FROM future_words";
        
        try (Statement statement = con.createStatement()) {
            ResultSet rs = statement.executeQuery(query);
            
            while (rs.next()) {
                FutureWord fw = new FutureWord(rs.getString("future_word"));
                fw.setPriority(rs.getInt("priority"));
                fw.setDateAdded(rs.getString("date_added"));
                fw.setDateChanged(rs.getString("date_created"));
                map.put(fw.getWord(), fw);
            }
            
            return map;
        } catch (SQLException sqle) {
            defaultExceptionHandler(sqle);
            return Collections.emptyMap();
        }
    }
    
    @Override
    public void updateFutureWord(String word) {
        String query = "INSERT INTO future_words " +
            "(future_word, priority, date_added, date_changed) " +
            "VALUES(?, 0, CURDATE(), CURDATE()) " +
            "ON DUPLICATE KEY UPDATE " +
            "priority = priority + 1, date_changed = CURDATE()";
        
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, word);
            ps.executeUpdate();
            
            con.commit();
        } catch (SQLException sqle) {
            rollback();
            defaultExceptionHandler(sqle);
        }
    }
    
    @Override
    public void deleteFutureWords(Collection<String> words) {
        String query = "DELETE FROM future_words WHERE future_word = ?";
        
        try (PreparedStatement ps = con.prepareStatement(query)) {
            words.forEach(word -> {
                try {
                    ps.setString(1, word);
                    ps.executeUpdate();
                } catch (SQLException sqle) {
                    rollback();
                    defaultExceptionHandler(sqle);
                }
            });
            
            con.commit();
        } catch (SQLException sqle) {
            rollback();
            defaultExceptionHandler(sqle);
        }
    }
    
    private Collection<Word> getRepeatWordsWithCondition(String condition) {
        String query = getWordQuery("JOIN repeat_words ON " +
            "words.word_id = repeat_words.word_id " + condition);
        
        Collection<Word> words = new ArrayList<>();
        
        try (Statement statement = con.createStatement()) {
            ResultSet rs = statement.executeQuery(query);
            
            while (rs.next()) {
                Word w = getWordFromResultSet(rs);
                words.add(w);
            }
            
            return words;
        } catch (SQLException sqle) {
            defaultExceptionHandler(sqle);
            return Collections.emptyList();
        }
    }
    
    @Override
    public Collection<Word> getRepeatWords() {
        String query = "WHERE date_added >= DATE(DATE_SUB('" + today +
            "', INTERVAL " + REPEAT_DAYS_TO_EXPIRE + " DAY))";
        
        Collection<Word> repeatWords = getRepeatWordsWithCondition(query);
        repeatWords.forEach(w -> w.setWordType(WordType.REPEAT));
        
        return repeatWords;
    }
    
    @Override
    public Collection<Word> getExpiredRepeatWords() {
        String query = "WHERE date_added = DATE(DATE_SUB('" + today +
            "', INTERVAL " + (REPEAT_DAYS_TO_EXPIRE + 1) + " DAY))";
        return getRepeatWordsWithCondition(query);
    }
    
    @Override
    public void addRepeatWord(String word) {
        LocalDate insertDate = today.plusDays(1);
        String query = "INSERT IGNORE INTO repeat_words (word_id, date_added) " +
            "SELECT word_id, ? FROM words WHERE word = ?";
        
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setDate(1, Date.valueOf(insertDate));
            ps.setString(2, word);
            ps.executeUpdate();
            
            con.commit();
        } catch (SQLException sqle) {
            rollback();
            defaultExceptionHandler(sqle);
        }
    }
    
    @Override
    public void deleteRepeatWord(String word) {
        String query = "DELETE FROM repeat_words WHERE word_id IN " +
            "(SELECT word_id FROM words WHERE word = ?) " +
            "ORDER BY date_added DESC LIMIT 1";
        
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, word);
            ps.executeUpdate();
            
            con.commit();
        } catch (SQLException sqle) {
            rollback();
            defaultExceptionHandler(sqle);
        }
    }
    
    @Override
    public boolean addNewWord(Word word) {
        String query = "INSERT INTO words (word, translation, synonyms, " +
            "bundle_id, times_picked, last_picked_timestamp, complexity_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        int complexityId = getIdFromComplexity(word.getComplexity());
        
        try (PreparedStatement ps = con.prepareStatement(query)) {
            LocalDate bundle = word.getBundle();
            int bundleId = getBundleId(bundle);
            if (bundleId == -1) bundleId = registerNewBundle(bundle);
            
            ps.setString(1, word.getWord());
            ps.setString(2, word.getTranslation());
            ps.setString(3, word.getSynonyms());
            ps.setInt(4, bundleId);
            ps.setInt(5, word.getTimesPicked());
            ps.setLong(6, System.currentTimeMillis());
            ps.setInt(7, complexityId);
            
            int result = ps.executeUpdate();
            
            if (result != 0) {
                con.commit();
                wordMap.put(word.getWord(), word);
                
                return true;
            }
            
            return false;
        } catch (SQLException sqle) {
            rollback();
            defaultExceptionHandler(sqle);
            return false;
        }
    }
    
    @Override
    public boolean isExistingBundle(LocalDate bundle) {
        Objects.requireNonNull(bundle);
        
        String query = "SELECT 1 FROM words JOIN bundles ON " +
            "words.bundle_id = bundles.bundle_id WHERE bundle_date = ?";
        
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setDate(1, Date.valueOf(bundle));
            return ps.executeQuery().next();
        } catch (SQLException sqle) {
            defaultExceptionHandler(sqle);
            return false;
        }
    }
    
    @Override
    public LocalDate getLastBundleName() {
        String query = "SELECT DISTINCT bundle_date FROM bundles " +
            "JOIN words ON bundles.bundle_id = words.bundle_id " +
            "ORDER BY bundle_date DESC LIMIT 1";
        
        try (Statement statement = con.createStatement()) {
            ResultSet rs = statement.executeQuery(query);
            if (rs.next()) return rs.getDate("bundle_date").toLocalDate();
            else return null;
        } catch (SQLException sqle) {
            defaultExceptionHandler(sqle);
            return null;
        }
    }
    
    @Override
    public LocalDate getPenultimateBundleName() {
        String query = "SELECT DISTINCT bundle_date FROM bundles " +
            "JOIN words ON bundles.bundle_id = words.bundle_id " +
            "ORDER BY bundle_date DESC LIMIT 1, 1";
        
        try (Statement statement = con.createStatement()) {
            ResultSet rs = statement.executeQuery(query);
            if (rs.next()) return rs.getDate("bundle_date").toLocalDate();
            else return null;
        } catch (SQLException sqle) {
            defaultExceptionHandler(sqle);
            return null;
        }
    }
    
    @Override
    public NavigableSet<LocalDate> allBundlesSorted() {
        TreeSet<LocalDate> bundles = new TreeSet<>();
        String query = "SELECT DISTINCT bundle_date FROM bundles " +
            "JOIN words ON bundles.bundle_id = words.bundle_id";
        
        try (Statement statement = con.createStatement()) {
            ResultSet rs = statement.executeQuery(query);
            while (rs.next())
                bundles.add(rs.getDate("bundle_date").toLocalDate());
            return bundles;
        } catch (SQLException sqle) {
            defaultExceptionHandler(sqle);
            return Collections.emptyNavigableSet();
        }
    }
    
    @Override
    public String getDefinition(String word) {
        String query = "SELECT definition FROM words WHERE word = ?";
        
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, word);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("definition");
            else return null;
        } catch (SQLException sqle) {
            defaultExceptionHandler(sqle);
            return null;
        }
    }
    
    @Override
    public void setDefinition(String word, String definition) {
        String query = "UPDATE words SET definition = ? WHERE word = ?";
        
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, definition);
            ps.setString(2, word);
            ps.executeUpdate();
            con.commit();
        } catch (SQLException sqle) {
            rollback();
            defaultExceptionHandler(sqle);
        }
    }
    
    @Override
    public void setComplexity(String word, WordComplexity complexity) {
        String query = "UPDATE words SET complexity_id = ? WHERE word = ?";
        
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, getIdFromComplexity(complexity));
            ps.setString(2, word);
            ps.executeUpdate();
            con.commit();
        } catch (SQLException sqle) {
            rollback();
            defaultExceptionHandler(sqle);
        }
    }
    
    @Override
    public void setLastPickedTimestamp(String word, long timestamp) {
        String query = "UPDATE words SET last_picked_timestamp = ?, "
            + "times_picked = times_picked + 1 WHERE word = ?";
        
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setLong(1, timestamp);
            ps.setString(2, word);
            ps.executeUpdate();
            con.commit();
        } catch (SQLException sqle) {
            rollback();
            defaultExceptionHandler(sqle);
        }
    }
    
    @Override
    public void editWords(Map<Word, Word> map) {
        for (Map.Entry<Word, Word> entry : map.entrySet()) {
            try {
                Word originalWord = entry.getValue();
                Word editedWord = entry.getKey();
                
                if (!originalWord.getWord().equals(editedWord.getWord())) {
                    String wordToDelete = originalWord.getWord();
                    deleteWord(wordToDelete);
                    wordMap.remove(wordToDelete);
                    
                    addNewWord(editedWord); // commits changes
                    
                    continue; // move to the next word
                }
                
                String query = "UPDATE words SET translation = ? , synonyms = ?, " +
                    "bundle_id = ?, times_picked = ?, last_picked_timestamp = ?, " +
                    "complexity_id = ?, definition = ? WHERE word = ?";
                
                try (PreparedStatement ps = con.prepareStatement(query)) {
                    int complexityId = getIdFromComplexity(editedWord.getComplexity());
                    LocalDate bundle = editedWord.getBundle();
                    
                    int bundleId = getBundleId(bundle);
                    if (bundleId == -1) bundleId = registerNewBundle(bundle);
                    
                    ps.setString(1, editedWord.getTranslation());
                    ps.setString(2, editedWord.getSynonyms());
                    ps.setInt(3, bundleId);
                    ps.setInt(4, editedWord.getTimesPicked());
                    ps.setLong(5, editedWord.getLastPickedTimestamp());
                    ps.setInt(6, complexityId);
                    ps.setString(7, getDefinition(editedWord.getWord()));
                    ps.setString(8, editedWord.getWord());
                    
                    ps.executeUpdate();
                    
                    con.commit();
                    
                    wordMap.put(editedWord.getWord(), editedWord);
                }
            } catch (SQLException sqle) {
                rollback();
                defaultExceptionHandler(sqle);
            }
        }
    }
    
    @Override
    public boolean addNewBundle(LocalDate bundle, Collection<Word> words) {
        if (isExistingBundle(bundle)) return false;
        
        words.forEach(word -> {
            word.setBundle(bundle);
            addNewWord(word);
        });
        
        return true;
    }
    
    @Override
    public boolean isEmpty() {
        String query = "SELECT COUNT(word) FROM words";
        try (Statement statement = con.createStatement()) {
            ResultSet rs = statement.executeQuery(query);
            rs.next();
            return rs.getInt(1) == 0;
        } catch (SQLException sqle) {
            defaultExceptionHandler(sqle);
            return true;
        }
    }
    
    @Override
    public void destroy() {
        try (PreparedStatement ps = con.prepareStatement(
            "DROP DATABASE IF EXISTS ?")) {
            ps.setString(1, dbName);
            ps.executeUpdate();
        } catch (SQLException ex) { } 
        System.err.println("Model has been completely destroyed");
    }
}
