package com.equipment.ui.controller;

import com.equipment.model.*;
import com.equipment.service.FaultService;
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
import java.util.Optional;
import java.util.ResourceBundle;

public class TechnicianDashboardController implements Initializable {

    // ── Panels ──
    @FXML private VBox panelTasks, panelUpdate, panelHistory, panelFaultDetail;
    @FXML private Label welcomeLabel, statusBar;

    // ── UC01: Assigned Tasks ──
    @FXML private TableView<MaintenanceTask> tasksTable;
    @FXML private TableColumn<MaintenanceTask, Integer> colTaskId;
    @FXML private TableColumn<MaintenanceTask, String>  colTaskEquip, colTaskDesc, colTaskPriority, colTaskStatus, colTaskAssigned;

    // ── UC02: Update Status ──
    @FXML private TextField updateTaskIdField, updateNotesField;
    @FXML private ComboBox<MaintenanceTask.Status> updateStatusBox;
    @FXML private Label updateStatusLabel;

    // ── UC03: Resolve Fault ──
    @FXML private TextField resolveTaskIdField;
    @FXML private Label resolveStatusLabel;

    // ── UC04: Fault Detail ──
    @FXML private TextField detailFaultIdField;
    @FXML private Label detailEquipLabel, detailDescLabel, detailSevLabel,
                        detailStatusLabel, detailReporterLabel, detailDateLabel;

    // ── UC05: Maintenance History ──
    @FXML private TextField historyEqIdField;
    @FXML private TableView<MaintenanceTask> historyTable;
    @FXML private TableColumn<MaintenanceTask, Integer> colHstTaskId;
    @FXML private TableColumn<MaintenanceTask, String>  colHstEquip, colHstDesc, colHstStatus, colHstCompleted;

    private final FaultService faultService = new FaultService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        welcomeLabel.setText("Welcome, " + SessionManager.getCurrentUser().getFullName());

        setupTasksTable();
        setupHistoryTable();
        updateStatusBox.setItems(FXCollections.observableArrayList(
                MaintenanceTask.Status.IN_PROGRESS, MaintenanceTask.Status.COMPLETED));

