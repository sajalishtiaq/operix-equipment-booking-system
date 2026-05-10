package com.equipment.ui.controller;

import com.equipment.model.*;
import com.equipment.service.EquipmentService;
import com.equipment.service.FaultService;
import com.equipment.ui.MainApp;
import com.equipment.ui.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.ResourceBundle;

/**
 * TeacherDashboardController
 *
 * BACKEND CONNECTIONS (zero changes in services):
 *   UC01 - equipmentService.searchEquipments()
 *   UC02 - equipmentService.checkAvailability()
 *   UC03 - equipmentService.bookEquipment()
 *   UC04 - faultService.reportFault()
 *   UC05 - equipmentService.getBookingStatus()
 */
public class TeacherDashboardController implements Initializable {

    // ── Sidebar nav panels ──
    @FXML private VBox panelSearch, panelBook, panelBookings, panelFault;
    @FXML private Label welcomeLabel;
    @FXML private Label statusBar;

    // ── UC01: Search ──
    @FXML private TextField searchField;
    @FXML private TableView<Equipment> searchTable;
    @FXML private TableColumn<Equipment, Integer> colEqId;
    @FXML private TableColumn<Equipment, String>  colEqName, colEqCategory, colEqStatus, colEqLocation;

    // ── UC03: Book ──
    @FXML private TextField bookEqIdField, bookPurposeField;
    @FXML private DatePicker bookDatePicker;
    @FXML private TextField bookStartField, bookEndField;
    @FXML private Label bookStatusLabel;

    // ── UC05: My Bookings ──
    @FXML private TableView<Booking> bookingsTable;
    @FXML private TableColumn<Booking, Integer> colBkId;
    @FXML private TableColumn<Booking, String>  colBkEquipment, colBkDate, colBkTime, colBkStatus, colBkPurpose;

    // ── UC04: Report Fault ──
    @FXML private TextField faultEqIdField;
    @FXML private TextArea  faultDescField;
    @FXML private ComboBox<FaultReport.Severity> faultSeverityBox;
    @FXML private Label faultStatusLabel;

    // ── Services (your existing classes) ──
    private final EquipmentService equipmentService = new EquipmentService();
    private final FaultService     faultService     = new FaultService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = SessionManager.getCurrentUser();
        welcomeLabel.setText("Welcome, " + user.getFullName());

        setupSearchTable();
        setupBookingsTable();
        faultSeverityBox.setItems(FXCollections.observableArrayList(FaultReport.Severity.values()));
        faultSeverityBox.setValue(FaultReport.Severity.MEDIUM);

        showPanel("search");
        loadMyBookings();
    }

    // ── Navigation ──
    @FXML private void showSearch()   { showPanel("search"); }
    @FXML private void showBook()     { showPanel("book"); }
    @FXML private void showBookings() { showPanel("bookings"); loadMyBookings(); }
    @FXML private void showFault()    { showPanel("fault"); }

    private void showPanel(String name) {
        panelSearch.setVisible(false);   panelSearch.setManaged(false);
        panelBook.setVisible(false);     panelBook.setManaged(false);
        panelBookings.setVisible(false); panelBookings.setManaged(false);
        panelFault.setVisible(false);    panelFault.setManaged(false);
        switch (name) {
            case "search"   -> { panelSearch.setVisible(true);   panelSearch.setManaged(true); }
            case "book"     -> { panelBook.setVisible(true);     panelBook.setManaged(true); }
            case "bookings" -> { panelBookings.setVisible(true); panelBookings.setManaged(true); }
            case "fault"    -> { panelFault.setVisible(true);    panelFault.setManaged(true); }
        }
    }

    // ── UC01: Search Equipment ──
    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) { setStatus("Enter a search keyword."); return; }
        try {
            List<Equipment> results = equipmentService.searchEquipments(
                    SessionManager.getCurrentUser().getUserId(), keyword);
            searchTable.setItems(FXCollections.observableArrayList(results));
            setStatus("Found " + results.size() + " result(s) for '" + keyword + "'");
        } catch (Exception e) { setStatus("Error: " + e.getMessage()); }
    }

    // ── UC02: Auto-fill equipment ID from selected row ──
    @FXML
    private void handleSelectEquipment() {
        Equipment sel = searchTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            bookEqIdField.setText(String.valueOf(sel.getEquipmentId()));
            showPanel("book");
            setStatus("Selected: " + sel.getName() + " [" + sel.getStatus() + "]");
        }
    }

    // ── UC03: Book Equipment ──
    @FXML
    private void handleBookEquipment() {
        try {
            int eqId       = Integer.parseInt(bookEqIdField.getText().trim());
            LocalDate date = bookDatePicker.getValue();
            LocalTime start = LocalTime.parse(bookStartField.getText().trim());
            LocalTime end   = LocalTime.parse(bookEndField.getText().trim());
            String purpose  = bookPurposeField.getText().trim();

            Booking b = equipmentService.bookEquipment(
                    SessionManager.getCurrentUser().getUserId(),
                    eqId, date, start, end, purpose);

            if (b != null) {
                bookStatusLabel.setStyle("-fx-text-fill: #2ecc71;");
                bookStatusLabel.setText("✓ Booking #" + b.getBookingId() + " submitted — awaiting approval.");
            } else {
                bookStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                bookStatusLabel.setText("✗ Booking conflict detected.");
            }
        } catch (NumberFormatException e) {
            bookStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            bookStatusLabel.setText("✗ Invalid Equipment ID.");
        } catch (Exception e) {
            bookStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            bookStatusLabel.setText("✗ " + e.getMessage());
        }
    }

    // ── UC05: My Bookings ──
    private void loadMyBookings() {
        try {
            List<Booking> list = equipmentService.getBookingStatus(
                    SessionManager.getCurrentUser().getUserId());
            bookingsTable.setItems(FXCollections.observableArrayList(list));
        } catch (Exception e) { setStatus("Error loading bookings: " + e.getMessage()); }
    }

    // ── UC04: Report Fault ──
    @FXML
    private void handleReportFault() {
        try {
            int eqId           = Integer.parseInt(faultEqIdField.getText().trim());
            String desc        = faultDescField.getText().trim();
            FaultReport.Severity sev = faultSeverityBox.getValue();

            FaultReport fr = faultService.reportFault(
                    SessionManager.getCurrentUser().getUserId(), eqId, desc, sev);

            if (fr != null) {
                faultStatusLabel.setStyle("-fx-text-fill: #2ecc71;");
                faultStatusLabel.setText("✓ Fault #" + fr.getFaultId() + " reported successfully.");
                faultDescField.clear(); faultEqIdField.clear();
            }
        } catch (Exception e) {
            faultStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            faultStatusLabel.setText("✗ " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() throws Exception {
        SessionManager.clearSession();
        MainApp.navigateTo("login");
    }

    // ── Table setup ──
    private void setupSearchTable() {
        colEqId.setCellValueFactory(new PropertyValueFactory<>("equipmentId"));
        colEqName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEqCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colEqStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colEqLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
    }

    private void setupBookingsTable() {
        colBkId.setCellValueFactory(new PropertyValueFactory<>("bookingId"));
        colBkEquipment.setCellValueFactory(new PropertyValueFactory<>("equipmentName"));
        colBkDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getBookingDate() != null ? d.getValue().getBookingDate().toString() : ""));
        colBkTime.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getStartTime() + " – " + d.getValue().getEndTime()));
        colBkStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
        colBkPurpose.setCellValueFactory(new PropertyValueFactory<>("purpose"));
    }

    private void setStatus(String msg) {
        statusBar.setText(msg);
    }
}
