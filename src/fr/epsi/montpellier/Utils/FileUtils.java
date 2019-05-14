package fr.epsi.montpellier.Utils;

import java.io.File;

public final class FileUtils {

    private static boolean deleteRecursively(File fileToDelete) {

        if(fileToDelete == null) {
            return false;
        }
        // Suppression du fichier
        if (fileToDelete.isFile()) {
            return fileToDelete.delete();
        }
        // Sinon ce doit être un répertoire
        if (!fileToDelete.isDirectory()) {
            return false;
        }

        // Dans ce cas, on liste tous les fichiers pour les supprimer
        File[] files = fileToDelete.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (!deleteDirectory(file)) {
                    return false;
                }
            }
        }

        return fileToDelete.delete();
    }

    public static boolean deleteDirectory(File directory) {

        return deleteRecursively(directory);
    }
}