        showPanel("tasks");
        loadMyTasks();
    }

    // ── Navigation ──
    @FXML private void showTasks()       { showPanel("tasks");       loadMyTasks(); }
    @FXML private void showUpdate()      { showPanel("update"); }
    @FXML private void showHistory()     { showPanel("history"); }
    @FXML private void showFaultDetail() { showPanel("faultDetail"); }

    private void showPanel(String name) {
        panelTasks.setVisible(false);       panelTasks.setManaged(false);
        panelUpdate.setVisible(false);      panelUpdate.setManaged(false);
        panelHistory.setVisible(false);     panelHistory.setManaged(false);
        panelFaultDetail.setVisible(false); panelFaultDetail.setManaged(false);
        switch (name) {
            case "tasks"       -> { panelTasks.setVisible(true);       panelTasks.setManaged(true); }
            case "update"      -> { panelUpdate.setVisible(true);      panelUpdate.setManaged(true); }
            case "history"     -> { panelHistory.setVisible(true);     panelHistory.setManaged(true); }
            case "faultDetail" -> { panelFaultDetail.setVisible(true); panelFaultDetail.setManaged(true); }
        }
    }

    // ── UC01: Load Assigned Tasks ──
    private void loadMyTasks() {
        try {
            List<MaintenanceTask> list = faultService.getAssignedTasks(
                    SessionManager.getCurrentUser().getUserId());
            tasksTable.setItems(FXCollections.observableArrayList(list));
            setStatus(list.size() + " task(s) assigned to you.");
        } catch (Exception e) { setStatus("Error: " + e.getMessage()); }
    }

    // ── Quick-fill from selected task row ──
    @FXML
    private void handleSelectTask() {
        MaintenanceTask sel = tasksTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            updateTaskIdField.setText(String.valueOf(sel.getTaskId()));
            resolveTaskIdField.setText(String.valueOf(sel.getTaskId()));
            showPanel("update");
        }
    }

    // ── UC02: Update Repair Status ──
    @FXML
    private void handleUpdateStatus() {
        try {
            int taskId = Integer.parseInt(updateTaskIdField.getText().trim());
            MaintenanceTask.Status newStatus = updateStatusBox.getValue();
            String notes = updateNotesField.getText().trim();

            // ── Priority check ──
            List<MaintenanceTask> allTasks = faultService.getAssignedTasks(
                    SessionManager.getCurrentUser().getUserId());

            MaintenanceTask selectedTask = allTasks.stream()
                    .filter(t -> t.getTaskId() == taskId)
                    .findFirst()
                    .orElse(null);

            if (selectedTask == null) {
                updateStatusLabel.setStyle("-fx-text-fill:#e74c3c;");
                updateStatusLabel.setText("✗ Task #" + taskId + " not found.");
                return;
            }

            boolean higherPriorityExists = allTasks.stream()
                    .filter(t -> t.getTaskId() != taskId)
                    .filter(t -> t.getStatus() != MaintenanceTask.Status.COMPLETED)
                    .anyMatch(t -> priorityRank(t.getPriority()) > priorityRank(selectedTask.getPriority()));

            if (higherPriorityExists) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Priority Warning");
                alert.setHeaderText("Higher priority task exists!");
                alert.setContentText(
                    "You are trying to update a " + selectedTask.getPriority().name() +
                    " priority task while a higher priority task is still pending.\n\n" +
                    "Are you sure you want to continue?"
                );
                alert.getDialogPane().setStyle("-fx-background-color: #1c3c52;");
                alert.getDialogPane().lookup(".content.label").setStyle("-fx-text-fill: #e8eaf0; -fx-font-size: 13px;");
                alert.getDialogPane().lookup(".header-panel").setStyle("-fx-background-color: #532626;");
                alert.getDialogPane().lookup(".header-panel .label").setStyle("-fx-text-fill: #e8eaf0; -fx-font-size: 15px; -fx-font-weight: bold;");

                Optional<ButtonType> result = alert.showAndWait();

                if (result.isEmpty() || result.get() != ButtonType.OK) {
                    updateStatusLabel.setStyle("-fx-text-fill:#f39c12;");
                    updateStatusLabel.setText("⚠ Update cancelled. Resolve higher priority tasks first.");
                    return;
                }
            }

            boolean ok = faultService.updateRepairStatus(
                    SessionManager.getCurrentUser().getUserId(), taskId, newStatus, notes);

            updateStatusLabel.setStyle(ok ? "-fx-text-fill:#2ecc71;" : "-fx-text-fill:#e74c3c;");
            updateStatusLabel.setText(ok ? "✓ Task #" + taskId + " updated to " + newStatus
                                        : "✗ Update failed.");
            loadMyTasks();

        } catch (Exception e) {
            updateStatusLabel.setStyle("-fx-text-fill:#e74c3c;");
            updateStatusLabel.setText("✗ " + e.getMessage());
        }
    }

    // ── UC03: Mark Fault Resolved ──
    @FXML
    private void handleResolve() {
        try {
            int taskId = Integer.parseInt(resolveTaskIdField.getText().trim());
            boolean ok = faultService.markFaultResolved(
                    SessionManager.getCurrentUser().getUserId(), taskId);

            resolveStatusLabel.setStyle(ok ? "-fx-text-fill:#2ecc71;" : "-fx-text-fill:#e74c3c;");
            resolveStatusLabel.setText(ok ? "✓ Fault resolved. Equipment restored to AVAILABLE."
                                         : "✗ Resolution failed.");
            loadMyTasks();
        } catch (Exception e) {
            resolveStatusLabel.setStyle("-fx-text-fill:#e74c3c;");
            resolveStatusLabel.setText("✗ " + e.getMessage());
        }
    }

    // ── UC04: View Fault Details ──
    @FXML
    private void handleLoadFaultDetail() {
        try {
            int faultId = Integer.parseInt(detailFaultIdField.getText().trim());
            FaultReport fr = faultService.getFaultDetails(faultId);

            if (fr == null) {
                detailEquipLabel.setText("—");
                detailDescLabel.setText("—");
                detailSevLabel.setText("—");
                detailStatusLabel.setText("—");
                detailReporterLabel.setText("—");
                detailDateLabel.setText("—");
                setStatus("✗ Fault #" + faultId + " does not exist.");
                return;
            }

            detailEquipLabel.setText(fr.getEquipmentName() != null
                    ? fr.getEquipmentName() : String.valueOf(fr.getEquipmentId()));
            detailDescLabel.setText(fr.getFaultDescription());
            detailSevLabel.setText(fr.getSeverity().name());
            detailStatusLabel.setText(fr.getStatus().name());
            detailReporterLabel.setText(fr.getReporterName() != null
                    ? fr.getReporterName() : String.valueOf(fr.getReportedBy()));
            detailDateLabel.setText(fr.getReportedDate() != null
                    ? fr.getReportedDate().toString() : "—");

        } catch (NumberFormatException e) {
            detailEquipLabel.setText("—");
            detailDescLabel.setText("—");
            detailSevLabel.setText("—");
            detailStatusLabel.setText("—");
            detailReporterLabel.setText("—");
            detailDateLabel.setText("—");
            setStatus("✗ Please enter a valid numeric Fault ID.");

        } catch (Exception e) {
            setStatus("✗ Error: " + e.getMessage());
        }
    }

    // ── UC05: Maintenance History ──
    @FXML
    private void handleLoadHistory() {
        try {
            int eqId = Integer.parseInt(historyEqIdField.getText().trim());
            List<MaintenanceTask> list = faultService.getMaintenanceHistory(eqId);
            historyTable.setItems(FXCollections.observableArrayList(list));
            setStatus("Showing " + list.size() + " completed task(s) for equipment #" + eqId);
        } catch (Exception e) { setStatus("Error: " + e.getMessage()); }
    }

    @FXML
    private void handleLogout() throws Exception {
        SessionManager.clearSession();
        MainApp.navigateTo("login");
    }

    // ── Priority rank helper (higher number = higher priority) ──
    private int priorityRank(MaintenanceTask.Priority p) {
        return switch (p) {
            case HIGH   -> 3;
            case MEDIUM -> 2;
            case LOW    -> 1;
        };
    }

    // ── Table setup ──
    private void setupTasksTable() {
        colTaskId.setCellValueFactory(new PropertyValueFactory<>("taskId"));
        colTaskEquip.setCellValueFactory(new PropertyValueFactory<>("equipmentName"));
        colTaskDesc.setCellValueFactory(new PropertyValueFactory<>("faultDescription"));
        colTaskPriority.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPriority().name()));
        colTaskStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
        colTaskAssigned.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getAssignedAt() != null ? d.getValue().getAssignedAt().toLocalDate().toString() : ""));
    }

    private void setupHistoryTable() {
        colHstTaskId.setCellValueFactory(new PropertyValueFactory<>("taskId"));
        colHstEquip.setCellValueFactory(new PropertyValueFactory<>("equipmentName"));
        colHstDesc.setCellValueFactory(new PropertyValueFactory<>("faultDescription"));
        colHstStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
        colHstCompleted.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCompletedAt() != null ? d.getValue().getCompletedAt().toLocalDate().toString() : "—"));
    }

    private void setStatus(String msg) { statusBar.setText(msg); }
}