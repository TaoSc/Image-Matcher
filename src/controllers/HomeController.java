package projetprog.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import projetprog.models.ExtendedImage;
import projetprog.models.TableResult;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.util.*;

public class HomeController implements Initializable {

    //<editor-fold desc="FXML Control Definitions">
    @FXML
    private Button selRefImageButton;

    @FXML
    private Button selCompDirButton;

    /**
     * Bouton qui a pour but de lancer le calcul normal.
     */
    @FXML
    private Button computeButton;

    /**
     * Bouton qui a pour but de lancer le calul en fonction de couleur.
     */
    @FXML
    private Button computeButtonOpponentColorAxes;

    /**
     * Objet representant l'histogramme graphique.
     */
    @FXML
    private BarChart histogramChart;

    /**
     * Objet representant l'image de reference.
     */
    @FXML
    private ImageView referenceImageView;

    /**
     * Objet representant le tableau de informations de paths et distance.
     */
    @FXML
    private TableView<TableResult> filesTable;

    /**
     * La colonne representant le nom du fichier.
     */
    @FXML
    private TableColumn<TableResult, String> fileNameColumn;

    /**
     * La colonne representant la distance par rapport a l'image de reference.
     */
    @FXML
    private TableColumn<TableResult, Double> distanceColumn;

    /**
     * La colonne representant le rang de l'image.
     */
    @FXML
    private TableColumn<TableResult, Integer> rankColumn;

    /**
     * Objet representant l'image selectionne. Par defaut c'est l'image la plus proche apres computation.
     */
    @FXML
    private ImageView closestImageView;
    //</editor-fold>

    /**
     * Mapping contenant toutes les images comparees avec leur distance par rapport a la reference.
     */
    private HashMap<ExtendedImage, Double> _imagesList;
    private File _lastAccessedFolder;

    /**
     * Objet representant l'image de reference.
     */
    private ExtendedImage _referenceImage;

    /**
     * C'est le controlleur principale qui relie le modele et la vue.
     */
    public HomeController() {
        _imagesList = null;
        _lastAccessedFolder = new File(System.getProperty("user.dir"));
        _referenceImage = null;
    }

    /**
     * Charge et calcule l'histogramme de l'image de reference.
     */
    public void bttImageSelectClicked() {
        FileChooser fileChooser = new FileChooser(); // Ouvre l'explorateur de fichiers de l'OS.
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Images", "*.jpg", "*.jpeg", "*.png", "*.bmp", "*.gif"),
                new FileChooser.ExtensionFilter("PNG", "*.png"),
                new FileChooser.ExtensionFilter("GIF", "*.gif"),
                new FileChooser.ExtensionFilter("BMP", "*.bmp"),
                new FileChooser.ExtensionFilter("JPEG", "*.jpg", "*.jpeg")
        );
        fileChooser.setTitle("Choose Reference Image");
        File file = fileChooser.showOpenDialog(null);

        if (file == null || !file.exists())
            return;

        _referenceImage = new ExtendedImage(file.toURI().toString()); // On cree une instance de l'image. Comme a chaque creation d'instance, on cherche s'il existe un HIS associe a cette image.
        _referenceImage.computeHistogram("RGB"); // On appelle la methode computeHistogram qui, s'il n'y a pas deja un HIS associe a l'image, le calcule. Sinon il ne se passe rien.

        referenceImageView.setImage(_referenceImage);
        updateBarChart(_referenceImage.getHistogram());

