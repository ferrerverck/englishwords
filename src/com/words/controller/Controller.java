package com.words.controller;

import com.words.controller.callbacks.ConsoleCallback;
import com.words.controller.definition.AutomaticDefinition;
import com.words.controller.definition.DictOrgDefinition;
import com.words.controller.futurewords.FutureWord;
import com.words.controller.futurewords.FwAlreadyUsedException;
import com.words.controller.futurewords.FwEmptyWordException;
import com.words.controller.futurewords.FwTimeoutException;
import com.words.controller.preferences.ApplicationLocationException;
import com.words.controller.preferences.ApplicationPreferences;
import com.words.controller.preferences.AutoMode;
import com.words.controller.preferences.SoundPreferences;
import com.words.controller.preferences.TooltipPreferences;
import com.words.controller.sound.PlayMp3;
import com.words.controller.sound.PlayWav;
import com.words.controller.sound.downloadmp3.AlreadyDownloadingException;
import com.words.controller.sound.downloadmp3.Mp3Downloader;
import com.words.controller.words.Word;
import com.words.controller.words.wordkinds.display.WordDisplayType;
import com.words.controller.words.wordkinds.WordType;
import com.words.model.Model;
import com.words.controller.utils.DateTimeUtils;
import com.words.controller.utils.Utils;
import com.words.controller.words.WordFactory;
import com.words.controller.words.wordkinds.WordComplexity;
import com.words.controller.words.wordkinds.display.strategy.DisplayStrategy;
import com.words.controller.words.wordkinds.display.strategy.WordDisplayFactory;
import com.words.controller.words.wordpool.WordPool;
import com.words.controller.words.wordpool.pickstrategy.PickStrategyFactory;
import com.words.main.EnglishWords;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Controller
 * @author vlad
 */
public final class Controller {
    
    private static final Random RAND = Utils.RANDOM;
    
    public static final int MAX_GENERATED_NEW_WORDS = 50;
    
    // sound directory with downloaded mp3 files
    public static final String SOUND_DIR_NAME = "sound";
    
    private static final int EBBINGHAUS_WORD_COUNT = 4;
    
    private static final long MIN_DELAY = 800L; // min delay between words
    
    private final Model model;
    
    private final WordPool wordPool;
    
    private int iters;
    private Word currentWord;
    
    private final WordDisplayFactory wordDisplayFactory;
    private DisplayStrategy wordDisplayStrategy;
    private WordDisplayType wordType = WordDisplayType.BOTH;
    
    private final ApplicationPreferences settings;
    private SoundPreferences soundPrefs;
    private TooltipPreferences tooltipPrefs;
    
    // Single threaded executor to do background tasks
    private final ExecutorService controllerExec;
    
    // Auto mode options
    private AutoMode autoMode;
    
    // Auto action. Runs in a separate thread.
    private Runnable autoAction;
    
    // scheduling service for auto action
    private final ScheduledExecutorService autoExec;
    // used to cancel tasks
    private ScheduledFuture<?> futureTask;
    
    // gui callbacks should be executed on a gui thread
    // which is a liability of programmer
    private BiConsumer<LocalDate, String> showBundleCb = null;
    private BiConsumer<Word, Object> repeatWordCb = null;
    private BiConsumer<String, String> showDefinitionCb = null;
    private Consumer<Word> hintCb = null;
    private Consumer<Boolean> stateChangedCb = null;
    private Runnable modelChangedCb = null;
    private Consumer<Word> verifyCharsCb = null;
    
    private final Console console;
    
    // for statistics to display in the console
    private int regularAmount = 0;
    private int repeatAmount = 0;
    private int randomAmount = 0;
    private int ebbinghausAmount = 0;
    
    // used to prevent clicking too fast
    private long lastTimestamp = 0L;
    
    private final Path projectDirectory;
    private final Path soundDirectory;
    
    private AutomaticDefinition definitionDownloader = null;
    private Mp3Downloader mp3Downloader = null;
    
    public Controller(Model mdl, Path projectDirectory) throws IOException {
        this.model = mdl;
        
        this.projectDirectory = projectDirectory;
        soundDirectory = projectDirectory.resolve(SOUND_DIR_NAME);
        if (Files.notExists(soundDirectory))
            Files.createDirectories(soundDirectory);
        
        settings = new ApplicationPreferences(projectDirectory.toString());
        soundPrefs = settings.getSoundPreferences();
        tooltipPrefs = settings.getTooltipPreferences();
        console = new Console();
        
        autoMode = AutoMode.OFF;
        autoExec = Executors.newSingleThreadScheduledExecutor();
        
        currentWord = WordFactory.newWord();
        
        // create new if doesn't exist
        if (model.isEmpty()) createDefaultWords(model);
        
        iters = model.getTodayIterations();
        
        wordPool = new WordPool(
            PickStrategyFactory.getStandardEverydayStrategy());
        wordPool.setDrainedWordPoolAction(() -> resetPoolToLastBundle());
        
        wordDisplayFactory = new WordDisplayFactory();
        wordDisplayStrategy = wordDisplayFactory.getDefaultStrategy();
        
        // init word pool
        if (DateTimeUtils.getDayOfWeek() != DayOfWeek.SUNDAY) {
            resetPoolToLastBundle();
        } else {
            wordPool.setPickStrategy(PickStrategyFactory.getStandardStrategy(
                Duration.ofDays(1L), 5));
            addAllWordsToPool();
        }
        
        controllerExec = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() + 1);
        
