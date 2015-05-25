package com.words.model.filemodel;

import com.words.main.EnglishWords;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;

/**
 * Class required to back up file model.
 * It simply copies all important files into backup folder.
 * @author vlad
 */
class BackupFileModel {
    
    private final Path projectDir;
    private final Path currentDir;
    
    public static void main(String[] args) throws Exception {
        BackupFileModel bfm = new BackupFileModel(EnglishWords.PROJECT_DIRECTORY);
        
        bfm.backup();
    }
    
    public BackupFileModel(Path projectDir) throws IOException {
        this.projectDir = projectDir;
        
        currentDir = projectDir.resolve("backup")
            .resolve(LocalDate.now() + " filemodel");
        
        if (Files.exists(currentDir)) {
            // then delete
            Files.walkFileTree(currentDir, new SimpleFileVisitor<Path>() {
                
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        
        Files.createDirectories(currentDir);
    }
    
    private void backupFiles(Path dir, Path destDir) throws IOException {
        Files.createDirectories(destDir);
        
        Files
            .list(dir)
//            .parallel()
            .filter(path -> {
                try {
                    return Files.isRegularFile(path) && !Files.isHidden(path);
                } catch (IOException ex) {
                    return  false;
                }
            }).forEach(path -> {
                try {
                    Files.copy(path, destDir.resolve(path.getFileName()));
//                    System.out.println("Copied " + path.getFileName());
                } catch (IOException ex) { }
            });
    }
    
    public void backup() throws IOException {
        backupFiles(projectDir, currentDir);
        
        backupFiles(projectDir.resolve("sound"), currentDir.resolve("sound"));
    }
}
