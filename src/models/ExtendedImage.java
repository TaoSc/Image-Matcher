package projetprog.models;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

import java.io.*;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ExtendedImage extends Image {

    private static final String _histDir = System.getProperty("user.dir") + "/his/";
    private static final int _numberOfBins = 256;
    private double[][] _histogram; // Type double à cause de la normalisation des histogrammes.
    private boolean _isComputed; // On vérifie avant chaque génération d'histogramme que celui-ci n'a pas été généré précédemment.
    private String _path; // Chemin de l'image.
    private String _pathHash; // Chemin de l'image hashé.
    private String _lastHistMethod; // Dernière méthode de calcul de l'histogramme utilisée (SwBa ou RGB).

    /**
     * Crée une instance d'ExtendedImage et charge l'histogramme si ele existe
     *
     * @param uri : adresse (URI) de l'image
     */
    public ExtendedImage(String uri) { // Chemin en paramètre.
        super(uri); // Fait appel au constructeur du parent.
        _histogram = new double[3][_numberOfBins]; // Trois dimensions car trois couleurs.

        try { // Comme le constructeur de URI renvoie une exception, il faut l'intercepter.
            _path = (new URI(uri)).getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            System.err.println("Image path set to fed string (\"" + uri + "\").");
            _path = uri;
        }

        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            BigInteger intHash = new BigInteger(1, digest.digest(_path.getBytes()));
            _pathHash = intHash.toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        loadHistogram("RGB");
    }

    /**
     * Affiche dans la console les données contenues dans le tableau histogramme en pourcentage.
     *
     * @param histogram : Histogramme à afficher
     */
    public static void printHist(double[][] histogram) {
        for (int intensity = 0; intensity < _numberOfBins; intensity++) {
            System.out.print("R:" + String.format("%03d", intensity) + " = " + String.format("%.2f", histogram[0][intensity]) + "%\t\t");
            System.out.print("V:" + String.format("%03d", intensity) + " = " + String.format("%.2f", histogram[1][intensity]) + "%\t\t");
            System.out.println("B:" + String.format("%03d", intensity) + " = " + String.format("%.2f", histogram[2][intensity]) + "%");
        }
    }

    /**
     * Charge l'histogramme associé à l'image et change la valeur de _isComputed de false à true.
     * @param histMethod Chaine de caracteres indiquant le format de l'image
     * @return Booleen indiquant si l'histogramme s'est bien chargée
     */
    private boolean loadHistogram(String histMethod) {
        File file = new File(_histDir + histMethod + "." + _pathHash + ".his");

        if (!file.exists())
            return false;

        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file));
            _histogram = (double[][]) objectInputStream.readObject();
            objectInputStream.close();

            if (_histogram[0].length != _numberOfBins) {
                _histogram = new double[3][_numberOfBins];
                computeHistogram(histMethod);
            } else {
                _isComputed = true;
                _lastHistMethod = histMethod;
            }

            return true;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Sauvegarde l'histogramme en tant que fichier .his
     * @param histMethod Format de l'image (ex: 'RGB')
     */
    private void saveHistogram(String histMethod) {
        File dirTest = (new File(_histDir));
        if (!dirTest.exists() && !dirTest.mkdirs())
            return;

        File file = new File(_histDir + histMethod + "." + _pathHash + ".his");

        if (file.exists() && !file.delete()) {
            System.out.println("Deletion failed. Abort saving.");
            return;
        }

        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(file));
            objectOutputStream.writeObject(_histogram);
            objectOutputStream.flush();
            objectOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Calcule l'histogramme associé à l'image. La dimension de l'image n'influe pas sur l'histogramme.
     * @param histMethod Format de l'image (ex: 'RGB')
     */
    public void computeHistogram(String histMethod) {
        if ((_isComputed && _lastHistMethod.equals(histMethod) || loadHistogram(histMethod)))
            return;

        PixelReader pixelReader = this.getPixelReader(); // Objet PixelReader permet de lire une image pixel par pixel et décode de nombreux formats.
        int width = (int) this.getWidth();
        int height = (int) this.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color color = pixelReader.getColor(x, y);

                if (histMethod.equals("RGB")) {
                    _histogram[0][(int) (color.getRed() * (_numberOfBins - 1))]++;
                    _histogram[1][(int) (color.getBlue() * (_numberOfBins - 1))]++;
                    _histogram[2][(int) (color.getRed() * (_numberOfBins - 1))]++;
                } else if (histMethod.equals("SwBa")) { // Implémentation de la page 16 de Swain et Ballard.
                    _histogram[0][(int) ((color.getRed() - color.getGreen() + 1.0) * ((_numberOfBins / 2.0) - 1))]++; // RG
                    _histogram[1][(int) ((2 * color.getBlue() - color.getRed() - color.getGreen() + 2.0) * (_numberOfBins / 4.0 - 1))]++; // BY
                    _histogram[2][(int) ((color.getRed() + color.getGreen() + color.getBlue()) * (_numberOfBins / 3.0 - 1))]++; // WB
                }
            }
        }

        for (int channel = 0; channel < _histogram.length; channel++) { // Normalisation des valeurs.
            for (int intensity = 0; intensity < _histogram[channel].length; intensity++)
                _histogram[channel][intensity] = _histogram[channel][intensity] / (getWidth() * getHeight()) * 100.;
        }

        _isComputed = true;
        _lastHistMethod = histMethod;
        saveHistogram(histMethod); // Sauvegarde de l'histogramme sous la forme <hash de l'image>.his.
    }

    /**
     * Calcule la distance entre l'image instanciée et une image de référence.
     *
     * @param refImage : l'image avec laquelle on veut comparer l'image instanciée
     * @return double : la distance, en pourcentage, entre l'image instanciée et l'image en paramètre
     */
    public double computeDistance(ExtendedImage refImage) {
        double distance = 0;

        for (int channel = 0; channel < 3; channel++) { // On boucle 3 fois (R, V et B)
            for (int intensity = 0; intensity < _numberOfBins; intensity++) { // On regarde tous les bins
                distance += Math.abs(_histogram[channel][intensity] - refImage.getHistogram()[channel][intensity]); // Somme de tous les écarts entre l'image de référence et l'image instanciée
            }
        }

        return distance;
    }

    /**
     * Retourne l'histogramme de l'image.
     *
     * @return double[3][_numberOfBins] : histogramme
     */
    public double[][] getHistogram() {
        return _histogram;
    }

    /**
     * Retourne le booléen signifiant si l'histogramme a déjà été calculé ou non.
     */
    public boolean isComputed() {
        return _isComputed;
    }

    public String getPath() {
        return _path;
    }
}