        // backup and clean
        if (Utils.RANDOM.nextInt(100) == 93) backup(() -> {
            try { // delay backuping
                Thread.sleep(10_000);
            } catch (InterruptedException ex) { }
        }, null);
        
        // clean up everything
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            autoExec.shutdownNow();
            controllerExec.shutdown();
            
            try {
                // await termination to save proress
                controllerExec.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException ex) { }
            
            System.out.println("Cleaned up controller");
        }));
    }
    
    /**
     * Back up model.Any argument can be null if no action required.
     * @param pre action which should be executed before backuping
     * @param post action executed after backuping. Used to notify gui on
     *             completion. Should be executed in a gui thread.
     */
    public void backup(Runnable pre, Runnable post) {
        controllerExec.execute(() -> {
            if (pre != null) pre.run();
            console.addErrorMessage("Starting backing up");
            
            synchronized(model) {
                model.backup();
            }
            
            console.addErrorMessage("Backup has been completed");
            if (post != null) post.run();
        });
    }
    
    /**
     * Sound preferences getter.
     * @return current sound mode
     */
    public SoundPreferences getSoundPreferences() {
        return soundPrefs;
    }
    
    /**
     * Set controllers sound preferences
     * @param sp sound mode to set
     */
    public final void setSoundPreferences(SoundPreferences sp) {
        soundPrefs = sp;
    }
    
    /**
     * Tooltip preferences getter.
     * @return tooltip show mode
     */
    public TooltipPreferences getTooltipPreferences() {
        return tooltipPrefs;
    }
    
    /**
     * Set tooltip mode.
     * @param tp enum const to set
     */
    public final void setTooltipPreferences(TooltipPreferences tp) {
        tooltipPrefs = tp;
    }
    
    /**
     * Auto mode getter.
     * @return enum constant
     */
    public AutoMode getAutoMode() {
        return autoMode;
    }
    
    public void rescheduleTimer() {
        if (futureTask != null) {
            futureTask.cancel(false);
            futureTask = null;
        }
        
        if ((autoMode != AutoMode.OFF) && (autoAction != null)) {
            futureTask = autoExec.schedule(autoAction, autoMode.getDelay(),
                AutoMode.getTimeUnits());
        }
    }
    
    /**
     * Next word. Executes gui update at the end.
     * Required to set stateChangedCallback.
     */
    public void nextWord() {
        long timestamp = System.currentTimeMillis();
        if (timestamp - lastTimestamp < MIN_DELAY) return;
        lastTimestamp = timestamp;
        
        rescheduleTimer();
        
        iters++;
        
        Word previousWord = currentWord;
        currentWord = wordPool.nextWord(timestamp, previousWord.getWord());
        wordType = wordDisplayStrategy.getNextType(iters, currentWord);
        
        updateLastPickedTimestamp(currentWord.getWord(), timestamp);
        
        stateChanged(true);
        
        console.addWordMessage(String.format("%s", getDisplayText()));
        
        autoPronounceWord();
        
        verifyKnowledge();
    }
    
    // verify knowledge by showing verify dialogs with small probability
    // if word is complex enough or is a repeat word
    private void verifyKnowledge() {
//        Word word = model.getWordInstance(currentWord.getWord());
        
//        if (wordType.isTranslation()) {
//            if () {
//                verifyChars(word);
//            }
//        }
        
        if (wordType.isTranslation() && !currentWord.isSingleWord() &&
            currentWord.getComplexity().isNotEasierThan(WordComplexity.TOUGH) &&
            RAND.nextInt(20) == 19) {
            Word word = model.getWordInstance(currentWord.getWord());
            verifyChars(word);
        }
    }
    
    private void updateLastPickedTimestamp(String word, long timestamp) {
        controllerExec.execute(() ->
            model.setLastPickedTimestamp(word, timestamp));
    }
    
    /**
     * Get text for displaying on the main button.
     * @return text to display
     */
    public String getDisplayText() {
        if (!hasCurrentWord()) return EnglishWords.TITLE;
        return wordType.getText(currentWord);
    }
    
    /**
     * Current number of iterations.
     * @return current number of iterations
     */
    public int getIters() {
        return iters;
    }
    
    /**
     * Check if mp3 already exists.
     * Caches mp3-file references in the word instance.
     * @param word word to check
     * @return false if it's required to download mp3
     */
    private boolean mp3Exists(Word word) {
        if (word.getMp3File() != null) return true;
        
        Path mp3File = soundDirectory.resolve(
            Utils.getMp3FileName(word.getWord()));
        
        if (Files.exists(mp3File)) {
            word.setMp3File(mp3File);
            return true;
        } else {
            downloadRequiredData(word);
            return false;
        }
    }
    
    private void autoPronounceWord() {
        if (!hasCurrentWord()) return;
        
        if (soundPrefs == SoundPreferences.MUTE) return;
        
        if (soundPrefs == SoundPreferences.NEXT_WORD) {
            PlayMp3.nextWord();
            return;
        }
        
        Word word = currentWord;
        if (mp3Exists(word)) {
            if (soundPrefs == SoundPreferences.ONLY_ENGLISH) pronounceCurrentWord();
            else wordType.sound(word);
        }
    }
    
    /**
     * Pronounce current word.
     */
    public void pronounceCurrentWord() {
        pronounceWord(currentWord);
    }
    
    /**
     * Pronounce specific word. Used in bundle dialog.
     * @param word word
     */
    public void pronounceWord(Word word) {
        if (word == null || word.getWord() == null) return;
        if (mp3Exists(word)) PlayMp3.playFile(word.getMp3File());
    }
    
    // download required data
    // sound and automatic definition
    private void downloadRequiredData(Word word) {
        // done to prevent wrappers which can change word contents
        final Word wordToUpdate = model.getWordInstance(word.getWord());
        if (wordToUpdate == null) return;
        
        if (wordToUpdate.getMp3File() != null) return;
        
        if (mp3Downloader == null)
            mp3Downloader = new Mp3Downloader(soundDirectory);
        
        PlayWav.exclamation();
        
        controllerExec.execute(() -> {
            try {
                Path mp3File = mp3Downloader.download(wordToUpdate.getWord());
                wordToUpdate.setMp3File(mp3File);
                console.addErrorMessage("Downloaded sound file for «" +
                    wordToUpdate.getWord() + "»");
            } catch (AlreadyDownloadingException ex) { }
        });
        
        controllerExec.execute(() -> {
            downloadDefinition(wordToUpdate.getWord());
            console.addErrorMessage("Definition for «" +
                wordToUpdate.getWord() + "» has been downloaded");
        });
    }
    
    /**
     * Get current word.
     * @return current word
     */
    public Word getCurrentWord() {
        return currentWord;
    }
    
    /**
     * Get word display type.
     * Used in a complexity gui to enable/disable buttons.
     * @return what will be displayed on the main button
     */
    public WordDisplayType getWordDisplayType() {
        return wordType;
    }
    
    /**
     * Application location.
     * @return Point instance as frame location
     * @throws ApplicationLocationException if no prefs exist
     */
    public int[] getApplicationLocation() throws ApplicationLocationException {
        return settings.getLocation();
    }
    
    /**
     * Saving preferences.
     * @param x x application coordinate
     * @param y y application coordinate
     */
    public void saveApplicationSettings(int x, int y) {
        settings.setLocation(x, y);
        settings.setSoundPreferences(soundPrefs);
        settings.setTooltipPreferences(tooltipPrefs);
        
        int oldIters = model.getTodayIterations();
        if (oldIters < iters) model.setTodayIterations(iters);
        
        settings.setWordLog(console.getWordLog());
    }
    
    /**
     * Set auto action.
     * @param action timer action
     */
    public void setAutoAction(Runnable action) {
        autoAction = action;
    }
    
    /**
     * Start or stop timer with predefined action.
     * @param amp new auto mode
     */
    public void changeAutoMode(AutoMode amp) {
        autoMode = amp;
        console.addInfoMessage("Setting new auto mode to " + autoMode.name());
        rescheduleTimer();
    }
    
    /**
     * Switch to the next auto mode.
     */
    public void nextAutoMode() {
        changeAutoMode(autoMode.nextAutoMode());
    }
    
    /**
     * Get words bundle.
     * @param bundle bundle
     * @return words or empty list
     */
    public Collection<Word> getWordsBundle(LocalDate bundle) {
        return model.getBundle(bundle);
    }
    
    /**
     * Show words bundle according to parameter.
     * @param bundle name of the bundle to show
     * @param word word to select
     */
    public void showWordsBundle(LocalDate bundle, String word) {
        if (showBundleCb != null) showBundleCb.accept(bundle, word);
    }
    
    /**
     * Setter for showBundleAction
     * @param cb callback to set
     */
    public void setShowBundleCallback(BiConsumer<LocalDate, String> cb) {
        showBundleCb = cb;
    }
    
    /*
    * Saves word bundle.
    * @param bundle name
    * @param list words
    */
    private void saveWordBundle(LocalDate bundle, Collection<Word> list) {
        controllerExec.execute(() -> {
            model.addNewBundle(bundle, list);
            
            PlayWav.notification();
            
            modelChanged();
            showWordsBundle(bundle, null);
        });
    }
    
    /**
     * Last bundle name. Required to initialize bundle panel.
     * @return actual last bundle name
     */
    public LocalDate getLastBundleName() {
        return model.getLastBundleName();
    }
    
    /**
     * Future words
     * @return future words
     */
    public Collection<String> getFutureWords() {
        Collection<String> futureList = new LinkedList<>();
        futureList.addAll(model.getFutureWords().keySet());
        return futureList;
    }
    
    /**
     * Update future words.
     * @param word word to add, can be represented by word and translation
     *             delimited by " - " (space hyphen space)
     *             translation is optional
     * @throws FwEmptyWordException if word is effectively empty
     * @throws FwAlreadyUsedException if the word is already in use
     * @throws FwTimeoutException if not enough time has passed
     */
    public void updateFutureWords(String word) throws
        FwEmptyWordException, FwAlreadyUsedException, FwTimeoutException {
        word = word.trim().toLowerCase();
        
        if (word.isEmpty()) throw new FwEmptyWordException();
        
        if (wordExists(word.split(" - ")[0]))
            throw new FwAlreadyUsedException();
        
        if (model.getFutureWords().containsKey(word) &&
            !model.getFutureWords().get(word).hasEnoughTimePassed())
            throw new FwTimeoutException();
        
        executeFutureWordsUpdate(word);
    }
    
    // Update future words in a separate thread.
    private void executeFutureWordsUpdate(final String word) {
        controllerExec.execute(() -> {
            model.updateFutureWord(word);
            console.addErrorMessage("Added future word «" +
                word.split(" - ")[0] + "»");
        });
    }
    
    /**
     * Creates new word bundle.
     * @param amount number of words to generate
     * @return Word list or empty list if creation failed
     */
    public List<Word> generateNewBundle(int amount) {
        LocalDate bundle = DateTimeUtils.getCurrentLocalDate();
        List<Word> bundleList = new ArrayList<>();
        
        if (model.isExistingBundle(bundle)) return bundleList;
        
        NavigableSet<FutureWord> treeSet = new TreeSet<>();
        treeSet.addAll(model.getFutureWords().values());
        List<String> futureWordsToDelete = new ArrayList<>();
        
        for (int i = 0; i < amount; i++) {
            FutureWord fw = treeSet.pollFirst();
            if (fw == null) break;
            
            futureWordsToDelete.add(fw.getWord());
            
            if (!model.wordExists(Utils.formatString(fw.getWord().split(" - ")[0]))) {
                Word w = WordFactory.newWord();
                w.setWord(fw.getWord());
                w.setTranslation(fw.getDateAdded() + ";" +
                    fw.getDateChanged() + ";" + fw.getPriority());
                w.setBundle(bundle);
                
                bundleList.add(w);
            } else {
                console.addErrorMessage(
                    fw.getWord() + " already exists in the model");
            }
        }
        
        saveWordBundle(bundle, bundleList);
        deleteFutureWords(futureWordsToDelete);
        
        return bundleList;
    }
    
    /**
     * Delete future word.
     * @param words words to delete.
     */
    public void deleteFutureWords(Collection<String> words) {
        List<String> formattedWords =
            words.stream().map(word -> word.trim().toLowerCase())
                .filter(word -> !word.isEmpty()).collect(Collectors.toList());
        
        controllerExec.execute(() -> model.deleteFutureWords(formattedWords));
        
        formattedWords.stream().map(word -> word.split(" - ")[0])
            .forEach(word -> console.addErrorMessage(
                "FutureWord «" + word + "» has been deleted"));
    }
    
    /**
     * AllWords as list.
     * @return all words
     */
    public List<Word> getAllWordsAsList() {
        List<Word> allWordsList = new ArrayList<>();
        allWordsList.addAll(model.getAllWords().values());
        return allWordsList;
    }
    
    /**
     * Set repeat word callback to notify user about changes.
     * @param rwc BiConsumer callback. Should be executed in a gui thread.
     *            parameters of the callback are word and gui parent to show
     *            info message.
     */
    public void setRepeatWordCallback(BiConsumer<Word, Object> rwc) {
        repeatWordCb = rwc;
    }
    
    /**
     * Add/delete repeat word.
     * @param word word to mark
     * @param parent parent component or null
     */
    public void toggleRepeatWord(Word word, Object parent) {
        if (word == null || word.getWord() == null) return;
        
        if (word.getWordType() == WordType.REPEAT) {
            word.setWordType(WordType.STANDARD);
            deleteRepeatWord(word);
        } else {
            word.setWordType(WordType.REPEAT);
            addRepeatWord(word);
        }
        
        if (repeatWordCb != null) repeatWordCb.accept(word, parent);
        
        modelChanged();
    }
    
    /*
    * Add new word for future repeating.
    * @param word new repeatWord
    */
    private void addRepeatWord(final Word word) {
        Word wordToAdd = model.getWordInstance(word.getWord());
        if (wordToAdd == null) {
            console.addErrorMessage("Unable to mark word «" + word.getWord() +
                "» for repeating");
            return;
        }
        
        WordFactory.addRepeatWord(wordToAdd);
        
        controllerExec.execute(() -> model.addRepeatWord(word.getWord()));
        
        console.addErrorMessage("Marked word «" + word.getWord() +
            "» for repeating");
    }
    
    /*
    * Delete repeat word.
    * @param word word to delete
    */
    private void deleteRepeatWord(final Word word) {
        Word wordToDelete = model.getWordInstance(word.getWord());
        if (wordToDelete == null) return;
        
        WordFactory.deleteRepeatWord(wordToDelete);
        
        controllerExec.execute(() -> {
            model.deleteRepeatWord(wordToDelete.getWord());
            console.addErrorMessage("Word «" + wordToDelete.getWord() +
                "» is not marked for repeating anymore");
        });
    }
    
    /**
     * Add word to today's queue.
     * @param word new word
     */
    public void addWord(Word word) {
        wordPool.addWord(word);
    }
    
    /*
    * Delete word from controller.
    * @param word word to delete
    */
    private void deleteWordFromPool(Word word) {
        wordPool.deleteWord(word);
        WordFactory.deleteWordFromPools(word);
    }
    
    public Path getProjectDirectory() {
        return projectDirectory;
    }
    
    // Random words count depending on a day of the week
    private int getRandomWordsCount() {
        DayOfWeek dayOfWeek = DateTimeUtils.getDayOfWeek();
        return 3 + dayOfWeek.getValue() / 2;
    }
    
    /**
     * Add new word to model permanently.
     * @param word word to add
     */
    public void addNewWord(Word word) {
        // add word to the queue if it's date is the date of last bundle
        if (word.getBundle().equals(model.getLastBundleName())) {
            wordPool.addWordToQueue(word);
        }
        
        controllerExec.execute(() -> {
            model.addNewWord(word);
            console.addErrorMessage("Added «" + word.getWord() + "» to «" +
                DateTimeUtils.localDateToString(word.getBundle()) +
                "» bundle");
            modelChanged();
            PlayWav.notification();
        });
    }
    
    /**
     * Checks if word exists.
     * @param wordToSearch word
     * @return true if word is already in the model
     */
    public boolean wordExists(String wordToSearch) {
        return model.wordExists(wordToSearch);
    }
    
    /**
     * Get word instance by string representation.
     * @param wordToSearch string to search
     * @return word or null if word doesn't exist
     */
    public Word getWordInstance(String wordToSearch) {
        Objects.requireNonNull(wordToSearch);
        return model.getWordInstance(wordToSearch);
    }
    
    /**
     * Edits words permanently.
     * @param map word pairs to change
     */
    public void editWords(Map<Word, Word> map) {
        controllerExec.execute(() -> {
            model.editWords(map);
            
            String prefix = (map.size() == 1) ?
                "Updated word " : "Updated words: ";
            String message = map.keySet().stream()
                .map(w -> "«" + w.getWord() + "»")
                .collect(Collectors.joining(", ", prefix, ""));
            console.addErrorMessage(message);
            
            modelChanged();
            
            PlayWav.notification();
        });
    }
    
    /**
     * Set console callback and initialize callback.
     * @param cc callback to set
     */
    public void addConsoleCallback(ConsoleCallback cc) {
        console.addConsoleCallback(cc);
        
        // initial information
        int totalWordAmount = regularAmount + repeatAmount + randomAmount +
            ebbinghausAmount;
        if (totalWordAmount != regularAmount) {
            console.addInfoMessage(String.format("Loaded %s (%s, %s, %s, %s)",
                Utils.getNumeralWithWord(totalWordAmount, "word"),
                Utils.getNumeralWithWord(regularAmount, "regular word"),
                Utils.getNumeralWithWord(repeatAmount, "repeat word"),
                Utils.getNumeralWithWord(randomAmount, "random word"),
                Utils.getNumeralWithWord(ebbinghausAmount, "ebbinghaus word")));
        } else {
            console.addInfoMessage(String.format("Loaded %s",
                Utils.getNumeralWithWord(totalWordAmount, "word")));
        }
        
        // show loaded words
        if (wordPool.size() < 100) {
            console.addMessage("Current word pool:\n" +
                wordPool.getWordsAsString());
        }
        
        addExpiredRepeatWordInfo();
    }
    
    // show expired words
    private void addExpiredRepeatWordInfo() {
        String expiredWordString = getExpiredRepeatWords();
        
        if (expiredWordString.isEmpty()) return;
        
        console.addErrorMessage("Expired repeat words: " + expiredWordString);
    }
    
    /**
     * Get expired repeat words for today.
     * @return String of concatenated repeat words or empty string
     */
    public String getExpiredRepeatWords() {
        Collection<Word> expiredWords = model.getExpiredRepeatWords();
        
        if (expiredWords.isEmpty()) return "";
        
        return expiredWords.stream().map(Word::getWord)
            .collect(Collectors.joining(", "));
    }
    
    /**
     * Add loading statistics to the application.
     * Sends new information message to the console
     * @param nanos loading time in nanoseconds
     */
    public void addLoadingStatistics(long nanos) {
        String homeDirMessage = "Home directory path: " + projectDirectory;
        long[] runStats = settings.getAverageLoadingTime(nanos);
        
        console.addInfoMessage(homeDirMessage);
        console.addInfoMessage(String.format("Total loading time: %.2f seconds",
            1d * nanos / 1e9));
        console.addErrorMessage(String.format(
            "Application has been started %d times with average loading time " +
                "of %.2f seconds",
            runStats[1], 1d * runStats[0] / 1e9));
        console.addErrorMessage("Current iteration number: " + iters);
//        console.addEmptyLine();
        console.addMessage("********************************************");
        
        console.setWordLog(settings.getWordLog());
    }
    
    public NavigableSet<LocalDate> allBundlesSorted() {
        return model.allBundlesSorted();
    }
    
    /**
     * Modify current word pull.
     * @param bundle bundle
     */
    public void addBundleToPool(LocalDate bundle) {
        wordPool.clear();
//        wordPool.addWords(model.getBundle(bundle));
        WordFactory.addWordsToPool(model.getBundle(bundle), wordPool);
        
        regularAmount = wordPool.size();
        addAuxiliaryWords();
    }
    
    /**
     * Reset word pool and add all words.
     */
    public void addAllWordsToPool() {
        if (wordPool.size() >= model.getAllWords().size()) return;
        
        wordPool.clear();
        
        WordFactory.addWordsToPool(model.getAllWords().values(), wordPool);
        
        randomAmount = 0;
        repeatAmount = 0;
        ebbinghausAmount = 0;
        regularAmount = wordPool.size();
        
        console.addErrorMessage("Added all available words to the pool");
    }
    
    /**
     * Reset word pool and add last bundle
     * + repeat words and random words.
     */
    public void resetPoolToLastBundle() {
        wordPool.clear();
        addBundleToPool(model.getLastBundleName());
        console.addErrorMessage("Reseted word pool to standard suite");
        
        // add complex words of previous week
        Collection<Word> lastRedWords =
            model.getBundle(model.getPenultimateBundleName()).stream()
                .filter(w -> w.getComplexity().isNotEasierThan(WordComplexity.COMPLEX))
                .filter(w -> w.getWordType() != WordType.REPEAT)
                .map(w -> WordFactory.getSelfDeletingWord(w, wordPool, null, console))
                .collect(Collectors.toSet());
        
        regularAmount += lastRedWords.size();
        wordPool.addWords(lastRedWords);
    }
    
    /**
     * Check online definition for specified word.
     * Uses executorService since blocking call to socket.
     * Register definition callback {setShowDefinitionCallback} if you
     * want to show message in your gui.
     * @param word word to check
     */
    public void showDefinition(String word) {
        if (word == null) return;
        
        controllerExec.execute(() -> {
            String normalizedWord = Utils.normalizeFor3rdParties(word);
            
            String definition = model.getDefinition(word);
            if (definition == null) definition = downloadDefinition(word);
            
            PlayWav.notification();
            
            console.addInfoMessage("Definition of «" + normalizedWord + "»:\n"
                + definition);
            
            if (showDefinitionCb != null)
                showDefinitionCb.accept(normalizedWord, definition);
        });
    }
    
    /**
     * Sets default callback to show freshly downloaded definition
     * in the console.
     * @param cb new callback
     */
    public void setShowDefinitionCallback(BiConsumer<String, String> cb) {
        showDefinitionCb = cb;
    }
    
    /**
     * Get definition for word.
     * @param word word to check
     * @return definition or null if definition is not found
     */
    public String getDefinition(Word word) {
        return model.getDefinition(word.getWord());
    }
    
    private String downloadDefinition(String word) {
        if (definitionDownloader == null) definitionDownloader = new DictOrgDefinition();
        
        String definition = definitionDownloader.getDefinition(word);
        
        if (!AutomaticDefinition.NO_MATCH_FOUND.equals(definition)) {
            model.setDefinition(word, definition);
        }
        
        return definition;
    }
    
    // This method potentially can become a bottleneck during bootstrapping.
    private void addAuxiliaryWords() {
        Collection<Word> wordsToAdd = new ArrayList<>(30);
        
        // repeat words
        Collection<Word> allRepeatWords = model.getRepeatWords();
        
        Collection<Word> repeatWordsWrapped = WordFactory.getRepeatWords(
            getRandomWordsCount(), allRepeatWords);
        repeatAmount = repeatWordsWrapped.size();
        wordsToAdd.addAll(repeatWordsWrapped);
        
        // ebbinghaus words
        Collection<Word> ebbWords = model.getEbbinghausWords();
        // ebbWords.removeAll(allRepeatWords);
        
        Collection<Word> ebbWordsWrapped = WordFactory.getEbbinghausWords(
            EBBINGHAUS_WORD_COUNT, ebbWords);
        ebbinghausAmount = ebbWordsWrapped.size();
        wordsToAdd.addAll(ebbWordsWrapped);
        
        // random words
        Collection<Word> allWords = new ArrayList<>(
            model.getAllWords().values());
        allWords.removeAll(allRepeatWords);
        allWords.removeAll(model.getLastWords());
        allWords.removeAll(ebbWords);
        
        Collection<Word> randomWords = WordFactory.getRandomWords(
            getRandomWordsCount(), allWords);
        randomAmount = randomWords.size();
        wordsToAdd.addAll(randomWords);
        
        wordPool.addWords(wordsToAdd);
    }
    
    /**
     * Dump current word pool to the console.
     * Shows queue and list states.
     */
    public void dumpWordPool() {
        wordPool.dumpWordPool(console, "Current main word pool");
        WordFactory.dumpWordPools(console);
    }
    
    /**
     * Show hint dialog with complete information about word.
     * @param hintCb action to invoke as functional interface
     */
    public void setHintCallback(Consumer<Word> hintCb) {
        this.hintCb = hintCb;
    }
    
    /**
     * Show hint dialog in a gui. Uses show hint callback which should
     * be executed via gui thread.
     * @param word word to show
     */
    public void showHint(Word word) {
        if (hintCb != null) hintCb.accept(word);
    }
    
    /**
     * Sets word complexity and saves it permanently to the model.
     * @param word word to change
     * @param complexity new complexity
     */
    public void setComplexity(Word word, WordComplexity complexity) {
        if (complexity == null || word == null) return;
        
        final Word normalizedWord = model.getWordInstance(word.getWord());
        if (normalizedWord == null) return;
        
        WordComplexity oldComplexity = normalizedWord.getComplexity();
        if (oldComplexity == complexity) return;
        word.setComplexity(complexity);
        normalizedWord.setComplexity(complexity);
        
        // if word is complex enough add it to the word pool
        if (complexity.isNotEasierThan(WordComplexity.COMPLEX) &&
            oldComplexity.isNotHarderThan(WordComplexity.TOUGH) &&
            !wordPool.containsWord(normalizedWord.getWord())) {
            
            Word replaceWord = WordFactory.getSelfDeletingWord(normalizedWord, wordPool,
                w -> w.getComplexity().isNotEasierThan(WordComplexity.COMPLEX),
                console);
            
            // replace current word
            wordPool.addWordToQueue(replaceWord);
            if (hasCurrentWord() &&
                currentWord.getWord().equals(replaceWord.getWord()))
                currentWord = replaceWord;
            stateChanged(false);
            
            console.addErrorMessage("Added «" + normalizedWord.getWord() +
                "» to the current word pool");
        }
        
        // if this is current word check Challenging complexity and
        // change word position on the queue
        Word curWord = currentWord;
        if (hasCurrentWord() && curWord.isSingleWord() &&
            wordPool.containsWord(curWord.getWord()) &&
            normalizedWord.getWord().equals(curWord.getWord()) &&
            (oldComplexity == WordComplexity.CHALLENGING ||
            complexity == WordComplexity.CHALLENGING)) {
            
            wordPool.deleteWord(curWord);
            wordPool.insertIntoQueue(curWord);
        }
        
        controllerExec.execute(() -> {
            model.setComplexity(normalizedWord.getWord(), complexity);
            console.addErrorMessage("Updated «" + normalizedWord.getWord() +
                "» complexity to " + complexity.toString());
            
            modelChanged();
            PlayWav.notification();
        });
    }
    
    /**
     * Get current mode of the gui.
     * @return true if mode is compact
     */
    public boolean getCompactGuiMode() {
        return settings.getCompactGuiMode();
    }
    
    /**
     * Sets compact gui mode.
     * @param compactMode boolean mode
     */
    public void setCompactGuiMode(boolean compactMode) {
        settings.setCompactGuiMode(compactMode);
    }
    
    /**
     * Add to pool words with specified complexity.
     * @param complexity filtering complexity
     * @return true if word pool has been changed
     */
    public boolean updatePoolByComplexity(WordComplexity complexity) {
        if (complexity == null) return false;
        
        Collection<Word> words = model.getAllWords().values().stream()
            .filter(w -> w.getComplexity().isNotEasierThan(complexity))
            .map(w -> WordFactory.getComplexityWord(w, wordPool, complexity))
            .collect(Collectors.toSet());
        
        if (words.size() > 1) {
            wordPool.clear();
            wordPool.addWords(words);
            
            randomAmount = 0;
            repeatAmount = 0;
            ebbinghausAmount = 0;
            regularAmount = wordPool.size();
            
            console.addErrorMessage(words.size() + " «" + complexity +
                "» words have been added to the word pool");
            
            return true;
        }
        
        return false;
    }
    
    /**
     * One of the most important callbacks to update gui after word state
     * changes. Controller will throw NullPointerException if your gui doesn't
     * invoke this method with meaningful callback instance.
     * @param scc callback with boolean parameter to define if current word
     *            has been replaced by the other one
     */
    public void setStateChangedCallback(Consumer<Boolean> scc) {
        stateChangedCb = scc;
    }
    
    // This method is used after every state change in this controller.
    private void stateChanged(boolean currentWordReplaced) {
        if (stateChangedCb != null) {
            stateChangedCb.accept(currentWordReplaced);
        } else {
            PlayWav.exclamation();
            throw new NullPointerException("Please set stateChangedCallback");
        }
    }
    
    /**
     * Set an action to invoke after any type of model change has been executed.
     * @param mcc callback to execute in gui thread
     */
    public void setModelChangedCallback(Runnable mcc) {
        modelChangedCb = mcc;
    }
    
    // this method is required when you need to execute huge model update
    // it is typically very heavy task, so it should be used sparingly
    private void modelChanged() {
        if (modelChangedCb != null) modelChangedCb.run();
    }
    
    //*********************************************************
    //*********************************************************
    //*********************************************************
    
    /**
     * Gets current number of iterations.
     * @return iteration amount for today
     */
    public int getTodayIterations() { return iters; }
    
    /**
     * @return total number of words
     */
    public int getTotalWordAmount() { return model.getAllWords().size(); }
    
    /**
     * @return amount of bundles
     */
    public int getTotalBundleAmount() { return model.allBundlesSorted().size(); }
    
    /**
     * @return total amount of repeat words
     */
    public int getRepeatWordAmount() { return model.getRepeatWords().size(); }
    
    /**
     * @return average word length
     */
    public double getAverageWordLength() {
        return model.getAllWords().values().stream()
            .mapToInt(w -> w.getWord().length()).average().orElse(0d);
    }
    
    /**
     * Gets most frequently used words. According to timesPicked.
     * @param size amount of words
     * @return collection with most used words
     */
    public List<Word> getMostFrequentlyUsedWords(int size) {
        return model.getAllWords().values().stream()
            .sorted(Comparator.comparingInt(Word::getTimesPicked).reversed())
            .limit(size).collect(Collectors.toList());
    }
    
    /**
     * Gets oldest picked words from the model.
     * @param size amount of words to return
     * @return collection with oldest words
     */
    public List<Word> getOldestPickedWords(int size) {
        return model.getAllWords().values().stream()
            .sorted(Comparator.comparingLong(Word::getLastPickedTimestamp))
            .limit(size).collect(Collectors.toList());
    }
    
    /**
     * @return average complexity weight
     */
    public int getAverageComplexityWeight() {
        return (int) model.getAllWords().values().stream()
            .mapToInt(word -> word.getComplexity().getWeight()).average()
            .orElse(0d);
    }
    
    /**
     * @return total amount of future words
     */
    public int getFutureWordsAmount() {
        return model.getFutureWords().size();
    }
    
    /**
     * Gets amount of words grouped by their complexity
     * @return map where key is complexity, and value is long amount
     */
    public Map<WordComplexity, Long> groupWordsByComplexity() {
        return model.getAllWords().values().stream().collect(
            Collectors.groupingBy(Word::getComplexity, Collectors.counting()));
    }
    
    public long getTotalIterations() { return model.getTotalIterations(); }
    
    // bridge method should add iters by contract
    public int getThisWeekIterations() {
        return iters + model.getThisWeekIterations();
    }
    
    // bridge method should add iters by contract
    public int getIterationsForDays(int n) {
        return iters + model.getIterationsForDays(n);
    }
    
    /**
     * Permanently deletes word from model.
     * @param word word to delete
     */
    public void deleteWord(String word) {
        controllerExec.execute(() -> {
            Word wordToDelete = model.getWordInstance(word);
            
            if (wordToDelete != null) {
                deleteWordFromPool(wordToDelete);
                model.deleteWord(word);
                
                console.addErrorMessage("Word «" + word + "» has been deleted");
                
                modelChanged();
            }
        });
    }
    
    /**
     * Set new display strategy.
     * Which determines what will be displayed on the main button.
     * @param actionCommand strategy description
     */
    public void setDisplayStrategy(String actionCommand) {
        wordDisplayStrategy =
            wordDisplayFactory.getDisplayStrategy(actionCommand);
        
        console.addErrorMessage("Updated display strategy to «" +
            actionCommand.toLowerCase() + "»");
        
        PlayWav.notification();
    }
    
    /**
     * Sets verify chars cb. Special window to verify knowledge of specified
     * word by means of dragging chars.
     * @param cb new callback
     */
    public void setVerifyCharsCallback(Consumer<Word> cb) {
        verifyCharsCb = cb;
    }
    
    private void verifyChars(Word word) {
        if (verifyCharsCb != null) {
            PlayWav.notification();
            verifyCharsCb.accept(word);
        }
    }
    
    /**
     * Defines whether controller has current word.
     * Controller has no current word at the very beginning.
     * @return true if button has been pressed at least once
     */
    public boolean hasCurrentWord() {
        return currentWord.getWord() != null;
    }
    
    // fills model with default words
    private void createDefaultWords(Model model) throws IOException {
        try {
            LocalDate bundle = DateTimeUtils.getCurrentLocalDate();
            Collection<Word> words = new ArrayList<>(200);
            
            for (String line : Files.readAllLines(Paths.get(getClass()
                .getResource("/resources/irregular_verbs").toURI()),
                StandardCharsets.UTF_8)) {
                String[] tokens = line.split("\\t+");
                Word word = WordFactory.newWord();
                word.setWord(tokens[0]);
                word.setTranslation(tokens[1]);
                word.setSynonyms(tokens[2]);
                words.add(word);
            }
            
            model.addNewBundle(DateTimeUtils.getCurrentLocalDate(), words);
            
            console.addErrorMessage("Created initial bundle with " +
                words.size() + " words");
        } catch (URISyntaxException ex) { }
    }
}
