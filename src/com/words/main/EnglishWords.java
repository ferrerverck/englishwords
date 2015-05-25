package com.words.main;

import com.words.controller.Controller;
import com.words.controller.utils.Utils;
import com.words.gui.MainFrame;
import com.words.model.Model;
import com.words.model.mysqlmodel.MysqlModel;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class EnglishWords {
    
    public static final String TITLE = "EnglishWords";
    
    public static final Path PROJECT_DIRECTORY =
//        Paths.get(System.getProperty("user.home") +
//            System.getProperty("file.separator") + TITLE.toLowerCase());
        Paths.get(System.getProperty("user.home") +
            System.getProperty("file.separator") + ".java" +
            System.getProperty("file.separator") + TITLE.toLowerCase());
    
    public static void main(String[] args) throws Exception {
        long startTime = System.nanoTime();
        
//        Model model = new FileModel(PROJECT_DIRECTORY);
        Model model = new MysqlModel(TITLE);
//        Model model = new MysqlModel("EnglishWordsTest");
        Controller controller = new Controller(model, PROJECT_DIRECTORY);
        
        useSwingGui(controller, startTime);
    }
    
    public static void restartApplicaiton() {
        try {
            List<String> cmdList = new LinkedList<>();
            cmdList.add(System.getProperty("java.home") + File.separator
                + "bin" + File.separator + "java");
            
            ManagementFactory.getRuntimeMXBean()
                .getInputArguments().stream().forEach(jvmArg -> {
                    cmdList.add(jvmArg);
                });
            
            cmdList.add("-cp");
            cmdList.add(ManagementFactory.getRuntimeMXBean().getClassPath());
            cmdList.add(EnglishWords.class.getName());
            
            String[] commandArray = cmdList.toArray(
                new String[cmdList.size()]);
            
            // print relaunch command
            for (String string : commandArray) {
                System.out.print(string + " ");
            }
            System.out.println();
            
            Runtime.getRuntime().exec(commandArray);
            System.exit(0);
        } catch (IOException ioe) {
            System.out.println("Unable to relaunch application");
        }
    }
    
    private static void useSwingGui(Controller controller, long startTime) {
        SwingUtilities.invokeLater(() -> {
            try {
                List<String> lafList =
                    Stream.of(UIManager.getInstalledLookAndFeels())
                        .map(lf -> lf.getClassName())
                        .filter(lfcn -> !lfcn.contains("Motif"))
                        .collect(Collectors.toList());
                
                UIManager.setLookAndFeel(
                    lafList.get(Utils.RANDOM.nextInt(lafList.size())));
            } catch (ClassNotFoundException | IllegalAccessException |
                InstantiationException | UnsupportedLookAndFeelException ex) { }
            
            JFrame frame = new MainFrame(controller);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            controller.addLoadingStatistics(System.nanoTime() - startTime);
            
            frame.setVisible(true);
        });
    }
    
    private EnglishWords() { throw new AssertionError(); }
}
