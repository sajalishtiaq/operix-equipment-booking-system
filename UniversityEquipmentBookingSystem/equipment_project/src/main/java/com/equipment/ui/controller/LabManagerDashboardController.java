package com.equipment.ui.controller;

import com.equipment.model.*;
import com.equipment.service.LabManagerService;
import com.equipment.ui.MainApp;
import com.equipment.ui.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class LabManagerDashboardController implements Initializable {

    // ── Sidebar panels ──
    @FXML private VBox panelBookings, panelEquipment, panelReports, panelTasks;
    @FXML private Label welcomeLabel, statusBar;

    // ── UC01: Pending Bookings ──
    @FXML private TableView<Booking> pendingTable;
    @FXML private TableColumn<Booking, Integer> colBkId;
    @FXML private TableColumn<Booking, String> colBkTeacher, colBkEquipment, colBkDate, colBkTime, colBkPurpose, colBkStatus;

    // ── UC02: Equipment Monitor ──
    @FXML private TableView<Equipment> equipTable;
    @FXML private TableColumn<Equipment, Integer> colEqId;
    @FXML private TableColumn<Equipment, String> colEqName, colEqCategory, colEqStatus, colEqLocation;

    // ── UC03: Update Equipment Status ──
    @FXML private TextField updateEqIdField;
    @FXML private ComboBox<Equipment.Status> updateStatusBox;
    @FXML private Label updateStatusLabel;

    // ── UC04: Reports ──
    @FXML private TableView<FaultReport> faultsTable;
    @FXML private TableColumn<FaultReport, Integer> colFaultId;
    @FXML private TableColumn<FaultReport, String> colFaultEquip, colFaultDesc, colFaultSev, colFaultStatus;
    @FXML private Label totalBookingsLabel, unresolvedFaultsLabel;

    // ── UC05: Assign Task ──
    @FXML private TextField taskFaultIdField, taskNotesField;
    @FXML private ComboBox<String> taskTechnicianBox;
    @FXML private ComboBox<MaintenanceTask.Priority> taskPriorityBox;
    @FXML private Label taskStatusLabel;

    private final LabManagerService labManagerService = new LabManagerService();
    private List<User> technicians;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        if (SessionManager.getCurrentUser() != null) {
            welcomeLabel.setText("Welcome, " + SessionManager.getCurrentUser().getFullName());
        } else {
            welcomeLabel.setText("Welcome");
        }

        setupPendingTable();
        setupEquipmentTable();
        setupFaultsTable();

        updateStatusBox.setItems(FXCollections.observableArrayList(Equipment.Status.values()));
        taskPriorityBox.setItems(FXCollections.observableArrayList(MaintenanceTask.Priority.values()));
        taskPriorityBox.setValue(MaintenanceTask.Priority.MEDIUM);

        loadTechnicians();
        showPanel("bookings");
        loadPendingBookings();
    }

    // ── Navigation ──
    @FXML private void showBookings() {
        showPanel("bookings");
        loadPendingBookings();
    }

    @FXML private void showEquipment() {
        showPanel("equipment");
        loadEquipment();
    }

    @FXML private void showReports() {
        showPanel("reports");
        loadReports();
    }

    @FXML private void showTasks() {
        showPanel("tasks");
    }

    private void showPanel(String name) {
        panelBookings.setVisible(false); panelBookings.setManaged(false);
        panelEquipment.setVisible(false); panelEquipment.setManaged(false);
        panelReports.setVisible(false); panelReports.setManaged(false);
        panelTasks.setVisible(false); panelTasks.setManaged(false);

        switch (name) {
            case "bookings"  -> { panelBookings.setVisible(true);  panelBookings.setManaged(true); }
            case "equipment" -> { panelEquipment.setVisible(true); panelEquipment.setManaged(true); }
            case "reports"   -> { panelReports.setVisible(true);   panelReports.setManaged(true); }
            case "tasks"     -> { panelTasks.setVisible(true);     panelTasks.setManaged(true); }
        }
    }

    // ── UC01: Bookings ──
    private void loadPendingBookings() {
        try {
            List<Booking> list = labManagerService.getPendingBookings();
            pendingTable.setItems(FXCollections.observableArrayList(list));
            setStatus(list.size() + " pending booking(s)");
        } catch (Exception e) {
            e.printStackTrace();
            setStatus("Error: " + e.getMessage());
        }
    }

    @FXML private void handleApprove() {
        processSelected(Booking.Status.APPROVED);
    }

    @FXML private void handleReject() {
        processSelected(Booking.Status.REJECTED);
    }

    private void processSelected(Booking.Status status) {
        Booking sel = pendingTable.getSelectionModel().getSelectedItem();

        if (sel == null) {
            setStatus("Select a booking first.");
            return;
        }

        try {
            boolean ok = labManagerService.processBooking(
                    SessionManager.getCurrentUser().getUserId(),
                    sel.getBookingId(),
                    status
            );

            setStatus(ok ? "Booking #" + sel.getBookingId() + " " + status.name().toLowerCase()
                         : "Update failed.");

            loadPendingBookings();

        } catch (Exception e) {
            e.printStackTrace();
            setStatus("Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleSelectTask() {
        Booking selected = pendingTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            setStatus("Selected booking ID: " + selected.getBookingId());
        }
    }

    // ── Equipment ──
    private void loadEquipment() {
        try {
            List<Equipment> list = labManagerService.monitorEquipmentAvailability();
            equipTable.setItems(FXCollections.observableArrayList(list));
        } catch (Exception e) {
            e.printStackTrace();
            setStatus("Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateStatus() {
        try {
            int eqId = Integer.parseInt(updateEqIdField.getText().trim());
            Equipment.Status newStatus = updateStatusBox.getValue();

            if (newStatus == null) {
                updateStatusLabel.setStyle("-fx-text-fill:#e74c3c;");
                updateStatusLabel.setText("✗ Select a status.");
                return;
            }

            // ── Check equipment exists ──
            List<Equipment> allEquipment = labManagerService.monitorEquipmentAvailability();
            Equipment selected = allEquipment.stream()
                    .filter(e -> e.getEquipmentId() == eqId)
                    .findFirst()
                    .orElse(null);

            if (selected == null) {
                updateStatusLabel.setStyle("-fx-text-fill:#e74c3c;");
                updateStatusLabel.setText("✗ Equipment #" + eqId + " does not exist.");
                return;
            }

            // ── FAULTY / UNDER_REPAIR lock ──
            if (selected.getStatus() == Equipment.Status.FAULTY ||
                selected.getStatus() == Equipment.Status.UNDER_REPAIR) {

                boolean activeFaultExists = labManagerService.hasActiveFault(eqId);

                if (activeFaultExists) {
                    updateStatusLabel.setStyle("-fx-text-fill:#e74c3c;");
                    updateStatusLabel.setText(
                        "✗ Cannot change status. Equipment has an active fault that must " +
                        "be resolved by a technician first."
                    );
                    return;
                }
            }

            // ── Proceed with update ──
            boolean ok = labManagerService.updateEquipmentStatus(
                    SessionManager.getCurrentUser().getUserId(), eqId, newStatus);

            updateStatusLabel.setStyle(ok ? "-fx-text-fill:#2ecc71;" : "-fx-text-fill:#e74c3c;");
            updateStatusLabel.setText(ok ? "✓ Updated successfully." : "✗ Update failed.");

            loadEquipment();

        } catch (NumberFormatException e) {
            updateStatusLabel.setStyle("-fx-text-fill:#e74c3c;");
            updateStatusLabel.setText("✗ Enter a valid numeric Equipment ID.");
        } catch (Exception e) {
            e.printStackTrace();
            updateStatusLabel.setStyle("-fx-text-fill:#e74c3c;");
            updateStatusLabel.setText("✗ " + e.getMessage());
        }
    }

    // ── Reports ──
    private void loadReports() {
        try {
            List<FaultReport> faults = labManagerService.getUnresolvedFaults();
            faultsTable.setItems(FXCollections.observableArrayList(faults));
            unresolvedFaultsLabel.setText(String.valueOf(faults.size()));

            List<Booking> bookings = labManagerService.getAllBookings();
            totalBookingsLabel.setText(String.valueOf(bookings.size()));

        } catch (Exception e) {
            e.printStackTrace();
            setStatus("Error loading reports: " + e.getMessage());
        }
    }

    // ── Tasks ──
    private void loadTechnicians() {
        try {
            technicians = labManagerService.getAllTechnicians();

            taskTechnicianBox.setItems(FXCollections.observableArrayList(
                    technicians.stream()
                            .map(u -> u.getUserId() + " - " + u.getFullName())
                            .toList()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            setStatus("Error loading technicians.");
        }
    }

    @FXML
    private void handleAssignTask() {
        try {
            int faultId = Integer.parseInt(taskFaultIdField.getText().trim());
            String notes = taskNotesField.getText().trim();
            MaintenanceTask.Priority priority = taskPriorityBox.getValue();

            String sel = taskTechnicianBox.getValue();
            if (sel == null) {
                taskStatusLabel.setText("Select a technician.");
                return;
            }

            int techId = Integer.parseInt(sel.split(" - ")[0]);

            MaintenanceTask task = labManagerService.assignMaintenanceTask(
                    SessionManager.getCurrentUser().getUserId(),
                    faultId, techId, priority, notes);

            taskStatusLabel.setText(task != null ? "✓ Assigned" : "✗ Failed");

        } catch (Exception e) {
            e.printStackTrace();
            taskStatusLabel.setText("✗ " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() throws Exception {
        SessionManager.clearSession();
        MainApp.navigateTo("login");
    }

    // ── Table Setup ──
    private void setupPendingTable() {
        colBkId.setCellValueFactory(new PropertyValueFactory<>("bookingId"));
        colBkTeacher.setCellValueFactory(new PropertyValueFactory<>("teacherName"));
        colBkEquipment.setCellValueFactory(new PropertyValueFactory<>("equipmentName"));
        colBkDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getBookingDate() != null ? d.getValue().getBookingDate().toString() : ""));
        colBkTime.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getStartTime() + " - " + d.getValue().getEndTime()));
        colBkPurpose.setCellValueFactory(new PropertyValueFactory<>("purpose"));
        colBkStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
    }

    private void setupEquipmentTable() {
        colEqId.setCellValueFactory(new PropertyValueFactory<>("equipmentId"));
        colEqName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEqCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colEqStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colEqLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
    }

    private void setupFaultsTable() {
        colFaultId.setCellValueFactory(new PropertyValueFactory<>("faultId"));
        colFaultEquip.setCellValueFactory(new PropertyValueFactory<>("equipmentName"));
        colFaultDesc.setCellValueFactory(new PropertyValueFactory<>("faultDescription"));
        colFaultSev.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSeverity().name()));
        colFaultStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
    }

    private void setStatus(String msg) {
        statusBar.setText(msg);
    }
}