        isComputable();
    }

    /**
     * Fonction lancant le paneau de selection de dossier.
     */
    public void bttFolderSelectClicked() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose a directory to use");
        chooser.setInitialDirectory(_lastAccessedFolder);
        File selectedDirectory = chooser.showDialog(null);

        if (selectedDirectory != null) {
            _imagesList = listImagesForFolder(selectedDirectory);
            _lastAccessedFolder = selectedDirectory;
        }

        isComputable();
    }

    /**
     * Mise a jour du statut de bouton de calcul en fonction du statut des images de reference et la liste des images slectionnees.
     */
    private void isComputable() {
        if (_referenceImage != null && _imagesList != null) {
            computeButton.setDisable(false);
            computeButtonOpponentColorAxes.setDisable(false);
        }
    }

    /**
     * Fonction permettant de lister tous les images avec leur distance par rapport a la reference.
     * @param folder Le dossier contenant les images a comparer
     * @return Mapping avec les images et leur distance.
     */
    private HashMap<ExtendedImage, Double> listImagesForFolder(final File folder) {
        HashMap list = new HashMap<>();

        File[] parsedFolder = Objects.requireNonNull(folder.listFiles(new FileFilter() {
            private final FileNameExtensionFilter filter =
                    new FileNameExtensionFilter("Images",
                            "jpg", "jpeg", "png", "bmp", "gif");

            public boolean accept(File file) {
                return filter.accept(file);
            }
        }));

        for (final File fileEntry : parsedFolder) {
            ExtendedImage image = new ExtendedImage(fileEntry.toURI().toString());

            if (!image.errorProperty().get())
                list.put(image, 0.);
        }

        return list;
    }

    public void bttComputeClicked() {
        if (_referenceImage != null) {
            for (Map.Entry<ExtendedImage, Double> entry : _imagesList.entrySet()) {
                final ExtendedImage img = entry.getKey();

                img.computeHistogram("RGB");
                final Double distance = img.computeDistance(_referenceImage);

                entry.setValue(distance);
            }

            ArrayList<Map.Entry<ExtendedImage, Double>> entryList = new ArrayList<>(_imagesList.entrySet());
            entryList.sort((img1, img2) -> {
                if (img1.getValue().equals(img2.getValue()))
                    return 0;

                return img1.getValue() > img2.getValue() ? 1 : -1;
            });

            final ExtendedImage closestImage = entryList.get(0).getKey();
            closestImageView.setImage(closestImage);

            updateFileTable(_imagesList);
        }
    }

    public void bttComputeClickedOpponentColorAxes() {
        if (_referenceImage != null) {
            for (Map.Entry<ExtendedImage, Double> entry : _imagesList.entrySet()) {
                final ExtendedImage img = entry.getKey();

                img.computeHistogram("SwBa");
                final Double distance = img.computeDistance(_referenceImage);

                entry.setValue(distance);
            }

            ArrayList<Map.Entry<ExtendedImage, Double>> entryList = new ArrayList<>(_imagesList.entrySet());
            entryList.sort((img1, img2) -> {
                if (img1.getValue().equals(img2.getValue()))
                    return 0;

                return img1.getValue() > img2.getValue() ? 1 : -1;
            });

            final ExtendedImage closestImage = entryList.get(0).getKey();
            closestImageView.setImage(closestImage);

            updateFileTable(_imagesList);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("FileName"));
        distanceColumn.setCellValueFactory(new PropertyValueFactory<>("Distance"));
        rankColumn.setCellValueFactory(new PropertyValueFactory<>("Rank"));

        filesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                closestImageView.setImage(new Image(newVal.getFileName()));
            }
        });
    }

    /**
     * Met a jour le graphique de l'histogramme
     * @param histogram L'histogramme de l'image
     */
    private void updateBarChart(double[][] histogram) {
        ArrayList<XYChart.Series> series = new ArrayList<>();

        for (int colIndex = 0; colIndex < histogram.length; colIndex++) {
            XYChart.Series chartSeries = new XYChart.Series();

            double[] colorOccurrences = histogram[colIndex];
            for (int i = 0; i < colorOccurrences.length; i++) {
                chartSeries.getData().add(new XYChart.Data<>(String.format("%s", i), histogram[colIndex][i]));
            }

            series.add(colIndex, chartSeries);
        }

        histogramChart.setData(FXCollections.observableArrayList(series));
    }

    /**
     * Met a jour la table graphique contenant le nom du fichier, le rang et la distance par rapport a l'image de reference
     * @param distances Un liste de tuples contenant l'image comparee et sa distance par rapport a la reference
     */
    private void updateFileTable(HashMap<ExtendedImage, Double> distances) {
        ArrayList<TableResult> results = new ArrayList<>();
        Iterator<Map.Entry<ExtendedImage, Double>> iter = distances.entrySet().iterator();

        for (int i = 0; iter.hasNext(); i++) {
            Map.Entry<ExtendedImage, Double> set = iter.next();
            results.add(new TableResult(i, set.getKey().getPath(), set.getValue()));
        }

        results.sort(Comparator.comparingDouble(TableResult::getDistance)); // Trie les resultats dans l'ordre croissant.
        int i = 1;
        for (TableResult result : results)
            result.setRank(i++);

        filesTable.setItems(FXCollections.observableList(results));
    }
}
